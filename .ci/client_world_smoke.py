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
        match = expression.search(text)
        if match:
            print('PASS:', label, flush=True)
            return match
        if process.poll() is not None:
            raise RuntimeError(f'Client exited before {label}, code {process.returncode}')
        time.sleep(1)
    raise RuntimeError(f'Timed out waiting for {label}')


def wait_for_count(pattern, count, seconds, label, process):
    expression = re.compile(pattern, re.IGNORECASE)
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        text = log.read_text(errors='replace') if log.exists() else ''
        if len(expression.findall(text)) >= count:
            print('PASS:', label, flush=True)
            return
        if process.poll() is not None:
            raise RuntimeError(f'Client exited before {label}, code {process.returncode}')
        time.sleep(0.25)
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
            # The shield stowed on Captain America's suit is rendered with the item
            # renderer, whose units are blocks rather than model pixels.
            command('suit fiskheroes:captain_america')
            match = wait_for(r'Equipped suit item rendered: hero=fiskheroes:captain_america, item=fiskheroes:captain_americas_shield, poseScale=([-+0-9.eE]+)',
                             45, 'Captain America stowed shield render', process)
            if not 0.90 < float(match.group(1)) < 0.96:
                raise RuntimeError(f'Unexpected equipped shield scale: {match.group(1)}')
            subprocess.run(['scrot', 'ci-equipped-shield.jpg'], check=True, timeout=30)
            # Use a controlled, unobstructed third-person view for stowed equipment.
            command('time set day')
            command('tp @s 0 185 0 0 0')
            time.sleep(3)
            command('fill -5 179 -5 5 179 5 minecraft:stone')
            command('tp @s 0 180 0 0 0')
            command('suit fiskheroes:deadpool_xmen')
            wait_for(r'Equipped suit item rendered: hero=fiskheroes:deadpool_xmen, item=fiskheroes:katana',
                     45, 'Deadpool stowed katanas render', process)
            wait_for(r'Stowed dual katana meshes per anchor: 1', 45,
                     'one katana mesh per stowed anchor', process)
            wait_for(r'Equipped suit item rendered: hero=fiskheroes:deadpool_xmen, item=fiskheroes:katana, poseScale=[-+0-9.eE]+, livery=fiskheroes:textures/heroes/deadpool_xmen_katana.png',
                     45, 'Deadpool katana uses the suit livery', process)
            wait_for(r'Equipped suit item rendered: hero=fiskheroes:deadpool_xmen, item=fiskheroes:desert_eagle, poseScale=[-+0-9.eE]+, livery=fiskheroes:textures/heroes/deadpool_xmen_gun.png',
                     45, 'Deadpool stowed pistols use the suit livery', process)
            time.sleep(2)
            subprocess.run(['scrot', 'ci-equipped-deadpool.jpg'], check=True, timeout=30)
            command('item replace entity @s weapon.mainhand with fiskheroes:katana{Dual:1b}')
            wait_for(r'Held weapon livery: wearer=Dev, hero=fiskheroes:deadpool_xmen, item=fiskheroes:katana, context=THIRD_PERSON_RIGHT_HAND, texture=fiskheroes:textures/heroes/deadpool_xmen_katana\.png',
                     45, 'Deadpool held katana uses livery in third person', process)
            xdo('key', 'F5')
            time.sleep(1)
            xdo('key', 'F5')
            wait_for(r'Held weapon livery: wearer=Dev, hero=fiskheroes:deadpool_xmen, item=fiskheroes:katana, context=FIRST_PERSON_RIGHT_HAND, texture=fiskheroes:textures/heroes/deadpool_xmen_katana\.png',
                     45, 'Deadpool held katana uses livery in first person', process)
            wait_for(r'Held weapon livery: wearer=Dev, hero=fiskheroes:deadpool_xmen, item=fiskheroes:katana, context=FIRST_PERSON_LEFT_HAND, texture=fiskheroes:textures/heroes/deadpool_xmen_katana\.png',
                     45, 'dual katana renders in the first-person offhand', process)
            command('execute unless data entity @s Inventory[{Slot:-106b}] run say DUAL_OFFHAND_EMPTY_OK')
            wait_for(r'\[CHAT\].*DUAL_OFFHAND_EMPTY_OK', 30, 'dual offhand is visual only on the server', process)
            time.sleep(2)
            subprocess.run(['scrot', 'ci-held-katana.jpg'], check=True, timeout=30)
            command('item replace entity @s weapon.offhand with minecraft:torch')
            wait_for(r'Dual visual offhand: player=Dev, item=fiskheroes:katana, mode=occupied',
                     45, 'real offhand item takes precedence over visual duplicate', process)
            command('item replace entity @s weapon.offhand with minecraft:air')
            wait_for_count(r'Dual visual offhand: player=Dev, item=fiskheroes:katana, mode=synthetic', 2,
                           45, 'dual visual offhand returns when real offhand is empty', process)
            command('item replace entity @s weapon.mainhand with fiskheroes:desert_eagle')
            wait_for(r'Held weapon livery: wearer=Dev, hero=fiskheroes:deadpool_xmen, item=fiskheroes:desert_eagle, context=FIRST_PERSON_RIGHT_HAND, texture=fiskheroes:textures/heroes/deadpool_xmen_gun\.png',
                     45, 'Deadpool held pistol uses suit livery', process)
            command('suit fiskheroes:deathstroke_dceu')
            wait_for(r'Held weapon livery: wearer=Dev, hero=fiskheroes:deathstroke_dceu, item=fiskheroes:desert_eagle, context=FIRST_PERSON_RIGHT_HAND, texture=fiskheroes:textures/heroes/deathstroke_dceu_gun\.png',
                     45, 'changing suits changes held weapon livery', process)
            command('suit fiskheroes:the_flash')
            wait_for(r'Held weapon livery: wearer=Dev, hero=fiskheroes:the_flash, item=fiskheroes:desert_eagle, context=FIRST_PERSON_RIGHT_HAND, texture=default',
                     45, 'weapon without livery restores its default texture', process)
            command('clear @s')
            xdo('key', 'F5')  # Restore third-person rear view.
            command('suit fiskheroes:black_canary')
            wait_for(r'Equipped suit item rendered: hero=fiskheroes:black_canary, item=fiskheroes:tactical_tonfa, poseScale=([-+0-9.eE]+)',
                     45, 'Black Canary stowed tonfas use centered poses', process)
            time.sleep(2)
            subprocess.run(['scrot', 'ci-canary-stowed.jpg'], check=True, timeout=30)
            command('item replace entity @s weapon.mainhand with fiskheroes:tactical_tonfa{Dual:1b}')
            wait_for(r'Tonfa hand pose: wearer=Dev, context=THIRD_PERSON_LEFT_HAND, toggled=false',
                     45, 'Black Canary renders the second tonfa in third person', process)
            xdo('key', 'F5')
            time.sleep(1)
            xdo('key', 'F5')
            wait_for(r'Tonfa hand pose: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND, toggled=false',
                     45, 'first-person tonfa starts in the normal pose', process)
            wait_for(r'Tonfa hand pose: wearer=Dev, context=FIRST_PERSON_LEFT_HAND, toggled=false',
                     45, 'first-person second tonfa uses its own state', process)
            time.sleep(2)
            subprocess.run(['scrot', 'ci-tonfa-before.jpg'], check=True, timeout=30)
            xdo('click', '1')
            wait_for(r'Tonfa hand pose: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND, toggled=true',
                     45, 'swing flips the right tonfa pose', process)
            time.sleep(2)
            subprocess.run(['scrot', 'ci-tonfa-after.jpg'], check=True, timeout=30)
            command('clear @s')
            xdo('key', 'F5')  # Return to third-person rear view.
            command('suit fiskheroes:batman_dceu')
            wait_for(r'Equipped suit item rendered: hero=fiskheroes:batman_dceu, item=fiskheroes:grappling_gun',
                     45, 'Batman stowed grappling gun renders', process)
            time.sleep(2)
            subprocess.run(['scrot', 'ci-equipped-batman.jpg'], check=True, timeout=30)
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
            meter = wait_for(r'Suit protection HUD: hero=fiskheroes:spider_man, fill=(\d+), armor=false, x=(-?\d+), y=(-?\d+), screen=(\d+)x(\d+)',
                             45, 'tier protection meter in the survival HUD', process)
            if not 1 <= int(meter.group(1)) <= 79:
                raise RuntimeError(f'Unexpected protection meter fill: {meter.group(1)}')
            time.sleep(10)  # Let chat notifications fade before examining HUD pixels.
            subprocess.run(['scrot', 'ci-protection-hud.png'], check=True, timeout=30)
            # Confirm the actual pixels, not merely that the render callback logged. The
            # original meter has a solid light-gray interior against a dark-gray frame.
            from PIL import Image
            geometry = subprocess.check_output(['xdotool', 'getwindowgeometry', '--shell', window], text=True, timeout=30)
            values = dict(line.split('=', 1) for line in geometry.splitlines() if '=' in line)
            gx, gy = int(values['X']), int(values['Y'])
            sx = int(values['WIDTH']) / int(meter.group(4))
            sy = int(values['HEIGHT']) / int(meter.group(5))
            px = int(gx + (int(meter.group(2)) + 8) * sx)
            py = int(gy + (int(meter.group(3)) + 3) * sy)
            image = Image.open('ci-protection-hud.png').convert('RGB')
            sample = [image.getpixel((px + int(i * sx), py)) for i in range(30)]
            matching = sum(1 for r, g, b in sample if 165 <= r <= 200 and 165 <= g <= 200 and 175 <= b <= 210)
            if matching < 20:
                raise RuntimeError(f'Protection meter pixels missing at {(px, py)}: {matching}/30 match, samples={sample}')
            print('PASS: suit protection meter rendered pixels, fill=', meter.group(1), 'matching=', matching, flush=True)
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
            # Exercise the actual web wings and model pose in third person, well above
            # the terrain so the suit has time to unfold before reaching the ground.
            command('tp @s 0 300 0 0 0')
            xdo('key', 'g')
            wait_for(r'Gliding suit pose applied: hero=fiskheroes:spider_man, wingsuit=true', 45,
                     'Spider-Man gliding pose affects the shared player model', process)
            wait_for(r'Drawing wingsuit membrane: texture=fiskheroes:textures/heroes/spider_man_wings\.png',
                     45, 'web-wing membrane enters the renderer', process)
            wait_for(r'Gliding body rotation applied: hero=fiskheroes:spider_man', 45,
                     'web wings rotate the complete player while gliding', process)
            time.sleep(2)
            subprocess.run(['scrot', 'ci-wingsuit-screenshot.jpg'], check=True, timeout=30)
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
            # Obsidian disappears in shadowform, including the local hand. Its cloud
            # must still emit in first person without relying on a hand/player render.
            command('tp @s 0 185 0 0 0')
            command('fill -4 179 -4 4 179 4 minecraft:stone')
            command('suit fiskheroes:obsidian')
            wait_for(r'\[CHAT\] Equipped Obsidian|\[Dev: Equipped Obsidian', 45, 'equip Obsidian', process)
            command('tp @s 0 180 0 0 0')
            xdo('key', 'c')
            wait_for(r'Ability modifier executed: hero=fiskheroes:obsidian, key=SHADOWFORM', 45,
                     'activate Obsidian shadowform', process)
            wait_for(r'First-person particle cloud emitted: fiskheroes:shadow_smoke', 45,
                     'shadow cloud emits without a rendered first-person hand', process)
            time.sleep(2)
            subprocess.run(['scrot', 'ci-shadow-screenshot.jpg'], check=True, timeout=30)
            # Batman's jump while falling deploys the cape; watch the client render the wing mesh.
            command('suit fiskheroes:batman_dceu')
            # Prepare the wall while invulnerable, in a loaded, fixed area. Otherwise the
            # player can drift into the wall or die while commands are being entered.
            command('gamemode creative')
            command('tp @s 0 300 0 0 0')
            time.sleep(3)
            command('fill -10 -64 20 10 319 20 minecraft:stone')
            command('gamemode survival')
            command('tp @s 0 300 0 0 0')
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
            # Crossbones: exercise the actual overlay in both camera modes and observe the
            # dynamic texture change driven by the server's punch-mode timer.
            command('gamemode creative')
            command('clear @s')
            command('suit fiskheroes:crossbones')
            wait_for(r'Suit arm overlay rendered: effect=fiskheroes:arm_overlay, side=RIGHT, firstPerson=null, texture=fiskheroes:textures/heroes/crossbones_gauntlets_1\.png',
                     45, 'Crossbones third-person right gauntlet render', process)
            wait_for(r'Suit arm overlay rendered: effect=fiskheroes:arm_overlay\|left, side=LEFT, firstPerson=null, texture=fiskheroes:textures/heroes/crossbones_gauntlets_1\.png',
                     45, 'Crossbones third-person left gauntlet render', process)
            xdo('key', 'c')
            wait_for(r'Ability modifier executed: hero=fiskheroes:crossbones, key=CHARGED_PUNCH',
                     45, 'activate Crossbones punch mode on server', process)
            wait_for(r'Suit arm overlay rendered: effect=fiskheroes:arm_overlay, side=RIGHT, firstPerson=null, texture=fiskheroes:textures/heroes/crossbones_gauntlets_6\.png',
                     45, 'Crossbones gauntlet animation reaches its active texture', process)
            xdo('key', 'F5')
            time.sleep(1)
            xdo('key', 'F5')
            wait_for(r'Suit arm overlay rendered: effect=fiskheroes:arm_overlay, side=RIGHT, firstPerson=true, texture=fiskheroes:textures/heroes/crossbones_gauntlets_6\.png',
                     45, 'Crossbones first-person gauntlet render', process)
            subprocess.run(['scrot', 'ci-crossbones-screenshot.jpg'], check=True, timeout=30)
            # Exercise a full-strength charged punch through the real client, server and
            # Player.attack path. An unarmoured, stationary golem starts at 100 health.
            # Teleport first so these absolute coordinates are in loaded chunks.
            command('tp @s 0 165 0 0 0')
            time.sleep(3)
            command('fill -4 159 -4 4 159 4 minecraft:stone')
            command('fill -4 160 -4 4 164 4 minecraft:air')
            command('tp @s 0 160 0 0 0')
            command('summon minecraft:iron_golem 0 160 2 {NoAI:1b,Tags:["fisk_melee_smoke"]}')
            xdo('keydown', 'Shift_L')
            time.sleep(2)
            xdo('keyup', 'Shift_L')
            time.sleep(1)
            xdo('click', '1')
            time.sleep(2)
            command('execute if data entity @e[type=minecraft:iron_golem,tag=fisk_melee_smoke,limit=1] {Health:90.5f} run say CROSSBONES_CHARGED_HIT_OK')
            wait_for(r'\[CHAT\].*CROSSBONES_CHARGED_HIT_OK', 45,
                     'real client charged punch deals the 9.5 damage profile', process)
            # A five-control suit used to extend below the screen and overlap the hotbar.
            # Read the same geometry used by the renderer and click hit testing, then use F6
            # and click the third row to activate intangibility on the server.
            command('suit fiskheroes:martian_manhunter_comics')
            wait_for(r'Ability HUD layout: hero=fiskheroes:martian_manhunter_comics, screen=',
                     45, 'five-row ability HUD layout', process)
            hud = re.search(r'Ability HUD layout: hero=fiskheroes:martian_manhunter_comics, screen=(\d+)x(\d+), titleY=(\d+), rows=\[(.*)\]',
                            log.read_text(errors='replace'))
            if not hud:
                raise RuntimeError('Missing ability HUD geometry')
            gui_width, gui_height, title_y = map(int, hud.group(1, 2, 3))
            rects = [tuple(map(int, values)) for values in re.findall(
                r'Rect\[x=(\d+), y=(\d+), width=(\d+), height=(\d+)\]', hud.group(4))]
            if len(rects) != 5:
                raise RuntimeError(f'Expected five ability controls, got {rects}')
            for x, y, w, h in rects:
                if x < 0 or y <= title_y or x + w > gui_width or y + h > gui_height - 60:
                    raise RuntimeError(f'Ability HUD row out of bounds or over the hotbar: {(x, y, w, h)}')
            for i, a in enumerate(rects):
                for b in rects[i + 1:]:
                    if a[0] < b[0] + b[2] and b[0] < a[0] + a[2] and a[1] < b[1] + b[3] and b[1] < a[1] + a[3]:
                        raise RuntimeError(f'Ability HUD rows overlap: {a}, {b}')
            geometry = subprocess.check_output(['xdotool', 'getwindowgeometry', '--shell', window], text=True, timeout=30)
            dimensions = dict(line.split('=', 1) for line in geometry.splitlines() if '=' in line)
            win_width, win_height = int(dimensions['WIDTH']), int(dimensions['HEIGHT'])
            x, y, w, h = rects[2]
            transition = r'Ability input transition: index=3, pressed=(true|false)'
            initial = len(re.findall(transition, log.read_text(errors='replace')))
            xdo('key', 'F6')
            time.sleep(1)
            xdo('mousemove', '--window', window, str(int((x + w / 2) * win_width / gui_width)),
                str(int((y + h / 2) * win_height / gui_height)))
            xdo('mousedown', '1')
            time.sleep(0.4)
            xdo('mouseup', '1')
            wait_for(r'Ability modifier executed: hero=fiskheroes:martian_manhunter_comics, key=INTANGIBILITY',
                     45, 'click third ability row and activate intangibility', process)
            wait_for_count(transition, initial + 2, 45, 'HUD click press and release', process)
            # Holding B and the same HUD control at once must not send an early release
            # when the mouse is released. Only the final B release ends the combined input.
            before = len(re.findall(transition, log.read_text(errors='replace')))
            xdo('keydown', 'b')
            wait_for_count(transition, before + 1, 45, 'keyboard ability press', process)
            xdo('mousedown', '1')
            time.sleep(0.5)
            xdo('mouseup', '1')
            time.sleep(2)
            during = re.findall(transition, log.read_text(errors='replace'))[before:]
            if during != ['true']:
                raise RuntimeError(f'Overlapping keyboard/HUD holds emitted wrong transitions: {during}')
            xdo('keyup', 'b')
            wait_for_count(transition, before + 2, 45, 'ability key release is delivered', process)
            after = re.findall(transition, log.read_text(errors='replace'))[before:]
            if after != ['true', 'false']:
                raise RuntimeError(f'Combined input did not release exactly once: {after}')
            xdo('key', 'F6')
            # Repulsors are an interaction while aiming, not an extra suit key. Verify a
            # real use click reaches the authoritative server, and cannot fire after release.
            command('clear @s')
            command('suit fiskheroes:iron_man')
            # Stand on a small high platform so this shooting test does not rely on a
            # previous hero leaving flight active or on falling past the target area.
            command('setblock 0 249 0 minecraft:stone')
            command('tp @s 0 250 0 0 0')
            wait_for(r'\[CHAT\] Equipped Iron Man|\[Dev: Equipped Iron Man', 45, 'equip Iron Man', process)
            time.sleep(2)
            shot = r'Aimed suit shot: player=Dev, modifier=fiskheroes:repulsor_blast'
            before_shots = len(re.findall(shot, log.read_text(errors='replace')))
            xdo('keydown', 'c')
            try:
                wait_for(r'Ability input accepted: hero=fiskheroes:iron_man, index=1', 45, 'Iron Man aims', process)
                time.sleep(1)
                xdo('click', '3')
                wait_for_count(shot, before_shots + 1, 45, 'aimed use fires an Iron Man repulsor', process)
            finally:
                xdo('keyup', 'c')
            time.sleep(2)
            xdo('click', '3')
            time.sleep(2)
            if len(re.findall(shot, log.read_text(errors='replace'))) != before_shots + 1:
                raise RuntimeError('Iron Man fired a repulsor after releasing aim')
            # Vision's charged beam should pass through two targets, stop at a wall and
            # render via the charged-beam model rather than the arm-projection effect.
            command('suit fiskheroes:vision')
            command('tp @s 0 180 0 0 0')
            time.sleep(2)
            command('fill -2 179 0 2 179 14 minecraft:stone')
            command('fill -2 180 0 2 184 14 minecraft:air')
            command('fill -1 181 10 1 183 10 minecraft:stone')
            # Keep the shooter at target height even if an earlier suit left flight active.
            # The beam leaves below this roof; it must still hit the two targets and the wall.
            command('setblock 0 182 0 minecraft:stone')
            command('summon minecraft:iron_golem 0.5 180 4.5 {NoAI:1b,NoGravity:1b,Tags:["beam_first"]}')
            command('summon minecraft:iron_golem 0.5 180 7.5 {NoAI:1b,NoGravity:1b,Tags:["beam_second"]}')
            command('summon minecraft:iron_golem 0.5 180 12.5 {NoAI:1b,NoGravity:1b,Tags:["beam_blocked"]}')
            command('tp @s 0 180 0 0 0')
            time.sleep(2)
            # Record actual setup coordinates and orientation to distinguish input/movement
            # problems from beam collision errors when this end-to-end check fails.
            command('data get entity @s Pos')
            command('data get entity @s Rotation')
            command('data get entity @e[type=minecraft:iron_golem,tag=beam_first,limit=1] Pos')
            command('data get entity @e[type=minecraft:iron_golem,tag=beam_second,limit=1] Pos')
            command('tp @s 0 180 0 0 0')
            firing = r'Charged beam firing: entity=Dev, charge=1\.0'
            before_firing = len(re.findall(firing, log.read_text(errors='replace')))
            xdo('keydown', 'c')
            try:
                wait_for_count(firing, before_firing + 1, 60, 'Vision charged beam fires', process)
                wait_for(r'Charged beam render path: hero=fiskheroes:vision, length=', 45,
                         'Vision charged beam reaches the client render path', process)
                time.sleep(2)
            finally:
                xdo('keyup', 'c')
            command('scoreboard objectives add beam_health dummy')
            for tag, name in [('beam_first', '#first'), ('beam_second', '#second'), ('beam_blocked', '#blocked')]:
                command(f'execute store result score {name} beam_health run data get entity @e[type=minecraft:iron_golem,tag={tag},limit=1] Health 1')
            command('execute if score #first beam_health matches ..99 if score #second beam_health matches ..99 if score #blocked beam_health matches 100 run say VISION_BEAM_TARGETS_OK')
            wait_for(r'\[CHAT\].*VISION_BEAM_TARGETS_OK', 45,
                     'charged beam damages both targets and does not pass the wall', process)
            # An unanchored player must hover while suit flight is active. Measure actual
            # server positions rather than merely checking velocity calculations in isolation.
            command('setblock 0 182 0 minecraft:air')
            command('tp @s 0 240 0 0 0')
            xdo('key', 'v')  # Vision's intangibility enables controlled flight.
            wait_for_count(r'Ability modifier executed: hero=fiskheroes:vision, key=INTANGIBILITY',
                           2, 45, 'activate Vision flight for hover test', process)
            time.sleep(3)
            command('scoreboard objectives add flight_test dummy')
            command('execute store result score #start flight_test run data get entity @s Pos[1] 100')
            time.sleep(3)
            command('execute store result score #end flight_test run data get entity @s Pos[1] 100')
            command('scoreboard players operation #delta flight_test = #end flight_test')
            command('scoreboard players operation #delta flight_test -= #start flight_test')
            command('scoreboard players get #delta flight_test')
            command('execute if score #delta flight_test matches -75..75 run say SUIT_FLIGHT_HOVER_OK')
            wait_for(r'\[CHAT\].*SUIT_FLIGHT_HOVER_OK', 45, 'unanchored controlled flight hovers', process)
            command('execute store result score #start flight_test run data get entity @s Pos[1] 100')
            xdo('keydown', 'space')
            time.sleep(2)
            xdo('keyup', 'space')
            command('execute store result score #end flight_test run data get entity @s Pos[1] 100')
            command('scoreboard players operation #delta flight_test = #end flight_test')
            command('scoreboard players operation #delta flight_test -= #start flight_test')
            command('scoreboard players get #delta flight_test')
            command('execute if score #delta flight_test matches 50.. run say SUIT_FLIGHT_ASCENT_OK')
            wait_for(r'\[CHAT\].*SUIT_FLIGHT_ASCENT_OK', 45, 'jump input moves the flying player upward', process)
            command('execute store result score #start flight_test run data get entity @s Pos[2] 100')
            xdo('keydown', 'w')
            time.sleep(2)
            xdo('keyup', 'w')
            command('execute store result score #end flight_test run data get entity @s Pos[2] 100')
            command('scoreboard players operation #delta flight_test = #end flight_test')
            command('scoreboard players operation #delta flight_test -= #start flight_test')
            command('scoreboard players get #delta flight_test')
            command('execute if score #delta flight_test matches 50.. run say SUIT_FLIGHT_FORWARD_OK')
            wait_for(r'\[CHAT\].*SUIT_FLIGHT_FORWARD_OK', 45, 'forward input steers the flying player', process)
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
