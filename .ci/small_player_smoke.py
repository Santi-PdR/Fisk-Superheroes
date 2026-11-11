#!/usr/bin/env python3
"""Check shrunken pose and actual first-person camera in a one-block tunnel."""
import os
import re
import signal
import subprocess
import time
from pathlib import Path
from tonfa_visual_smoke import wait_for, xdo

SERVER = Path('ci-small-server.log')
CLIENT = Path('ci-small-client.log')
NUMBER = r'([-+0-9.eE]+)'


def wait_sample(path, start, pattern, predicate, seconds, label, process):
    deadline = time.monotonic() + seconds
    regex = re.compile(pattern)
    while time.monotonic() < deadline:
        text = path.read_text(errors='replace')[start:] if path.exists() else ''
        for match in regex.finditer(text):
            if predicate(match):
                print(f'PASS: {label}: {match.group(0)}', flush=True)
                return match
        if process.poll() is not None:
            raise RuntimeError(f'{label}: process exited {process.returncode}')
        time.sleep(0.5)
    raise RuntimeError(f'Timed out: {label}; recent logs:\n' + text[-5000:])


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciSizeDiagnostics=true')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    processes = []
    with SERVER.open('w') as sout, CLIENT.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-small-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(SERVER, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-small-client',
                                       '-PciUsername=Dev', '-PquickPlayServer=127.0.0.1:25565', *common],
                                      stdout=cout, stderr=subprocess.STDOUT, env=env, start_new_session=True)
            processes.append(client)
            wait_for(SERVER, r'Dev\[/.*logged in with entity id', 360, 'client joined', client)
            time.sleep(8)
            window = subprocess.check_output(['xdotool', 'search', '--onlyvisible', '--name', 'Minecraft'],
                                             text=True, timeout=30).strip().splitlines()[-1]
            xdo('windowfocus', '--sync', window)
            xdo('mousemove', '--window', window, '640', '360', 'click', '1')
            def command(line):
                if any(p.poll() is not None for p in processes): raise RuntimeError('Minecraft exited unexpectedly')
                server.stdin.write(line + '\n')
                server.stdin.flush()
                time.sleep(1)
            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('tp Dev 0 185 0 0 0')
            command('fill -4 179 -4 4 179 15 minecraft:stone')
            command('fill -1 180 2 -1 182 12 minecraft:stone')
            command('fill 1 180 2 1 182 12 minecraft:stone')
            command('fill 0 181 2 0 181 12 minecraft:stone')
            for hero in ('atom', 'ant_man'):
                command('tp Dev 0.5 180 0.5 0 0')
                command('execute as Dev run suit fiskheroes:the_flash')
                command('execute as Dev run suit fiskheroes:' + hero)
                time.sleep(2)
                start_server = len(SERVER.read_text(errors='replace'))
                start_client = len(CLIENT.read_text(errors='replace'))
                xdo('key', 'c')
                pattern = r'Size pose: side=server, hero=fiskheroes:' + hero + r', scale=0\.0625, pose=STANDING,'
                wait_sample(SERVER, start_server, pattern, lambda m: True, 40, hero + ' shrinks on server', server)
                time.sleep(1)
                # Walk through the open end into the one-block-high tunnel.
                xdo('keydown', 'w')
                time.sleep(1.2)
                xdo('keyup', 'w')
                time.sleep(3)
                pose = (r'Size pose: side=server, hero=fiskheroes:' + hero + r', scale=0\.0625, pose=STANDING, x='
                        + NUMBER + r', y=' + NUMBER + r', z=' + NUMBER + r', height=' + NUMBER + r', eye=' + NUMBER)
                wait_sample(SERVER, start_server, pose,
                            lambda m: 2.1 < float(m.group(3)) < 12.0 and abs(float(m.group(2)) - 180) < .1
                            and abs(float(m.group(4)) - .1125) < .005 and abs(float(m.group(5)) - .10125) < .005,
                            40, hero + ' enters tunnel standing at scaled eye height on server', server)
                camera = (r'Size camera: hero=fiskheroes:' + hero + r', scale=0\.0625, pose=STANDING, x='
                          + NUMBER + r', y=' + NUMBER + r', z=' + NUMBER + r', cameraY=' + NUMBER + r', eye=' + NUMBER)
                wait_sample(CLIENT, start_client, camera,
                            lambda m: 2.1 < float(m.group(3)) < 12.0
                            and abs((float(m.group(4)) - float(m.group(2))) - .10125) < .03,
                            40, hero + ' has a stable first-person camera inside tunnel', client)
                if hero == 'atom':
                    # Expansion cannot clip through the roof while the player remains inside.
                    start_server = len(SERVER.read_text(errors='replace'))
                    xdo('key', 'c')
                    time.sleep(3)
                    growth = (r'Size pose: side=server, hero=fiskheroes:atom, scale=' + NUMBER
                              + r', pose=STANDING, x=' + NUMBER + r', y=' + NUMBER + r', z=' + NUMBER
                              + r', height=' + NUMBER + r', eye=' + NUMBER)
                    wait_sample(SERVER, start_server, growth,
                                lambda m: .0625 < float(m.group(1)) < .56 and 2.1 < float(m.group(4)) < 12
                                and float(m.group(5)) <= 1.0,
                                30, 'growth stops before ceiling collision', server)
            # Ordinary players must retain vanilla crawling in the very same tunnel.
            command('execute as Dev run suit fiskheroes:the_flash')
            start_server = len(SERVER.read_text(errors='replace'))
            start_client = len(CLIENT.read_text(errors='replace'))
            pose = r'Size pose: side=server, hero=fiskheroes:the_flash, scale=1\.0, pose=SWIMMING,'
            wait_sample(SERVER, start_server, pose, lambda m: True, 30, 'normal player still crawls', server)
            camera = r'Size camera: hero=fiskheroes:the_flash, scale=1\.0, pose=SWIMMING,'
            wait_sample(CLIENT, start_client, camera, lambda m: True, 30, 'normal client sees crawling pose', client)
            print('PASS: scaled and vanilla tunnel poses verified', flush=True)
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    try: os.killpg(process.pid, signal.SIGTERM)
                    except ProcessLookupError: pass
            for process in reversed(processes):
                try: process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    try: os.killpg(process.pid, signal.SIGKILL)
                    except ProcessLookupError: pass
                    process.wait(timeout=10)


if __name__ == '__main__':
    main()
