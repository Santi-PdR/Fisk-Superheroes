#!/usr/bin/env python3
"""Focused real-client geometry check using a diagnostic magenta tonfa texture.

The texture override is confined to the CI runner, never shipped in the mod.
"""
import os
import re
import signal
import subprocess
import time
from pathlib import Path
from PIL import Image


def wait_for(log, pattern, seconds, label, process):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if log.exists() and re.search(pattern, log.read_text(errors='replace'), re.I):
            print('PASS:', label, flush=True)
            return
        if process.poll() is not None:
            raise RuntimeError(f'{label}: process exited with {process.returncode}')
        time.sleep(1)
    raise RuntimeError(f'Timed out: {label}')


def xdo(*args):
    subprocess.run(['xdotool', *args], check=True, timeout=30)


def magenta(image, region):
    """Diagnostic texture pixels in the fixed 1280x720 CI screenshot."""
    return sum(1 for r, g, b in image.crop(region).convert('RGB').getdata()
               if r > 90 and b > 90 and g < 50 and abs(r - b) < 40)


def assert_geometry():
    stowed = Image.open('ci-tonfa-stowed-front.png')
    third = Image.open('ci-tonfa-held-front.png')
    before = Image.open('ci-tonfa-held-first-before.png')
    after = Image.open('ci-tonfa-held-first-after.png')
    occupied = Image.open('ci-tonfa-held-occupied.png')
    invisible = Image.open('ci-tonfa-held-invisible.png')
    if any(image.size != (1280, 720) for image in (stowed, third, before, after, occupied, invisible)):
        raise RuntimeError('Unexpected screenshot dimensions')
    # Stowed batons must project outside both legs, not be embedded in them.
    for region in ((575, 440, 640, 490), (640, 440, 705, 490)):
        if magenta(stowed, region) < 70:
            raise RuntimeError('Stowed tonfa is hidden inside a leg')
    # In third person both batons should extend vertically along the forearms.
    for region in ((575, 385, 630, 425), (650, 385, 705, 425)):
        if magenta(third, region) < 15:
            raise RuntimeError('Held third-person tonfa is not aligned with the forearm')
    left, right = (300, 300, 620, 540), (660, 300, 980, 540)
    if min(magenta(before, left), magenta(before, right)) < 1000:
        raise RuntimeError('A first-person tonfa is missing from view')
    # Avoid the previous regression where the camera clipped giant close-up bars.
    if magenta(before, (213, 120, 300, 550)) or magenta(before, (980, 120, 1067, 550)):
        raise RuntimeError('First-person tonfa is clipped at a screen edge')
    b = before.crop(right).convert('RGB')
    a = after.crop(right).convert('RGB')
    changed = sum(1 for p, q in zip(b.getdata(), a.getdata()) if (p[0] > 90 and p[2] > 90 and p[1] < 50) != (q[0] > 90 and q[2] > 90 and q[1] < 50))
    if changed < 500:
        raise RuntimeError('Attack did not visibly flip the right-hand tonfa')
    if magenta(occupied, left) > 100 or magenta(occupied, right) < 1000:
        raise RuntimeError('Real offhand item failed to replace the visual tonfa')
    def dark(image, region):
        return sum(1 for r, g, b in image.crop(region).convert('RGB').getdata() if max(r, g, b) < 50)
    for region in ((385, 490, 440, 540), (840, 490, 895, 540)):
        if dark(before, region) < 500 or dark(invisible, region) > 100:
            raise RuntimeError('First-person arm does not respect player invisibility')
    print('PASS: screenshot geometry, flipped pose, occupied offhand and invisible arms', flush=True)


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log, client_log = Path('ci-tonfa-server.log'), Path('ci-tonfa-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-tonfa-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-tonfa-client',
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
                subprocess.run(['scrot', 'ci-tonfa-' + name + '.png'], check=True, timeout=30)
                print('Captured:', name, flush=True)

            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 5 minecraft:stone')
            command('tp Dev 0 180 0 0 0')
            command('execute as Dev run suit fiskheroes:black_canary')
            xdo('key', 'F5')
            wait_for(client_log, r'Equipped suit item rendered: hero=fiskheroes:black_canary, item=fiskheroes:tactical_tonfa',
                     60, 'stowed tonfas rendered', client)
            screenshot('stowed-back')
            xdo('key', 'F5')
            screenshot('stowed-front')
            command('item replace entity Dev weapon.mainhand with fiskheroes:tactical_tonfa{Dual:1b}')
            wait_for(client_log, r'Tonfa hand pose: wearer=Dev, context=THIRD_PERSON_LEFT_HAND, toggled=false',
                     60, 'third person dual tonfas', client)
            screenshot('held-front')
            xdo('key', 'F5')
            wait_for(client_log, r'Tonfa hand pose: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND, toggled=false',
                     60, 'first person tonfa', client)
            wait_for(client_log, r'First-person tonfa arm rendered: RIGHT',
                     60, 'right first-person arm rendered', client)
            wait_for(client_log, r'First-person tonfa arm rendered: LEFT',
                     60, 'left first-person arm rendered', client)
            screenshot('held-first-before')
            xdo('click', '1')
            wait_for(client_log, r'Tonfa hand pose: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND, toggled=true',
                     60, 'tonfa flips on attack', client)
            screenshot('held-first-after')
            xdo('key', 'F5')
            xdo('key', 'F5')
            screenshot('held-front-after')
            xdo('key', 'F5')  # back to first person
            command('item replace entity Dev weapon.offhand with minecraft:torch')
            wait_for(client_log, r'Dual visual offhand: player=Dev, item=fiskheroes:tactical_tonfa, mode=occupied',
                     60, 'real offhand replaces synthetic tonfa', client)
            screenshot('held-occupied')
            command('item replace entity Dev weapon.offhand with minecraft:air')
            command('effect give Dev minecraft:invisibility 30 0 true')
            wait_for(server_log, r'(?i)applied effect.*invisibility', 30, 'invisibility effect applied', server)
            screenshot('held-invisible')
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
