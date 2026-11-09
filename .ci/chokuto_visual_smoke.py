#!/usr/bin/env python3
"""Capture a chokuto with a runner-only high-contrast diagnostic texture."""
import os
import shutil
import signal
import subprocess
import time
from pathlib import Path
from tonfa_visual_smoke import magenta, wait_for, xdo
from PIL import Image


def assert_geometry():
    stowed = Image.open('ci-chokuto-stowed-back.png')
    third = Image.open('ci-chokuto-dual-front.png')
    first = Image.open('ci-chokuto-dual-first.png')
    offhand = Image.open('ci-chokuto-offhand.png')
    dual_icon = Image.open('ci-chokuto-inventory-dual.png')
    single_icon = Image.open('ci-chokuto-inventory-single.png')
    if any(im.size != (1280, 720) for im in (stowed, third, first, offhand, dual_icon, single_icon)):
        raise RuntimeError('Unexpected screenshot dimensions')
    if magenta(stowed, (665, 330, 700, 380)) < 60:
        raise RuntimeError('Stowed chokuto grip is missing above the shoulder')
    if min(magenta(third, (598, 300, 618, 335)), magenta(third, (664, 300, 682, 335))) < 20:
        raise RuntimeError('Both third-person blades should extend above the hands')
    if min(magenta(first, (480, 180, 625, 395)), magenta(first, (655, 180, 800, 395))) < 1000:
        raise RuntimeError('Both first-person blades should be visible')
    if magenta(offhand, (480, 180, 625, 395)) < 1000:
        raise RuntimeError('Real offhand blade is not visible')
    for image in (dual_icon, single_icon):
        slot = magenta(image, (461, 448, 496, 484))
        around = magenta(image, (450, 430, 505, 495))
        if slot < 15 or around - slot > 5:
            raise RuntimeError(f'Chokuto inventory icon clips outside its slot: {slot=}, {around=}')
    if magenta(dual_icon, (480, 448, 496, 460)) < 8 or magenta(single_icon, (480, 448, 496, 460)) > 3:
        raise RuntimeError('Dual icon must include a second crossed sword')
    print('PASS: stowed grip, dual held blades, real offhand, and single/crossed icon bounds', flush=True)


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log, client_log = Path('ci-chokuto-server.log'), Path('ci-chokuto-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-chokuto-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-chokuto-client',
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
                subprocess.run(['scrot', 'ci-chokuto-' + name + '.png'], check=True, timeout=30)
                print('Captured:', name, flush=True)

            def capture_swing():
                reference = Image.open('ci-chokuto-single-first.png').crop((550, 140, 950, 540)).convert('RGB')
                mask = lambda image: [(r > 90 and b > 90 and g < 50) for r, g, b in image.get_flattened_data()]
                reference_mask = mask(reference)
                best = 0
                xdo('mousedown', '1')
                try:
                    for _ in range(12):
                        time.sleep(0.06)
                        Path('ci-chokuto-sample.png').unlink(missing_ok=True)
                        subprocess.run(['scrot', 'ci-chokuto-sample.png'], check=True, timeout=30)
                        frame = Image.open('ci-chokuto-sample.png').crop((550, 140, 950, 540)).convert('RGB')
                        current = mask(frame)
                        changed = sum(a != b for a, b in zip(reference_mask, current))
                        if sum(current) > 200 and changed > best:
                            best = changed
                            shutil.copyfile('ci-chokuto-sample.png', 'ci-chokuto-swing.png')
                finally:
                    xdo('mouseup', '1')
                    Path('ci-chokuto-sample.png').unlink(missing_ok=True)
                if best < 500:
                    raise RuntimeError(f'Chokuto did not visibly swing: {best} changed pixels')
                print(f'PASS: chokuto swing changes {best} diagnostic pixels', flush=True)

            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 5 minecraft:stone')
            command('tp Dev 0 180 0 0 0')
            command('execute as Dev run suit fiskheroes:prometheus')
            xdo('key', 'F5')
            wait_for(client_log, r'Equipped suit item rendered: hero=fiskheroes:prometheus, item=fiskheroes:chokuto',
                     60, 'stowed chokutos rendered', client)
            screenshot('stowed-back')
            xdo('key', 'F5')
            screenshot('stowed-front')
            command('item replace entity Dev weapon.mainhand with fiskheroes:chokuto{Dual:1b}')
            screenshot('dual-front')
            xdo('key', 'F5')
            wait_for(client_log, r'First-person chokuto arm rendered: RIGHT', 60, 'main-hand chokuto arm', client)
            wait_for(client_log, r'First-person chokuto arm rendered: LEFT', 60, 'synthetic offhand chokuto arm', client)
            screenshot('dual-first')
            xdo('key', 'e')
            screenshot('inventory-dual')
            xdo('key', 'e')
            command('item replace entity Dev weapon.mainhand with fiskheroes:chokuto')
            screenshot('single-first')
            capture_swing()
            xdo('key', 'e')
            screenshot('inventory-single')
            xdo('key', 'e')
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('item replace entity Dev weapon.offhand with fiskheroes:chokuto')
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
