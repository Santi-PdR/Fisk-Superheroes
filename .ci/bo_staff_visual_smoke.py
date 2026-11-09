#!/usr/bin/env python3
"""Capture a bo staff with a runner-only high-contrast diagnostic texture."""
import os
import signal
import subprocess
import time
from pathlib import Path
from tonfa_visual_smoke import magenta, wait_for, xdo
from PIL import Image


def assert_geometry():
    first = Image.open('ci-staff-first.png')
    third = Image.open('ci-staff-front.png')
    inventory = Image.open('ci-staff-inventory.png')
    offhand = Image.open('ci-staff-offhand.png')
    if any(image.size != (1280, 720) for image in (first, third, inventory, offhand)):
        raise RuntimeError('Unexpected screenshot dimensions')
    if magenta(first, (600, 170, 800, 450)) < 500:
        raise RuntimeError('The first-person staff is not visible above the hand')
    if magenta(offhand, (480, 170, 680, 450)) < 500:
        raise RuntimeError('The first-person offhand staff is not visible')
    if magenta(third, (550, 300, 625, 400)) < 50 or magenta(third, (550, 440, 625, 490)) < 30:
        raise RuntimeError('Staff must extend both above and below the hand')
    slot = magenta(inventory, (461, 448, 496, 484))
    around = magenta(inventory, (450, 430, 505, 495))
    if slot < 10 or around - slot > 5:
        raise RuntimeError(f'Collapsed inventory staff clips outside its slot: {slot=}, {around=}')
    print('PASS: symmetric staff, first-person hand, offhand and compact inventory icon', flush=True)


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log, client_log = Path('ci-staff-server.log'), Path('ci-staff-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-staff-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-staff-client',
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
                subprocess.run(['scrot', 'ci-staff-' + name + '.png'], check=True, timeout=30)
                print('Captured:', name, flush=True)

            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 5 minecraft:stone')
            command('tp Dev 0 180 0 0 0')
            command('execute as Dev run suit fiskheroes:black_canary_dinah')
            command('item replace entity Dev weapon.mainhand with fiskheroes:bo_staff')
            wait_for(server_log, r'Replaced a slot on Dev with .*Bo.Staff', 30, 'staff equipped', server)
            wait_for(client_log, r'First-person bo staff arm rendered: RIGHT', 60, 'first-person staff arm rendered', client)
            screenshot('first')
            xdo('click', '1')
            wait_for(client_log, r'Bo staff swing pose: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND',
                     60, 'first-person staff swing animation', client)
            xdo('key', 'F5')
            screenshot('back')
            xdo('key', 'F5')
            screenshot('front')
            xdo('click', '1')
            wait_for(client_log, r'Bo staff swing pose: wearer=Dev, context=THIRD_PERSON_RIGHT_HAND',
                     60, 'third-person staff swing animation', client)
            xdo('key', 'F5')
            xdo('key', 'e')
            screenshot('inventory')
            xdo('key', 'e')
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('item replace entity Dev weapon.offhand with fiskheroes:bo_staff')
            wait_for(client_log, r'First-person bo staff arm rendered: LEFT', 60, 'first-person offhand staff arm rendered', client)
            screenshot('offhand')
            assert_geometry()
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
