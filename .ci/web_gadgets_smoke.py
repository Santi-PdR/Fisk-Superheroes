#!/usr/bin/env python3
"""Exercise Spider-Man's gadget webs with a real Forge client and dedicated server."""
import os
import re
import signal
import subprocess
import time
from pathlib import Path
from tonfa_visual_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciEquipmentDiagnostics=true')
    server_log, client_log = Path('ci-web-gadgets-server.log'), Path('ci-web-gadgets-client.log')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    processes = []
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-web-gadgets-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-web-gadgets-client',
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
                if any(p.poll() is not None for p in processes): raise RuntimeError('Minecraft exited unexpectedly')
                server.stdin.write(line + '\n'); server.stdin.flush(); time.sleep(1.0)

            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -8 179 -5 8 179 15 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:spider_man')
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('item replace entity Dev weapon.offhand with minecraft:air')
            command('fill -1 180 7 1 183 7 minecraft:stone')
            xdo('keydown', 'c')
            try:
                time.sleep(1)
                xdo('click', '4')
                wait_for(server_log, r'Utility belt selection: player=Dev, gadget=fiskheroes:sticky_web',
                         30, 'Spider-Man selects sticky web', server)
            finally:
                xdo('keyup', 'c')
            time.sleep(0.5); xdo('click', '3')
            wait_for(server_log, r'Sticky web attached to block: face=NORTH', 30, 'sticky web sticks to wall', server)
            wait_for(client_log, r'Sticky web patch rendered: anchor=2', 30, 'wall web renders on real client', client)
            command('execute if entity @e[type=fiskheroes:equipment_projectile,nbt={Gadget:"fiskheroes:sticky_web",WebAnchor:2b}] run say WALL_WEB_OK')
            wait_for(server_log, r'WALL_WEB_OK', 20, 'wall attachment replicated and saved as an entity', server)
            command('kill @e[type=fiskheroes:equipment_projectile]')
            command('fill -1 180 7 1 183 7 minecraft:air')
            command('summon minecraft:iron_golem 0.5 180 7.5 {NoAI:1b,NoGravity:1b,Silent:1b,Tags:["web_target"]}')
            command('tp Dev 0.5 180 0.5 0 10')
            xdo('click', '3')
            wait_for(server_log, r'Sticky web attached to entity: target=\d+, slows=true', 30, 'sticky web hits target legs', server)
            wait_for(client_log, r'Sticky web patch rendered: anchor=6', 30, 'attached entity web renders on real client', client)
            command('execute as @e[type=minecraft:iron_golem,tag=web_target,limit=1] if data entity @s ActiveEffects[{Id:2}] run say WEB_SLOW_OK')
            wait_for(server_log, r'WEB_SLOW_OK', 20, 'leg web slows target', server)
            command('tp @e[type=minecraft:iron_golem,tag=web_target,limit=1] 2.5 180 7.5')
            command('execute as @e[type=minecraft:iron_golem,tag=web_target,limit=1] at @s if entity @e[type=fiskheroes:equipment_projectile,distance=..2,nbt={Gadget:"fiskheroes:sticky_web"}] run say WEB_FOLLOWS_OK')
            wait_for(server_log, r'WEB_FOLLOWS_OK', 20, 'web follows moving target', server)
            time.sleep(5)
            command('execute unless entity @e[type=fiskheroes:equipment_projectile,nbt={Gadget:"fiskheroes:sticky_web"}] run say WEB_DISSOLVED_OK')
            wait_for(server_log, r'WEB_DISSOLVED_OK', 20, 'sticky web dissolves after configured lifetime', server)
            command('kill @e[type=minecraft:iron_golem,tag=web_target]')
            command('summon minecraft:iron_golem 0.5 180 7.5 {NoAI:1b,NoGravity:1b,Silent:1b,Tags:["impact_target"]}')
            command('tp Dev 0.5 180 0.5 0 0')
            xdo('keydown', 'c')
            try:
                time.sleep(1); xdo('click', '4')
                wait_for(server_log, r'Utility belt selection: player=Dev, gadget=fiskheroes:impact_web', 30, 'select impact web', server)
            finally:
                xdo('keyup', 'c')
            time.sleep(0.5); xdo('click', '3')
            wait_for(server_log, r'Utility belt used: player=Dev, gadget=fiskheroes:impact_web', 30, 'shoot impact web', server)
            command('execute as @e[type=minecraft:iron_golem,tag=impact_target,limit=1] unless data entity @s {Health:100.0f} unless data entity @s ActiveEffects[{Id:2}] run say IMPACT_WEB_OK')
            wait_for(server_log, r'IMPACT_WEB_OK', 20, 'impact web damages without sticky slowdown', server)
            command('kill @e[type=minecraft:iron_golem,tag=impact_target]')
            command('tp Dev 0.5 180 0.5 0 0')
            command('summon minecraft:iron_golem 0.5 180 7.5 {NoAI:1b,NoGravity:1b,Silent:1b,Tags:["rapid_target"],Attributes:[{Name:"minecraft:generic.knockback_resistance",Base:1.0d}]}')
            command('scoreboard objectives add web_smoke dummy')
            xdo('keydown', 'c')
            try:
                time.sleep(1); xdo('click', '4')
                wait_for(server_log, r'Utility belt selection: player=Dev, gadget=fiskheroes:rapid_webs', 30, 'select rapid webs', server)
            finally:
                xdo('keyup', 'c')
            time.sleep(0.5); xdo('click', '3')
            wait_for(server_log, r'Rapid web shot: player=Dev, shot=5, tick=\d+', 30, 'rapid webs finish a five-shot burst', server)
            shots = re.findall(r'Rapid web shot: player=Dev, shot=(\d+), tick=(\d+)', server_log.read_text(errors='replace'))
            if [int(n) for n, _ in shots] != [1, 2, 3, 4, 5] or any(
                    int(shots[i + 1][1]) - int(shots[i][1]) != 2 for i in range(4)):
                raise RuntimeError(f'Rapid web cadence is not two ticks: {shots}')
            print('PASS: rapid web burst fires one projectile every two server ticks', flush=True)
            command('execute store result score webhealth web_smoke run data get entity @e[type=minecraft:iron_golem,tag=rapid_target,limit=1] Health 10')
            command('execute if score webhealth web_smoke matches ..949 run say RAPID_WEB_MULTIHIT_OK')
            wait_for(server_log, r'RAPID_WEB_MULTIHIT_OK', 20, 'rapid burst lands multiple damaging hits', server)
            command('kill @e[type=minecraft:iron_golem,tag=rapid_target]')
            command('kill @e[type=fiskheroes:equipment_projectile]')
            command('tp Dev 0.5 180 0.5 0 0')
            command('fill -2 180 7 2 183 7 minecraft:stone')
            command('fill -2 180 -5 2 183 -5 minecraft:stone')
            xdo('keydown', 'c')
            try:
                time.sleep(1); xdo('click', '4')
                wait_for(server_log, r'Utility belt selection: player=Dev, gadget=fiskheroes:ricochet_web', 30, 'select ricochet web', server)
            finally:
                xdo('keyup', 'c')
            time.sleep(0.5); xdo('click', '3')
            wait_for(server_log, r'Ricochet web bounced: face=north', 30, 'ricochet web reflects from first wall', server)
            wait_for(server_log, r'Web attached to block: gadget=fiskheroes:ricochet_web, face=south', 30, 'ricochet web sticks on second impact', server)
            command('tp Dev 0.5 180 0.5 180 0')
            wait_for(client_log, r'Web patch rendered: anchor=3', 30, 'ricochet web patch renders on real client', client)
        finally:
            for p in reversed(processes):
                if p.poll() is None:
                    try: os.killpg(p.pid, signal.SIGTERM)
                    except ProcessLookupError: pass
            for p in reversed(processes):
                try: p.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    try: os.killpg(p.pid, signal.SIGKILL)
                    except ProcessLookupError: pass
                    p.wait(timeout=10)


if __name__ == '__main__': main()
