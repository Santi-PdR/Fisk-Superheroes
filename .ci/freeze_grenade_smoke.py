#!/usr/bin/env python3
"""Exercise Batman's actual utility-belt input and the freeze grenade on a dedicated server."""
import os
import signal
import subprocess
import time
from pathlib import Path
from tonfa_visual_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciEquipmentDiagnostics=true -Dfiskheroes.ciFreezeGrenadeDiagnostics=true')
    server_log, client_log = Path('ci-freeze-grenade-server.log'), Path('ci-freeze-grenade-client.log')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    processes = []
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-freeze-grenade-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-freeze-grenade-client',
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
                server.stdin.write(line + '\n')
                server.stdin.flush()
                time.sleep(1.2)

            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 10 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:batman_dceu')
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            command('item replace entity Dev weapon.offhand with minecraft:air')
            command('summon minecraft:iron_golem 0.5 180 7.5 {NoAI:1b,Silent:1b,Tags:["freeze_test"]}')
            command('setblock 1 180 7 minecraft:water')
            xdo('keydown', 'c')
            try:
                time.sleep(1.0)
                xdo('click', '4')
                time.sleep(0.6)
                xdo('click', '4')
                wait_for(server_log, r'Utility belt selection: player=Dev, gadget=fiskheroes:freeze_grenade',
                         30, 'select freeze grenade with utility-belt input', server)
            finally:
                xdo('keyup', 'c')
            time.sleep(0.5)
            xdo('click', '3')
            wait_for(server_log, r'Utility belt used: player=Dev, gadget=fiskheroes:freeze_grenade',
                     30, 'use selected gadget with empty hand', server)
            wait_for(server_log, r'Freeze grenade impact: .*owner=ServerPlayer',
                     30, 'freeze grenade detonates on impact', server)
            wait_for(client_log, r'Freeze grenade model rendered:', 30, '3D grenade renders on the real client', client)
            command('execute as @e[type=minecraft:iron_golem,tag=freeze_test,limit=1] if data entity @s ActiveEffects[{Id:2}] run say FREEZE_TARGET_OK')
            wait_for(server_log, r'FREEZE_TARGET_OK', 30, 'blast slows the target', server)
            command('execute as @e[type=minecraft:iron_golem,tag=freeze_test,limit=1] unless data entity @s {Health:100.0f} run say FREEZE_DAMAGE_OK')
            wait_for(server_log, r'FREEZE_DAMAGE_OK', 30, 'blast damages the target', server)
            command('execute if block 1 180 7 minecraft:ice run say FREEZE_WATER_OK')
            wait_for(server_log, r'FREEZE_WATER_OK', 30, 'blast freezes nearby water', server)
            # Batman has one charge: another click during cooldown must not spawn a second grenade.
            before = server_log.read_text(errors='replace').count('Utility belt used: player=Dev, gadget=fiskheroes:freeze_grenade')
            xdo('click', '3')
            time.sleep(2)
            after = server_log.read_text(errors='replace').count('Utility belt used: player=Dev, gadget=fiskheroes:freeze_grenade')
            if after != before: raise RuntimeError('Freeze grenade ignored its single-use cooldown')
            print('PASS: freeze grenade charge is consumed and cannot be reused during cooldown', flush=True)

            # Select a fresh batarang, then ensure ordinary block use is not stolen.
            xdo('keydown', 'c')
            try:
                time.sleep(1.0)
                xdo('click', '5')
                time.sleep(0.5)
            finally:
                xdo('keyup', 'c')
            command('setblock 0 181 3 minecraft:stone')
            command('setblock 0 181 2 minecraft:lever[face=wall,facing=north,powered=false]')
            xdo('click', '3')
            command('execute if block 0 181 2 minecraft:lever[powered=true] run say BELT_BLOCK_USE_OK')
            wait_for(server_log, r'BELT_BLOCK_USE_OK', 30, 'normal empty-hand lever interaction still works', server)
            if 'Utility belt used: player=Dev, gadget=fiskheroes:batarang' in server_log.read_text(errors='replace'):
                raise RuntimeError('Ordinary block interaction unexpectedly threw a batarang')
            # Holding the belt key deliberately overrides block interaction for a close throw.
            xdo('keydown', 'c')
            try:
                time.sleep(0.8)
                xdo('click', '3')
                wait_for(server_log, r'Utility belt used: player=Dev, gadget=fiskheroes:batarang',
                         30, 'held belt key allows a close-range throw', server)
            finally:
                xdo('keyup', 'c')
            count = server_log.read_text(errors='replace').count('Utility belt used: player=Dev, gadget=fiskheroes:batarang')
            command('setblock 0 181 2 minecraft:air')
            command('setblock 0 181 3 minecraft:air')
            command('tp Dev 0.5 180 0.5 0 -10')
            command('gamemode survival Dev')
            command('item replace entity Dev weapon.offhand with minecraft:snowball 2')
            xdo('click', '3')
            command('execute if data entity Dev Inventory[{Slot:-106b,id:"minecraft:snowball",Count:1b}] run say BELT_OFFHAND_OK')
            wait_for(server_log, r'BELT_OFFHAND_OK', 30, 'real offhand item keeps vanilla use', server)
            if server_log.read_text(errors='replace').count('Utility belt used: player=Dev, gadget=fiskheroes:batarang') != count:
                raise RuntimeError('Offhand item use unexpectedly triggered the belt')
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    try: os.killpg(process.pid, signal.SIGTERM)
                    except ProcessLookupError: pass
            for process in reversed(processes):
                try: process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()


if __name__ == '__main__': main()
