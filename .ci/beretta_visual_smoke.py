#!/usr/bin/env python3
"""Exercise Beretta poses and firing using a runner-only magenta texture."""
import os
import signal
import subprocess
import time
from pathlib import Path
from tonfa_visual_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log, client_log = Path('ci-beretta-server.log'), Path('ci-beretta-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-beretta-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-beretta-client',
                                       '-PciUsername=Dev', '-PquickPlayServer=127.0.0.1:25565', *common],
                                      stdout=cout, stderr=subprocess.STDOUT, env=env, start_new_session=True)
            processes.append(client)
            wait_for(server_log, r'Dev\[/.*logged in with entity id', 360, 'client joined', client)
            time.sleep(8)
            window = subprocess.check_output(['xdotool', 'search', '--onlyvisible', '--name', 'Minecraft'],
                                             text=True, timeout=30).strip().splitlines()[-1]
            xdo('windowfocus', '--sync', window)
            xdo('mousemove', '--window', window, '640', '360', 'click', '1')
            time.sleep(2)

            def command(line):
                if any(p.poll() is not None for p in processes): raise RuntimeError('Minecraft exited unexpectedly')
                server.stdin.write(line + '\n')
                server.stdin.flush()
                time.sleep(1.5)

            def screenshot(name):
                time.sleep(2)
                subprocess.run(['scrot', 'ci-beretta-' + name + '.png'], check=True, timeout=30)
                print('Captured:', name, flush=True)

            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 5 minecraft:stone')
            command('tp Dev 0 180 0 0 0')
            command('execute as Dev run suit fiskheroes:wild_dog')
            xdo('key', 'F5')
            wait_for(client_log, r'Equipped suit item rendered: hero=fiskheroes:wild_dog, item=fiskheroes:beretta_93r',
                     60, 'holstered Berettas rendered', client)
            screenshot('stowed-back')
            xdo('key', 'F5')
            screenshot('stowed-front')
            command('item replace entity Dev weapon.mainhand with fiskheroes:beretta_93r{Dual:1b}')
            wait_for(client_log, r'Held weapon livery: wearer=Dev, hero=fiskheroes:wild_dog, item=fiskheroes:beretta_93r, context=THIRD_PERSON_LEFT_HAND',
                     60, 'dual Berettas third-person offhand', client)
            screenshot('dual-front')
            xdo('key', 'F5')
            wait_for(client_log, r'Held weapon livery: wearer=Dev, hero=fiskheroes:wild_dog, item=fiskheroes:beretta_93r, context=FIRST_PERSON_LEFT_HAND',
                     60, 'dual Berettas first-person offhand', client)
            wait_for(client_log, r'First-person beretta arm rendered: RIGHT', 60, 'main-hand pistol arm', client)
            wait_for(client_log, r'First-person beretta arm rendered: LEFT', 60, 'offhand pistol arm', client)
            screenshot('dual-first')
            command('execute unless data entity Dev Inventory[{Slot:-106b}] run say BERETTA_OFFHAND_EMPTY_OK')
            wait_for(server_log, r'BERETTA_OFFHAND_EMPTY_OK', 30, 'dual offhand is visual only', server)
            xdo('key', 'e')
            screenshot('inventory-dual')
            xdo('key', 'e')
            command('item replace entity Dev weapon.offhand with minecraft:torch')
            wait_for(client_log, r'Dual visual offhand: player=Dev, item=fiskheroes:beretta_93r, mode=occupied',
                     60, 'real offhand takes precedence', client)
            screenshot('occupied')
            command('item replace entity Dev weapon.offhand with minecraft:air')
            command('item replace entity Dev weapon.mainhand with fiskheroes:beretta_93r{Ammo:12}')
            screenshot('single-first')
            xdo('click', '1')
            time.sleep(1)
            command('data get entity Dev SelectedItem.tag')
            command('execute if data entity Dev SelectedItem.tag{Ammo:9} run say BERETTA_BURST_OK')
            wait_for(server_log, r'BERETTA_BURST_OK', 30, 'burst consumes three rounds', server)
            xdo('key', 'c')
            time.sleep(2)
            command('execute if data entity Dev SelectedItem.tag{Ammo:15} run say BERETTA_RELOAD_OK')
            wait_for(server_log, r'BERETTA_RELOAD_OK', 30, 'reload restores magazine', server)
            command('item replace entity Dev weapon.mainhand with fiskheroes:beretta_93r{Ammo:12,BurstMode:0b}')
            xdo('click', '1')
            command('execute if data entity Dev SelectedItem.tag{Ammo:11} run say BERETTA_SINGLE_OK')
            wait_for(server_log, r'BERETTA_SINGLE_OK', 30, 'single mode consumes one round', server)
            xdo('keydown', 'Shift_L')
            try:
                xdo('click', '3')
            finally:
                xdo('keyup', 'Shift_L')
            time.sleep(1)
            command('execute if data entity Dev SelectedItem.tag{BurstMode:1b} run say BERETTA_MODE_OK')
            wait_for(server_log, r'BERETTA_MODE_OK', 30, 'sneak-use switches firing mode', server)
            xdo('key', 'e')
            screenshot('inventory-single')
            xdo('key', 'e')
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('item replace entity Dev weapon.offhand with fiskheroes:beretta_93r')
            screenshot('offhand')
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    try: os.killpg(process.pid, signal.SIGTERM)
                    except ProcessLookupError: pass
            for process in reversed(processes):
                try: process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()


if __name__ == '__main__': main()
