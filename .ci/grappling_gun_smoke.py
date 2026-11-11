#!/usr/bin/env python3
"""Exercise Batman's grappling gun in a dedicated server with a real rendered client."""
import os
import re
import signal
import subprocess
import time
from pathlib import Path
from small_player_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciGrappleDiagnostics=true')
    server_log, client_log = Path('ci-grapple-server.log'), Path('ci-grapple-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-grapple-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-grapple-client',
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
                server.stdin.write(line + '\n'); server.stdin.flush(); time.sleep(1)
            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -4 179 -4 18 179 20 minecraft:stone')
            command('fill 12 180 -3 12 184 3 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:batman_dceu')
            command('item replace entity Dev weapon.mainhand with fiskheroes:grappling_gun')
            command('summon minecraft:iron_golem 0.5 180 8.5 {NoAI:1b,Silent:1b,Tags:["grapple_target"]}')
            xdo('click', '3')
            wait_for(server_log, r'Grapple entity attached: owner=.*target=', 30, 'first shot attaches to target', server)
            wait_for(client_log, r'Grapple 3D model rendered: pair=false, attached=true', 30, 'hook 3D model rendered', client)
            wait_for(client_log, r'Grapple rope rendered: pair=false, attached=true, endpoint=', 30, 'first hook cable rendered', client)
            subprocess.run(['scrot', 'ci-grapple-first.png'], check=True, timeout=30)
            command('tp Dev 0.5 180 0.5 -90 0')
            xdo('click', '3')
            wait_for(server_log, r'Grapple pair launched: owner=Dev, target=', 30, 'second shot launches paired projectile', server)
            wait_for(server_log, r'Grapple block attached: pair=true, target=IronGolem', 30, 'paired hook anchors to wall', server)
            wait_for(client_log, r'Grapple 3D model rendered: pair=true, attached=true', 30, 'paired hook 3D model rendered', client)
            wait_for(client_log, r'Grapple rope rendered: pair=true, attached=true, endpoint=', 30, 'paired cable anchored to target', client)
            subprocess.run(['scrot', 'ci-grapple-pair.png'], check=True, timeout=30)
            print('PASS: actual first and paired grapples rendered and hit their intended targets', flush=True)
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
