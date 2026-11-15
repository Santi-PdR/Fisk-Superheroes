#!/usr/bin/env python3
"""Exercise every custom weapon mesh with a real client and a dedicated server."""
import base64
import io
import os
import re
import signal
import subprocess
import time
from pathlib import Path
from PIL import Image, ImageChops


def wait_for(log, pattern, seconds, label, process):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if log.exists() and re.search(pattern, log.read_text(errors='replace')):
            print('PASS:', label, flush=True)
            return
        if process.poll() is not None: raise RuntimeError(f'{label}: process exited {process.returncode}')
        time.sleep(.3)
    raise RuntimeError(f'Timed out: {label}')


def xdo(*args):
    subprocess.run(['xdotool', *args], check=True, timeout=30)


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciItemVisualDiagnostics=true')
    server_log, client_log = Path('ci-item-models-server.log'), Path('ci-item-models-client.log')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    processes = []
    weapons = ['katana', 'chokuto', 'scimitar', 'bo_staff', 'ruptures_scythe', 'tactical_tonfa',
               'captain_americas_shield', 'grappling_gun', 'chronos_rifle', 'rip_hunters_gun',
               'cold_gun', 'heat_gun', 'desert_eagle', 'beretta_93r', 'flash_ring']
    enchant = '{Enchantments:[{id:"minecraft:unbreaking",lvl:1s}]}'
    arms = {'captain_americas_shield': 'captain america shield', 'grappling_gun': 'grappling gun',
            'chronos_rifle': 'chronos rifle', 'rip_hunters_gun': 'rip hunter gun'}
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-item-models-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-item-models-client',
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
                server.stdin.write(line + '\n'); server.stdin.flush(); time.sleep(.7)

            def equip(item, tag='', slot='weapon.mainhand'):
                command(f'item replace entity Dev {slot} with fiskheroes:{item}{tag}')

            def expect(item, context, foil, texture=None):
                pattern = (r'Item model base pass: item=fiskheroes:' + re.escape(item) + ', context=' + context
                           + ', foil=' + str(foil).lower() + ', texture=' + (re.escape(texture) if texture else r'[^\s]+'))
                try:
                    wait_for(client_log, pattern, 45, f'{item} {context} foil={foil}' + (f' livery={texture}' if texture else ''), client)
                except RuntimeError:
                    subprocess.run(['scrot', 'ci-item-models-failure.png'], check=True, timeout=30)
                    image = Image.open('ci-item-models-failure.png').convert('RGB')
                    image.thumbnail((640, 360)); out = io.BytesIO(); image.save(out, format='PNG')
                    print('DIAGNOSTIC_SCREENSHOT_BASE64=' + base64.b64encode(out.getvalue()).decode(), flush=True)
                    raise

            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('gamerule doDaylightCycle false')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 10 minecraft:stone')
            command('fill -5 180 8 5 185 8 minecraft:black_concrete')
            command('tp Dev 0.5 180 0.5 0 0')
            # Compare captures against the same stationary scene, so foil is verified on pixels,
            # not solely by a diagnostic log about the chosen render buffer.
            equip('katana'); expect('katana', 'FIRST_PERSON_RIGHT_HAND', False)
            time.sleep(3); subprocess.run(['scrot', 'ci-item-models-katana-plain.png'], check=True, timeout=30)
            equip('katana', enchant); expect('katana', 'FIRST_PERSON_RIGHT_HAND', True)
            time.sleep(3); subprocess.run(['scrot', 'ci-item-models-katana-foil.png'], check=True, timeout=30)
            plain = Image.open('ci-item-models-katana-plain.png').convert('RGB').crop((500, 140, 1020, 550))
            foil = Image.open('ci-item-models-katana-foil.png').convert('RGB').crop((500, 140, 1020, 550))
            delta = sum(1 for r, g, b in ImageChops.difference(plain, foil).get_flattened_data() if max(r, g, b) > 18)
            if delta < 80:
                out = io.BytesIO(); foil.thumbnail((520, 410)); foil.save(out, format='PNG')
                print('DIAGNOSTIC_SCREENSHOT_BASE64=' + base64.b64encode(out.getvalue()).decode(), flush=True)
                raise RuntimeError(f'Enchanted katana glint not visible: {delta} changed pixels')
            print(f'PASS: enchanted katana changes {delta} pixels in first person', flush=True)
            for item in weapons[1:]:
                equip(item); expect(item, 'FIRST_PERSON_RIGHT_HAND', False)
                if item in arms:
                    wait_for(client_log, r'First-person ' + re.escape(arms[item]) + r' arm rendered: RIGHT',
                             45, item + ' main hand has arm', client)
                equip(item, enchant); expect(item, 'FIRST_PERSON_RIGHT_HAND', True)
            print(f'PASS: {len(weapons)} different 3D items show both plain and foil base passes', flush=True)

            # GUI has a separate geometry path for each of the dual swords.
            for item in ('katana', 'chokuto', 'scimitar'):
                equip(item, '{Dual:1b,' + enchant[1:])
                xdo('key', 'e')
                expect(item, 'GUI', True)
                xdo('key', 'e'); time.sleep(.6)
            print('PASS: dual sword inventory meshes retain foil', flush=True)

            command('item replace entity Dev weapon.mainhand with minecraft:air')
            for item, label in arms.items():
                equip(item, enchant, 'weapon.offhand')
                expect(item, 'FIRST_PERSON_LEFT_HAND', True)
                wait_for(client_log, r'First-person ' + re.escape(label) + r' arm rendered: LEFT',
                         45, item + ' offhand has arm', client)
                subprocess.run(['scrot', f'ci-item-models-{item}-offhand.png'], check=True, timeout=30)
            command('item replace entity Dev weapon.offhand with minecraft:air')

            # Runner-only pack fixture adds liveries to the existing Deadpool model. No
            # diagnostic textures or pack modifications are included in the built JAR.
            command('execute as Dev run suit fiskheroes:deadpool_xmen')
            for item in ('tactical_tonfa', 'captain_americas_shield', 'grappling_gun', 'chronos_rifle', 'rip_hunters_gun'):
                equip(item, enchant)
                expect(item, 'FIRST_PERSON_RIGHT_HAND', True, 'fiskheroes:textures/heroes/deadpool_xmen_gun.png')
            print('PASS: newly supported suit liveries resolve on enchanted held items', flush=True)
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
