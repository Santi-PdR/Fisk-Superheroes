#!/usr/bin/env python3
"""Verify miniature suit previews and deployment with a real client and server."""
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
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciMiniSuitDiagnostics=true -Dfiskheroes.ciMaskDiagnostics=true')
    server_log, client_log = Path('ci-mini-suit-server.log'), Path('ci-mini-suit-client.log')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    processes = []
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-mini-suit-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-mini-suit-client',
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
                server.stdin.write(line + '\n'); server.stdin.flush(); time.sleep(1.2)

            def capture(label):
                time.sleep(2)
                path = f'ci-mini-suit-{label}.png'
                subprocess.run(['scrot', path], check=True, timeout=30)
                original = Image.open(path).convert('RGB')
                if os.environ.get('CI_MINI_SUIT_CAPTURES') == '1':
                    original.thumbnail((640, 360)); out = io.BytesIO(); original.save(out, format='PNG')
                    print('CAPTURE_' + label + '=' + base64.b64encode(out.getvalue()).decode(), flush=True)
                    original = Image.open(path).convert('RGB')
                image = original.crop((600, 200, 1067, 550))
                return sum(1 for r, g, b in image.get_flattened_data()
                           if r > 90 and b > 90 and g < 50 and abs(r - b) < 50)

            command('difficulty peaceful'); command('gamemode creative Dev'); command('time set day')
            command('tp Dev 0 185 0 0 0'); command('fill -5 179 -5 5 179 10 minecraft:stone')
            command('fill -5 180 8 5 185 8 minecraft:black_concrete'); command('tp Dev 0.5 180 0.5 0 0')
            def piece(slot, name):
                return '{Slot:' + str(slot) + 'b,id:"fiskheroes:superhero_' + name + '",Count:1b,tag:{HeroType:"fiskheroes:atom",Marker:"mini-test"}}'
            head = piece(0, 'helmet')
            full = ','.join(piece(i, name) for i, name in enumerate(('helmet', 'chestplate', 'leggings', 'boots')))
            command('item replace entity Dev weapon.mainhand with fiskheroes:mini_suit')
            wait_for(client_log, r'First-person miniature suit arm rendered: RIGHT', 60, 'miniature renders right hand', client)
            empty_pixels = capture('empty')
            command('item replace entity Dev weapon.mainhand with fiskheroes:mini_suit{Items:[' + head + ']}')
            wait_for(client_log, r'Mini suit preview: context=FIRST_PERSON_RIGHT_HAND, pieces=0=fiskheroes:atom;',
                     60, 'partial miniature renders stored helmet', client)
            partial_pixels = capture('head')
            command('item replace entity Dev weapon.mainhand with fiskheroes:mini_suit{Items:[' + full + ']}')
            wait_for(client_log, r'Mini suit preview: context=FIRST_PERSON_RIGHT_HAND, pieces=0=fiskheroes:atom;1=fiskheroes:atom;2=fiskheroes:atom;3=fiskheroes:atom;',
                     60, 'complete miniature renders all four stored pieces', client)
            full_pixels = capture('full')
            # The hand preview must fit inside the game viewport rather than
            # showing a cropped torso with the legs below the screen. Ignore
            # the central hotbar by checking only the right-hand region.
            full_frame = Image.open('ci-mini-suit-full.png').convert('RGB')
            edge = full_frame.crop((840, 580, 1067, 600))
            clipped = sum(1 for r, g, b in edge.get_flattened_data()
                          if r > 90 and b > 90 and g < 50 and abs(r - b) < 50)
            if clipped > 10: raise RuntimeError(f'First-person miniature is clipped at the bottom: {clipped} diagnostic pixels')
            print('PASS: complete first-person miniature fits above bottom edge', flush=True)
            # The fixture maps Flash's layer1 to a getter-driven texture. Set the
            # holder to a visibly different mask state before resolving its preview.
            command('execute as Dev run suit fiskheroes:iron_man')
            xdo('keydown', 'x')
            try:
                wait_for(server_log, r'Mask toggled: player=Dev, open=true', 45,
                         'holder mask opened on the server', server)
            finally: xdo('keyup', 'x')
            time.sleep(2)
            flash_chest = '{Slot:1b,id:"fiskheroes:superhero_chestplate",Count:1b,tag:{HeroType:"fiskheroes:the_flash"}}'
            command('item replace entity Dev weapon.mainhand with fiskheroes:mini_suit{Items:[' + head + ',' + flash_chest + ']}')
            wait_for(client_log, r'Mini suit preview: context=FIRST_PERSON_RIGHT_HAND, pieces=0=fiskheroes:atom;1=fiskheroes:the_flash;',
                     60, 'mixed miniature resolves each actual hero piece', client)
            wait_for(client_log, r'Mini suit texture: context=FIRST_PERSON_RIGHT_HAND, hero=fiskheroes:the_flash, slot=1, texture=fiskheroes:textures/heroes/ci_mini_display_true_false_false\.png, holderMaskOpen=true, holderMaskTimer=(?:1(?:\.0+)?|0\.\d*[1-9]\d*)',
                     60, 'texture getters use display data while the holder mask is open', client)
            capture('mixed')
            mixed = Image.open('ci-mini-suit-mixed.png').convert('RGB').crop((600, 200, 1067, 550))
            cyan = sum(1 for r, g, b in mixed.get_flattened_data() if g > 90 and b > 90 and r < 50)
            if cyan < 25: raise RuntimeError(f'Mixed miniature is missing the Flash chest texture: {cyan} cyan pixels')
            print(f'PASS: mixed miniature displays the second hero texture: {cyan} cyan pixels', flush=True)
            iron_chest = '{Slot:1b,id:"fiskheroes:superhero_chestplate",Count:1b,tag:{HeroType:"fiskheroes:iron_man_mk50"}}'
            command('item replace entity Dev weapon.mainhand with fiskheroes:mini_suit{Items:[' + iron_chest + ']}')
            wait_for(client_log, r'Mini suit preview: context=FIRST_PERSON_RIGHT_HAND, pieces=1=fiskheroes:iron_man_mk50;',
                     60, 'single-piece Iron Man miniature is resolved', client)
            wait_for(client_log, r'Mini suit mask: context=FIRST_PERSON_RIGHT_HAND, hero=fiskheroes:iron_man_mk50, slot=1, head=true, body=true, rightArm=true, leftArm=true, rightLeg=true, leftLeg=true',
                     60, 'Iron Man chest piece renders the full body mask', client)
            wait_for(client_log, r'Mini suit texture: context=FIRST_PERSON_RIGHT_HAND, hero=fiskheroes:iron_man_mk50, slot=1, texture=fiskheroes:textures/heroes/iron_man_mk50\.png',
                     60, 'Iron Man display resolves the intact suit texture rather than the holder state', client)
            capture('iron-man')
            iron = Image.open('ci-mini-suit-iron-man.png').convert('RGB').crop((600, 200, 1067, 550))
            cyan_iron = sum(1 for r, g, b in iron.get_flattened_data() if g > 90 and b > 90 and r < 50)
            if cyan_iron < 2000: raise RuntimeError(f'Iron Man miniature lacks visible body: {cyan_iron} cyan pixels')
            print(f'PASS: single-piece Iron Man suit visibly renders: {cyan_iron} cyan pixels', flush=True)
            if partial_pixels < 25 or full_pixels < partial_pixels * 1.4 or empty_pixels > partial_pixels * .25:
                raise RuntimeError(f'Preview pixels not proportional to stored armor: empty={empty_pixels}, head={partial_pixels}, full={full_pixels}')
            print(f'PASS: only stored pieces are visible: empty={empty_pixels}, head={partial_pixels}, full={full_pixels} diagnostic pixels', flush=True)
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('item replace entity Dev weapon.offhand with fiskheroes:mini_suit{Items:[' + full + ']}')
            wait_for(client_log, r'First-person miniature suit arm rendered: LEFT', 60, 'miniature renders offhand', client)
            capture('offhand')
            command('item replace entity Dev weapon.offhand with minecraft:air')
            command('item replace entity Dev armor.head with minecraft:iron_helmet{Marker:"old-helmet"}')
            command('item replace entity Dev armor.chest with minecraft:iron_chestplate{Marker:"keep-chest"}')
            command('item replace entity Dev weapon.mainhand with fiskheroes:mini_suit{Items:[' + head + ']}')
            xdo('click', '3'); time.sleep(1.5)
            command('execute if entity @a[name=Dev,nbt={Inventory:[{Slot:103b,id:"fiskheroes:superhero_helmet",tag:{HeroType:"fiskheroes:atom",Marker:"mini-test"}},{Slot:102b,id:"minecraft:iron_chestplate",tag:{Marker:"keep-chest"}},{id:"minecraft:iron_helmet",tag:{Marker:"old-helmet"}}]}] unless data entity Dev SelectedItem run say MINI_PARTIAL_DEPLOY_OK')
            wait_for(server_log, r'\[Server\] MINI_PARTIAL_DEPLOY_OK', 30, 'partial suit deploys, preserves unrelated armor and returns displaced helmet', server)
            command('item replace entity Dev weapon.mainhand with fiskheroes:mini_suit')
            xdo('click', '3'); time.sleep(1)
            command('execute if entity @a[name=Dev,nbt={Inventory:[{Slot:103b,id:"fiskheroes:superhero_helmet",tag:{Marker:"mini-test"}},{Slot:102b,id:"minecraft:iron_chestplate",tag:{Marker:"keep-chest"}},{Slot:0b,id:"fiskheroes:mini_suit"}]}] run say MINI_EMPTY_UNCHANGED_OK')
            wait_for(server_log, r'\[Server\] MINI_EMPTY_UNCHANGED_OK', 30, 'empty miniature leaves armor and itself unchanged', server)
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    try: os.killpg(process.pid, signal.SIGTERM)
                    except ProcessLookupError: pass
            for process in reversed(processes):
                try: process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL); process.wait()


if __name__ == '__main__': main()
