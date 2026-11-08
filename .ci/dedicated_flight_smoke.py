#!/usr/bin/env python3
"""Drive a real Forge client against an offline dedicated Forge server in survival mode."""
import os
import re
import signal
import subprocess
import time
from pathlib import Path

SERVER_LOG = Path('ci-dedicated-server.log')
CLIENT_LOG = Path('ci-dedicated-client.log')


def wait_for(path, pattern, seconds, label, process):
    deadline = time.monotonic() + seconds
    regex = re.compile(pattern, re.IGNORECASE)
    while time.monotonic() < deadline:
        text = path.read_text(errors='replace') if path.exists() else ''
        match = regex.search(text)
        if match:
            print('PASS:', label, flush=True)
            return match
        if process.poll() is not None:
            raise RuntimeError(f'{label}: process exited with code {process.returncode}')
        time.sleep(0.5)
    raise RuntimeError(f'Timed out: {label}')


def xdo(*args):
    subprocess.run(['xdotool', *args], check=True, timeout=30)


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null')
    processes = []
    with SERVER_LOG.open('w') as server_output, CLIENT_LOG.open('w') as client_output:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-dedicated',
                                       '--no-daemon', '--console=plain'], stdin=subprocess.PIPE,
                                      stdout=server_output, stderr=subprocess.STDOUT, env=env,
                                      text=True, start_new_session=True)
            processes.append(server)
            wait_for(SERVER_LOG, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-dedicated-client',
                                       '-PquickPlayServer=127.0.0.1:25565', '--no-daemon', '--console=plain'],
                                      stdout=client_output, stderr=subprocess.STDOUT, env=env, start_new_session=True)
            processes.append(client)
            wait_for(SERVER_LOG, r'Dev\[/.*logged in with entity id', 360, 'client joined dedicated server', client)
            time.sleep(8)
            window = subprocess.check_output(['xdotool', 'search', '--onlyvisible', '--name', 'Minecraft'],
                                             text=True, timeout=30).strip().splitlines()[-1]
            xdo('windowfocus', '--sync', window)
            xdo('mousemove', '--window', window, '640', '360', 'click', '1')
            time.sleep(2)

            def command(text):
                if server.poll() is not None or client.poll() is not None:
                    raise RuntimeError('Dedicated server or client exited')
                xdo('key', '--clearmodifiers', 'slash')
                time.sleep(0.8)
                xdo('type', '--clearmodifiers', '--delay', '15', text)
                time.sleep(0.4)
                xdo('key', 'Return')
                time.sleep(1)

            command('gamemode survival Dev')
            command('suit fiskheroes:vision')
            wait_for(CLIENT_LOG, r'\[CHAT\] Equipped Vision', 30, 'Vision suit equipped on dedicated server', client)
            command('tp Dev 0 220 0 0 0')
            xdo('key', 'v')
            wait_for(SERVER_LOG, r'Ability modifier executed: hero=fiskheroes:vision, key=INTANGIBILITY',
                     30, 'flight activation reached dedicated server', server)
            time.sleep(3)
            command('scoreboard objectives add flight_test dummy')
            command('execute store result score #start flight_test run data get entity Dev Pos[1] 100')
            # Vanilla rejects sustained flight after roughly four seconds when allow-flight is
            # false. Stay airborne substantially longer and verify the client remains connected.
            time.sleep(12)
            command('execute store result score #end flight_test run data get entity Dev Pos[1] 100')
            command('scoreboard players operation #delta flight_test = #end flight_test')
            command('scoreboard players operation #delta flight_test -= #start flight_test')
            command('scoreboard players get #delta flight_test')
            wait_for(CLIENT_LOG, r'#delta has (-?\d+) \[flight_test\]', 20, 'server measured hover delta', client)
            text = SERVER_LOG.read_text(errors='replace')
            client_text = CLIENT_LOG.read_text(errors='replace')
            value = int(re.findall(r'#delta has (-?\d+) \[flight_test\]', client_text)[-1])
            if abs(value) > 100:
                raise RuntimeError(f'Dedicated server hover drifted by {value / 100:.2f} blocks')
            if re.search(r'Dev lost connection|flying is not enabled|Dev was kicked', text, re.IGNORECASE):
                raise RuntimeError('Player was disconnected during dedicated server flight')
            print(f'PASS: dedicated survival hover, delta={value / 100:.2f} blocks', flush=True)
            command('execute store result score #start flight_test run data get entity Dev Pos[1] 100')
            xdo('keydown', 'space')
            time.sleep(2)
            xdo('keyup', 'space')
            command('execute store result score #end flight_test run data get entity Dev Pos[1] 100')
            command('scoreboard players operation #delta flight_test = #end flight_test')
            command('scoreboard players operation #delta flight_test -= #start flight_test')
            command('execute if score #delta flight_test matches 50.. run say DEDICATED_FLIGHT_UP_OK')
            wait_for(SERVER_LOG, r'DEDICATED_FLIGHT_UP_OK', 20, 'dedicated survival flight ascends', server)
            command('execute store result score #start flight_test run data get entity Dev Pos[2] 100')
            xdo('keydown', 'w')
            time.sleep(2)
            xdo('keyup', 'w')
            command('execute store result score #end flight_test run data get entity Dev Pos[2] 100')
            command('scoreboard players operation #delta flight_test = #end flight_test')
            command('scoreboard players operation #delta flight_test -= #start flight_test')
            command('execute if score #delta flight_test matches 50.. run say DEDICATED_FLIGHT_FORWARD_OK')
            wait_for(SERVER_LOG, r'DEDICATED_FLIGHT_FORWARD_OK', 20, 'dedicated survival flight steers forward', server)
            text = SERVER_LOG.read_text(errors='replace') + CLIENT_LOG.read_text(errors='replace')
            if re.search(r'Dev lost connection|flying is not enabled|#@!@# Game crashed|\[.*?/FATAL\]', text, re.IGNORECASE):
                raise RuntimeError('Client/server reported a flight disconnect or crash')
            print('PASS: dedicated client flight smoke completed', flush=True)
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
