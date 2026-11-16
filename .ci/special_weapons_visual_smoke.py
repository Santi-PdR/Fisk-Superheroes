#!/usr/bin/env python3
"""Check the 3D special weapons in both hands on a real client."""
import base64
import io
import os
import signal
import subprocess
import time
from pathlib import Path
from PIL import Image
from tonfa_visual_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log, client_log = Path('ci-special-weapons-server.log'), Path('ci-special-weapons-client.log')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    processes = []
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-special-weapons-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-special-weapons-client',
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
                server.stdin.write(line + '\n'); server.stdin.flush(); time.sleep(1.5)

            def capture(item, context):
                time.sleep(2)
                path = f'ci-special-weapons-{item}-{context}.png'
                subprocess.run(['scrot', path], check=True, timeout=30)
                image = Image.open(path).convert('RGB')
                if image.size != (1280, 720): raise RuntimeError('Unexpected screenshot dimensions')
                # Mask excludes hotbar, sky and chat and reports the geometry bounds.
                pixels = [(x, y) for y in range(100, 600) for x in range(210, 1070)
                          if (lambda p: p[0] > 90 and p[2] > 90 and p[1] < 50 and abs(p[0] - p[2]) < 50)(image.getpixel((x, y)))]
                if len(pixels) < 100: raise RuntimeError(f'{item} {context}: model missing ({len(pixels)} pixels)')
                bounds = (min(x for x, y in pixels), min(y for x, y in pixels),
                          max(x for x, y in pixels), max(y for x, y in pixels))
                print(f'PASS: {item} {context}: {len(pixels)} model pixels, bounds={bounds}', flush=True)
                if os.environ.get('CI_ITEM_ARM_CAPTURES') == '1':
                    image.thumbnail((640, 360)); output = io.BytesIO(); image.save(output, format='PNG')
                    print('CAPTURE_' + item + '_' + context + '=' + base64.b64encode(output.getvalue()).decode(), flush=True)

            command('difficulty peaceful'); command('gamemode creative Dev'); command('gamerule doDaylightCycle false')
            command('time set day'); command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 8 minecraft:stone')
            command('fill -5 180 6 5 186 6 minecraft:black_concrete')
            command('tp Dev 0.5 180 0.5 0 0')
            for item, label in [('grappling_gun', 'grappling gun'), ('chronos_rifle', 'chronos rifle'), ('rip_hunters_gun', 'rip hunter gun')]:
                command(f'item replace entity Dev weapon.mainhand with fiskheroes:{item}')
                wait_for(client_log, rf'First-person {label} arm rendered: RIGHT', 60, item + ' right arm rendered', client)
                capture(item, 'main')
                command('item replace entity Dev weapon.mainhand with minecraft:air')
                command(f'item replace entity Dev weapon.offhand with fiskheroes:{item}')
                wait_for(client_log, rf'First-person {label} arm rendered: LEFT', 60, item + ' left arm rendered', client)
                capture(item, 'off')
                command('item replace entity Dev weapon.offhand with minecraft:air')
        finally:
            for p in reversed(processes):
                if p.poll() is None:
                    try: os.killpg(p.pid, signal.SIGTERM)
                    except ProcessLookupError: pass
            for p in reversed(processes):
                try: p.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    os.killpg(p.pid, signal.SIGKILL); p.wait()


if __name__ == '__main__': main()
