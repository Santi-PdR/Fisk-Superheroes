#!/usr/bin/env python3
"""Drive a real client in Xvfb; validate world entry, a suit, its render path and key input."""
import os
import re
import signal
import subprocess
import sys
import time
from pathlib import Path

log = Path('ci-client-world.log')


def wait_for(pattern, seconds, label, process):
    expression = re.compile(pattern, re.IGNORECASE)
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        text = log.read_text(errors='replace') if log.exists() else ''
        if expression.search(text):
            print('PASS:', label, flush=True)
            return
        if process.poll() is not None:
            raise RuntimeError(f'Client exited before {label}, code {process.returncode}')
        time.sleep(1)
    raise RuntimeError(f'Timed out waiting for {label}')


def xdo(*args):
    subprocess.run(['xdotool', *args], check=True, timeout=30)


def main():
    environment = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    with log.open('w') as output:
        process = subprocess.Popen(['./gradlew', 'runClient', '-PquickPlayWorld=SmokeWorld',
                                    '--no-daemon', '--console=plain'], stdout=output,
                                   stderr=subprocess.STDOUT, env=environment, start_new_session=True)
        try:
            wait_for(r'logged in with entity id', 360, 'integrated player login', process)
            time.sleep(8)  # Let the terrain-loading screen close before sending keyboard input.
            result = subprocess.check_output(['xdotool', 'search', '--onlyvisible', '--name', 'Minecraft'],
                                             text=True, timeout=30)
            window = result.strip().splitlines()[-1]
            xdo('windowfocus', '--sync', window)
            xdo('mousemove', '--window', window, '640', '360', 'click', '1')
            time.sleep(3)
            # Slash opens command chat directly; give the rendered UI a frame to receive it.
            xdo('key', '--clearmodifiers', 'slash')
            time.sleep(2)
            xdo('type', '--clearmodifiers', '--delay', '35', 'suit fiskheroes:the_flash')
            time.sleep(2)
            xdo('key', 'Return')
            wait_for(r'issued server command: /suit fiskheroes:the_flash', 45, 'suit command', process)
            time.sleep(3)
            xdo('key', 'F5')
            wait_for(r'Drawing suit fiskheroes:the_flash slot', 45, 'third-person suit render path', process)
            xdo('keydown', 'c')
            time.sleep(0.3)
            xdo('keyup', 'c')
            wait_for(r'Ability input accepted: hero=fiskheroes:the_flash, index=1', 45,
                     'server received the speedster ability key', process)
            time.sleep(4)
            text = log.read_text(errors='replace')
            if re.search(r'#@!@# Game crashed|\[.*?/FATAL\]|Error ticking modifier|Error running hero tick handler', text):
                raise RuntimeError('Client/server reported a crash or modifier error')
            print('PASS: client world smoke completed', flush=True)
        finally:
            try:
                os.killpg(process.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait()


if __name__ == '__main__':
    try:
        main()
    except Exception as exc:
        print('FAIL:', exc, file=sys.stderr)
        if log.exists():
            print('\n'.join(log.read_text(errors='replace').splitlines()[-100:]), file=sys.stderr)
        sys.exit(1)
