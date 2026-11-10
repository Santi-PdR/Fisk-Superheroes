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
    server_log, client_log = Path('ci-heat-gun-server.log'), Path('ci-heat-gun-client.log')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    processes = []
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-heat-gun-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-heat-gun-client',
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
                subprocess.run(['scrot', 'ci-heat-gun-' + name + '.png'], check=True, timeout=30)
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
            screenshot('first')
            xdo('key', 'F5')
            xdo('key', 'F5')
            screenshot('third-front')
            xdo('key', 'F5')
            xdo('key', 'e')
            screenshot('inventory')
            xdo('key', 'e')
            command('setblock 0 181 4 minecraft:ice')
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
            warm = sum(1 for r, g, b in first.convert('RGB').crop((700, 375, 850, 540)).get_flattened_data()
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
