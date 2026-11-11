#!/usr/bin/env python3
"""Exercise potion and firework arrow impacts with a real client and dedicated server."""
import os
import signal
import subprocess
import time
from pathlib import Path
from small_player_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log, client_log = Path('ci-arrow-server.log'), Path('ci-arrow-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-arrow-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-arrow-client',
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
            def shoot(capture):
                xdo('mousedown', '3'); time.sleep(1.7)
                subprocess.run(['scrot', capture], check=True, timeout=30)
                xdo('mouseup', '3'); time.sleep(2)
            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('fill -4 179 -4 4 179 18 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('item replace entity Dev weapon.mainhand with minecraft:bow')
            command('item replace entity Dev weapon.offhand with fiskheroes:trick_arrow{ArrowType:"vial",AttachedPotion:{id:"minecraft:splash_potion",Count:1b,tag:{Potion:"minecraft:swiftness"}}}')
            command('summon minecraft:iron_golem 0.5 180 8.5 {NoAI:1b,Silent:1b,Tags:["vial_target"]}')
            shoot('ci-arrow-vial.png')
            command('execute if entity @e[tag=vial_target,nbt={ActiveEffects:[{Id:1b}]}] run say ARROW_VIAL_SWIFTNESS_PASS')
            wait_for(server_log, r'ARROW_VIAL_SWIFTNESS_PASS', 15, 'vial arrow applies crafted effect', server)
            command('kill @e[tag=vial_target]')
            command('tp Dev 0.5 180 0.5 0 0')
            command('item replace entity Dev weapon.offhand with fiskheroes:trick_arrow{ArrowType:"firework",AttachedItem:{id:"minecraft:firework_rocket",Count:1b,tag:{Fireworks:{Flight:3b,Explosions:[{Type:0b,Colors:[I;15790320]}]}}}}')
            command('summon minecraft:iron_golem 0.5 180 8.5 {NoAI:1b,Silent:1b,Tags:["firework_target"]}')
            shoot('ci-arrow-firework.png')
            command('execute if entity @e[tag=firework_target,nbt={Health:100.0f,ActiveEffects:[{Id:15b}]}] run say ARROW_FIREWORK_BLIND_PASS')
            wait_for(server_log, r'ARROW_FIREWORK_BLIND_PASS', 15, 'firework flashes target without arrow damage', server)
            print('PASS: real client fired both payloads against dedicated-server targets', flush=True)
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
