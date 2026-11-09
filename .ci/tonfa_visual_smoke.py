#!/usr/bin/env python3
"""Focused real-client geometry check using a diagnostic magenta tonfa texture.

The texture override is confined to the CI runner, never shipped in the mod.
"""
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
        if process.poll() is not None:
            raise RuntimeError(f'{label}: process exited with {process.returncode}')
        time.sleep(1)
    raise RuntimeError(f'Timed out: {label}')


def xdo(*args):
    subprocess.run(['xdotool', *args], check=True, timeout=30)


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    server_log, client_log = Path('ci-tonfa-server.log'), Path('ci-tonfa-client.log')
    processes = []
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-tonfa-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-tonfa-client',
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
                time.sleep(1.5)

            def screenshot(name):
                time.sleep(2)
                subprocess.run(['scrot', 'ci-tonfa-' + name + '.png'], check=True, timeout=30)
                print('Captured:', name, flush=True)

            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 5 minecraft:stone')
            command('tp Dev 0 180 0 0 0')
            command('execute as Dev run suit fiskheroes:black_canary')
            xdo('key', 'F5')
            wait_for(client_log, r'Equipped suit item rendered: hero=fiskheroes:black_canary, item=fiskheroes:tactical_tonfa',
                     60, 'stowed tonfas rendered', client)
            screenshot('stowed-back')
            xdo('key', 'F5')
            screenshot('stowed-front')
            command('item replace entity Dev weapon.mainhand with fiskheroes:tactical_tonfa{Dual:1b}')
            wait_for(client_log, r'Tonfa hand pose: wearer=Dev, context=THIRD_PERSON_LEFT_HAND, toggled=false',
                     60, 'third person dual tonfas', client)
            screenshot('held-front')
            xdo('key', 'F5')
            wait_for(client_log, r'Tonfa hand pose: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND, toggled=false',
                     60, 'first person tonfa', client)
            wait_for(client_log, r'First-person tonfa arm rendered: RIGHT',
                     60, 'right first-person arm rendered', client)
            wait_for(client_log, r'First-person tonfa arm rendered: LEFT',
                     60, 'left first-person arm rendered', client)
            screenshot('held-first-before')
            xdo('click', '1')
            wait_for(client_log, r'Tonfa hand pose: wearer=Dev, context=FIRST_PERSON_RIGHT_HAND, toggled=true',
                     60, 'tonfa flips on attack', client)
            screenshot('held-first-after')
            xdo('key', 'F5')
            xdo('key', 'F5')
            screenshot('held-front-after')
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    try:
                        os.killpg(process.pid, signal.SIGTERM)
                    except ProcessLookupError:
                        pass
            for process in reversed(processes):
                try:
                    process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()


if __name__ == '__main__':
    main()
