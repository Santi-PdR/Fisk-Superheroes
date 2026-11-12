#!/usr/bin/env python3
"""Exercise cactus aiming and radial spike abilities with a real client and dedicated server."""
import os
import signal
import subprocess
import time
from pathlib import Path
from small_player_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciCactusDiagnostics=true')
    server_log, client_log = Path('ci-cactus-server.log'), Path('ci-cactus-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-cactus-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-cactus-client',
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
            command('setblock 0 179 0 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:senor_cactus')
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            time.sleep(2)
            command('scoreboard objectives add cactus_count dummy')
            command('kill @e[type=fiskheroes:cactus_spike]')
            xdo('keydown', 'c'); time.sleep(1)
            xdo('click', '3'); time.sleep(0.15)
            xdo('keyup', 'c')
            wait_for(server_log, r'Aimed suit shot: player=Dev, modifier=fiskheroes:spike_burst', 20,
                     'aimed cactus shot received by server', server)
            server.stdin.write('execute store result score spikes cactus_count run execute if entity @e[type=fiskheroes:cactus_spike]\nexecute if score spikes cactus_count matches 11 run say CACTUS_AIMED_ELEVEN_PASS\n'); server.stdin.flush()
            wait_for(server_log, r'CACTUS_AIMED_ELEVEN_PASS', 15, 'aimed burst has eleven projectiles', server)
            wait_for(client_log, r'Cactus spike 3D model rendered', 20, 'client renders the 3D cactus spike model', client)
            command('kill @e[type=fiskheroes:cactus_spike]')
            command('summon minecraft:iron_golem 0.5 180 3.5 {NoAI:1b,NoGravity:1b,Silent:1b,Tags:["cactus_close_target"]}')
            xdo('keydown', 'c'); time.sleep(1); xdo('click', '3'); time.sleep(.3); xdo('keyup', 'c')
            command('execute if entity @e[tag=cactus_close_target] unless entity @e[tag=cactus_close_target,nbt={Health:100.0f}] run say CACTUS_CLOSE_TARGET_PASS')
            wait_for(server_log, r'CACTUS_CLOSE_TARGET_PASS', 15, 'aimed burst hits a nearby target', server)
            command('kill @e[tag=cactus_close_target]')
            command('kill @e[type=fiskheroes:cactus_spike]')
            xdo('key', 'v'); time.sleep(0.15)
            server.stdin.write('execute store result score spikes cactus_count run execute if entity @e[type=fiskheroes:cactus_spike]\nexecute if score spikes cactus_count matches 72 run say CACTUS_RING_SEVENTY_TWO_PASS\n'); server.stdin.flush()
            wait_for(server_log, r'CACTUS_RING_SEVENTY_TWO_PASS', 15, 'ring has seventy-two projectiles', server)
            subprocess.run(['scrot', 'ci-cactus-ring.png'], check=True, timeout=30)
            print('PASS: real client triggered aimed and radial cactus attacks on dedicated server', flush=True)
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
