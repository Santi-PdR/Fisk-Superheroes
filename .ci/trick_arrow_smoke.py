#!/usr/bin/env python3
"""Exercise potion and firework arrow impacts with a real client and dedicated server."""
import os
import math
from PIL import Image
import re
import signal
import subprocess
import time
from pathlib import Path
from small_player_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciArrowDiagnostics=true -Dfiskheroes.ciBowDiagnostics=true -Dfiskheroes.ciPerformanceDiagnostics=true -Dfiskheroes.ciParticleDiagnostics=true')
    server_log, client_log = Path('ci-arrow-server.log'), Path('ci-arrow-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-arrow-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-arrow-client',
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
            def shoot(capture):
                xdo('mousedown', '3'); time.sleep(1.7)
                subprocess.run(['scrot', capture], check=True, timeout=30)
                xdo('mouseup', '3'); time.sleep(0.45)
                subprocess.run(['scrot', capture.replace('.png', '-impact.png')], check=True, timeout=30)
                time.sleep(1.5)
            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('fill -4 179 -4 4 179 18 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('item replace entity Dev weapon.mainhand with minecraft:bow')
            command('item replace entity Dev weapon.offhand with fiskheroes:trick_arrow{ArrowType:"vial",AttachedPotion:{id:"minecraft:splash_potion",Count:1b,tag:{Potion:"minecraft:swiftness"}}}')
            command('summon minecraft:iron_golem 0.5 180 8.5 {NoAI:1b,Silent:1b,Tags:["vial_target"]}')
            shoot('ci-arrow-vial.png')
            command('data get entity @e[tag=vial_target,limit=1]')
            command('data get entity Dev Inventory')
            command('execute as @e[type=fiskheroes:trick_arrow] run data get entity @s')
            command('execute if entity @e[tag=vial_target,nbt={ActiveEffects:[{Id:1}]}] run say ARROW_VIAL_SWIFTNESS_PASS')
            wait_for(server_log, r'\[Server\] ARROW_VIAL_SWIFTNESS_PASS', 15, 'vial arrow applies crafted effect', server)
            command('kill @e[tag=vial_target]')
            command('tp Dev 0.5 180 0.5 0 0')
            command('item replace entity Dev weapon.offhand with fiskheroes:trick_arrow{ArrowType:"firework",AttachedItem:{id:"minecraft:firework_rocket",Count:1b,tag:{Fireworks:{Flight:3b,Explosions:[{Type:0b,Colors:[I;15790320]}]}}}}')
            command('summon minecraft:iron_golem 0.5 180 8.5 {NoAI:1b,Silent:1b,Tags:["firework_target"]}')
            shoot('ci-arrow-firework.png')
            command('execute if entity @e[tag=firework_target,nbt={Health:100.0f,ActiveEffects:[{Id:15}]}] run say ARROW_FIREWORK_BLIND_PASS')
            wait_for(server_log, r'\[Server\] ARROW_FIREWORK_BLIND_PASS', 15, 'firework flashes target without arrow damage', server)
            wait_for(client_log, r'Firework arrow rendered payload explosions: 1', 15, 'colored rocket effect rendered by real client', client)
            command('kill @e[tag=firework_target]')
            command('fill -2 180 8 2 184 8 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 -8')
            command('item replace entity Dev weapon.offhand with fiskheroes:trick_arrow{ArrowType:"ender_pearl"}')
            shoot('ci-arrow-ender.png')
            command('data get entity Dev Pos')
            command('execute positioned 0.5 180 7.5 if entity @a[name=Dev,distance=..2] run say ARROW_ENDER_WALL_PASS')
            wait_for(server_log, r'\[Server\] ARROW_ENDER_WALL_PASS', 15, 'ender arrow teleports near actual wall impact', server)
            command('tp Dev 0.5 180 0.5 0 0')
            command('fill -2 180 6 2 184 7 minecraft:stone')
            command('item replace entity Dev weapon.offhand with fiskheroes:trick_arrow{ArrowType:"glitch"}')
            command('summon minecraft:iron_golem 0.5 180 12.5 {NoAI:1b,Silent:1b,Tags:["glitch_target"]}')
            shoot('ci-arrow-glitch.png')
            command('scoreboard objectives add arrow_health dummy')
            command('execute store result score target arrow_health run data get entity @e[tag=glitch_target,limit=1] Health 1')
            command('execute if score target arrow_health matches ..99 run say ARROW_GLITCH_WALL_PASS')
            wait_for(server_log, r'\[Server\] ARROW_GLITCH_WALL_PASS', 15, 'glitch arrow damages a target beyond a thick wall', server)
            wait_for(client_log, r'Trick arrow original 3D model rendered', 15, 'trick arrow model rendered by real client', client)
            # Modern levers have no projectile collision: hit the supporting wall and verify
            # that the adjacent lever remains untouched after the temporary pulse.
            command('kill @e[type=fiskheroes:trick_arrow]')
            command('kill @e[tag=glitch_target]')
            command('fill -2 180 3 2 184 8 minecraft:air')
            command('fill -2 180 4 2 183 4 minecraft:stone')
            command('setblock 1 181 3 minecraft:lever[face=wall,facing=north,powered=false]')
            command('tp Dev 0.5 180 0.5 0 0')
            command('item replace entity Dev weapon.offhand with fiskheroes:trick_arrow{ArrowType:"pulse"}')
            shoot('ci-arrow-pulse.png')
            command('execute as @e[type=fiskheroes:trick_arrow,nbt={ArrowType:"pulse"}] run data get entity @s')
            wait_for(server_log, r'Pulse arrow impact: block=minecraft:stone, signal=true', 20,
                     'pulse arrow produces a temporary signal on the wall', server)
            command('execute if block 1 181 3 minecraft:lever[powered=false] run say PULSE_LEVER_UNCHANGED_PASS')
            wait_for(server_log, r'\[Server\] PULSE_LEVER_UNCHANGED_PASS', 15,
                     'pulse arrow does not permanently toggle the lever', server)
            # Move the struck target before the fuse expires; the explosion should follow it.
            command('kill @e[type=fiskheroes:trick_arrow]')
            command('fill -2 180 3 2 184 8 minecraft:air')
            command('tp Dev 0.5 180 0.5 0 0')
            command('item replace entity Dev weapon.offhand with fiskheroes:trick_arrow{ArrowType:"explosive_pufferfish"}')
            command('summon minecraft:iron_golem 0.5 180 4.5 {NoAI:1b,Silent:1b,Tags:["puffer_target"]}')
            xdo('mousedown', '3'); time.sleep(1.7); xdo('mouseup', '3')
            time.sleep(.15)
            command('tp @e[tag=puffer_target,limit=1] 3.5 180 4.5')
            wait_for(server_log, r'Pufferfish arrow detonation: x=3\.5, y=[0-9.]+, z=4\.5, attached=true', 20,
                     'pufferfish explosion follows the moving target', server)
            # mobGriefing restricts mobs, not a player's torch and fire-charge arrows.
            command('kill @e[tag=puffer_target]')
            command('kill @e[type=fiskheroes:trick_arrow]')
            command('gamerule mobGriefing false')
            command('fill -2 180 4 2 183 4 minecraft:stone')
            command('setblock 0 180 3 minecraft:netherrack')
            command('tp Dev 0.5 180 0.5 0 0')
            command('item replace entity Dev weapon.offhand with fiskheroes:trick_arrow{ArrowType:"torch"}')
            shoot('ci-arrow-torch.png')
            command('execute if block 0 181 3 minecraft:wall_torch[facing=north] run say PLAYER_TORCH_ARROW_PASS')
            wait_for(server_log, r'\[Server\] PLAYER_TORCH_ARROW_PASS', 15,
                     'player torch arrow places a wall torch with mobGriefing disabled', server)
            command('kill @e[type=fiskheroes:trick_arrow]')
            command('setblock 0 181 3 minecraft:air')
            command('item replace entity Dev weapon.offhand with fiskheroes:trick_arrow{ArrowType:"fire_charge"}')
            shoot('ci-arrow-fire-charge.png')
            command('execute if block 0 181 3 minecraft:fire run say PLAYER_FIRE_ARROW_PASS')
            wait_for(server_log, r'\[Server\] PLAYER_FIRE_ARROW_PASS', 15,
                     'player fire-charge arrow places fire with mobGriefing disabled', server)
            # A direct hit must be FIREBALL damage, not vanilla ARROW damage. Use a blaze
            # for a real fire-immune target, then prove a normal arrow can hurt the same mob.
            command('kill @e[type=fiskheroes:trick_arrow]')
            command('fill -2 180 3 2 184 8 minecraft:air')
            command('difficulty normal')
            command('tp Dev 0.5 180 0.5 0 0')
            command('summon minecraft:blaze 0.5 180 5.5 {NoAI:1b,NoGravity:1b,Silent:1b,PersistenceRequired:1b,Tags:["fire_immune_target"]}')
            command('item replace entity Dev weapon.offhand with fiskheroes:trick_arrow{ArrowType:"fireball"}')
            shoot('ci-arrow-fireball-immune.png')
            wait_for(server_log, r'Fireball arrow direct impact: target=minecraft:blaze', 15,
                     'fireball arrow directly hits the blaze', server)
            command('data get entity @e[tag=fire_immune_target,limit=1]')
            command('execute if entity @e[tag=fire_immune_target,nbt={Health:20.0f}] run say FIREBALL_DIRECT_IMMUNITY_PASS')
            wait_for(server_log, r'\[Server\] FIREBALL_DIRECT_IMMUNITY_PASS', 15,
                     'fireball direct hit detonates without damaging a fire-immune blaze', server)
            command('kill @e[type=fiskheroes:trick_arrow]')
            command('item replace entity Dev weapon.offhand with minecraft:arrow')
            shoot('ci-arrow-ordinary-control.png')
            command('execute store result score target arrow_health run data get entity @e[tag=fire_immune_target,limit=1] Health 1')
            command('execute if score target arrow_health matches ..19 run say NORMAL_ARROW_BLAZE_CONTROL_PASS')
            wait_for(server_log, r'\[Server\] NORMAL_ARROW_BLAZE_CONTROL_PASS', 15,
                     'ordinary arrow still damages the same blaze', server)
            # Exercise script-heavy model variables and continuously changing texture getters
            # in the actual renderer. Diagnostic counters are enabled only in this test.
            command('kill @e[tag=fire_immune_target]')
            command('difficulty peaceful')
            command('execute as Dev run suit fiskheroes:iron_man_mk85')
            xdo('key', 'F5')
            model_pattern = r'Expression cache: owner=model:fiskheroes:iron_man_mk85, lookups=100, compilations=([0-9]+)'
            wait_for(client_log, model_pattern, 60, 'Iron Man model expressions are reused over 100 lookups', client)
            route_pattern = r'Texture route cache: lookups=100, resolutions=([0-9]+)'
            wait_for(client_log, route_pattern, 60, 'texture resource routes are reused over 100 lookups', client)
            route_cache = re.search(route_pattern, client_log.read_text(errors='replace'))
            if int(route_cache.group(1)) >= 80:
                raise RuntimeError('Too many repeated texture route resolutions: ' + route_cache.group(1))
            print('ROUTE_CACHE: 100 lookups, ' + route_cache.group(1) + ' resolutions', flush=True)
            model_cache = re.search(model_pattern, client_log.read_text(errors='replace'))
            if int(model_cache.group(1)) >= 30:
                raise RuntimeError('Too many model expression compilations: ' + model_cache.group(1))
            print('MODEL_CACHE: 100 lookups, ' + model_cache.group(1) + ' compilations', flush=True)
            command('execute as Dev run suit fiskheroes:savitar rgb')
            texture_pattern = r'Expression cache: owner=textures, lookups=100, compilations=([0-9]+)'
            wait_for(client_log, texture_pattern, 90, 'animated Savitar texture expressions are reused over 100 lookups', client)
            texture_cache = re.search(texture_pattern, client_log.read_text(errors='replace'))
            if int(texture_cache.group(1)) >= 30:
                raise RuntimeError('Too many texture expression compilations: ' + texture_cache.group(1))
            print('TEXTURE_CACHE: 100 lookups, ' + texture_cache.group(1) + ' compilations', flush=True)
            animated_values = re.findall(r'Texture expression value: expression=\(entity\.ticksExisted\(\) \* 6 % 360\) \| 0, value=([0-9]+)',
                                         client_log.read_text(errors='replace'))
            if len(set(animated_values)) < 2:
                raise RuntimeError('Savitar RGB texture did not produce changing tick values: ' + str(animated_values))
            print('PASS: animated texture getter returned changing values: ' + ', '.join(animated_values[:2]), flush=True)
            xdo('keydown', 'c'); time.sleep(.3); xdo('keyup', 'c')
            wait_for(server_log, r'Ability input accepted: hero=fiskheroes:savitar_rgb, index=1', 30,
                     'speed ability input reaches the server', server)
            wait_for(server_log, r'Ability modifier executed: hero=fiskheroes:savitar, key=SUPER_SPEED, modifier=fiskheroes:super_speed', 30,
                     'speed ability still activates after the optimizations', server)
            # Killer Frost's held charge must create cyan smoke, not snowflake sprites, in
            # both camera perspectives. Wait for the actual particle engine to create it.
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('item replace entity Dev weapon.offhand with minecraft:air')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:killer_frost')
            # Wait for the suit packet and render before holding C; a server command can
            # complete before the client stops interpreting the key as Savitar's power.
            wait_for(client_log, r'Drawing suit fiskheroes:killer_frost slot', 45,
                     'Killer Frost suit synchronized to the client', client)
            xdo('keydown', 'c')
            try:
                wait_for(server_log, r'Ability modifier executed: hero=fiskheroes:killer_frost, key=CHARGE_ICE',
                         30, 'Killer Frost charge reaches the server', server)
                wait_for(client_log, r'Suit emitter smoke rendered: type=CRYO_SMOKE, firstPerson=false',
                         45, 'Killer Frost emits cryogenic smoke in third person', client)
                anchor_pattern = (r'Suit emitter anchor: type=CRYO_SMOKE, anchor=rightArm, side=([01]), '
                                  r'yaw=([-+0-9.eE]+), viewYaw=([-+0-9.eE]+), viewPitch=([-+0-9.eE]+), '
                                  r'dx=([-+0-9.eE]+), dy=([-+0-9.eE]+), dz=([-+0-9.eE]+)')
                wait_for(client_log, anchor_pattern.replace('([01])', '(1)'), 45,
                         'both third-person arm anchors transformed', client)

                def read_anchors(view_yaw):
                    results = {}
                    for side, yaw, cam_yaw, pitch, dx, dy, dz in re.findall(anchor_pattern, client_log.read_text(errors='replace')):
                        values = tuple(map(float, (yaw, cam_yaw, pitch, dx, dy, dz)))
                        if abs(values[1] - view_yaw) < 3:
                            results[int(side)] = values
                    return results

                def verify_anchors(anchors, expected_pitch):
                    if set(anchors) != {0, 1}:
                        raise RuntimeError(f'Missing mirrored cryogenic anchors: {anchors}')
                    for side, (yaw, cam_yaw, pitch, dx, dy, dz) in anchors.items():
                        # Normalize by body yaw, which can differ from camera yaw while idle.
                        radians = math.radians(yaw)
                        local_x = dx * math.cos(radians) + dz * math.sin(radians)
                        local_z = -dx * math.sin(radians) + dz * math.cos(radians)
                        if (abs(pitch - expected_pitch) > 3 or not .4 < dy < 1.4 or abs(local_z) > .3
                                or not ((-.6 < local_x < -.2) if side == 0 else (.2 < local_x < .6))):
                            raise RuntimeError(f'Cryogenic smoke origin does not follow its hand: {anchors}')

                anchors = read_anchors(0)
                verify_anchors(anchors, 0)
                print(f'PASS: third-person smoke anchors track right and left hands: {anchors}', flush=True)
                time.sleep(1.5)
                subprocess.run(['scrot', 'ci-arrow-killer-frost-smoke-third.png'], check=True, timeout=30)
                # Turn and pitch the camera. The avatar's idle body yaw may lag its head,
                # but world-space emission must still track each actual arm.
                command('tp Dev 0.5 180 0.5 90 20')
                turned_pattern = anchor_pattern.replace('viewYaw=([-+0-9.eE]+)', 'viewYaw=(8[7-9][0-9.eE+-]*|9[0-3][0-9.eE+-]*)')
                wait_for(client_log, turned_pattern.replace('([01])', '(1)'), 45,
                         'third-person arm anchors transformed after camera turn', client)
                turned = read_anchors(90)
                verify_anchors(turned, 20)
                print(f'PASS: third-person smoke follows hands with a turned and pitched camera: {turned}', flush=True)
            finally:
                xdo('keyup', 'c')
            command('tp Dev 0.5 180 0.5 0 0')
            xdo('key', 'F5'); time.sleep(.5); xdo('key', 'F5'); time.sleep(2)
            subprocess.run(['scrot', 'ci-arrow-killer-frost-smoke-before.png'], check=True, timeout=30)
            # Compare against the same camera and HUD before charging. A particle-engine
            # creation log by itself does not prove any smoke is visible on screen.
            geometry = subprocess.check_output(['xdotool', 'getwindowgeometry', '--shell', window], text=True)
            values = dict(line.split('=', 1) for line in geometry.splitlines() if '=' in line)
            wx, wy, width, height = (int(values[key]) for key in ('X', 'Y', 'WIDTH', 'HEIGHT'))
            region = (wx + int(width * .51), wy + int(height * .64),
                      wx + int(width * .99), wy + int(height * .98))
            before = Image.open('ci-arrow-killer-frost-smoke-before.png').convert('RGB').crop(region)
            xdo('keydown', 'c')
            try:
                wait_for(client_log, r'Suit emitter smoke rendered: type=CRYO_SMOKE, firstPerson=true',
                         45, 'Killer Frost emits cryogenic smoke in first person', client)
                visible = 0
                # Smoke is generated at the camera plane and its random drift can
                # temporarily carry it outside the frustum. Sample several independent
                # frames while keeping the same visibility threshold.
                for _ in range(12):
                    time.sleep(.5)
                    subprocess.run(['scrot', '-o', 'ci-arrow-killer-frost-smoke-first.png'], check=True, timeout=30)
                    after = Image.open('ci-arrow-killer-frost-smoke-first.png').convert('RGB').crop(region)
                    visible = sum(1 for (r, g, b), (r0, g0, b0) in zip(after.getdata(), before.getdata())
                                  if r > 175 and g > 225 and b > 225 and g - r >= 5
                                  and abs(r-r0) + abs(g-g0) + abs(b-b0) > 35)
                    if visible >= 200:
                        break
                if visible < 200:
                    raise RuntimeError(f'First-person cryogenic smoke was not visibly present: {visible} changed cyan pixels')
                print(f'PASS: visible first-person cryogenic smoke ({visible} changed cyan pixels)', flush=True)
            finally:
                xdo('keyup', 'c')
            # Obsidian's chain must begin at the eye-relative first-person offset,
            # then follow the rendered arm when changing to third person.
            command('kill @e[type=minecraft:iron_golem]')
            command('kill @e[type=minecraft:blaze]')
            command('tp Dev 0.5 180 0.5 0 0')
            command('summon minecraft:iron_golem 0.5 180 7.5 {NoAI:1b,Silent:1b,Tags:["chain_target"]}')
            command('execute as Dev run suit fiskheroes:obsidian')
            wait_for(client_log, r'First-person suit arm rendered: hero=fiskheroes:obsidian,', 45,
                     'Obsidian suit synchronized to the client', client)
            xdo('keydown', 'v')
            try:
                wait_for(server_log, r'Ability modifier executed: hero=fiskheroes:obsidian, key=TELEKINESIS',
                         30, 'Obsidian telekinesis activates', server)
                pattern = (r'Telekinesis chain origin: firstPerson=(true|false), yaw=([-+0-9.eE]+), '
                           r'pitch=([-+0-9.eE]+), dx=([-+0-9.eE]+), dy=([-+0-9.eE]+), dz=([-+0-9.eE]+)')
                wait_for(client_log, pattern.replace('(true|false)', 'true'), 45,
                         'first-person telekinesis chain follows a grabbed target', client)
                match = re.search(pattern.replace('(true|false)', '(true)'), client_log.read_text(errors='replace'))
                _, yaw, pitch, dx, dy, dz = match.groups()
                if (abs(float(yaw)) > 2 or abs(float(pitch)) > 2 or abs(float(dx) + .296875) > .05
                        or abs(float(dy) - 1.37) > .15 or abs(float(dz) - .53125) > .05):
                    raise RuntimeError('Incorrect eye-relative telekinesis origin: ' + match.group())
                time.sleep(1)
                subprocess.run(['scrot', '-o', 'ci-arrow-chain-first.png'], check=True, timeout=30)
                command('tp @e[tag=chain_target,limit=1] 2.5 180 7.5')
                moving = (r'Telekinesis chain particle tracks moving target: targetDelta=([-+0-9.eE]+), '
                          r'pointDelta=([-+0-9.eE]+), progress=([-+0-9.eE]+)')
                wait_for(client_log, moving, 30, 'existing chain particles follow a moving target', client)
                motion = re.search(moving, client_log.read_text(errors='replace'))
                target_delta, point_delta, progress = map(float, motion.groups())
                if target_delta < 1 or point_delta < .1 or not .4 <= progress <= .6:
                    raise RuntimeError('Chain particle did not track the moved target: ' + motion.group())
                subprocess.run(['scrot', '-o', 'ci-arrow-chain-moved.png'], check=True, timeout=30)
                print(f'PASS: existing chain particles track target displacement {target_delta:.2f} '
                      f'with curve displacement {point_delta:.2f}', flush=True)
                xdo('key', 'F5')
                wait_for(client_log, pattern.replace('(true|false)', 'false'), 45,
                         'third-person telekinesis chain follows a grabbed target', client)
                match = re.search(pattern.replace('(true|false)', '(false)'), client_log.read_text(errors='replace'))
                _, yaw, pitch, dx, dy, dz = match.groups()
                if not (-.8 < float(dx) < -.1 and .2 < float(dy) < 1.7 and abs(float(dz)) < .8):
                    raise RuntimeError('Incorrect rendered-arm telekinesis origin: ' + match.group())
                time.sleep(1)
                subprocess.run(['scrot', '-o', 'ci-arrow-chain-third.png'], check=True, timeout=30)
                print('PASS: telekinesis chain originates at the eye-relative hand and rendered arm', flush=True)
            finally:
                xdo('keyup', 'v')
            # The Monitor's blue telekinesis aura belongs to the grabbed target,
            # not to the caster. First check that holding the key without a target
            # does not create an aura, then grab a golem and inspect the pixels.
            xdo('key', 'F5'); time.sleep(.5); xdo('key', 'F5'); time.sleep(1)
            command('kill @e[type=minecraft:iron_golem]')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:the_monitor')
            wait_for(client_log, r'First-person suit arm rendered: hero=fiskheroes:the_monitor,', 45,
                     'Monitor suit synchronized to the client', client)
            xdo('keydown', 'v')
            try:
                wait_for(server_log, r'Ability modifier executed: hero=fiskheroes:the_monitor, key=TELEKINESIS',
                         30, 'Monitor telekinesis activates without target', server)
                time.sleep(2)
                if 'Telekinesis aura emitted around target:' in client_log.read_text(errors='replace'):
                    raise RuntimeError('Monitor emitted telekinesis aura without a grabbed target')
                print('PASS: no telekinesis aura without a grabbed target', flush=True)
            finally:
                xdo('keyup', 'v')
            command('summon minecraft:iron_golem 0.5 180 7.5 {NoAI:1b,Silent:1b,Tags:["monitor_target"]}')
            time.sleep(1)
            subprocess.run(['scrot', '-o', 'ci-arrow-monitor-aura-before.png'], check=True, timeout=30)
            baseline = Image.open('ci-arrow-monitor-aura-before.png').convert('RGB').crop(
                    (wx + int(width * .3), wy + int(height * .2), wx + int(width * .7), wy + int(height * .8)))
            xdo('keydown', 'v')
            try:
                wait_for(client_log, r'Telekinesis aura emitted around target: type=fiskheroes:telekinesis_monitor, '
                         r'target=entity.minecraft.iron_golem, count=[0-9]+, distance=', 45,
                         'Monitor aura attaches to the grabbed golem', client)
                visible = 0
                for _ in range(8):
                    time.sleep(.5)
                    subprocess.run(['scrot', '-o', 'ci-arrow-monitor-aura.png'], check=True, timeout=30)
                    after = Image.open('ci-arrow-monitor-aura.png').convert('RGB').crop(
                            (wx + int(width * .3), wy + int(height * .2), wx + int(width * .7), wy + int(height * .8)))
                    visible = sum(1 for (r, g, b), (r0, g0, b0) in zip(after.getdata(), baseline.getdata())
                                  if r < 110 and g > 100 and b > 150 and g > r + 50 and b > r + 70
                                  and abs(r-r0) + abs(g-g0) + abs(b-b0) > 50)
                    if visible >= 80: break
                if visible < 80:
                    raise RuntimeError(f'Monitor target aura was not visibly blue: {visible} changed pixels')
                print(f'PASS: visible blue telekinesis aura around target ({visible} changed pixels)', flush=True)
            finally:
                xdo('keyup', 'v')
            # Check both radial endpoints and their animated colors in third person.
            command('kill @e[tag=monitor_target]')
            command('fill -2 180 6 2 184 6 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            xdo('key', 'F5'); time.sleep(1)
            xdo('keydown', 'b'); time.sleep(.25)
            subprocess.run(['scrot', '-o', 'ci-arrow-breach-cloud.png'], check=True, timeout=30)
            xdo('keyup', 'b')
            wait_for(server_log, r'Ability modifier executed: hero=fiskheroes:the_monitor, key=TELEPORT',
                     30, 'Monitor teleport activates', server)
            burst = (r'Teleport burst emitted: endpoint=(origin|destination), firstPerson=false, '
                     r'center=\(([-+0-9.eE]+), ([-+0-9.eE]+), ([-+0-9.eE]+)\), count=([0-9]+), cloud=fiskheroes:breach')
            wait_for(client_log, burst.replace('(origin|destination)', 'destination'), 30,
                     'teleport burst reaches the destination', client)
            bursts = {endpoint: (float(x), float(y), float(z), int(count)) for endpoint, x, y, z, count
                      in re.findall(burst, client_log.read_text(errors='replace'))}
            if (set(bursts) != {'origin', 'destination'} or bursts['origin'][3] != 200
                    or bursts['destination'][3] != 200 or bursts['destination'][2] - bursts['origin'][2] < 3):
                raise RuntimeError(f'Teleport must emit separate 200-particle endpoint bursts: {bursts}')
            print(f'PASS: teleport emitted radial bursts at both endpoints: {bursts}', flush=True)
            gradient = (r'Breach burst animated color: startR=([-+0-9.eE]+), startG=([-+0-9.eE]+), '
                        r'startB=([-+0-9.eE]+), midR=([-+0-9.eE]+), midG=([-+0-9.eE]+), midB=([-+0-9.eE]+)')
            wait_for(client_log, gradient, 45, 'breach cloud particles animate their configured colors', client)
            colors = tuple(map(float, re.search(gradient, client_log.read_text(errors='replace')).groups()))
            if not (colors[:3] == (1.0, 1.0, 1.0) and 0 <= colors[3] <= .2
                    and .2 <= colors[4] <= .7 and colors[5] == 1.0):
                raise RuntimeError(f'Incorrect animated breach color endpoints: {colors}')
            print(f'PASS: breach cloud transitions from white to sampled blue: {colors}', flush=True)
            # Switch through front-facing third person to first person and repeat. The
            # legacy renderer uses a sparser 5 by 10 lattice for the local player.
            xdo('key', 'F5'); time.sleep(.5); xdo('key', 'F5'); time.sleep(1)
            command('tp Dev 0.5 180 0.5 0 0')
            # The server may be behind the client under load; a fixed sleep can send
            # the ability input before the reset teleport has actually executed.
            command('say TELEPORT_FP_RESET_READY')
            wait_for(server_log, r'\[Server\] TELEPORT_FP_RESET_READY', 30,
                     'first-person teleport reset completed on server', server)
            xdo('keydown', 'b'); time.sleep(.25)
            subprocess.run(['scrot', '-o', 'ci-arrow-breach-first.png'], check=True, timeout=30)
            xdo('keyup', 'b')
            first_burst = (r'Teleport burst emitted: endpoint=(origin|destination), firstPerson=true, '
                           r'center=\(([-+0-9.eE]+), ([-+0-9.eE]+), ([-+0-9.eE]+)\), count=([0-9]+), cloud=fiskheroes:breach')
            wait_for(client_log, first_burst.replace('(origin|destination)', 'destination'), 30,
                     'first-person teleport burst reaches destination', client)
            first_bursts = {endpoint: (float(x), float(y), float(z), int(count)) for endpoint, x, y, z, count
                            in re.findall(first_burst, client_log.read_text(errors='replace'))}
            if (set(first_bursts) != {'origin', 'destination'} or first_bursts['origin'][3] != 50
                    or first_bursts['destination'][3] != 50 or first_bursts['destination'][2] - first_bursts['origin'][2] < 3):
                raise RuntimeError(f'First-person teleport must emit two 50-particle bursts: {first_bursts}')
            print(f'PASS: first-person teleport uses two 50-particle bursts: {first_bursts}', flush=True)
            # Firestorm's three-piece suit has no helmet. Its flames still require a
            # complete set, must render on the first-person arm and on the full model,
            # and each fire sheet layer must animate its own square sprite frames.
            command('tp Dev 0.5 180 0.5 0 0')
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('execute as Dev run suit fiskheroes:firestorm')
            wait_for(client_log, r'Suit flames rendered: hero=fiskheroes:firestorm, wearer=Dev, view=first-right',
                     60, 'three-piece Firestorm suit renders first-person flames', client)
            for layer, first in ((0, 16), (1, 0)):
                pattern = (rf'Booster sprite animation: texture=fiskheroes:textures/icons/fire_layer_{layer}\.png, '
                           rf'columns=1, rows=32, frames=32, first={first}, second={first + 1}')
                wait_for(client_log, pattern, 30, f'fire layer {layer} animates 32 separate frames', client)
            time.sleep(1)
            subprocess.run(['scrot', '-o', 'ci-arrow-firestorm-first.png'], check=True, timeout=30)
            xdo('key', 'F5')
            wait_for(client_log, r'Suit flames rendered: hero=fiskheroes:firestorm, wearer=Dev, view=third',
                     45, 'Firestorm renders third-person flames', client)
            time.sleep(1)
            subprocess.run(['scrot', '-o', 'ci-arrow-firestorm-third.png'], check=True, timeout=30)
            command('item replace entity Dev armor.feet with minecraft:air')
            wait_for(client_log, r'Suit flames suppressed: hero=fiskheroes:firestorm, wearer=Dev, reason=incomplete_suit',
                     45, 'Firestorm flames stop for an incomplete suit', client)
            time.sleep(1)
            subprocess.run(['scrot', '-o', 'ci-arrow-firestorm-incomplete.png'], check=True, timeout=30)
            xdo('key', 'F5'); time.sleep(.5); xdo('key', 'F5'); time.sleep(1)
            subprocess.run(['scrot', '-o', 'ci-arrow-firestorm-incomplete-first.png'], check=True, timeout=30)
            def flame_pixels(full, incomplete, box):
                before = Image.open(incomplete).convert('RGB').crop(box)
                after = Image.open(full).convert('RGB').crop(box)
                return sum(1 for (r,g,b), (r0,g0,b0) in zip(after.getdata(), before.getdata())
                           if r > 65 and r > g + 8 and g > b + 5 and abs(r-r0) + abs(g-g0) + abs(b-b0) > 35)
            third_box = (wx + int(width*.36), wy + int(height*.20), wx + int(width*.64), wy + int(height*.60))
            first_box = (wx + int(width*.50), wy + int(height*.40), wx + int(width*.99), wy + int(height*.99))
            third_pixels = flame_pixels('ci-arrow-firestorm-third.png', 'ci-arrow-firestorm-incomplete.png', third_box)
            first_pixels = flame_pixels('ci-arrow-firestorm-first.png', 'ci-arrow-firestorm-incomplete-first.png', first_box)
            if third_pixels < 40 or first_pixels < 15:
                raise RuntimeError(f'Firestorm flames must be visibly orange in both views: third={third_pixels}, first={first_pixels}')
            print(f'PASS: visible Firestorm flames against incomplete-suit control: third={third_pixels}, first={first_pixels}', flush=True)
            # Martian Manhunter's heat vision has one pack beam mirrored at both eyes.
            xdo('key', 'F5'); time.sleep(1)
            command('execute as Dev run suit fiskheroes:martian_manhunter_comics')
            command('tp Dev 0.5 180 0.5 0 0')
            command('say HEAT_VISION_SCENE_READY')
            wait_for(server_log, r'\[Server\] HEAT_VISION_SCENE_READY', 30, 'heat-vision scene ready', server)
            xdo('keydown', 'g')
            try:
                wait_for(client_log, r'Beam constellation rendered: hero=fiskheroes:martian_manhunter_comics, heat=true, firstPerson=false, beams=2',
                         60, 'Martian heat vision renders from both eyes in third person', client)
                subprocess.run(['scrot', '-o', 'ci-arrow-heat-third.png'], check=True, timeout=30)
                xdo('key', 'F5'); time.sleep(.5); xdo('key', 'F5')
                wait_for(client_log, r'Beam constellation rendered: hero=fiskheroes:martian_manhunter_comics, heat=true, firstPerson=true, beams=2',
                         60, 'Martian heat vision renders both first-person beams', client)
                subprocess.run(['scrot', '-o', 'ci-arrow-heat-first.png'], check=True, timeout=30)
            finally:
                xdo('keyup', 'g')
            # The Mk 50 cannon's component glow uses aimed_timer, not the hitscan
            # projection timer. It must appear while aiming without firing a shot.
            command('execute as Dev run suit fiskheroes:iron_man_mk50')
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('item replace entity Dev weapon.offhand with minecraft:air')
            command('say MK50_SCENE_READY')
            wait_for(server_log, r'\[Server\] MK50_SCENE_READY', 30, 'Mk 50 scene ready', server)
            xdo('keydown', 'g'); time.sleep(.2); xdo('keyup', 'g'); time.sleep(3)
            xdo('keydown', 'c')
            try:
                wait_for(client_log, r'Component beam rendered: hero=fiskheroes:iron_man_mk50, firstPerson=true, amount=([-+0-9.eE]+), beams=5',
                         60, 'Mk 50 cannon component beams render while aiming without a projection shot', client)
                subprocess.run(['scrot', '-o', 'ci-arrow-mk50-first.png'], check=True, timeout=30)
                xdo('key', 'F5')
                wait_for(client_log, r'Component beam rendered: hero=fiskheroes:iron_man_mk50, firstPerson=false, amount=([-+0-9.eE]+), beams=5',
                         60, 'Mk 50 cannon component beams render in third person', client)
                subprocess.run(['scrot', '-o', 'ci-arrow-mk50-third.png'], check=True, timeout=30)
            finally:
                xdo('keyup', 'c')
            print('PASS: real client fired fireball and ordinary arrow controls against dedicated server', flush=True)
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
