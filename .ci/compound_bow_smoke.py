#!/usr/bin/env python3
"""Exercise compound bow rendering and horizontal aiming with a real client and dedicated server."""
import os
import signal
import subprocess
import time
from pathlib import Path
from small_player_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciBowDiagnostics=true')
    server_log, client_log = Path('ci-bow-server.log'), Path('ci-bow-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-bow-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-bow-client',
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
            command('fill -5 179 -5 5 179 15 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:green_arrow')
            command('item replace entity Dev weapon.mainhand with fiskheroes:compound_bow')
            command('item replace entity Dev weapon.offhand with fiskheroes:trick_arrow')
            wait_for(client_log, r'Compound bow 3D model rendered: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND, drawing=false', 30,
                     'first-person compound bow model', client)
            xdo('keydown', 'v'); time.sleep(1); xdo('mousedown', '3'); time.sleep(1)
            wait_for(client_log, r'Compound bow 3D model rendered: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND, drawing=true, horizontal=true', 30,
                     'horizontal drawn bow rendered in first person', client)
            xdo('keyup', 'v'); time.sleep(.5)
            subprocess.run(['scrot', 'ci-bow-draw-first.png'], check=True, timeout=30)
            xdo('key', 'F5'); time.sleep(1)
            wait_for(client_log, r'Compound bow 3D model rendered: wearer=Dev, context=THIRD_PERSON_RIGHT_HAND, drawing=true, horizontal=true', 30,
                     'horizontal drawn bow rendered in third person', client)
            subprocess.run(['scrot', 'ci-bow-draw-third.png'], check=True, timeout=30)
            xdo('mouseup', '3'); time.sleep(.5)
            command('execute if entity @e[type=fiskheroes:trick_arrow] run say BOW_SHOT_PASS')
            wait_for(server_log, r'BOW_SHOT_PASS', 15, 'bow fires on dedicated server', server)
            xdo('key', 'F5'); xdo('key', 'F5'); time.sleep(1)
            command('item replace entity Dev weapon.mainhand with fiskheroes:compound_bow{Broken:1b}')
            wait_for(client_log, r'Compound bow 3D model rendered: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND, drawing=false, horizontal=false, broken=true', 30,
                     'broken bow renders without strings', client)
            subprocess.run(['scrot', 'ci-bow-broken.png'], check=True, timeout=30)
            print('PASS: real client renders and fires the 3D compound bow in both views', flush=True)
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
