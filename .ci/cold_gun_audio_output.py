#!/usr/bin/env python3
"""Capture actual client output on a virtual audio sink while firing the Cold Gun."""
import os
import signal
import subprocess
import time
from pathlib import Path

import numpy as np
from tonfa_visual_smoke import wait_for, xdo

RATE = 44100


def capture(name, seconds=2):
    path = Path('ci-cold-audio-' + name + '.raw')
    with path.open('wb') as output:
        proc = subprocess.Popen(['parec', '--device=fisk_capture.monitor', '--raw',
                                 '--format=s16le', '--rate=44100', '--channels=2'], stdout=output)
        try:
            time.sleep(seconds)
        finally:
            proc.terminate()
            proc.wait(timeout=10)
    data = np.fromfile(path, dtype='<i2').astype(np.float64).reshape(-1, 2) / 32768.0
    if len(data) < RATE: raise RuntimeError(f'{name}: virtual sink recorded too few samples ({len(data)})')
    rms = float(np.sqrt(np.mean(data * data)))
    print(f'Audio {name}: {len(data)} frames, RMS={rms:.6f}', flush=True)
    return data, rms


def similarity(reference, recorded):
    # Align one captured second with the entire 8-second source (including the loop seam).
    # A normalized correlation distinguishes the actual loop from incidental game sounds.
    template = recorded[len(recorded)//3:len(recorded)//3 + RATE].mean(axis=1)
    template -= template.mean()
    source = np.tile(reference.mean(axis=1), 2)
    source -= source.mean()
    n = len(template)
    size = 1 << (len(source) + n - 1).bit_length()
    correlation = np.fft.irfft(np.fft.rfft(source, size) * np.fft.rfft(template[::-1], size), size)[n-1:len(source)]
    power = np.concatenate(([0.0], np.cumsum(source * source)))
    energy = power[n:] - power[:-n]
    score = np.abs(correlation) / np.sqrt(np.maximum(energy, 1e-20) * max(float(template @ template), 1e-20))
    return float(np.max(score))


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='pulse',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciColdGunSoundDiagnostics=true')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    server_log, client_log = Path('ci-cold-audio-server.log'), Path('ci-cold-audio-client.log')
    processes = []
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-cold-audio-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-cold-audio-client',
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
                server.stdin.write(line + '\n'); server.stdin.flush(); time.sleep(1)
            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 5 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:captain_cold')
            command('item replace entity Dev weapon.mainhand with fiskheroes:cold_gun')
            time.sleep(4)
            _, baseline = capture('before')
            xdo('mousedown', '1')
            try:
                wait_for(client_log, r'Cold gun loop ticked: Dev, volume=1\.0, active=true', 60, 'loop reached sound engine', client)
                time.sleep(3)  # Let the one-shot start sound finish.
                loop, beam_rms = capture('beam')
            finally:
                xdo('mouseup', '1')
            wait_for(client_log, r'Cold gun loop stopped: Dev', 60, 'loop stopped on release', client)
            time.sleep(3)  # Let the one-shot stop sound finish.
            _, after = capture('after')
            if beam_rms < 0.001 or beam_rms < max(baseline, after) * 2:
                raise RuntimeError(f'Beam output not clearly present: before={baseline}, beam={beam_rms}, after={after}')
            raw = subprocess.check_output(['ffmpeg', '-v', 'error', '-i',
                'run-cold-audio-client/fiskheroes_sounds/pack/assets/fiskheroes/sounds/item/cold_gun/beam_loop.ogg',
                '-f', 'f32le', '-ac', '2', '-ar', str(RATE), '-'], timeout=30)
            reference = np.frombuffer(raw, dtype='<f4').reshape(-1, 2).astype(np.float64)
            score = similarity(reference, loop)
            print(f'Beam/source normalized correlation: {score:.4f}', flush=True)
            if score < 0.05: raise RuntimeError('Recorded output does not match the upstream loop waveform')
            print('PASS: actual virtual-sink audio matches the upstream loop and stops after release', flush=True)
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
