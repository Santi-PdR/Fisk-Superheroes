#!/usr/bin/env python3
"""Real dedicated-server/client checks for Chronos and Rip's energy weapons."""
import os
import signal
import subprocess
import time
from pathlib import Path
from PIL import Image, ImageChops
from tonfa_visual_smoke import wait_for, xdo


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciEnergyGunDiagnostics=true')
    server_log = Path('ci-energy-guns-server.log')
    client_log = Path('ci-energy-guns-client.log')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    processes = []
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-energy-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-energy-client',
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
                server.stdin.write(line + '\n')
                server.stdin.flush()
                time.sleep(1.0)
            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -6 179 -6 6 179 28 minecraft:stone')
            command('fill -5 180 25 5 185 25 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:chronos')
            command('item replace entity Dev weapon.mainhand with fiskheroes:chronos_rifle')
            time.sleep(2)
            # Secondary-use by itself must not fire or put the weapon on cooldown.
            xdo('click', '3')
            time.sleep(1)
            if 'Energy gun shot: item=chronos_rifle' in server_log.read_text(errors='replace'):
                raise RuntimeError('Chronos rifle fired without aim')
            print('PASS: rifle does not fire from use without aim', flush=True)
            xdo('mousedown', '1')
            try:
                wait_for(server_log, r'Energy gun shot: item=chronos_rifle, shooter=Dev, scoped=false, damage=9\.0, explosive=true',
                         60, 'unscoped Chronos shot has base energy damage', server)
                time.sleep(1.5)
                subprocess.run(['scrot', 'ci-energy-guns-unscoped.png'], check=True, timeout=30)
                xdo('keydown', 'Shift_L')
                try:
                    wait_for(server_log, r'Energy gun shot: item=chronos_rifle, shooter=Dev, scoped=true, damage=14\.4[0-9]*, explosive=true',
                             60, 'scoped Chronos shot has bonus and explosion', server)
                    wait_for(client_log, r'Chronos scope overlay rendered', 60, 'Chronos scope overlay rendered', client)
                    time.sleep(1)
                    subprocess.run(['scrot', 'ci-energy-guns-scoped.png'], check=True, timeout=30)
                finally:
                    xdo('keyup', 'Shift_L')
            finally:
                xdo('mouseup', '1')
            before = Image.open('ci-energy-guns-unscoped.png').convert('RGB')
            after = Image.open('ci-energy-guns-scoped.png').convert('RGB')
            changed = sum(1 for r, g, b in ImageChops.difference(before, after).get_flattened_data() if max(r, g, b) > 30)
            if changed < 5000: raise RuntimeError(f'Chronos scope not visible in screenshot: {changed} pixels')
            print(f'PASS: visible scope changes {changed} screen pixels', flush=True)
            command('execute as Dev run suit fiskheroes:rip_hunter')
            command('item replace entity Dev weapon.mainhand with fiskheroes:rip_hunters_gun')
            time.sleep(2)
            xdo('click', '3')
            time.sleep(1)
            if 'Energy gun shot: item=rip_hunters_gun' in server_log.read_text(errors='replace'):
                raise RuntimeError('Rip gun fired without aim')
            print('PASS: revolver does not fire from use without aim', flush=True)
            xdo('mousedown', '1')
            try:
                wait_for(server_log, r'Energy gun shot: item=rip_hunters_gun, shooter=Dev, scoped=false, damage=6\.0, explosive=false',
                         60, 'unscoped Rip shot has base energy damage', server)
                xdo('keydown', 'Shift_L')
                try:
                    wait_for(server_log, r'Energy gun shot: item=rip_hunters_gun, shooter=Dev, scoped=true, damage=9\.6, explosive=false',
                             60, 'scoped Rip shot has bonus without explosion', server)
                finally:
                    xdo('keyup', 'Shift_L')
            finally:
                xdo('mouseup', '1')
            command('summon minecraft:iron_golem 0.5 180 10.5 {NoAI:1b,Silent:1b,Tags:["energy_gun_target"]}')
            xdo('click', '1')
            time.sleep(2)
            command('execute as @e[type=minecraft:iron_golem,tag=energy_gun_target] unless data entity @s {Health:100.0f} run say ENERGY_GUN_TARGET_HIT')
            wait_for(server_log, r'ENERGY_GUN_TARGET_HIT', 30, 'real revolver bolt hits and damages target', server)
            wait_for(client_log, r'Energy bolt rendered: owner=Dev, color=005EFF', 60, 'Rip bolt has blue suit color', client)
            print('PASS: real energy weapons exercised', flush=True)
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


if __name__ == '__main__':
    main()
