#!/usr/bin/env python3
"""Exercise a real Cold Gun loop on the shooter and a remote tracking client."""
import os
import array
import math
import signal
import subprocess
import time
from pathlib import Path
from held_livery_multiplayer import wait_for, wait_for_new


def focus(window):
    subprocess.run(['xdotool', 'windowfocus', '--sync', window], check=True, timeout=30)
    subprocess.run(['xdotool', 'mousemove', '--window', window, '640', '360', 'click', '1'], check=True, timeout=30)


def xdo(*args):
    subprocess.run(['xdotool', *args], check=True, timeout=30)


def capture_rms(label):
    path = Path('ci-cold-sound-' + label + '.raw')
    with path.open('wb') as out:
        recorder = subprocess.Popen(['parec', '--device=fisk_capture.monitor', '--raw',
                                     '--format=s16le', '--rate=44100', '--channels=2'], stdout=out)
        try: time.sleep(2)
        finally:
            recorder.terminate()
            recorder.wait(timeout=10)
    samples = array.array('h')
    samples.frombytes(path.read_bytes())
    if len(samples) < 88200: raise RuntimeError(f'Audio capture {label} was too short')
    rms = math.sqrt(sum((x / 32768.0) ** 2 for x in samples) / len(samples))
    print(f'Observer audio {label}: RMS={rms:.6f}', flush=True)
    return rms


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciColdGunSoundDiagnostics=true')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    paths = [Path('ci-cold-sound-' + role + '.log') for role in ('server', 'observer', 'shooter')]
    server_log, observer_log, shooter_log = paths
    processes = []
    with server_log.open('w') as sout, observer_log.open('w') as oout, shooter_log.open('w') as wout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-cold-sound-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            server.stdin.write('difficulty peaceful\n'); server.stdin.flush()
            observer = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-cold-sound-observer',
                                         '-PciUsername=Dev', '-PquickPlayServer=127.0.0.1:25565', *common],
                                        stdout=oout, stderr=subprocess.STDOUT,
                                        env=dict(env, ALSOFT_DRIVERS='pulse', PULSE_SINK='fisk_capture'), start_new_session=True)
            processes.append(observer)
            wait_for(server_log, r'Dev\[/.*logged in with entity id', 360, 'observer joined', observer)
            time.sleep(5)
            existing = set(subprocess.check_output(['xdotool', 'search', '--onlyvisible', '--name', 'Minecraft'],
                                                    text=True, timeout=30).strip().splitlines())
            shooter = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-cold-sound-shooter',
                                        '-PciUsername=Partner', '-PquickPlayServer=127.0.0.1:25565', *common],
                                       stdout=wout, stderr=subprocess.STDOUT, env=env, start_new_session=True)
            processes.append(shooter)
            wait_for(server_log, r'Partner\[/.*logged in with entity id', 360, 'shooter joined', shooter)
            time.sleep(8)
            windows = set(subprocess.check_output(['xdotool', 'search', '--onlyvisible', '--name', 'Minecraft'],
                                                   text=True, timeout=30).strip().splitlines()) - existing
            if len(windows) != 1: raise RuntimeError(f'Cannot identify shooter window: {windows}')
            window = windows.pop()

            def command(line):
                if any(p.poll() is not None for p in processes): raise RuntimeError('Minecraft exited unexpectedly')
                server.stdin.write(line + '\n'); server.stdin.flush(); time.sleep(1)

            command('gamemode spectator Dev')
            command('gamemode creative Partner')
            command('tp Dev 2 180 0 0 0')
            command('tp Partner 0 180 0 0 0')
            command('fill -5 179 -5 5 179 5 minecraft:stone')
            command('execute as Partner run suit fiskheroes:captain_cold')
            command('item replace entity Partner weapon.mainhand with fiskheroes:cold_gun')
            focus(window)
            time.sleep(2)
            xdo('mousedown', '1')
            try:
                wait_for(shooter_log, r'Cold gun loop ticked: Partner, volume=1\.0, active=true', 60, 'shooter loop plays', shooter)
                wait_for(observer_log, r'Cold gun loop ticked: Partner, volume=0\.[0-9]+, active=true', 60, 'remote loop plays with distance attenuation', observer)
                time.sleep(3)  # Exclude the short beam-start one-shot from both recordings.
                near_rms = capture_rms('near')
                start = len(observer_log.read_text(errors='replace'))
                command('tp Dev 72 180 0')
                wait_for_new(observer_log, r'Cold gun loop stopped: Partner', start, 60, 'loop stops outside 64 blocks', observer)
                start = len(observer_log.read_text(errors='replace'))
                command('tp Dev 58 180 0')
                wait_for_new(observer_log, r'Cold gun loop ticked: Partner, volume=0\.0[0-9]+, active=true', start, 60, 'stereo loop resumes softly near range limit', observer)
                far_rms = capture_rms('far')
                if far_rms < 0.0001 or near_rms < far_rms * 4:
                    raise RuntimeError(f'Stereo distance falloff missing in audio output: {near_rms=} {far_rms=}')
                print('PASS: remote audio is quieter near the 64-block limit', flush=True)
                start = len(observer_log.read_text(errors='replace'))
                command('tp Dev 2 180 0')
                wait_for_new(observer_log, r'Cold gun loop ticked: Partner, volume=0\.9[0-9]+, active=true', start, 60, 'stereo loop gets louder approaching shooter', observer)
                start = len(observer_log.read_text(errors='replace'))
                command('tp Dev 220 180 0')
                wait_for_new(observer_log, r'Cold gun loop stopped: Partner', start, 60, 'loop stops when player untracks', observer)
                start = len(observer_log.read_text(errors='replace'))
                command('tp Dev 2 180 0')
                wait_for_new(observer_log, r'Cold gun loop ticked: Partner, volume=0\.[0-9]+, active=true', start, 60, 'late tracking synchronizes active beam', observer)
                start = len(observer_log.read_text(errors='replace'))
                command('item replace entity Partner weapon.mainhand with minecraft:stick')
                wait_for_new(observer_log, r'Cold gun loop stopped: Partner', start, 60, 'changing held item stops remote loop', observer)
            finally:
                xdo('mouseup', '1')
            before = len(observer_log.read_text(errors='replace'))
            command('item replace entity Partner weapon.mainhand with fiskheroes:cold_gun')
            time.sleep(1)
            xdo('keydown', 'Shift_L')
            try:
                time.sleep(0.5); xdo('click', '3'); time.sleep(0.5)
            finally:
                xdo('keyup', 'Shift_L')
            command('execute if data entity Partner SelectedItem.tag{GrenadeMode:1b} run say SOUND_GRENADE_MODE_OK')
            wait_for(server_log, r'SOUND_GRENADE_MODE_OK', 30, 'grenade mode enabled', server)
            xdo('mousedown', '1')
            try: time.sleep(2)
            finally: xdo('mouseup', '1')
            time.sleep(2)
            if 'Cold gun loop started: Partner' in observer_log.read_text(errors='replace')[before:]:
                raise RuntimeError('Grenade mode unexpectedly started beam loop')
            print('PASS: grenade mode does not start beam loop', flush=True)
        finally:
            for p in reversed(processes):
                if p.poll() is None:
                    try: os.killpg(p.pid, signal.SIGTERM)
                    except ProcessLookupError: pass
            for p in reversed(processes):
                try: p.wait(timeout=30)
                except subprocess.TimeoutExpired:
                    try: os.killpg(p.pid, signal.SIGKILL)
                    except ProcessLookupError: pass


if __name__ == '__main__':
    main()
