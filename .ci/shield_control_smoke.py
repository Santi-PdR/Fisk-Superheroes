#!/usr/bin/env python3
"""Exercise real attack inputs and shield recovery on a dedicated Forge server."""
import os
import hashlib
import threading
import uuid
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
        Path(run + '-client/options.txt').write_text('version:3465\nkey_key.attack:key.keyboard.r\n')
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
                wait_for(server_log, r'\[Server\] SHIELD_HOLD_ONLY_OK', 30, 'ability alone keeps shield', server)
                if 'Shield throw completed: player=Dev' in server_log.read_text(errors='replace'):
                    raise RuntimeError('Ability key alone launched the shield')
            finally: xdo('keyup', 'c')
            time.sleep(1)
            attack(); time.sleep(1)
            command('execute if data entity Dev SelectedItem{id:"fiskheroes:captain_americas_shield"} run say SHIELD_ATTACK_ONLY_OK')
            wait_for(server_log, r'\[Server\] SHIELD_ATTACK_ONLY_OK', 30, 'ordinary attack keeps shield', server)
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
            wait_for(server_log, r'\[Server\] SHIELD_RETURN_DATA_OK', 30, 'shield returns with item data', server)
            count = server_log.read_text(errors='replace').count('Shield throw completed: player=Dev')
            if count != 1: raise RuntimeError(f'Expected one launch, saw {count}')
            print('PASS: real', 'keyboard' if keyboard else 'mouse', 'attack requires held ability and returns shield', flush=True)
            if not keyboard:
                # Hold a server-owned projectile in a controlled front-facing pose. A
                # runner-only magenta texture makes actual model pixels distinguishable
                # from the background and the player's HUD.
                from PIL import Image
                command('item replace entity Dev weapon.mainhand with minecraft:air')
                command('fill -3 179 6 3 184 6 minecraft:black_concrete')
                command('tp Dev 0.5 180 0.5 0 0')
                time.sleep(1)
                subprocess.run(['scrot', prefix + '-before.png'], check=True, timeout=30)
                raw = hashlib.md5(b'OfflinePlayer:Dev').digest()
                identity = uuid.UUID(bytes=raw, version=3)
                chunks = [int.from_bytes(identity.bytes[i:i + 4], 'big', signed=True) for i in range(0, 16, 4)]
                owner_tag = '[I;' + ','.join(str(value) for value in chunks) + ']'
                log_offset = client_log.stat().st_size
                server.stdin.write('summon fiskheroes:thrown_shield 0.5 181.62 3.5 '
                        '{Owner:' + owner_tag + ',Tags:["visual_probe"],Motion:[0.0d,0.03d,0.0d],Rotation:[0.0f,90.0f]}\n')
                server.stdin.flush()
                stop = threading.Event()
                def anchor():
                    while not stop.wait(.12):
                        server.stdin.write('tp @e[type=fiskheroes:thrown_shield,tag=visual_probe,limit=1] 0.5 181.62 3.5 0 90\n')
                        server.stdin.write('data merge entity @e[type=fiskheroes:thrown_shield,tag=visual_probe,limit=1] {Motion:[0.0d,0.02d,0.0d]}\n')
                        server.stdin.flush()
                thread = threading.Thread(target=anchor, daemon=True); thread.start()
                try:
                    deadline = time.monotonic() + 30
                    while time.monotonic() < deadline:
                        if b'Thrown shield model rendered: entity=' in client_log.read_bytes()[log_offset:]: break
                        if client.poll() is not None: raise RuntimeError('Client exited before visual probe rendered')
                        time.sleep(.1)
                    else: raise RuntimeError('The visual probe never rendered')
                    command('execute if entity @e[type=fiskheroes:thrown_shield,tag=visual_probe] run say SHIELD_PROBE_PRESENT')
                    wait_for(server_log, r'\[Server\] SHIELD_PROBE_PRESENT', 5, 'visual probe remained in the world', server)
                    def magenta(path):
                        image = Image.open(path).convert('RGB').crop((520, 280, 760, 550))
                        return sum(1 for r, g, b in image.get_flattened_data() if r > 50 and b > 50 and g < 80 and r > 1.5 * g and b > 1.5 * g)
                    baseline = magenta(prefix + '-before.png')
                    gain = 0
                    # Client interpolation can briefly show the moving shield edge-on.
                    for attempt in range(25):
                        # scrot otherwise creates a numbered file and we repeatedly inspect
                        # the first frame instead of the latest interpolated orientation.
                        Path(prefix + '-projectile.png').unlink(missing_ok=True)
                        subprocess.run(['scrot', prefix + '-projectile.png'], check=True, timeout=30)
                        gain = magenta(prefix + '-projectile.png') - baseline
                        if gain >= 80: break
                        time.sleep(.2)
                finally:
                    stop.set(); thread.join(timeout=3)
                if gain < 80:
                    import base64, io
                    image = Image.open(prefix + '-projectile.png').convert('RGB')
                    image.thumbnail((640, 360)); encoded = io.BytesIO(); image.save(encoded, format='PNG')
                    print('DIAGNOSTIC_SCREENSHOT_BASE64=' + base64.b64encode(encoded.getvalue()).decode(), flush=True)
                    raise RuntimeError(f'Thrown shield model was not visible: {gain} magenta pixels')
                print(f'PASS: thrown shield model adds {gain} diagnostic pixels', flush=True)

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
