#!/usr/bin/env python3
"""Inspect Heatwave's weapon and exercise its server-authoritative heat effects."""
import os
import signal
import subprocess
import time
from pathlib import Path
from tonfa_visual_smoke import magenta, wait_for, xdo
from PIL import Image


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    real = os.environ.get('HEAT_GUN_REAL_TEXTURE') == '1'
    alt_aim = os.environ.get('HEAT_GUN_ALT_AIM') == '1'
    suffix = '-alt' if alt_aim else '-real' if real else ''
    prefix = 'ci-heat-gun' + suffix
    run_prefix = 'run-heat-gun' + suffix
    server_log, client_log = Path(prefix + '-server.log'), Path(prefix + '-client.log')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    processes = []
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', f'-PciRunDir={run_prefix}-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', f'-PciRunDir={run_prefix}-client',
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
                subprocess.run(['scrot', prefix + '-' + name + '.png'], check=True, timeout=30)
                print('Captured:', name, flush=True)

            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 5 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:heatwave')
            command('item replace entity Dev weapon.mainhand with fiskheroes:heat_gun')
            wait_for(client_log, r'First-person heat gun arm rendered: RIGHT', 60, 'first-person heat gun arm', client)
            if alt_aim:
                # The CI-only hero script binds AIM to C. Right-click alone must not fire;
                # C + held item use must continue past the one-second server timeout.
                command('setblock 0 181 4 minecraft:ice')
                xdo('mousedown', '3')
                time.sleep(2)
                xdo('mouseup', '3')
                command('execute if block 0 181 4 minecraft:ice run say HEAT_ALT_NO_AIM_OK')
                wait_for(server_log, r'HEAT_ALT_NO_AIM_OK', 30, 'item use alone cannot fire', server)
                xdo('keydown', 'c')
                try:
                    time.sleep(1)
                    xdo('mousedown', '3')
                    try:
                        time.sleep(2)
                        command('setblock 0 181 4 minecraft:ice')
                        command('execute if block 0 181 4 minecraft:water run say HEAT_ALT_HELD_OK')
                        wait_for(server_log, r'HEAT_ALT_HELD_OK', 30, 'alternate AIM and held use sustain beam', server)
                        xdo('keyup', 'c')
                        command('setblock 0 181 4 minecraft:ice')
                        command('execute if block 0 181 4 minecraft:ice run say HEAT_ALT_RELEASE_OK')
                        wait_for(server_log, r'HEAT_ALT_RELEASE_OK', 30, 'releasing AIM stops held-use beam', server)
                    finally:
                        xdo('mouseup', '3')
                finally:
                    xdo('keyup', 'c')
                print('PASS: alternate AIM binding and held item use', flush=True)
                return
            screenshot('first')
            command('time set midnight')
            screenshot('night')
            command('time set day')
            xdo('key', 'F5')
            xdo('key', 'F5')
            screenshot('third-front')
            xdo('key', 'F5')
            xdo('key', 'e')
            screenshot('inventory')
            xdo('key', 'e')
            # Capture a clear beam before melting ice and flowing water hide it.
            xdo('mousedown', '1')
            try:
                wait_for(client_log, r'Heat gun first-person aiming: arm=RIGHT', 60, 'heat gun aims in first person', client)
                wait_for(client_log, r'Heat gun flame emitted: wearer=Dev, samples=', 60, 'server-accepted heat gun creates local flame stream', client)
                screenshot('open-flame')
            finally:
                xdo('mouseup', '1')
            if real:
                print('PASS: captured original Heat Gun texture in daylight, at night and firing', flush=True)
                return
            # Once released, a fresh target must remain untouched; losing the suit must also
            # prevent a held input from melting it on a real dedicated server.
            command('setblock 0 181 4 minecraft:ice')
            command('execute if block 0 181 4 minecraft:ice run say HEAT_RELEASE_OK')
            wait_for(server_log, r'HEAT_RELEASE_OK', 30, 'beam stops on release', server)
            command('execute as Dev run hero null')
            xdo('mousedown', '1')
            time.sleep(2)
            xdo('mouseup', '1')
            command('execute if block 0 181 4 minecraft:ice run say HEAT_PERMISSION_OK')
            wait_for(server_log, r'HEAT_PERMISSION_OK', 30, 'unsuited player cannot melt ice', server)
            command('execute as Dev run suit fiskheroes:heatwave')
            command('setblock 0 181 2 minecraft:stone')
            xdo('mousedown', '1')
            time.sleep(2)
            xdo('mouseup', '1')
            command('execute if block 0 181 4 minecraft:ice run say HEAT_OCCLUSION_OK')
            wait_for(server_log, r'HEAT_OCCLUSION_OK', 30, 'wall blocks the beam', server)
            command('setblock 0 181 2 minecraft:air')
            xdo('mousedown', '1')
            try:
                wait_for(client_log, r'Heat gun first-person aiming: arm=RIGHT', 60, 'heat gun aims in first person', client)
                wait_for(client_log, r'Heat gun flame emitted: wearer=Dev, samples=', 60, 'server-accepted heat gun creates local flame stream', client)
                screenshot('aimed')
            finally:
                xdo('mouseup', '1')
            screenshot('restored')
            command('execute if block 0 181 4 minecraft:water run say HEAT_ICE_OK')
            wait_for(server_log, r'HEAT_ICE_OK', 30, 'heat gun melts ice', server)
            # A real ticking entity 30 blocks away checks the extended original range, not just
            # block clipping or a mock entity outside the GameTest world's active chunks.
            command('fill 0 180 1 0 182 34 minecraft:air')
            command('scoreboard objectives add heatHealth dummy')
            command('execute as Dev at @s anchored eyes positioned ^ ^ ^30 positioned ~ ~-1.2 ~ run summon minecraft:cow ~ ~ ~ {NoAI:1b,NoGravity:1b,Attributes:[{Name:"minecraft:generic.max_health",Base:100.0d}],Tags:["heat_range"]}')
            command('data merge entity @e[type=minecraft:cow,tag=heat_range,limit=1] {Health:100.0f}')
            command('execute as @e[type=minecraft:cow,tag=heat_range,limit=1] store result score @s heatHealth run data get entity @s Health 1')
            command('execute if entity @e[type=minecraft:cow,tag=heat_range,scores={heatHealth=100..}] run say HEAT_RANGE_READY')
            wait_for(server_log, r'HEAT_RANGE_READY', 30, 'long-range target starts at full health', server)
            xdo('mousedown', '1')
            time.sleep(2)
            xdo('mouseup', '1')
            command('execute as @e[type=minecraft:cow,tag=heat_range,limit=1] store result score @s heatHealth run data get entity @s Health 1')
            command('execute if entity @e[type=minecraft:cow,tag=heat_range,scores={heatHealth=..95}] run say HEAT_RANGE_OK')
            wait_for(server_log, r'HEAT_RANGE_OK', 30, 'beam damages a living target 30 blocks away', server)
            command('kill @e[type=minecraft:cow,tag=heat_range]')
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('item replace entity Dev weapon.offhand with fiskheroes:heat_gun')
            screenshot('offhand')
            first, third, icon, aimed, restored, offhand = (Image.open(f'ci-heat-gun-{name}.png') for name in
                                                   ('first', 'third-front', 'inventory', 'aimed', 'restored', 'offhand'))
            if any(im.size != (1280, 720) for im in (first, third, icon, aimed, restored, offhand)):
                raise RuntimeError('Unexpected screenshot dimensions')
            # The diagnostic base texture is magenta. An opaque/incorrectly
            # blended white emissive pass would hide it and fail these checks.
            if magenta(first, (700, 350, 900, 550)) < 500 or magenta(third, (580, 380, 620, 450)) < 100:
                raise RuntimeError('Heat gun base is missing/washed out in first or third person')
            slot = magenta(icon, (461, 448, 496, 484))
            around = magenta(icon, (450, 430, 505, 495))
            if slot < 50 or around - slot > 5:
                raise RuntimeError(f'Heat gun icon clips outside inventory slot: {slot=}, {around=}')
            if magenta(offhand, (350, 350, 580, 550)) < 500:
                raise RuntimeError('Real offhand heat gun is missing/washed out')
            def center(image):
                pts = [(x, y) for y in range(350, 555) for x in range(600, 950)
                       if (lambda c: c[0] > 90 and c[2] > 90 and c[1] < 50 and abs(c[0] - c[2]) < 40)(image.getpixel((x, y))[:3])]
                if len(pts) < 500: raise RuntimeError('Heat gun is missing from aim frame')
                return sum(x for x, _ in pts) / len(pts), sum(y for _, y in pts) / len(pts)
            before, after, released = center(first), center(aimed), center(restored)
            if abs(before[0] - released[0]) > 5 or abs(before[1] - released[1]) > 5:
                raise RuntimeError(f'Heat gun did not return to rest on release: {before}, {released}')
            if before[0] - after[0] < 15: raise RuntimeError(f'Heat gun did not move toward center when aiming: {before}, {after}')
            if abs(before[1] - after[1]) > 60:
                raise RuntimeError(f'Heat gun sank off-screen while firing: {before}, {after}')
            def flame_pixels(image):
                return sum(1 for r, g, b in image.convert('RGB').crop((580, 300, 740, 430)).get_flattened_data()
                           if r > 200 and 30 < g < 200 and b < 30 and r > g * 1.4)
            flame_gain = flame_pixels(aimed) - flame_pixels(first)
            if flame_gain < 15:
                raise RuntimeError(f'Heat gun flame stream is not visible while firing: {flame_gain} pixels')
            print(f'PASS: visible flame stream adds {flame_gain} orange pixels', flush=True)
            night = Image.open('ci-heat-gun-night.png')
            warm = sum(1 for r, g, b in night.convert('RGB').crop((700, 375, 850, 540)).get_flattened_data()
                       if r > 140 and 5 < g < 200 and b < 70 and r > g * 1.2)
            if warm < 10: raise RuntimeError(f'Emissive heat-gun lights are missing: {warm} pixels')
            print(f'PASS: poses, unwashed base, emissive lights ({warm} pixels), contained icon and stable aim/release: {before} -> {after} -> {released}', flush=True)
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
