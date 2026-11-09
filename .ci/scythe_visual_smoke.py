#!/usr/bin/env python3
"""Capture a scythe with a runner-only high-contrast diagnostic texture."""
import os
import signal
import subprocess
import time
from pathlib import Path
from tonfa_visual_smoke import magenta, wait_for, xdo
from PIL import Image


def assert_geometry():
    stowed = Image.open('ci-scythe-stowed-back.png')
    third = Image.open('ci-scythe-held-front.png')
    first = Image.open('ci-scythe-held-first.png')
    offhand = Image.open('ci-scythe-offhand.png')
    inventory = Image.open('ci-scythe-inventory.png')
    if any(image.size != (1280, 720) for image in (stowed, third, first, offhand, inventory)):
        raise RuntimeError('Unexpected screenshot dimensions')
    if magenta(stowed, (540, 310, 625, 390)) < 500:
        raise RuntimeError('Stowed scythe blade is missing from the shoulder')
    if magenta(third, (440, 320, 585, 390)) < 1000:
        raise RuntimeError('Third-person blade is hidden edge-on')
    if magenta(first, (625, 140, 920, 300)) < 2000 or magenta(offhand, (360, 140, 655, 300)) < 2000:
        raise RuntimeError('Scythe blade missing in a first-person hand')
    slot = magenta(inventory, (461, 448, 496, 484))
    around = magenta(inventory, (450, 430, 505, 495))
    if slot < 10 or around - slot > 5:
        raise RuntimeError(f'Inventory scythe clips outside its slot: {slot=}, {around=}')
    print('PASS: scythe silhouette, stowed anchor, both hands and inventory bounds', flush=True)


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log, client_log = Path('ci-scythe-server.log'), Path('ci-scythe-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-scythe-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-scythe-client',
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
                subprocess.run(['scrot', 'ci-scythe-' + name + '.png'], check=True, timeout=30)
                print('Captured:', name, flush=True)

            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 5 minecraft:stone')
            command('tp Dev 0 180 0 0 0')
            command('execute as Dev run suit fiskheroes:rupture')
            xdo('key', 'F5')
            wait_for(client_log, r'Equipped suit item rendered: hero=fiskheroes:rupture, item=fiskheroes:ruptures_scythe',
                     60, 'stowed scythe rendered', client)
            screenshot('stowed-back')
            xdo('key', 'F5')
            screenshot('stowed-front')
            command('item replace entity Dev weapon.mainhand with fiskheroes:ruptures_scythe')
            wait_for(server_log, r'Replaced a slot on Dev with .*Scythe', 30, 'scythe equipped', server)
            screenshot('held-front')
            xdo('key', 'F5')
            wait_for(client_log, r'First-person scythe arm rendered: RIGHT', 60, 'first-person scythe arm rendered', client)
            screenshot('held-first')
            if 'Scythe charge arcs:' in client_log.read_text(errors='replace'):
                raise RuntimeError('Uncharged scythe emitted energy arcs')
            xdo('keydown', 'c')
            wait_for(server_log, r'Ability modifier executed: hero=fiskheroes:rupture, key=CHARGE_ENERGY',
                     60, 'server activates energy charge', server)
            wait_for(client_log, r'Scythe charge arcs: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND',
                     60, 'charged scythe arcs render in first person', client)
            screenshot('charged-first')
            xdo('key', 'F5')
            xdo('key', 'F5')
            wait_for(client_log, r'Scythe charge arcs: wearer=Dev, context=THIRD_PERSON_RIGHT_HAND',
                     60, 'charged scythe arcs render in third person', client)
            screenshot('charged-front')
            xdo('keyup', 'c')
            xdo('key', 'F5')
            xdo('key', 'e')
            screenshot('inventory')
            xdo('key', 'e')
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('item replace entity Dev weapon.offhand with fiskheroes:ruptures_scythe')
            wait_for(client_log, r'First-person scythe arm rendered: LEFT', 60, 'first-person offhand scythe arm rendered', client)
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
