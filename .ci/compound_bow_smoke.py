#!/usr/bin/env python3
"""Exercise compound bow rendering and horizontal aiming with a real client and dedicated server."""
import os
import signal
import subprocess
import time
from pathlib import Path
from small_player_smoke import wait_for, wait_sample, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciBowDiagnostics=true')
    server_log, client_log = Path('ci-bow-server.log'), Path('ci-bow-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-bow-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-bow-client',
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
                server.stdin.write(line + '\n'); server.stdin.flush(); time.sleep(0.7)
            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('fill -5 179 -5 5 179 15 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:green_arrow')
            command('item replace entity Dev weapon.mainhand with fiskheroes:compound_bow')
            command('item replace entity Dev hotbar.1 with fiskheroes:quiver{Enchantments:[{id:"fiskheroes:capacity",lvl:1s}],QuiverInventory:{Size:5,Items:[{Slot:0,id:"fiskheroes:trick_arrow",Count:22b},{Slot:1,id:"fiskheroes:trick_arrow",Count:3b,tag:{ArrowType:"cactus"}}]}}')
            wait_for(client_log, r'Compound bow 3D model rendered: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND, drawing=false', 30,
                     'first-person compound bow model', client)
            wait_for(client_log, r'Compound bow icon rendered: context=GUI, broken=false, missing=false', 30,
                     'unbroken bow inventory icon is loaded', client)
            wait_for(client_log, r'Quiver HUD rendered: selected=0, occupied=2', 30,
                     'five-slot quiver HUD displays ammunition', client)
            xdo('keydown', 'Shift_L'); time.sleep(.5); xdo('click', '5'); time.sleep(.8); xdo('keyup', 'Shift_L')
            wait_for(client_log, r'Quiver HUD rendered: selected=1, occupied=2', 30,
                     'scrolling changes the highlighted quiver slot', client)
            subprocess.run(['scrot', 'ci-quiver-hud.png'], check=True, timeout=30)
            xdo('mousedown', '3'); time.sleep(1); xdo('mouseup', '3')
            command('execute if entity @e[type=fiskheroes:trick_arrow,nbt={ArrowType:"cactus"}] run say QUIVER_CACTUS_PASS')
            wait_for(server_log, r'QUIVER_CACTUS_PASS', 15,
                     'scroll selection reaches server and fires the chosen arrow', server)
            command('kill @e[type=fiskheroes:trick_arrow]')
            xdo('keydown', 'Shift_L'); time.sleep(.5); xdo('click', '5'); time.sleep(.8); xdo('keyup', 'Shift_L')
            wait_for(client_log, r'Quiver HUD rendered: selected=2, occupied=2, name=Normal Arrow', 30,
                     'empty selection shows fallback arrow name', client)
            xdo('mousedown', '3'); time.sleep(1); xdo('mouseup', '3')
            command('execute if entity @e[type=fiskheroes:trick_arrow,nbt={ArrowType:"normal"}] run say QUIVER_FALLBACK_PASS')
            wait_for(server_log, r'QUIVER_FALLBACK_PASS', 15,
                     'server fires fallback arrow from an empty selected slot', server)
            command('kill @e[type=fiskheroes:trick_arrow]')
            start = len(client_log.read_text(errors='replace'))
            xdo('keydown', 'Shift_L'); time.sleep(.5); xdo('click', '4'); time.sleep(.3); xdo('click', '4'); time.sleep(.8); xdo('keyup', 'Shift_L')
            wait_sample(client_log, start, r'Quiver HUD rendered: selected=0, occupied=2, name=Normal Arrow',
                        lambda _: True, 30, 'scroll selection wraps back to normal arrow', client)
            xdo('keydown', 'v'); time.sleep(1); xdo('mousedown', '3'); time.sleep(1)
            wait_for(client_log, r'Compound bow 3D model rendered: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND, drawing=true, horizontal=true', 30,
                     'horizontal drawn bow rendered in first person', client)
            wait_for(client_log, r'Compound bow nocked arrow rendered: type=normal', 30, 'nocked arrow rendered with bow', client)
            wait_for(client_log, r'Trick arrow original 3D model rendered', 30,
                     'original 3D trick arrow model is drawn', client)
            xdo('keyup', 'v'); time.sleep(.5)
            subprocess.run(['scrot', 'ci-bow-draw-first.png'], check=True, timeout=30)
            xdo('key', 'F5'); time.sleep(1)
            wait_for(client_log, r'Compound bow 3D model rendered: wearer=Dev, context=THIRD_PERSON_RIGHT_HAND, drawing=true, horizontal=true', 30,
                     'horizontal drawn bow rendered in third person', client)
            wait_for(client_log, r'Quiver shafts rendered: wearer=Dev, counts=21000, total=3', 30,
                     'third-person quiver displays its arrows', client)
            subprocess.run(['scrot', 'ci-bow-draw-third.png'], check=True, timeout=30)
            xdo('mouseup', '3'); time.sleep(.5)
            command('execute if entity @e[type=fiskheroes:trick_arrow] run say BOW_SHOT_PASS')
            wait_for(server_log, r'BOW_SHOT_PASS', 15, 'bow fires on dedicated server', server)
            xdo('key', 'F5'); xdo('key', 'F5'); time.sleep(1)
            xdo('mousedown', '3')
            try:
                wait_for(client_log, r'Compound bow 3D model rendered: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND, drawing=true, horizontal=false', 30,
                         'vertical drawn bow rendered in first person', client)
                time.sleep(.5)
                subprocess.run(['scrot', 'ci-bow-draw-first-vertical.png'], check=True, timeout=30)
            finally:
                xdo('mouseup', '3')
            command('item replace entity Dev hotbar.1 with minecraft:air')
            command('item replace entity Dev weapon.offhand with minecraft:spectral_arrow')
            xdo('mousedown', '3')
            try:
                wait_for(client_log, r'Compound bow nocked arrow rendered: type=minecraft:spectral_arrow, wearer=Dev', 30,
                         'nocked spectral arrow uses its own renderer', client)
            finally:
                xdo('mouseup', '3')
            for kind in ('vibranium', 'tutridium', 'blaze'):
                command(f'item replace entity Dev weapon.offhand with fiskheroes:trick_arrow{{ArrowType:"{kind}"}}')
                xdo('mousedown', '3')
                try:
                    glow = 'true' if kind == 'blaze' else 'false'
                    wait_for(client_log, fr'Trick arrow tipped head rendered: type={kind}, glow={glow}, missing=false', 30,
                             f'{kind} arrow uses its original textured tip', client)
                    time.sleep(.5)
                    subprocess.run(['scrot', f'ci-arrow-tipped-{kind}.png'], check=True, timeout=30)
                finally:
                    xdo('mouseup', '3')
            command('item replace entity Dev weapon.mainhand with fiskheroes:compound_bow{Broken:1b}')
            wait_for(client_log, r'Compound bow 3D model rendered: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND, drawing=false, horizontal=false, broken=true', 30,
                     'broken bow renders without strings', client)
            wait_for(client_log, r'Compound bow icon rendered: context=GUI, broken=true, missing=false', 30,
                     'broken bow inventory icon is loaded', client)
            subprocess.run(['scrot', 'ci-bow-broken.png'], check=True, timeout=30)
            print('PASS: real client renders and fires the 3D compound bow in both views', flush=True)
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    try: os.killpg(process.pid, signal.SIGTERM)
                    except ProcessLookupError: pass
            for process in processes:
                try: process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    try: os.killpg(process.pid, signal.SIGKILL)
                    except ProcessLookupError: pass


if __name__ == '__main__': main()
