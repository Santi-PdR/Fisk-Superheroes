#!/usr/bin/env python3
"""Exercise real attack inputs and shield recovery on a dedicated Forge server."""
import os
import re
import signal
import subprocess
import time
from pathlib import Path


def wait_for(log, pattern, seconds, label, process):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if log.exists() and re.search(pattern, log.read_text(errors='replace'), re.I):
            print('PASS:', label, flush=True)
            return
        if process.poll() is not None: raise RuntimeError(f'{label}: process exited with {process.returncode}')
        time.sleep(1)
    raise RuntimeError(f'Timed out: {label}')


def xdo(*args):
    subprocess.run(['xdotool', *args], check=True, timeout=30)


def main():
    keyboard = os.environ.get('SHIELD_KEYBOARD') == '1'
    suffix = '-keyboard' if keyboard else '-mouse'
    prefix = 'ci-shield-control' + suffix
    run = 'run-shield-control' + suffix
    server_log, client_log = Path(prefix + '-server.log'), Path(prefix + '-client.log')
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciShieldDiagnostics=true')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    if keyboard:
        Path(run + '-client/options.txt').write_text('key_key.attack:key.keyboard.r\n')
    processes = []
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', f'-PciRunDir={run}-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', f'-PciRunDir={run}-client',
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

            def attack():
                if keyboard: xdo('key', 'r')
                else: xdo('click', '1')

            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 10 minecraft:stone')
            command('fill -2 180 6 2 183 6 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:captain_america')
            command('item replace entity Dev weapon.mainhand with fiskheroes:captain_americas_shield{ControlMarker:42}')
            xdo('keydown', 'c')
            try:
                wait_for(server_log, r'Ability input accepted: hero=fiskheroes:captain_america, index=1', 45,
                         'held shield key received on server', server)
                command('execute if data entity Dev SelectedItem{id:"fiskheroes:captain_americas_shield"} run say SHIELD_HOLD_ONLY_OK')
                wait_for(server_log, r'SHIELD_HOLD_ONLY_OK', 30, 'ability alone keeps shield', server)
                if 'Shield throw completed: player=Dev' in server_log.read_text(errors='replace'):
                    raise RuntimeError('Ability key alone launched the shield')
            finally: xdo('keyup', 'c')
            time.sleep(1)
            attack(); time.sleep(1)
            command('execute if data entity Dev SelectedItem{id:"fiskheroes:captain_americas_shield"} run say SHIELD_ATTACK_ONLY_OK')
            wait_for(server_log, r'SHIELD_ATTACK_ONLY_OK', 30, 'ordinary attack keeps shield', server)
            if 'Shield throw completed: player=Dev' in server_log.read_text(errors='replace'):
                raise RuntimeError('Attack without held shield key launched it')
            xdo('keydown', 'c')
            try:
                time.sleep(1)
                attack()
                wait_for(server_log, r'Shield throw completed: player=Dev', 45,
                         'attack with held shield key launches exactly one shield', server)
            finally: xdo('keyup', 'c')
            time.sleep(3)
            command('execute if data entity Dev Inventory[{id:"fiskheroes:captain_americas_shield",tag:{ControlMarker:42}}] run say SHIELD_RETURN_DATA_OK')
            wait_for(server_log, r'SHIELD_RETURN_DATA_OK', 30, 'shield returns with item data', server)
            count = server_log.read_text(errors='replace').count('Shield throw completed: player=Dev')
            if count != 1: raise RuntimeError(f'Expected one launch, saw {count}')
            print('PASS: real', 'keyboard' if keyboard else 'mouse', 'attack requires held ability and returns shield', flush=True)
        finally:
            for p in reversed(processes):
                if p.poll() is None:
                    try: os.killpg(p.pid, signal.SIGTERM)
                    except ProcessLookupError: pass
            for p in reversed(processes):
                try: p.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    os.killpg(p.pid, signal.SIGKILL); p.wait()


if __name__ == '__main__': main()
