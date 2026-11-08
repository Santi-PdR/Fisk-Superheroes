#!/usr/bin/env python3
"""Verify suit weapon liveries are resolved for the actual remote wearer."""
import os
import re
import signal
import subprocess
import time
from pathlib import Path


def wait_for(path, pattern, seconds, label, process):
    regex = re.compile(pattern)
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if path.exists() and regex.search(path.read_text(errors='replace')):
            print('PASS:', label, flush=True)
            return
        if process.poll() is not None:
            raise RuntimeError(f'{label}: process exited with {process.returncode}')
        time.sleep(1)
    raise RuntimeError(f'Timed out: {label}')


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    processes = []
    server_log = Path('ci-held-multiplayer-server.log')
    observer_log = Path('ci-held-multiplayer-observer.log')
    wearer_log = Path('ci-held-multiplayer-wearer.log')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    with server_log.open('w') as sout, observer_log.open('w') as oout, wearer_log.open('w') as wout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-livery-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            # Flat-world slimes can kill the observer while the second client starts.
            server.stdin.write('difficulty peaceful\n')
            server.stdin.flush()
            wait_for(server_log, r'(?i)difficulty has been set to peaceful', 30, 'prevent hostile mobs during startup', server)
            observer = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-livery-observer',
                                         '-PciUsername=Dev', '-PquickPlayServer=127.0.0.1:25565', *common],
                                        stdout=oout, stderr=subprocess.STDOUT, env=env, start_new_session=True)
            processes.append(observer)
            wait_for(server_log, r'Dev\[/.*logged in with entity id', 360, 'observer joined', observer)
            wearer = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-livery-wearer',
                                       '-PciUsername=Partner', '-PquickPlayServer=127.0.0.1:25565', *common],
                                      stdout=wout, stderr=subprocess.STDOUT, env=env, start_new_session=True)
            processes.append(wearer)
            wait_for(server_log, r'Partner\[/.*logged in with entity id', 360, 'second player joined', wearer)
            time.sleep(8)

            def command(line):
                if any(p.poll() is not None for p in processes):
                    raise RuntimeError('A Minecraft process exited unexpectedly')
                server.stdin.write(line + '\n')
                server.stdin.flush()
                time.sleep(1)

            command('say LIVERY_COMMAND_CHANNEL_OK')
            wait_for(server_log, r'LIVERY_COMMAND_CHANNEL_OK', 30, 'server command channel accepts input', server)
            command('gamemode creative Dev')
            command('gamemode creative Partner')
            command('tp Dev 0 185 0 0 0')
            command('tp Partner 0 185 4 180 0')
            time.sleep(3)
            command('fill -5 179 -5 5 179 10 minecraft:stone')
            command('tp Dev 0 180 0 0 0')
            command('tp Partner 0 180 4 180 0')
            command('execute as Dev run suit fiskheroes:the_flash')
            command('execute as Partner run suit fiskheroes:deadpool_xmen')
            command('item replace entity Dev weapon.mainhand with fiskheroes:desert_eagle')
            command('item replace entity Partner weapon.mainhand with fiskheroes:desert_eagle')
            command('execute at Dev if entity Partner[distance=..8] run say LIVERY_PLAYERS_NEARBY')
            wait_for(server_log, r'LIVERY_PLAYERS_NEARBY', 30, 'both players are within render range', server)
            wait_for(observer_log, r'Held weapon livery: wearer=Dev, hero=fiskheroes:the_flash, item=fiskheroes:desert_eagle, context=FIRST_PERSON_RIGHT_HAND, texture=default',
                     90, 'observer without a livery uses the default', observer)
            wait_for(observer_log, r'Held weapon livery: wearer=Partner, hero=fiskheroes:deadpool_xmen, item=fiskheroes:desert_eagle, context=THIRD_PERSON_RIGHT_HAND, texture=fiskheroes:textures/heroes/deadpool_xmen_gun\.png',
                     90, 'observer sees remote Deadpool pistol livery', observer)
            command('execute as Partner run suit fiskheroes:deathstroke_dceu')
            wait_for(observer_log, r'Held weapon livery: wearer=Partner, hero=fiskheroes:deathstroke_dceu, item=fiskheroes:desert_eagle, context=THIRD_PERSON_RIGHT_HAND, texture=fiskheroes:textures/heroes/deathstroke_dceu_gun\.png',
                     90, 'observer sees remote suit change update the pistol livery', observer)
            command('execute as Partner run suit fiskheroes:the_flash')
            wait_for(observer_log, r'Held weapon livery: wearer=Partner, hero=fiskheroes:the_flash, item=fiskheroes:desert_eagle, context=THIRD_PERSON_RIGHT_HAND, texture=default',
                     90, 'remote wearer without a livery restores the default', observer)
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    try:
                        os.killpg(process.pid, signal.SIGTERM)
                    except ProcessLookupError:
                        pass
            for process in reversed(processes):
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print(f'FAIL: {error}', flush=True)
        raise
