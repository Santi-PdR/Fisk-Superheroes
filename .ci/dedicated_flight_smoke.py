#!/usr/bin/env python3
"""Drive real Forge client flight and tentacle interactions on a dedicated server."""
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

            # Baseline: fall for over four seconds with no suit, before any flight
            # or wall-crawl grace state has ever existed for this player.
            command('gamemode creative Dev')
            command('tp Dev 0 260 0 0 0')
            command('fill -8 -50 -8 8 -50 8 minecraft:stone')
            command('fill -1 259 -1 1 259 1 minecraft:stone')
            command('tp Dev 0.5 260 0.5 0 0')
            command('gamemode survival Dev')
            command('effect give Dev minecraft:resistance 120 255 true')
            command('scoreboard objectives add flight_test dummy')
            command('execute store result score #fallstart flight_test run data get entity Dev Pos[1] 100')
            xdo('keydown', 'w')
            time.sleep(2)
            xdo('keyup', 'w')
            time.sleep(7)
            command('execute store result score #fallend flight_test run data get entity Dev Pos[1] 100')
            command('scoreboard players operation #fallstart flight_test -= #fallend flight_test')
            command('execute if score #fallstart flight_test matches 10000.. run say ORDINARY_LONG_FALL_OK')
            wait_for(SERVER_LOG, r'ORDINARY_LONG_FALL_OK', 20, 'ordinary long fall without a suit', server)
            if re.search(r'Dev lost connection|flying is not enabled|Dev was kicked',
                         SERVER_LOG.read_text(errors='replace'), re.IGNORECASE):
                raise RuntimeError('Player disconnected during ordinary long fall')
            command('gamemode survival Dev')
            command('suit fiskheroes:vision')
            wait_for(CLIENT_LOG, r'\[CHAT\] Equipped Vision', 30, 'Vision suit equipped on dedicated server', client)
            command('tp Dev 0 220 0 0 0')
            xdo('key', 'v')
            wait_for(SERVER_LOG, r'Ability modifier executed: hero=fiskheroes:vision, key=INTANGIBILITY',
                     30, 'flight activation reached dedicated server', server)
            time.sleep(3)
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
            # Exercise actual key packets and server-side tentacle targeting as well. The
            # target is immobilized so the wall and health checks remain deterministic.
            command('gamemode creative Dev')
            command('tp Dev 0 180 0 0 0')
            command('fill -2 179 0 2 179 8 minecraft:stone')
            command('fill -2 180 0 2 184 8 minecraft:air')
            command('suit fiskheroes:doctor_octopus')
            wait_for(CLIENT_LOG, r'\[CHAT\] Equipped Doctor Octopus', 30, 'Doctor Octopus suit equipped', client)
            command('summon minecraft:iron_golem 0.5 180 5.5 {NoAI:1b,NoGravity:1b,Tags:["tentacle_smoke"]}')
            command('setblock 0 181 2 minecraft:stone')
            command('tp Dev 0 180 0 0 0')
            xdo('key', 'F5')  # Render the local player and all four mechanical arms.
            xdo('key', 'g')
            wait_for(SERVER_LOG, r'Ability modifier executed: hero=fiskheroes:doctor_octopus, key=TENTACLES',
                     30, 'deploy tentacles through real client', server)
            time.sleep(2)
            xdo('key', 'c')
            wait_for(SERVER_LOG, r'Ability modifier executed: hero=fiskheroes:doctor_octopus, key=TENTACLE_JAB',
                     30, 'send tentacle jab through real client', server)
            wait_for(CLIENT_LOG, r'Tentacle action pose rendered: player=Dev, action=1, arm=[0-3]',
                     30, 'dedicated server jab animates the client tentacle', client)
            xdo('key', 'v')
            wait_for(CLIENT_LOG, r'Octopus claw grab pose applied', 30,
                     'grab opens the actual animated claw model', client)
            command('execute if data entity @e[type=minecraft:iron_golem,tag=tentacle_smoke,limit=1] {Health:100.0f} run say TENTACLE_BLOCKED_OK')
            wait_for(SERVER_LOG, r'TENTACLE_BLOCKED_OK', 20, 'dedicated tentacle jab is blocked by wall', server)
            command('setblock 0 181 2 minecraft:air')
            xdo('keydown', 'b')
            wait_for(SERVER_LOG, r'Ability modifier executed: hero=fiskheroes:doctor_octopus, key=TENTACLE_STRIKE',
                     30, 'charge tentacle strike through real client', server)
            wait_for(CLIENT_LOG, r'Octopus claw strike pose applied', 30,
                     'charging animates the original claw blade model', client)
            subprocess.run(['scrot', 'ci-tentacle-charge.jpg'], check=True, timeout=30)
            dimensions = subprocess.check_output(['identify', '-format', '%wx%h', 'ci-tentacle-charge.jpg'],
                                                 text=True, timeout=30).strip()
            if dimensions != '1280x720':
                raise RuntimeError(f'Unexpected screenshot dimensions: {dimensions}')
            # In this fixed third-person scene the upper arms cross clear sky in
            # both regions. Logs alone missed a 1/16 scale regression: require
            # substantial dark geometry on both sides, not merely a rendered part.
            for region in ('110x120+470+290', '110x120+700+290'):
                fraction = float(subprocess.check_output([
                    'convert', 'ci-tentacle-charge.jpg', '-crop', region, '+repage',
                    '-colorspace', 'gray', '-threshold', '30%', '-format', '%[fx:1-mean]', 'info:'
                ], text=True, timeout=30))
                if fraction < 0.02:
                    raise RuntimeError(f'Tentacle geometry too small or missing in {region}: {fraction:.4f}')
                print(f'PASS: visible tentacle geometry in {region}: {fraction:.4f}', flush=True)
            time.sleep(2)
            xdo('key', 'g')
            time.sleep(1)
            xdo('keyup', 'b')
            time.sleep(2)
            command('execute if data entity @e[type=minecraft:iron_golem,tag=tentacle_smoke,limit=1] {Health:100.0f} run say TENTACLE_CANCEL_OK')
            wait_for(SERVER_LOG, r'TENTACLE_CANCEL_OK', 20, 'retracting cancels held strike on server', server)
            xdo('key', 'g')
            time.sleep(1)
            xdo('key', 'c')
            time.sleep(2)
            command('execute store result score #tentacle flight_test run data get entity @e[type=minecraft:iron_golem,tag=tentacle_smoke,limit=1] Health 1')
            command('execute if score #tentacle flight_test matches ..99 run say TENTACLE_HIT_OK')
            wait_for(SERVER_LOG, r'TENTACLE_HIT_OK', 20, 'dedicated tentacle jab hits exposed target', server)
            print('PASS: dedicated client tentacle smoke completed', flush=True)
            xdo('key', 'F5')
            time.sleep(1)
            xdo('key', 'F5')  # Return to first person for the remaining tests.
            # Passive flight has different physics and is gated by Obsidian's shadowform.
            # Reuse the existing high platform, avoiding large teleports while velocity
            # packets from the previous scenario can still be in flight.
            command('fill -2 -50 0 2 -50 3 minecraft:stone')
            command('effect give Dev minecraft:resistance 120 255 true')
            command('suit fiskheroes:obsidian')
            wait_for(CLIENT_LOG, r'\[CHAT\] Equipped Obsidian', 30, 'Obsidian suit equipped', client)
            command('gamemode survival Dev')
            xdo('key', 'c')
            wait_for(SERVER_LOG, r'Ability modifier executed: hero=fiskheroes:obsidian, key=SHADOWFORM',
                     30, 'activate shadowform through real client', server)
            # Rapidly sample airborne state before the small takeoff hop ends, without
            # pressing jump. This distinguishes the original ground impulse from flight input.
            xdo('key', '--clearmodifiers', 'slash')
            time.sleep(0.35)
            xdo('type', '--clearmodifiers', '--delay', '1',
                'execute if entity @a[name=Dev,nbt={OnGround:0b}] run say SHADOWFORM_TAKEOFF_OK')
            time.sleep(0.2)
            xdo('key', 'Return')
            wait_for(SERVER_LOG, r'SHADOWFORM_TAKEOFF_OK', 20, 'shadowform lifts player from ground without jumping', server)
            command('fill -2 179 0 2 179 8 minecraft:air')
            command('execute store result score #start flight_test run data get entity Dev Pos[1] 100')
            xdo('keydown', 'space')
            time.sleep(2)
            xdo('keyup', 'space')
            command('execute store result score #end flight_test run data get entity Dev Pos[1] 100')
            command('execute if score #end flight_test > #start flight_test run say PASSIVE_FLIGHT_UP_OK')
            wait_for(SERVER_LOG, r'PASSIVE_FLIGHT_UP_OK', 20, 'Obsidian shadowform ascends in survival', server)
            # Let the 400-tick shadowform expire, then wait over four seconds of falling.
            # The player must not be kicked when suit flight becomes inactive midair.
            wait_for(CLIENT_LOG, r'Playing pack sound definition fiskheroes:shadowform_disable',
                     30, 'shadowform naturally expires', client)
            time.sleep(8)
            command('execute if entity Dev run say PASSIVE_FLIGHT_RECOVERY_OK')
            wait_for(SERVER_LOG, r'PASSIVE_FLIGHT_RECOVERY_OK', 20, 'post-flight descent remains connected', server)
            if re.search(r'Dev lost connection|flying is not enabled|Dev was kicked',
                         SERVER_LOG.read_text(errors='replace'), re.IGNORECASE):
                raise RuntimeError('Player disconnected during passive flight or its descent')
            print('PASS: dedicated passive flight smoke completed', flush=True)
            # The block and entity are outside vanilla's normal use reach; the mod must
            # request a server-side raycast even while the empty hand points into air.
            command('gamemode creative Dev')
            command('tp Dev 0 180 0 0 0')
            command('fill -2 179 -2 2 179 14 minecraft:stone')
            command('fill -2 180 -2 2 184 14 minecraft:air')
            command('kill @e[type=minecraft:iron_golem,tag=tentacle_smoke]')
            command('setblock 0 181 12 minecraft:stone')
            command('suit fiskheroes:spider_man')
            wait_for(CLIENT_LOG, r'\[CHAT\] Equipped Spider-Man', 30, 'Spider-Man equipped for web test', client)
            command('tp Dev 0.5 180 0.5 0 0')
            xdo('key', 'b')
            wait_for(SERVER_LOG, r'Ability callback executed: hero=fiskheroes:spider_man, key=func_WEB_SWINGING',
                     30, 'enable web swinging through real client', server)
            time.sleep(2)
            command('execute store result score #webstart flight_test run data get entity Dev Pos[2] 100')
            xdo('mousedown', '3')
            time.sleep(2)
            xdo('mouseup', '3')
            command('execute if data entity Dev ForgeData.FiskHeroesWebSwingAnchor{Type:"block"} run say WEB_BLOCK_ATTACH_OK')
            wait_for(SERVER_LOG, r'WEB_BLOCK_ATTACH_OK', 20, 'long-range empty-hand web attaches to block once while held', server)
            command('execute store result score #webend flight_test run data get entity Dev Pos[2] 100')
            command('execute if score #webend flight_test > #webstart flight_test run say WEB_LAUNCH_OK')
            wait_for(SERVER_LOG, r'WEB_LAUNCH_OK', 20, 'web launch impulse moves the real client', server)
            xdo('click', '3')
            command('execute unless data entity Dev ForgeData.FiskHeroesWebSwingAnchor run say WEB_RELEASE_OK')
            wait_for(SERVER_LOG, r'WEB_RELEASE_OK', 20, 'second use releases web', server)
            command('tp Dev 0.5 180 0.5 0 0')
            command('summon minecraft:iron_golem 0.5 180 7.5 {NoAI:1b,NoGravity:1b,Tags:["web_target"]}')
            xdo('click', '3')
            command('execute if data entity Dev ForgeData.FiskHeroesWebSwingAnchor{Type:"entity"} run say WEB_ENTITY_ATTACH_OK')
            wait_for(SERVER_LOG, r'WEB_ENTITY_ATTACH_OK', 20, 'long-range web attaches to a living entity', server)
            command('execute store result score #webx flight_test run data get entity Dev ForgeData.FiskHeroesWebSwingAnchor.X 100')
            command('tp @e[tag=web_target,limit=1] 1.5 180 7.5')
            command('execute store result score #webx2 flight_test run data get entity Dev ForgeData.FiskHeroesWebSwingAnchor.X 100')
            command('execute if score #webx2 flight_test > #webx flight_test run say WEB_ENTITY_MOVES_OK')
            wait_for(SERVER_LOG, r'WEB_ENTITY_MOVES_OK', 20, 'web follows the moving entity anchor', server)
            xdo('click', '3')
            command('execute unless data entity Dev ForgeData.FiskHeroesWebSwingAnchor run say WEB_ENTITY_RELEASE_OK')
            wait_for(SERVER_LOG, r'WEB_ENTITY_RELEASE_OK', 20, 'entity web releases cleanly', server)
            xdo('key', 'b')
            command('kill @e[tag=web_target]')
            command('setblock 0 181 12 minecraft:air')
            print('PASS: dedicated long-range web interactions completed', flush=True)
            # Test actual client prediction and synchronized movement keys on a tall wall.
            command('gamemode creative Dev')
            command('tp Dev 0 180 20 0 0')
            command('fill -2 179 8 2 179 26 minecraft:stone')
            command('fill -2 180 18 2 210 26 minecraft:air')
            command('fill -2 180 22 2 210 22 minecraft:stone')
            command('suit fiskheroes:spider_man')
            wait_for(CLIENT_LOG, r'\[CHAT\] Equipped Spider-Man', 30, 'Spider-Man equipped on dedicated server', client)
            command('tp Dev 0.5 180 21.65 0 0')
            command('gamemode survival Dev')
            command('effect give Dev minecraft:resistance 120 255 true')
            command('execute store result score #crawlstart flight_test run data get entity Dev Pos[1] 100')
            xdo('keydown', 'w')
            xdo('keydown', 'space')
            time.sleep(0.3)
            xdo('keyup', 'space')
            time.sleep(3)
            xdo('keyup', 'w')
            command('execute store result score #crawltop flight_test run data get entity Dev Pos[1] 100')
            command('scoreboard players operation #crawldelta flight_test = #crawltop flight_test')
            command('scoreboard players operation #crawldelta flight_test -= #crawlstart flight_test')
            command('execute if score #crawldelta flight_test matches 200.. run say WALL_CLIMB_UP_OK')
            wait_for(SERVER_LOG, r'WALL_CLIMB_UP_OK', 20, 'Spider-Man climbs a wall in survival', server)
            # No directional input: contact should survive without repeated horizontal collisions.
            time.sleep(6)
            command('execute store result score #crawlrest flight_test run data get entity Dev Pos[1] 100')
            command('scoreboard players operation #crawldelta flight_test = #crawlrest flight_test')
            command('scoreboard players operation #crawldelta flight_test -= #crawltop flight_test')
            command('execute if score #crawldelta flight_test matches -100..100 run say WALL_CLIMB_REST_OK')
            wait_for(SERVER_LOG, r'WALL_CLIMB_REST_OK', 20, 'Spider-Man stays attached while idle', server)
            xdo('keydown', 's')
            time.sleep(2)
            xdo('keyup', 's')
            # The platform extends behind the wall: land and remain connected after release.
            time.sleep(8)
            command('execute store result score #crawloff flight_test run data get entity Dev Pos[1] 100')
            command('execute if score #crawloff flight_test < #crawlrest flight_test run say WALL_CLIMB_RELEASE_OK')
            wait_for(SERVER_LOG, r'WALL_CLIMB_RELEASE_OK', 20, 'moving away releases the wall', server)
            if re.search(r'Dev lost connection|flying is not enabled|Dev was kicked',
                         SERVER_LOG.read_text(errors='replace'), re.IGNORECASE):
                raise RuntimeError('Player disconnected during wall crawling')
            print('PASS: dedicated wall crawl smoke completed', flush=True)
            # Reproduce the original failure: land after crawling, then walk off the
            # high platform and fall for several seconds with no wall or flight active.
            command('fill -2 179 -20 2 179 7 minecraft:air')
            command('execute store result score #ledge flight_test run data get entity Dev Pos[1] 100')
            xdo('keydown', 's')
            time.sleep(4)
            xdo('keyup', 's')
            time.sleep(7)
            command('execute store result score #below flight_test run data get entity Dev Pos[1] 100')
            command('scoreboard players operation #ledge flight_test -= #below flight_test')
            command('execute if score #ledge flight_test matches 10000.. run say POST_WALL_LONG_FALL_OK')
            wait_for(SERVER_LOG, r'POST_WALL_LONG_FALL_OK', 20, 'long fall after wall landing stays connected', server)
            if re.search(r'Dev lost connection|flying is not enabled|Dev was kicked',
                         SERVER_LOG.read_text(errors='replace'), re.IGNORECASE):
                raise RuntimeError('Player disconnected after leaving the wall platform')
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
