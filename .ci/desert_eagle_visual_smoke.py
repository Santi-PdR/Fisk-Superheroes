#!/usr/bin/env python3
"""Capture a desert_eagle with a runner-only high-contrast diagnostic texture."""
import os
import shutil
import signal
import subprocess
import time
from pathlib import Path
from tonfa_visual_smoke import magenta, wait_for, xdo
from PIL import Image


def assert_geometry():
    stowed = Image.open('ci-desert_eagle-stowed-back.png')
    third = Image.open('ci-desert_eagle-dual-front.png')
    first = Image.open('ci-desert_eagle-dual-first.png')
    offhand = Image.open('ci-desert_eagle-offhand.png')
    dual_icon = Image.open('ci-desert_eagle-inventory-dual.png')
    single_icon = Image.open('ci-desert_eagle-inventory-single.png')
    if any(im.size != (1280, 720) for im in (stowed, third, first, offhand, dual_icon, single_icon)):
        raise RuntimeError('Unexpected screenshot dimensions')
    if min(magenta(stowed, (600, 430, 626, 480)), magenta(stowed, (654, 430, 680, 480))) < 30:
        raise RuntimeError('Both holstered pistols must be visible at the legs')
    if min(magenta(third, (588, 400, 618, 460)), magenta(third, (663, 410, 692, 475))) < 100:
        raise RuntimeError('Both third-person pistols should be visible by the hands')
    if min(magenta(first, (465, 375, 540, 500)), magenta(first, (740, 375, 815, 500))) < 500:
        raise RuntimeError('Both first-person pistols should be visible')
    if magenta(offhand, (465, 375, 540, 500)) < 500:
        raise RuntimeError('Real offhand pistol is not visible')
    counts = []
    for image in (dual_icon, single_icon):
        slot = magenta(image, (461, 448, 496, 484))
        around = magenta(image, (450, 430, 505, 495))
        if slot < 40 or around - slot > 5:
            raise RuntimeError(f'Desert Eagle inventory icon clips outside its slot: {slot=}, {around=}')
        counts.append(slot)
    if counts[0] < counts[1] * 1.25:
        raise RuntimeError('Dual pistol icon does not show a second pistol')
    print('PASS: holstered, dual held, real offhand, and compact single/dual icons', flush=True)


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

            def capture_shots():
                reference = Image.open('ci-desert_eagle-dual-first.png').convert('RGB')
                region = (445, 330, 835, 515)
                def warm(image):
                    return sum(1 for r, g, b in image.crop(region).get_flattened_data()
                               if r > 180 and g > 80 and b < 140 and r > b * 1.5)
                baseline = warm(reference)
                best = -1
                xdo('mousedown', '1')
                try:
                    for _ in range(28):
                        time.sleep(0.06)
                        Path('ci-desert_eagle-sample.png').unlink(missing_ok=True)
                        subprocess.run(['scrot', 'ci-desert_eagle-sample.png'], check=True, timeout=30)
                        frame = Image.open('ci-desert_eagle-sample.png').convert('RGB')
                        score = warm(frame) - baseline
                        if score > best:
                            best = score
                            shutil.copyfile('ci-desert_eagle-sample.png', 'ci-desert_eagle-flash.png')
                finally:
                    xdo('mouseup', '1')
                    Path('ci-desert_eagle-sample.png').unlink(missing_ok=True)
                # The flash lasts only a few ticks; a captured edge frame may contain
                # far fewer warm pixels than a peak frame under software rendering.
                if best < 30:
                    raise RuntimeError(f'Muzzle flash not visible in captured frames: warm-pixel increase {best}')
                print(f'PASS: visible muzzle flash, warm-pixel increase: {best}', flush=True)

            def capture_reload(label, regions):
                screenshot(label + '-before')
                base = Image.open('ci-desert_eagle-' + label + '-before.png').convert('RGB')
                def mask(image, region):
                    return [r > 90 and b > 90 and g < 50 and abs(r - b) < 40
                            for r, g, b in image.crop(region).get_flattened_data()]
                reference = [mask(base, region) for region in regions]
                maxima = [0] * len(regions)
                best = -1
                xdo('key', 'c')
                for _ in range(14):
                    Path('ci-desert_eagle-sample.png').unlink(missing_ok=True)
                    subprocess.run(['scrot', 'ci-desert_eagle-sample.png'], check=True, timeout=30)
                    frame = Image.open('ci-desert_eagle-sample.png').convert('RGB')
                    changes = [sum(a != b for a, b in zip(old, mask(frame, region)))
                               for old, region in zip(reference, regions)]
                    maxima = [max(a, b) for a, b in zip(maxima, changes)]
                    if sum(changes) > best:
                        best = sum(changes)
                        shutil.copyfile('ci-desert_eagle-sample.png', 'ci-desert_eagle-' + label + '-motion.png')
                    time.sleep(0.04)
                Path('ci-desert_eagle-sample.png').unlink(missing_ok=True)
                if min(maxima) < 100:
                    raise RuntimeError(f'{label} reload did not visibly move each pistol: {maxima}')
                print(f'PASS: {label} reload moved pistol pixels: {maxima}', flush=True)
                screenshot(label + '-restored')
                restored = Image.open('ci-desert_eagle-' + label + '-restored.png').convert('RGB')
                residual = [sum(a != b for a, b in zip(old, mask(restored, region)))
                            for old, region in zip(reference, regions)]
                if max(residual) > 200:
                    raise RuntimeError(f'{label} reload did not return to rest: {residual}')
                print(f'PASS: {label} reload returned to rest: {residual}', flush=True)

            def capture_aim(label, sides, fixed_region=None):
                screenshot(label + '-before')
                xdo('mousedown', '1')
                try:
                    screenshot(label + '-aimed')
                finally:
                    xdo('mouseup', '1')
                screenshot(label + '-restored')
                images = [Image.open('ci-desert_eagle-' + label + '-' + suffix + '.png').convert('RGB')
                          for suffix in ('before', 'aimed', 'restored')]
                def center(image, region):
                    x1, y1, x2, y2 = region
                    points = [(x + x1, y + y1) for y in range(y2 - y1) for x in range(x2 - x1)
                              if (lambda c: c[0] > 90 and c[2] > 90 and c[1] < 50 and abs(c[0] - c[2]) < 40)
                              (image.getpixel((x + x1, y + y1)))]
                    if len(points) < 100:
                        raise RuntimeError(f'{label} pistol not visible in {region}: {len(points)} pixels')
                    return sum(p[0] for p in points) / len(points)
                for side, region in sides:
                    before, aimed, restored = (center(image, region) for image in images)
                    direction = 1 if side == 'LEFT' else -1
                    if (aimed - before) * direction < 8:
                        raise RuntimeError(f'{label} {side} pistol did not move toward center: {before}, {aimed}')
                    if abs(restored - before) > 4:
                        raise RuntimeError(f'{label} {side} pistol did not return to rest: {before}, {restored}')
                    print(f'PASS: {label} {side} aims and returns: {before:.1f} -> {aimed:.1f} -> {restored:.1f}', flush=True)
                if fixed_region is not None:
                    before = images[0].crop(fixed_region)
                    aimed = images[1].crop(fixed_region)
                    changed = sum(a != b for a, b in zip(before.get_flattened_data(), aimed.get_flattened_data()))
                    if changed > 200:
                        raise RuntimeError(f'{label} real offhand moved during aim: {changed} pixels')
                    print(f'PASS: {label} real offhand remains stationary: {changed} pixels changed', flush=True)

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
            command('item replace entity Dev weapon.mainhand with fiskheroes:desert_eagle{Dual:1b,Ammo:0}')
            capture_aim('dual-aim', [('LEFT', (380, 340, 640, 545)), ('RIGHT', (640, 340, 900, 545))])
            xdo('keydown', 'Shift_L')
            xdo('mousedown', '1')
            try:
                time.sleep(3)
                if 'Desert eagle scope zoom active' in client_log.read_text(errors='replace'):
                    raise RuntimeError('Dual pistols incorrectly activated scope zoom')
            finally:
                xdo('mouseup', '1')
                xdo('keyup', 'Shift_L')
            print('PASS: dual pistols do not scope while crouched and aiming', flush=True)
            wait_for(client_log, r'Desert eagle first-person aiming: arm=RIGHT', 60, 'main arm aims', client)
            wait_for(client_log, r'Desert eagle first-person aiming: arm=LEFT', 60, 'offhand arm aims', client)
            command('item replace entity Dev weapon.mainhand with fiskheroes:desert_eagle{Dual:1b}')
            capture_shots()
            wait_for(client_log, r'Gun shot effect rendered: wearer=Dev, hand=MAIN_HAND, context=FIRST_PERSON_RIGHT_HAND',
                     60, 'main-hand shot effect rendered', client)
            wait_for(client_log, r'Gun shot effect rendered: wearer=Dev, hand=OFF_HAND, context=FIRST_PERSON_LEFT_HAND',
                     60, 'dual offhand shot effect rendered', client)
            capture_reload('dual-reload', [(430, 340, 570, 545), (710, 340, 850, 545)])
            wait_for(client_log, r'Desert eagle reload pose: wearer=Dev, hand=MAIN_HAND, context=FIRST_PERSON_RIGHT_HAND',
                     60, 'main-hand reload pose rendered', client)
            wait_for(client_log, r'Desert eagle reload pose: wearer=Dev, hand=OFF_HAND, context=FIRST_PERSON_LEFT_HAND',
                     60, 'dual offhand reload pose rendered', client)
            command('execute unless data entity Dev Inventory[{Slot:-106b}] run say EAGLE_OFFHAND_EMPTY_OK')
            wait_for(server_log, r'EAGLE_OFFHAND_EMPTY_OK', 30, 'dual offhand is visual only', server)
            command('item replace entity Dev weapon.offhand with minecraft:torch')
            wait_for(client_log, r'Dual visual offhand: player=Dev, item=fiskheroes:desert_eagle, mode=occupied',
                     60, 'real offhand takes precedence', client)
            command('item replace entity Dev weapon.mainhand with fiskheroes:desert_eagle{Dual:1b,Ammo:0}')
            capture_aim('occupied-aim', [('RIGHT', (600, 340, 900, 450))], (300, 340, 600, 450))
            command('item replace entity Dev weapon.mainhand with fiskheroes:desert_eagle{Dual:1b,Ammo:15}')
            before = len(client_log.read_text(errors='replace'))
            xdo('click', '1')
            command('execute if data entity Dev SelectedItem.tag{Ammo:14} run say EAGLE_OCCUPIED_SHOT_OK')
            wait_for(server_log, r'EAGLE_OCCUPIED_SHOT_OK', 30, 'occupied-offhand gun fired', server)
            time.sleep(1)
            received = client_log.read_text(errors='replace')[before:]
            if 'Gun shot effect received: wearer=Dev, hand=MAIN_HAND' not in received or 'Gun shot effect received: wearer=Dev, hand=OFF_HAND' in received:
                raise RuntimeError('Occupied offhand did not force the shot effect to the main hand')
            print('PASS: occupied offhand forces main-hand shot effect', flush=True)
            command('item replace entity Dev weapon.offhand with minecraft:air')
            xdo('key', 'e')
            screenshot('inventory-dual')
            xdo('key', 'e')
            command('item replace entity Dev weapon.mainhand with fiskheroes:desert_eagle{Ammo:5}')
            screenshot('single-first')
            xdo('click', '1')
            command('execute if data entity Dev SelectedItem.tag{Ammo:4} run say EAGLE_SHOT_OK')
            wait_for(server_log, r'EAGLE_SHOT_OK', 30, 'gun fires and consumes exactly one round', server)
            capture_reload('single-reload', [(710, 340, 850, 545)])
            command('execute if data entity Dev SelectedItem.tag{Ammo:8} run say EAGLE_RELOAD_OK')
            wait_for(server_log, r'EAGLE_RELOAD_OK', 30, 'gun reload restores magazine', server)
            command('item replace entity Dev weapon.mainhand with fiskheroes:desert_eagle{Ammo:0}')
            before = client_log.read_text(errors='replace').count('Gun shot effect received: wearer=Dev')
            if before < 1:
                raise RuntimeError('No accepted gun shot effect packets received')
            xdo('click', '1')
            time.sleep(2)
            after = client_log.read_text(errors='replace').count('Gun shot effect received: wearer=Dev')
            if after != before:
                raise RuntimeError('Empty gun incorrectly emitted a shot effect')
            print('PASS: empty gun emits no shot effect', flush=True)
            capture_aim('single-aim', [('RIGHT', (600, 340, 900, 545))])
            xdo('mousedown', '1')
            xdo('keydown', 'Shift_L')
            try:
                wait_for(client_log, r'Desert eagle scope zoom active', 60, 'single pistol scope zoom activates', client)
                screenshot('single-scoped')
                xdo('keyup', 'Shift_L')
                wait_for(client_log, r'Desert eagle scope zoom released', 60, 'scope zoom releases when standing', client)
                screenshot('single-unscoped')
            finally:
                xdo('keyup', 'Shift_L')
                xdo('mouseup', '1')
            scoped = Image.open('ci-desert_eagle-single-scoped.png').convert('RGB').crop((250, 190, 1000, 440))
            unscoped = Image.open('ci-desert_eagle-single-unscoped.png').convert('RGB').crop((250, 190, 1000, 440))
            changed = sum(a != b for a, b in zip(scoped.get_flattened_data(), unscoped.get_flattened_data()))
            if changed < 1000:
                raise RuntimeError(f'Scope zoom did not visibly change the view: {changed} pixels')
            print(f'PASS: scope zoom visibly changes the view: {changed} pixels', flush=True)
            xdo('key', 'e')
            screenshot('inventory-single')
            xdo('key', 'e')
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('item replace entity Dev weapon.offhand with fiskheroes:desert_eagle')
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
