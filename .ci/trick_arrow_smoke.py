#!/usr/bin/env python3
"""Exercise potion and firework arrow impacts with a real client and dedicated server."""
import os
import signal
import subprocess
import time
from pathlib import Path
from small_player_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciArrowDiagnostics=true -Dfiskheroes.ciBowDiagnostics=true')
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
            command('setblock 0 181 3 minecraft:lever[face=wall,facing=north,powered=false]')
            command('tp Dev 0.5 180 0.5 0 0')
            command('item replace entity Dev weapon.offhand with fiskheroes:trick_arrow{ArrowType:"pulse"}')
            shoot('ci-arrow-pulse.png')
            wait_for(server_log, r'Pulse arrow impact: block=minecraft:stone, signal=true', 20,
                     'pulse arrow produces a temporary signal on the wall', server)
            command('execute if block 0 181 3 minecraft:lever[powered=false] run say PULSE_LEVER_UNCHANGED_PASS')
            wait_for(server_log, r'\[Server\] PULSE_LEVER_UNCHANGED_PASS', 15,
                     'pulse arrow does not permanently toggle the lever', server)
            print('PASS: real client fired five trick arrows against dedicated server', flush=True)
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
