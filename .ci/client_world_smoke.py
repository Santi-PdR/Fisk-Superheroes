#!/usr/bin/env python3
"""Drive a real client in Xvfb; validate world entry, a suit, its render path and key input."""
import os
import re
import signal
import subprocess
import sys
import time
from pathlib import Path

log = Path('ci-client-world.log')


def wait_for(pattern, seconds, label, process):
    expression = re.compile(pattern, re.IGNORECASE)
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        text = log.read_text(errors='replace') if log.exists() else ''
        if expression.search(text):
            print('PASS:', label, flush=True)
            return
        if process.poll() is not None:
            raise RuntimeError(f'Client exited before {label}, code {process.returncode}')
        time.sleep(1)
    raise RuntimeError(f'Timed out waiting for {label}')


def xdo(*args):
    subprocess.run(['xdotool', *args], check=True, timeout=30)


def command(text):
    # Opening/closing chat takes a few rendered frames on software rendering.
    xdo('key', '--clearmodifiers', 'slash')
    time.sleep(0.8)
    xdo('type', '--clearmodifiers', '--delay', '15', text)
    time.sleep(0.4)
    xdo('key', 'Return')
    time.sleep(1)


def main():
    environment = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    with log.open('w') as output:
        process = subprocess.Popen(['./gradlew', 'runClient', '-PquickPlayWorld=SmokeWorld',
                                    '--no-daemon', '--console=plain'], stdout=output,
                                   stderr=subprocess.STDOUT, env=environment, start_new_session=True)
        try:
            wait_for(r'logged in with entity id', 360, 'integrated player login', process)
            time.sleep(8)  # Let the terrain-loading screen close before sending keyboard input.
            result = subprocess.check_output(['xdotool', 'search', '--onlyvisible', '--name', 'Minecraft'],
                                             text=True, timeout=30)
            window = result.strip().splitlines()[-1]
            xdo('windowfocus', '--sync', window)
            xdo('mousemove', '--window', window, '640', '360', 'click', '1')
            time.sleep(3)
            # Slash opens command chat directly; give the rendered UI a frame to receive it.
            xdo('key', '--clearmodifiers', 'slash')
            time.sleep(2)
            xdo('type', '--clearmodifiers', '--delay', '35', 'suit fiskheroes:the_flash')
            time.sleep(2)
            xdo('key', 'Return')
            wait_for(r'\[CHAT\] Equipped The Flash|\[Dev: Equipped The Flash', 45, 'suit command', process)
            time.sleep(3)
            xdo('key', 'F5')
            wait_for(r'Drawing suit fiskheroes:the_flash slot', 45, 'third-person suit render path', process)
            time.sleep(2)
            subprocess.run(['scrot', 'ci-world-screenshot.jpg'], check=True, timeout=30)
            xdo('keydown', 'c')
            time.sleep(0.3)
            xdo('keyup', 'c')
            wait_for(r'Ability input accepted: hero=fiskheroes:the_flash, index=1', 45,
                     'server received the speedster ability key', process)
            time.sleep(3)
            command('suit fiskheroes:vision')
            wait_for(r'\[CHAT\] Equipped Vision|\[Dev: Equipped Vision', 45, 'equip Vision', process)
            # A high platform and a short wall make the movement test independent of terrain.
            command('fill ~-5 119 ~-5 ~5 119 ~9 minecraft:stone')
            command('fill ~-5 120 ~-5 ~5 126 ~9 minecraft:air')
            command('tp @s ~ 120 ~ 0 0')
            command('fill ~-3 ~ ~3 ~3 ~4 ~3 minecraft:stone')
            command('summon minecraft:marker ~ ~ ~')
            xdo('keydown', 'w')
            time.sleep(3)
            xdo('keyup', 'w')
            command('execute as @e[type=minecraft:marker,limit=1] at @s positioned ~-3 ~-10 ~-2 if entity @p[dx=6,dy=20,dz=4] run say BEFORE_PHASE_BLOCKED')
            wait_for(r'\[CHAT\].*BEFORE_PHASE_BLOCKED', 30, 'wall blocks normal movement', process)
            xdo('keydown', 'v')
            time.sleep(0.3)
            xdo('keyup', 'v')
            wait_for(r'Ability modifier executed: hero=.*Vision.*, key=INTANGIBILITY', 45,
                     'activate intangibility on the server', process)
            time.sleep(2)
            xdo('keydown', 'w')
            time.sleep(5)
            xdo('keyup', 'w')
            command('execute as @e[type=minecraft:marker,limit=1] at @s positioned ~-8 ~-64 ~4 if entity @p[dx=16,dy=128,dz=128] run say PHASE_CROSSED_WALL')
            wait_for(r'\[CHAT\].*PHASE_CROSSED_WALL', 45, 'client and server crossed the wall while phased', process)
            wait_for(r'Playing pack sound definition fiskheroes:vision_intang_on as fiskheroes:suit\.vision\.intangibility\.enable',
                     45, 'resolve Vision sound to its downloaded event', process)
            # Guardian deploys a toggle shield with C; right click controls blocking separately.
            command('suit fiskheroes:guardian')
            wait_for(r'\[CHAT\] Equipped Guardian|\[Dev: Equipped Guardian', 45, 'equip Guardian', process)
            xdo('key', 'c')
            wait_for(r'Ability modifier executed: hero=fiskheroes:guardian, key=SHIELD', 45,
                     'deploy Guardian shield', process)
            xdo('mousedown', '3')
            wait_for(r'Suit shield blocking changed: entity=Dev, blocking=true', 45,
                     'use key raises the shield on the server', process)
            xdo('mouseup', '3')
            wait_for(r'Suit shield blocking changed: entity=Dev, blocking=false', 45,
                     'releasing use lowers the shield on the server', process)
            text = log.read_text(errors='replace')
            if 'Downloading the FiskHeroes sound repository' not in text or not Path('run/fiskheroes_sounds/sound-events.json').is_file():
                raise RuntimeError('Legacy sound cache was not migrated from the original archive')
            if re.search(r'Unable to play (?:unknown|empty) soundEvent: fiskheroes:(?:vision_intang|suit\.vision\.intangibility|modifier\.flight\.cape|ambient\.flight)', text):
                raise RuntimeError('Vision or flight sound event was missing from the downloaded pack')
            if re.search(r'#@!@# Game crashed|\[.*?/FATAL\]|Error ticking modifier|Error running hero tick handler', text):
                raise RuntimeError('Client/server reported a crash or modifier error')
            print('PASS: client world smoke completed', flush=True)
        finally:
            try:
                os.killpg(process.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait()


if __name__ == '__main__':
    try:
        main()
    except Exception as exc:
        print('FAIL:', exc, file=sys.stderr)
        if log.exists():
            print('\n'.join(log.read_text(errors='replace').splitlines()[-100:]), file=sys.stderr)
        sys.exit(1)
