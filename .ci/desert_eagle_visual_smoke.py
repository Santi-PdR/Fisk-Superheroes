#!/usr/bin/env python3
"""Capture a desert_eagle with a runner-only high-contrast diagnostic texture."""
import os
import signal
import subprocess
import time
from pathlib import Path
from tonfa_visual_smoke import magenta, wait_for, xdo
from PIL import Image


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log, client_log = Path('ci-desert_eagle-server.log'), Path('ci-desert_eagle-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-desert_eagle-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-desert_eagle-client',
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
                if any(p.poll() is not None for p in processes):
                    raise RuntimeError('Minecraft exited unexpectedly')
                server.stdin.write(line + '\n')
                server.stdin.flush()
                time.sleep(1.5)

            def screenshot(name):
                time.sleep(2)
                subprocess.run(['scrot', 'ci-desert_eagle-' + name + '.png'], check=True, timeout=30)
                print('Captured:', name, flush=True)

            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 5 minecraft:stone')
            command('tp Dev 0 180 0 0 0')
            command('execute as Dev run suit fiskheroes:deadpool_xmen')
            xdo('key', 'F5')
            wait_for(client_log, r'Equipped suit item rendered: hero=fiskheroes:deadpool_xmen, item=fiskheroes:desert_eagle',
                     60, 'stowed desert_eagles rendered', client)
            screenshot('stowed-back')
            xdo('key', 'F5')
            screenshot('stowed-front')
            command('item replace entity Dev weapon.mainhand with fiskheroes:desert_eagle{Dual:1b}')
            wait_for(client_log, r'Held weapon livery: wearer=Dev, hero=fiskheroes:deadpool_xmen, item=fiskheroes:desert_eagle, context=THIRD_PERSON_LEFT_HAND',
                     60, 'dual desert_eagle third-person offhand', client)
            screenshot('dual-front')
            xdo('key', 'F5')
            wait_for(client_log, r'Held weapon livery: wearer=Dev, hero=fiskheroes:deadpool_xmen, item=fiskheroes:desert_eagle, context=FIRST_PERSON_LEFT_HAND',
                     60, 'dual desert_eagle first-person offhand', client)
            wait_for(client_log, r'First-person desert eagle arm rendered: RIGHT', 60, 'main-hand gun arm', client)
            wait_for(client_log, r'First-person desert eagle arm rendered: LEFT', 60, 'synthetic offhand gun arm', client)
            screenshot('dual-first')
            xdo('key', 'e')
            screenshot('inventory-dual')
            xdo('key', 'e')
            command('item replace entity Dev weapon.mainhand with fiskheroes:desert_eagle')
            screenshot('single-first')
            xdo('key', 'e')
            screenshot('inventory-single')
            xdo('key', 'e')
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('item replace entity Dev weapon.offhand with fiskheroes:desert_eagle')
            screenshot('offhand')
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    try:
                        os.killpg(process.pid, signal.SIGTERM)
                    except ProcessLookupError:
                        pass
            for process in reversed(processes):
                try:
                    process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()


if __name__ == '__main__':
    main()
