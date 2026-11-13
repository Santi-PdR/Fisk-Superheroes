#!/usr/bin/env python3
"""Exercise potion and firework arrow impacts with a real client and dedicated server."""
import os
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
                time.sleep(1.5)
                subprocess.run(['scrot', 'ci-arrow-killer-frost-smoke-third.png'], check=True, timeout=30)
            finally:
                xdo('keyup', 'c')
            xdo('key', 'F5'); time.sleep(.5); xdo('key', 'F5'); time.sleep(.5)
            xdo('keydown', 'c')
            try:
                wait_for(client_log, r'Suit emitter smoke rendered: type=CRYO_SMOKE, firstPerson=true',
                         45, 'Killer Frost emits cryogenic smoke in first person', client)
                time.sleep(1.5)
                subprocess.run(['scrot', 'ci-arrow-killer-frost-smoke-first.png'], check=True, timeout=30)
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
