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
            # Check the local-player jump event after a real keypress. This measures the
            # initial impulse only, not the resulting trajectory or maximum height.
            command('fill ~-3 119 ~-3 ~3 119 ~3 minecraft:stone')
            command('tp @s ~ 120 ~')
            command('suit fiskheroes:spider_man')
            wait_for(r'\[CHAT\] Equipped Spider-Man|\[Dev: Equipped Spider-Man', 45,
                     'equip Spider-Man', process)
            command('gamemode survival')
            time.sleep(4)  # Wait for the player to land and the suit attributes to sync.
            xdo('keydown', 'space')
            time.sleep(0.3)
            xdo('keyup', 'space')
            wait_for(r'Suit jump hero=fiskheroes:spider_man side=client vertical=', 45,
                     'Spider-Man jump event on the client', process)
            match = re.search(r'Suit jump hero=fiskheroes:spider_man side=client vertical=([-+0-9.eE]+)',
                              log.read_text(errors='replace'))
            if not match or float(match.group(1)) < 0.75:
                raise RuntimeError(f'Spider-Man jump impulse too small: {match.group(1) if match else "missing"}')
            print('PASS: Spider-Man local jump impulse', match.group(1), flush=True)
            # The comics suit changes both its equipped base texture and emissive layer when
            # speeding. Check real client resource selection before and after the ability key.
            command('gamemode creative')
            command('suit fiskheroes:godspeed_comics')
            wait_for(r'Drawing suit fiskheroes:godspeed_comics slot fiskheroes:chestplate from fiskheroes:textures/heroes/godspeed_comics_layer1\.png',
                     45, 'Godspeed normal equipped texture', process)
            xdo('key', 'c')
            wait_for(r'Drawing suit fiskheroes:godspeed_comics slot fiskheroes:chestplate from fiskheroes:textures/heroes/godspeed_comics_layer1_on\.png',
                     45, 'Godspeed active equipped texture', process)
            command('suit fiskheroes:ant_man')
            wait_for(r'\[CHAT\] Equipped Ant-Man|\[Dev: Equipped Ant-Man', 45, 'equip Ant-Man', process)
            time.sleep(3)
            xdo('key', 'c')
            wait_for(r'Player dimensions entity=Dev side=server scale=0\.0625 width=', 45,
                     'Ant-Man shrinking changes the server collision dimensions', process)
            wait_for(r'Player dimensions entity=Dev side=client scale=0\.0625 width=', 45,
                     'Ant-Man scale synchronizes to the client', process)
            match = re.search(r'Player dimensions entity=Dev side=client scale=0\.0625 width=([-+0-9.eE]+)',
                              log.read_text(errors='replace'))
            if not match or not 0.03 < float(match.group(1)) < 0.05:
                raise RuntimeError(f'Unexpected client collision width after shrinking: {match.group(1) if match else "missing"}')
            subprocess.run(['scrot', 'ci-antman-screenshot.jpg'], check=True, timeout=30)
            # Mysterio shares V between AIM and a beam gated on the two-stage aim timer.
            # Keep the physical key down until the server actually starts the modifier.
            command('clear @s')
            command('suit fiskheroes:mysterio')
            wait_for(r'\[CHAT\] Equipped Mysterio|\[Dev: Equipped Mysterio', 45, 'equip Mysterio', process)
            time.sleep(4)
            xdo('keydown', 'v')
            try:
                wait_for(r'Ability modifier executed: hero=fiskheroes:mysterio, key=ENERGY_PROJECTION',
                         60, 'held Mysterio aim activates the gated beam on the server', process)
            finally:
                xdo('keyup', 'v')
            wait_for(r'Shared suit arm pose applied: hero=fiskheroes:mysterio, sleeveAligned=true',
                     45, 'player skin and suit share the animated arm pose', process)
            xdo('keydown', 'c')
            try:
                wait_for(r'Charged beam firing: entity=Dev, charge=1\.0', 60,
                         'charged beam reaches its normalized firing phase', process)
            finally:
                xdo('keyup', 'c')
            # Cycle from third-person back through front view to first person.
            xdo('key', 'F5')
            time.sleep(1)
            xdo('key', 'F5')
            wait_for(r'First-person suit arm rendered: hero=fiskheroes:mysterio, texture=fiskheroes:textures/heroes/mysterio_layer1\.png, poseAligned=true',
                     45, 'first-person suit arm matches the actual vanilla arm pose', process)
            # Batman's jump while falling deploys the cape; watch the client render the wing mesh.
            command('suit fiskheroes:batman_dceu')
            command('gamemode survival')
            command('fill ~-10 -64 ~20 ~10 319 ~20 minecraft:stone')
            command('tp @s ~ 300 ~ 0 0')
            xdo('key', 'F5')  # Third-person rear view.
            time.sleep(1)
            xdo('keydown', 'space')
            try:
                wait_for(r'Gliding input: player=Dev, active=true', 45, 'falling jump activates Batman glide', process)
            finally:
                xdo('keyup', 'space')
            wait_for(r'Local gliding travel for Dev: horizontal=([-+0-9.eE]+), vertical=',
                     45, 'local player uses gliding travel', process)
            match = re.search(r'Local gliding travel for Dev: horizontal=([-+0-9.eE]+), vertical=',
                              log.read_text(errors='replace'))
            if not match or float(match.group(1)) <= 0.0001:
                raise RuntimeError('Gliding travel did not generate forward speed')
            wait_for(r'Drawing deployed cape for Dev with texture fiskheroes:textures/heroes/batman_dceu_cape\.png',
                     45, 'client renders Batman deployed cape texture', process)
            subprocess.run(['scrot', 'ci-batman-cape-screenshot.jpg'], check=True, timeout=30)
            # A real block impact should be processed by the server, not just by the local
            # player's collision prediction. The wall was built before starting the glide.
            wait_for(r'Gliding wall impact: player=Dev, severity=[-+0-9.eE]+, stopped=true',
                     45, 'server stops Batman after a real gliding wall impact', process)
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
