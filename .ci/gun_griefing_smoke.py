#!/usr/bin/env python3
"""Exercise both disabled beam-grief rules on a real server and client."""
import os
import signal
import subprocess
import time
from pathlib import Path

from tonfa_visual_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log = Path('ci-gun-grief-server.log')
    client_log = Path('ci-gun-grief-client.log')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    processes = []
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-gun-grief-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-gun-grief-client',
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

            def fire(duration=3):
                xdo('mousedown', '1')
                try:
                    time.sleep(duration)
                finally:
                    xdo('mouseup', '1')
                time.sleep(1)

            command('difficulty normal')
            command('gamerule doMobSpawning false')
            command('gamemode creative Dev')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 6 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('scoreboard objectives add beamHealth dummy')
            for hero, item, target, marker, max_health in [
                    ('heatwave', 'heat_gun', 'ice', 'HEAT', 75),
                    ('captain_cold', 'cold_gun', 'water', 'COLD', 77)]:
                command('fill -2 180 1 2 183 6 minecraft:air')
                command('tp Dev 0.5 180 0.5 0 0')
                command(f'execute as Dev run suit fiskheroes:{hero}')
                command(f'item replace entity Dev weapon.mainhand with fiskheroes:{item}')
                command(f'setblock 0 181 4 minecraft:{target}')
                fire()
                command(f'execute if block 0 181 4 minecraft:{target} run say {marker}_BLOCK_PRESERVED')
                wait_for(server_log, f'{marker}_BLOCK_PRESERVED', 30, f'{item} preserves its block', server)
                if item == 'cold_gun':
                    command('execute unless block 0 181 1 minecraft:ice unless block 0 181 2 minecraft:ice '
                            'unless block 0 181 3 minecraft:ice unless block 0 181 4 minecraft:ice run say COLD_NO_ICE')
                    wait_for(server_log, 'COLD_NO_ICE', 30, 'flowing water was not frozen either', server)
                # With the same disabled rule, a separate living target must still
                # take damage. This also proves the beam actually fired.
                command('fill -2 180 1 2 183 6 minecraft:air')
                # A cow at foot level is shorter than the player's eye ray.
                command('summon minecraft:cow 0.5 180.5 3.5 {NoAI:1b,NoGravity:1b,Silent:1b,Attributes:[{Name:"minecraft:generic.max_health",Base:100.0d}],Tags:["beam_test"]}')
                command('data merge entity @e[type=minecraft:cow,tag=beam_test,limit=1] {Health:100.0f}')
                command('execute as @e[type=minecraft:cow,tag=beam_test,limit=1] store result score @s beamHealth run data get entity @s Health 1')
                command(f'execute if entity @e[type=minecraft:cow,tag=beam_test,scores={{beamHealth=100..}}] run say {marker}_TARGET_READY')
                wait_for(server_log, f'{marker}_TARGET_READY', 30, 'target starts at full health', server)
                # One second is long enough for cold's charge, but short enough to
                # distinguish the configured high damage from the vanilla defaults.
                fire(1.0)
                command('execute as @e[type=minecraft:cow,tag=beam_test,limit=1] store result score @s beamHealth run data get entity @s Health 1')
                command(f'execute if entity @e[type=minecraft:cow,tag=beam_test,scores={{beamHealth=..{max_health}}}] run say {marker}_DAMAGE_OK')
                wait_for(server_log, f'{marker}_DAMAGE_OK', 30, f'{item} uses configured damage', server)
                command('kill @e[type=minecraft:cow,tag=beam_test]')
                # With range configured to six blocks, a target at ten must be
                # unaffected even though a default 32-block beam would reach it.
                command('summon minecraft:cow 0.5 180.5 10.5 {NoAI:1b,NoGravity:1b,Silent:1b,Attributes:[{Name:"minecraft:generic.max_health",Base:100.0d}],Tags:["beam_far"]}')
                command('data merge entity @e[type=minecraft:cow,tag=beam_far,limit=1] {Health:100.0f}')
                fire()
                command('execute as @e[type=minecraft:cow,tag=beam_far,limit=1] store result score @s beamHealth run data get entity @s Health 1')
                command(f'execute if entity @e[type=minecraft:cow,tag=beam_far,scores={{beamHealth=100..}}] run say {marker}_RANGE_OK')
                wait_for(server_log, f'{marker}_RANGE_OK', 30, f'{item} respects configured range', server)
                command('kill @e[type=minecraft:cow,tag=beam_far]')
            print('PASS: both beam rules, ranges and damage values take effect', flush=True)
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
