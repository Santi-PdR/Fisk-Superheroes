#!/usr/bin/env python3
"""Exercise Black Canary's sonic waves with an actual client and dedicated server."""
import hashlib
import os
import signal
import subprocess
import time
import uuid
from pathlib import Path

from tonfa_visual_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log, client_log = Path('ci-sonic-server.log'), Path('ci-sonic-client.log')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    processes = []
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-sonic-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-sonic-client',
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
                time.sleep(1.2)

            def cry(duration):
                xdo('keydown', 'c')
                try:
                    time.sleep(duration)
                finally:
                    xdo('keyup', 'c')
                time.sleep(1)

            command('difficulty normal')
            command('gamerule doMobSpawning false')
            command('gamemode creative Dev')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 10 minecraft:stone')
            command('fill -2 180 1 2 184 10 minecraft:air')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:black_canary')
            command('scoreboard objectives add sonic dummy')
            command('summon minecraft:cow 0.5 180.5 4.5 {NoAI:1b,NoGravity:1b,Silent:1b,Attributes:[{Name:"minecraft:generic.max_health",Base:100.0d}],Tags:["sonic_target"]}')
            command('data merge entity @e[type=minecraft:cow,tag=sonic_target,limit=1] {Health:100.0f}')
            command('execute as @e[type=minecraft:cow,tag=sonic_target,limit=1] store result score @s sonic run data get entity @s Health 1')
            command('execute if entity @e[type=minecraft:cow,tag=sonic_target,scores={sonic=100..}] run say SONIC_TARGET_READY')
            wait_for(server_log, 'SONIC_TARGET_READY', 30, 'living target starts at full health', server)
            cry(2)
            wait_for(server_log, r'Ability input accepted: hero=fiskheroes:black_canary, index=1', 30,
                     'Black Canary ability key accepted', server)
            command('execute as @e[type=minecraft:cow,tag=sonic_target,limit=1] store result score @s sonic run data get entity @s Health 1')
            command('execute if entity @e[type=minecraft:cow,tag=sonic_target,scores={sonic=..99}] run say SONIC_DAMAGE_OK')
            wait_for(server_log, 'SONIC_DAMAGE_OK', 30, 'held cry damages a target', server)
            command('kill @e[type=minecraft:cow,tag=sonic_target]')
            command('kill @e[type=fiskheroes:sonic_wave]')
            # Keep probe projectiles stationary and first verify that they exist:
            # a missing probe must not be mistaken for a successful impact.
            raw = hashlib.md5(b'OfflinePlayer:Dev').digest()
            identity = uuid.UUID(bytes=raw, version=3)
            chunks = [int.from_bytes(identity.bytes[i:i + 4], 'big', signed=True) for i in range(0, 16, 4)]
            owner = '[I;' + ','.join(str(value) for value in chunks) + ']'
            command('summon fiskheroes:icicle 0.5 181.45 4.5 {Tags:["sonic_icicle"],NoGravity:1b,Motion:[0.0d,0.0d,0.0d]}')
            command('summon fiskheroes:thrown_shield 0.5 181.45 5.5 '
                    '{Owner:' + owner + ',Tags:["sonic_shield"],NoGravity:1b,Motion:[0.0d,0.0d,0.0d],'
                    'Shield:{id:"fiskheroes:captain_americas_shield",Count:1b,tag:{SonicProbe:1b}}}')
            command('execute if entity @e[type=fiskheroes:icicle,tag=sonic_icicle] '
                    'if entity @e[type=fiskheroes:thrown_shield,tag=sonic_shield] run say SONIC_PROJECTILES_READY')
            wait_for(server_log, 'SONIC_PROJECTILES_READY', 30, 'both projectile probes are present', server)
            cry(2)
            command('execute unless entity @e[type=fiskheroes:icicle,tag=sonic_icicle] '
                    'unless entity @e[type=fiskheroes:thrown_shield,tag=sonic_shield] run say SONIC_PROJECTILES_REMOVED')
            wait_for(server_log, 'SONIC_PROJECTILES_REMOVED', 30, 'cry shatters icicle and dislodges shield', server)
            command('execute store result score #drops sonic if entity @e[type=minecraft:item,nbt={Item:{tag:{SonicProbe:1b}}}]')
            command('execute if score #drops sonic matches 1 run say SONIC_SHIELD_ITEM_OK')
            wait_for(server_log, 'SONIC_SHIELD_ITEM_OK', 30, 'one shield item retains its data', server)
            command('kill @e[type=minecraft:item,nbt={Item:{tag:{SonicProbe:1b}}}]')
            command('kill @e[type=fiskheroes:sonic_wave]')
            command('summon minecraft:item 0.5 181.45 4.5 {Tags:["sonic_item"],NoGravity:1b,PickupDelay:32767s,Motion:[0.0d,0.0d,0.0d],Item:{id:"minecraft:stone",Count:1b}}')
            command('execute as @e[type=minecraft:item,tag=sonic_item,limit=1] store result score @s sonic run data get entity @s Pos[2] 100')
            command('execute if entity @e[type=minecraft:item,tag=sonic_item,scores={sonic=450}] run say SONIC_ITEM_READY')
            wait_for(server_log, 'SONIC_ITEM_READY', 30, 'stationary item probe is present', server)
            cry(2)
            command('execute as @e[type=minecraft:item,tag=sonic_item,limit=1] store result score @s sonic run data get entity @s Pos[2] 100')
            command('execute if entity @e[type=minecraft:item,tag=sonic_item,scores={sonic=451..}] run say SONIC_PUSH_OK')
            wait_for(server_log, 'SONIC_PUSH_OK', 30, 'cry pushes an item along its direction', server)
            print('PASS: real client cry affects entities and projectiles', flush=True)
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    os.killpg(process.pid, signal.SIGTERM)
            for process in reversed(processes):
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait(timeout=10)


if __name__ == '__main__':
    main()
