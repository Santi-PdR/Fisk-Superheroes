#!/usr/bin/env python3
"""Exercise Beretta poses and firing using a runner-only magenta texture."""
import os
import shutil
import signal
import subprocess
import time
from pathlib import Path
from tonfa_visual_smoke import magenta, wait_for, xdo
from PIL import Image


def assert_geometry():
    stowed, third, first, occupied, offhand, dual_icon, single_icon = (
        Image.open('ci-beretta-' + name + '.png') for name in
        ('stowed-back', 'dual-front', 'dual-first', 'occupied', 'offhand', 'inventory-dual', 'inventory-single'))
    if any(im.size != (1280, 720) for im in (stowed, third, first, occupied, offhand, dual_icon, single_icon)):
        raise RuntimeError('Unexpected screenshot dimensions')
    if min(magenta(stowed, (590, 430, 614, 480)), magenta(stowed, (666, 430, 690, 480))) < 50:
        raise RuntimeError('Both stowed pistols must project outside the legs')
    if min(magenta(third, (588, 400, 618, 480)), magenta(third, (663, 400, 692, 480))) < 100:
        raise RuntimeError('Both third-person pistols must be visible by the hands')
    left, right = (465, 375, 540, 505), (740, 375, 815, 505)
    if min(magenta(first, left), magenta(first, right), magenta(offhand, left)) < 500:
        raise RuntimeError('A first-person pistol is missing')
    if magenta(occupied, left) > 50 or magenta(occupied, right) < 500:
        raise RuntimeError('Real offhand did not replace the visual Beretta')
    counts = []
    for image in (dual_icon, single_icon):
        slot = magenta(image, (461, 448, 496, 484))
        around = magenta(image, (450, 430, 505, 495))
        if slot < 40 or around - slot > 5:
            raise RuntimeError(f'Beretta inventory icon clips outside its slot: {slot=}, {around=}')
        counts.append(slot)
    if counts[0] < counts[1] * 1.25:
        raise RuntimeError('Dual icon does not show a second pistol')
    print('PASS: stowed, dual held, real offhand, and compact single/dual Beretta icons', flush=True)


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log, client_log = Path('ci-beretta-server.log'), Path('ci-beretta-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-beretta-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-beretta-client',
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
                if any(p.poll() is not None for p in processes): raise RuntimeError('Minecraft exited unexpectedly')
                server.stdin.write(line + '\n')
                server.stdin.flush()
                time.sleep(1.5)

            def screenshot(name):
                time.sleep(2)
                subprocess.run(['scrot', 'ci-beretta-' + name + '.png'], check=True, timeout=30)
                print('Captured:', name, flush=True)

            def capture_shots():
                reference = Image.open('ci-beretta-dual-first.png').convert('RGB')
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
                        Path('ci-beretta-sample.png').unlink(missing_ok=True)
                        subprocess.run(['scrot', 'ci-beretta-sample.png'], check=True, timeout=30)
                        frame = Image.open('ci-beretta-sample.png').convert('RGB')
                        score = warm(frame) - baseline
                        if score > best:
                            best = score
                            shutil.copyfile('ci-beretta-sample.png', 'ci-beretta-flash.png')
                finally:
                    xdo('mouseup', '1')
                    Path('ci-beretta-sample.png').unlink(missing_ok=True)
                # The flash lasts only a few ticks; a captured edge frame may contain
                # far fewer warm pixels than a peak frame under software rendering.
                if best < 30:
                    raise RuntimeError(f'Muzzle flash not visible in captured frames: warm-pixel increase {best}')
                print(f'PASS: visible muzzle flash, warm-pixel increase: {best}', flush=True)

            def capture_reload(label, regions):
                screenshot(label + '-before')
                base = Image.open('ci-beretta-' + label + '-before.png').convert('RGB')
                def mask(image, region):
                    return [r > 90 and b > 90 and g < 50 and abs(r - b) < 40
                            for r, g, b in image.crop(region).get_flattened_data()]
                reference = [mask(base, region) for region in regions]
                maxima = [0] * len(regions)
                best = -1
                xdo('key', 'c')
                for _ in range(14):
                    Path('ci-beretta-sample.png').unlink(missing_ok=True)
                    subprocess.run(['scrot', 'ci-beretta-sample.png'], check=True, timeout=30)
                    frame = Image.open('ci-beretta-sample.png').convert('RGB')
                    changes = [sum(a != b for a, b in zip(old, mask(frame, region)))
                               for old, region in zip(reference, regions)]
                    maxima = [max(a, b) for a, b in zip(maxima, changes)]
                    if sum(changes) > best:
                        best = sum(changes)
                        shutil.copyfile('ci-beretta-sample.png', 'ci-beretta-' + label + '-motion.png')
                    time.sleep(0.04)
                Path('ci-beretta-sample.png').unlink(missing_ok=True)
                if min(maxima) < 100:
                    raise RuntimeError(f'{label} reload did not visibly move each pistol: {maxima}')
                print(f'PASS: {label} reload moved pistol pixels: {maxima}', flush=True)
                screenshot(label + '-restored')
                restored = Image.open('ci-beretta-' + label + '-restored.png').convert('RGB')
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
                images = [Image.open('ci-beretta-' + label + '-' + suffix + '.png').convert('RGB')
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
            command('execute as Dev run suit fiskheroes:wild_dog')
            xdo('key', 'F5')
            wait_for(client_log, r'Equipped suit item rendered: hero=fiskheroes:wild_dog, item=fiskheroes:beretta_93r',
                     60, 'holstered Berettas rendered', client)
            screenshot('stowed-back')
            xdo('key', 'F5')
            screenshot('stowed-front')
            command('item replace entity Dev weapon.mainhand with fiskheroes:beretta_93r{Dual:1b}')
            wait_for(client_log, r'Held weapon livery: wearer=Dev, hero=fiskheroes:wild_dog, item=fiskheroes:beretta_93r, context=THIRD_PERSON_LEFT_HAND',
                     60, 'dual Berettas third-person offhand', client)
            screenshot('dual-front')
            xdo('key', 'F5')
            wait_for(client_log, r'Held weapon livery: wearer=Dev, hero=fiskheroes:wild_dog, item=fiskheroes:beretta_93r, context=FIRST_PERSON_LEFT_HAND',
                     60, 'dual Berettas first-person offhand', client)
            wait_for(client_log, r'First-person beretta arm rendered: RIGHT', 60, 'main-hand pistol arm', client)
            wait_for(client_log, r'First-person beretta arm rendered: LEFT', 60, 'offhand pistol arm', client)
            screenshot('dual-first')
            command('item replace entity Dev weapon.mainhand with fiskheroes:beretta_93r{Dual:1b,Ammo:0}')
            capture_aim('dual-aim', [('LEFT', (380, 340, 640, 545)), ('RIGHT', (640, 340, 900, 545))])
            wait_for(client_log, r'Beretta first-person aiming: arm=RIGHT', 60, 'Beretta main aim pose', client)
            wait_for(client_log, r'Beretta first-person aiming: arm=LEFT', 60, 'Beretta offhand aim pose', client)
            command('item replace entity Dev weapon.mainhand with fiskheroes:beretta_93r{Dual:1b,Ammo:30}')
            capture_shots()
            wait_for(client_log, r'Beretta shot effect rendered: wearer=Dev, hand=MAIN_HAND, context=FIRST_PERSON_RIGHT_HAND', 60, 'Beretta main-hand shot', client)
            wait_for(client_log, r'Beretta shot effect rendered: wearer=Dev, hand=OFF_HAND, context=FIRST_PERSON_LEFT_HAND', 60, 'Beretta offhand shot', client)
            time.sleep(1)
            capture_reload('dual-reload', [(430, 340, 570, 545), (710, 340, 850, 545)])
            wait_for(client_log, r'Beretta reload pose: wearer=Dev, hand=MAIN_HAND, context=FIRST_PERSON_RIGHT_HAND', 60, 'Beretta main reload', client)
            wait_for(client_log, r'Beretta reload pose: wearer=Dev, hand=OFF_HAND, context=FIRST_PERSON_LEFT_HAND', 60, 'Beretta offhand reload', client)
            command('execute if data entity Dev SelectedItem.tag{Ammo:30} run say BERETTA_DUAL_RELOAD_OK')
            wait_for(server_log, r'BERETTA_DUAL_RELOAD_OK', 30, 'dual reload restores both magazines', server)
            command('execute unless data entity Dev Inventory[{Slot:-106b}] run say BERETTA_OFFHAND_EMPTY_OK')
            wait_for(server_log, r'BERETTA_OFFHAND_EMPTY_OK', 30, 'dual offhand is visual only', server)
            xdo('key', 'e')
            screenshot('inventory-dual')
            xdo('key', 'e')
            command('item replace entity Dev weapon.offhand with minecraft:torch')
            wait_for(client_log, r'Dual visual offhand: player=Dev, item=fiskheroes:beretta_93r, mode=occupied',
                     60, 'real offhand takes precedence', client)
            screenshot('occupied')
            command('item replace entity Dev weapon.offhand with minecraft:air')
            command('item replace entity Dev weapon.mainhand with fiskheroes:beretta_93r{Ammo:12}')
            screenshot('single-first')
            xdo('click', '1')
            time.sleep(1)
            command('data get entity Dev SelectedItem.tag')
            command('execute if data entity Dev SelectedItem.tag{Ammo:9} run say BERETTA_BURST_OK')
            wait_for(server_log, r'BERETTA_BURST_OK', 30, 'burst consumes three rounds', server)
            xdo('key', 'c')
            time.sleep(2)
            command('execute if data entity Dev SelectedItem.tag{Ammo:15} run say BERETTA_RELOAD_OK')
            wait_for(server_log, r'BERETTA_RELOAD_OK', 30, 'reload restores magazine', server)
            command('item replace entity Dev weapon.mainhand with fiskheroes:beretta_93r{Ammo:12,BurstMode:0b}')
            xdo('click', '1')
            command('execute if data entity Dev SelectedItem.tag{Ammo:11} run say BERETTA_SINGLE_OK')
            wait_for(server_log, r'BERETTA_SINGLE_OK', 30, 'single mode consumes one round', server)
            xdo('keydown', 'Shift_L')
            try:
                time.sleep(0.6)  # Allow the client and server to observe crouching before use.
                xdo('click', '3')
                time.sleep(0.3)
            finally:
                xdo('keyup', 'Shift_L')
            time.sleep(1)
            command('execute if data entity Dev SelectedItem.tag{BurstMode:1b} run say BERETTA_MODE_OK')
            wait_for(server_log, r'BERETTA_MODE_OK', 30, 'sneak-use switches firing mode', server)
            xdo('key', 'e')
            screenshot('inventory-single')
            xdo('key', 'e')
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('item replace entity Dev weapon.offhand with fiskheroes:beretta_93r')
            screenshot('offhand')
            assert_geometry()
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    try: os.killpg(process.pid, signal.SIGTERM)
                    except ProcessLookupError: pass
            for process in reversed(processes):
                try: process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()


if __name__ == '__main__': main()
