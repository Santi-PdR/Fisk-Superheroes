#!/usr/bin/env python3
"""Exercise Flash ring deployment with a real client and dedicated server."""
import os
import signal
import subprocess
import time
from pathlib import Path
from small_player_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log, client_log = Path('ci-ring-server.log'), Path('ci-ring-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-ring-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-ring-client',
                                       '-PciUsername=Dev', '-PquickPlayServer=127.0.0.1:25565', *common],
                                      stdout=cout, stderr=subprocess.STDOUT, env=env, start_new_session=True)
            processes.append(client)
            wait_for(server_log, r'Dev\[/.*logged in with entity id', 360, 'client joined', client)
            time.sleep(8)
            window = subprocess.check_output(['xdotool', 'search', '--onlyvisible', '--name', 'Minecraft'],
                                             text=True, timeout=30).strip().splitlines()[-1]
            xdo('windowfocus', '--sync', window)
            xdo('mousemove', '--window', window, '640', '360', 'click', '1')
            def command(line):
                if any(p.poll() is not None for p in processes): raise RuntimeError('Minecraft exited unexpectedly')
                server.stdin.write(line + '\n'); server.stdin.flush(); time.sleep(0.7)
            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('fill -4 179 -4 4 179 15 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('item replace entity Dev armor.head with minecraft:iron_helmet')
            command('item replace entity Dev armor.chest with minecraft:iron_chestplate')
            command('item replace entity Dev weapon.mainhand with fiskheroes:flash_ring{Items:[{Slot:0b,id:"minecraft:diamond_helmet",Count:1b}]}')
            time.sleep(1)
            subprocess.run(['scrot', 'ci-ring-held.png'], check=True, timeout=30)
            xdo('click', '3'); time.sleep(1)
            command('data get entity Dev Inventory')
            command('execute if entity Dev[nbt={Inventory:[{Slot:103b,id:"minecraft:diamond_helmet"},{Slot:102b,id:"minecraft:iron_chestplate"},{id:"minecraft:iron_helmet"}]}] run say RING_PARTIAL_PASS')
            wait_for(server_log, r'RING_PARTIAL_PASS', 20, 'partial ring preserves other armor and returns the old helmet', server)
            command('execute as Dev run suit fiskheroes:the_flash')
            command('item replace entity Dev weapon.mainhand with fiskheroes:flash_ring')
            time.sleep(1)
            xdo('click', '3'); time.sleep(1)
            command('data get entity Dev Inventory')
            command('execute unless data entity Dev Inventory[{Slot:103b}] if data entity Dev Inventory[{Slot:0b}].tag.Items run say RING_RETRACT_PASS')
            wait_for(server_log, r'RING_RETRACT_PASS', 20, 'empty ring retracts Flash suit', server)
            subprocess.run(['scrot', 'ci-ring-retracted.png'], check=True, timeout=30)
            xdo('click', '3'); time.sleep(1)
            command('data get entity Dev Inventory')
            command('execute if entity Dev[nbt={Inventory:[{Slot:103b,id:"fiskheroes:superhero_helmet",tag:{HeroType:"fiskheroes:the_flash"}}]}] unless data entity Dev Inventory[{Slot:0b}].tag.Items run say RING_DEPLOY_PASS')
            wait_for(server_log, r'RING_DEPLOY_PASS', 20, 'ring redeploys the Flash suit', server)
            print('PASS: Flash ring used in a real client against dedicated server', flush=True)
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    try: os.killpg(process.pid, signal.SIGTERM)
                    except ProcessLookupError: pass
            for process in processes:
                try: process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    try: os.killpg(process.pid, signal.SIGKILL)
                    except ProcessLookupError: pass


if __name__ == '__main__': main()
