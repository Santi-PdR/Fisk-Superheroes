#!/usr/bin/env python3
"""Exercise the actual augmentor GUI against an authoritative dedicated server."""
import base64
import io
import os
import signal
import subprocess
import time
from pathlib import Path
from PIL import Image
from small_player_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log, client_log = Path('ci-augmentor-server.log'), Path('ci-augmentor-client.log')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    processes = []
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-augmentor-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-augmentor-client',
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
                if any(p.poll() is not None for p in processes): raise RuntimeError('Minecraft exited')
                server.stdin.write(line + '\n'); server.stdin.flush(); time.sleep(.8)
            geometry = subprocess.check_output(['xdotool', 'getwindowgeometry', '--shell', window], text=True, timeout=30)
            values = dict(line.split('=', 1) for line in geometry.splitlines() if '=' in line)
            wx, wy, width, height = (int(values[key]) for key in ('X', 'Y', 'WIDTH', 'HEIGHT'))
            print(f'Window geometry: {wx},{wy} {width}x{height}', flush=True)
            def click(x, y, shift=False):
                # Use desktop coordinates: the Minecraft window may be centered inside Xvfb.
                xdo('mousemove', str(x), str(y))
                if shift: xdo('keydown', 'Shift_L')
                xdo('click', '1')
                if shift: xdo('keyup', 'Shift_L')
                time.sleep(1)
            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('fill -4 179 -4 4 179 5 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('setblock 0 181 2 fiskheroes:suit_augmentor')
            command('clear Dev')
            command('item replace entity Dev hotbar.0 with fiskheroes:superhero_chestplate{HeroType:"fiskheroes:iron_man",Keep:"yes"}')
            command('item replace entity Dev hotbar.1 with fiskheroes:suit_upgrade_vibranium')
            time.sleep(2)
            xdo('click', '3')
            time.sleep(3)
            # Options specify guiScale 2. Position the GUI relative to the actual window,
            # not the desktop; those differ when the 854x480 game is centered in Xvfb.
            scale = 2
            left = wx + (width - 176*scale) // 2
            top = wy + (height - 215*scale) // 2
            print(f'GUI bounds: {left},{top} {176*scale}x{215*scale}', flush=True)
            click(left + 16*scale, top + 199*scale, True)  # suit core from hotbar
            click(left + 34*scale, top + 199*scale, True)  # tier IV upgrade
            xdo('mousemove', str(wx + 10), str(wy + 10))
            time.sleep(2)
            subprocess.run(['scrot', 'ci-augmentor-ready.png'], check=True, timeout=30)
            with Image.open('ci-augmentor-ready.png') as image:
                image = image.crop((left, top, left + 176*scale, top + 215*scale))
                buf = io.BytesIO(); image.convert('RGB').save(buf, 'JPEG', quality=85)
            print('AUGMENTOR_GUI_PREVIEW=' + base64.b64encode(buf.getvalue()).decode('ascii'), flush=True)
            click(left + 41*scale, top + 80*scale)  # Iron Man's second attribute: punch
            subprocess.run(['scrot', 'ci-augmentor-upgraded.png'], check=True, timeout=30)
            xdo('key', 'Escape'); time.sleep(2)
            command('data get entity Dev Inventory')
            # Match the actual data response, not a command string echoed in a parse error.
            wait_for(server_log, r'Dev has the following entity data: \[(?=[^\n]*Upgrades: 16)(?=[^\n]*Keep: "yes")(?![^\n]*id: "fiskheroes:suit_upgrade_vibranium")[^\n]*\]',
                     25, 'GUI upgrade persisted and consumed the correct item', server)
            print('PASS: client GUI and server-authoritative augment round trip', flush=True)
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
