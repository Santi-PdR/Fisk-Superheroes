#!/usr/bin/env python3
"""Verify suit weapon liveries are resolved for the actual remote wearer."""
import os
import hashlib
import uuid
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


def wait_for_new(path, pattern, start, seconds, label, process):
    regex = re.compile(pattern)
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if path.exists() and regex.search(path.read_text(errors='replace')[start:]):
            print('PASS:', label, flush=True)
            return
        if process.poll() is not None:
            raise RuntimeError(f'{label}: process exited with {process.returncode}')
        time.sleep(1)
    raise RuntimeError(f'Timed out: {label}')


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciAimDiagnostics=true -Dfiskheroes.ciColdBallDiagnostics=true -Dfiskheroes.ciBowDiagnostics=true -Dfiskheroes.ciParticleDiagnostics=true -Dfiskheroes.ciMaskDiagnostics=true -Dfiskheroes.ciSpeedSoundDiagnostics=true -Dfiskheroes.ciVelocityDiagnostics=true -Dfiskheroes.ciShieldDiagnostics=true')
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
            time.sleep(4)
            observer_windows = set(subprocess.check_output(['xdotool', 'search', '--onlyvisible', '--name', 'Minecraft'],
                                                          text=True, timeout=30).strip().splitlines())
            wearer = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-livery-wearer',
                                       '-PciUsername=Partner', '-PquickPlayServer=127.0.0.1:25565', *common],
                                      stdout=wout, stderr=subprocess.STDOUT, env=env, start_new_session=True)
            processes.append(wearer)
            wait_for(server_log, r'Partner\[/.*logged in with entity id', 360, 'second player joined', wearer)
            time.sleep(8)
            windows = set(subprocess.check_output(['xdotool', 'search', '--onlyvisible', '--name', 'Minecraft'],
                                                  text=True, timeout=30).strip().splitlines())
            partner_windows = windows - observer_windows
            if len(partner_windows) != 1:
                raise RuntimeError(f'Cannot identify second Minecraft window: {partner_windows}')
            partner_window = partner_windows.pop()

            def command(line):
                if any(p.poll() is not None for p in processes):
                    raise RuntimeError('A Minecraft process exited unexpectedly')
                server.stdin.write(line + '\n')
                server.stdin.flush()
                time.sleep(1)

            command('say LIVERY_COMMAND_CHANNEL_OK')
            wait_for(server_log, r'\[Server\] LIVERY_COMMAND_CHANNEL_OK', 30, 'server command channel accepts input', server)
            command('gamemode creative Dev')
            command('gamemode creative Partner')
            command('tp Dev 0 185 0 0 0')
            command('tp Partner 0 185 4 180 0')
            time.sleep(3)
            command('fill -5 179 -5 5 179 10 minecraft:stone')
            command('tp Dev 0 180 0 0 0')
            command('tp Partner 0 180 4 180 0')
            if os.environ.get('CI_SOUND_INHERITANCE_FIXTURE') == '1':
                # Exercise an actual held drink through a client and dedicated server.
                command('gamemode survival Partner')
                command('item replace entity Partner hotbar.0 with fiskheroes:velocity_nine')
                subprocess.run(['xdotool', 'windowfocus', '--sync', partner_window], check=True, timeout=30)
                subprocess.run(['xdotool', 'mousemove', '--window', partner_window, '640', '360', 'click', '1'], check=True, timeout=30)
                subprocess.run(['xdotool', 'key', '1'], check=True, timeout=30)
                dose_start = len(server_log.read_text(errors='replace'))
                client_start = len(wearer_log.read_text(errors='replace'))
                subprocess.run(['xdotool', 'mousedown', '3'], check=True, timeout=30)
                try:
                    wait_for_new(server_log, r'Velocity 9 dose: entity=Partner, amplifier=0, duration=6000',
                                 dose_start, 45, 'held Velocity 9 finishes drinking on server', server)
                finally:
                    subprocess.run(['xdotool', 'mouseup', '3'], check=True, timeout=30)
                wait_for_new(wearer_log, r'Velocity 9 synchronized: entity=Partner, amplifier=0',
                             client_start, 45, 'Velocity 9 status reaches the drinking client', wearer)
                wait_for_new(server_log, r'Velocity 9 automatic speed: entity=Partner, level=3',
                             dose_start, 30, 'Velocity 9 automatically grants unsuited speed on server', server)
                wait_for_new(wearer_log, r'Velocity 9 synchronized speed: entity=Partner, level=3',
                             client_start, 30, 'unsuited speed level reaches the drinking client', wearer)
                bottle_start = len(server_log.read_text(errors='replace'))
                command('execute if data entity Partner SelectedItem{id:"minecraft:glass_bottle"} run say VELOCITY_BOTTLE_RETURNED')
                wait_for_new(server_log, r'\[Server\] VELOCITY_BOTTLE_RETURNED', bottle_start, 30,
                             'survival drink returns a glass bottle', server)
                # Original V9 supplies speed sounds and a blue trail without a suit.
                command('fill -5 180 9 5 183 9 minecraft:stone')
                command('tp Partner 0 180 5 0 0')
                v9_start = len(observer_log.read_text(errors='replace'))
                subprocess.run(['xdotool', 'keydown', 'w'], check=True, timeout=30)
                subprocess.run(['xdotool', 'keydown', 'Control_L'], check=True, timeout=30)
                try:
                    wait_for_new(observer_log, r'Playing pack sound definition fiskheroes:super_speed_move_loop_cw as',
                                 v9_start, 60, 'observer receives unsuited Velocity 9 movement loop', observer)
                    wait_for_new(observer_log, r'Playing pack sound definition fiskheroes:super_speed_sprint_zoom as',
                                 v9_start, 60, 'observer receives unsuited Velocity 9 sprint sound', observer)
                    wait_for_new(observer_log, r'Velocity 9 trail rendered: player=Partner, trail=fiskheroes:velocity_nine, color=0059FF, samples=[1-9]',
                                 v9_start, 60, 'observer renders unsuited blue Velocity 9 trail', observer)
                    stop_start = len(observer_log.read_text(errors='replace'))
                    command('effect clear Partner fiskheroes:velocity_nine')
                    wait_for_new(observer_log, r'Sound loop stopping: id=fiskheroes:super_speed_move_loop_cw, entity=',
                                 stop_start, 30, 'unsuited V9 loop stops when the effect ends', observer)
                finally:
                    subprocess.run(['xdotool', 'keyup', 'w'], check=True, timeout=30)
                    subprocess.run(['xdotool', 'keyup', 'Control_L'], check=True, timeout=30)
                command('fill -5 180 9 5 183 9 minecraft:air')
                command('tp Partner 0 180 4 180 0')
                command('item replace entity Partner hotbar.0 with minecraft:air')
                command('gamemode creative Partner')
            # The shield's pose must use the remote wearer, not the observing
            # client's own use state. Exercise its real held/released input.
            command('tp Dev 0 180 0 0 0')
            command('tp Partner 0 180 4 180 0')
            command('execute as Partner run suit fiskheroes:captain_america')
            pose_start = len(observer_log.read_text(errors='replace'))
            command('item replace entity Partner hotbar.0 with fiskheroes:captain_americas_shield')
            subprocess.run(['xdotool', 'windowfocus', '--sync', partner_window], check=True, timeout=30)
            subprocess.run(['xdotool', 'key', '1'], check=True, timeout=30)
            wait_for_new(observer_log, r'Shield item pose: wearer=Partner, context=THIRD_PERSON_RIGHT_HAND, blocking=false',
                         pose_start, 60, 'observer sees the remote shield at rest', observer)
            raised = len(observer_log.read_text(errors='replace'))
            subprocess.run(['xdotool', 'mousedown', '3'], check=True, timeout=30)
            try:
                wait_for_new(observer_log, r'Shield item pose: wearer=Partner, context=THIRD_PERSON_RIGHT_HAND, blocking=true',
                             raised, 60, 'observer sees the remote shield raised', observer)
                released = len(observer_log.read_text(errors='replace'))
            finally:
                subprocess.run(['xdotool', 'mouseup', '3'], check=True, timeout=30)
            wait_for_new(observer_log, r'Shield item pose: wearer=Partner, context=THIRD_PERSON_RIGHT_HAND, blocking=false',
                         released, 60, 'observer sees the remote shield lowered', observer)
            # A late-tracking observer must not derive magnetic flight from its own
            # younger entity tickCount. This deterministic probe uses the actual server
            # projectile and the remote player's UUID; no client input is synthesized.
            command('item replace entity Partner hotbar.0 with minecraft:air')
            command('fill -3 179 -18 3 179 -6 minecraft:stone')
            command('tp Partner 0 180 -15 0 0')
            command('tp Dev 0 180 -100 0 0')
            identity = uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:Partner').digest(), version=3)
            chunks = [int.from_bytes(identity.bytes[i:i + 4], 'big', signed=True) for i in range(0, 16, 4)]
            owner_tag = '[I;' + ','.join(str(value) for value in chunks) + ']'
            observer_start = len(observer_log.read_text(errors='replace'))
            server_start = len(server_log.read_text(errors='replace'))
            command('summon fiskheroes:thrown_shield 0 181.6 18 '
                    '{Owner:' + owner_tag + ',Tags:["magnetic_probe"],NoGravity:1b,Returning:0b,FlightAge:0,'
                    'Shield:{id:"fiskheroes:captain_americas_shield",Count:1b,'
                    'tag:{Electromagnetic:1b,MagneticProbe:73}},Motion:[0.0d,0.0d,0.0d]}')
            time.sleep(.5)
            command('tp Dev 0 180 0 0 0')
            wait_for_new(observer_log, r'Thrown shield model rendered: entity=\d+', observer_start, 30,
                         'late observer renders the remote projectile', observer)
            match = re.search(r'Thrown shield model rendered: entity=(\d+)',
                              observer_log.read_text(errors='replace')[observer_start:])
            entity_id = match.group(1)
            wait_for_new(observer_log, rf'Shield owner resolved: entity={entity_id}, owner=Partner', observer_start, 30,
                         'late observer resolves the projectile owner', observer)
            # Start the return only after the client has actually loaded both entities.
            command('data merge entity @e[type=fiskheroes:thrown_shield,tag=magnetic_probe,limit=1] {Returning:1b}')
            wait_for_new(server_log, rf'Shield magnetic return: side=server, entity={entity_id}, owner=Partner, magnetic=true',
                         server_start, 30, 'server starts delayed magnetic return', server)
            wait_for_new(observer_log, rf'Shield magnetic return: side=client, entity={entity_id}, owner=Partner, magnetic=true',
                         observer_start, 30, 'late observer receives authoritative magnetic state', observer)
            wait_for_new(server_log, rf'Shield recovered: entity={entity_id}, owner=Partner, magnetic=true',
                         server_start, 30, 'server recovers the magnetic shield', server)
            command('execute if data entity Partner Inventory[{id:"fiskheroes:captain_americas_shield",'
                    'tag:{Electromagnetic:1b,MagneticProbe:73}}] run say SHIELD_MAGNETIC_DATA_OK')
            wait_for_new(server_log, r'\[Server\] SHIELD_MAGNETIC_DATA_OK', server_start, 30,
                         'magnetic recovery preserves item data in the owner inventory', server)
            command('tp Partner 0 180 4 180 0')
            command('execute as Partner run suit fiskheroes:iron_man')
            wait_for(observer_log, r'Drawing suit fiskheroes:iron_man slot', 60,
                     'observer sees remote Iron Man suit', observer)
            subprocess.run(['xdotool', 'windowfocus', '--sync', partner_window], check=True, timeout=30)
            subprocess.run(['xdotool', 'mousemove', '--window', partner_window, '640', '360', 'click', '1'], check=True, timeout=30)
            subprocess.run(['xdotool', 'keydown', 'x'], check=True, timeout=30)
            try:
                wait_for(observer_log, r'Remote mask animation: wearer=Partner, phase=open, progress=',
                         60, 'observer sees the remote mask open', observer)
                wait_for(observer_log, r'Playing pack sound definition fiskheroes:iron_man_mk46_mask_open as',
                         30, 'observer receives the Mk 46 mask-open sound', observer)
            finally:
                subprocess.run(['xdotool', 'keyup', 'x'], check=True, timeout=30)
            time.sleep(1)
            subprocess.run(['xdotool', 'keydown', 'x'], check=True, timeout=30)
            try:
                wait_for(observer_log, r'Remote mask animation: wearer=Partner, phase=closing, progress=',
                         60, 'observer sees the remote mask close', observer)
                wait_for(observer_log, r'Playing pack sound definition fiskheroes:iron_man_mk46_mask_close as',
                         30, 'observer receives the Mk 46 mask-close sound', observer)
            finally:
                subprocess.run(['xdotool', 'keyup', 'x'], check=True, timeout=30)
            # Reopen before switching to a different hero: the new suit must not
            # inherit the old mask state on either the server or the observer.
            time.sleep(1)
            start = len(server_log.read_text(errors='replace'))
            subprocess.run(['xdotool', 'keydown', 'x'], check=True, timeout=30)
            try:
                wait_for_new(server_log, r'Mask toggled: player=Partner, open=true', start,
                             30, 'server reopens the old mask before changing hero', server)
            finally:
                subprocess.run(['xdotool', 'keyup', 'x'], check=True, timeout=30)
            time.sleep(2)
            command('execute as Partner run suit fiskheroes:iron_man_mk50')
            wait_for(observer_log, r'Remote mask reset: wearer=Partner, hero=fiskheroes:iron_man_mk50, open=false, timer=0, progress=0',
                     60, 'observer sees the new hero with a reset mask', observer)
            # Slow-motion sounds in the CW speed profile are ONLY_SELF. Verify
            # the intended player hears both events and a nearby observer does not.
            command('execute as Partner run suit fiskheroes:savitar rgb')
            time.sleep(2)
            wearer_start = len(wearer_log.read_text(errors='replace'))
            observer_start = len(observer_log.read_text(errors='replace'))
            subprocess.run(['xdotool', 'windowfocus', '--sync', partner_window], check=True, timeout=30)
            subprocess.run(['xdotool', 'keydown', 'v'], check=True, timeout=30)
            try:
                wait_for_new(wearer_log, r'Playing pack sound definition fiskheroes:slow_motion_enable_cw as',
                             wearer_start, 60, 'wearer receives speed profile slow-motion enable sound', wearer)
            finally: subprocess.run(['xdotool', 'keyup', 'v'], check=True, timeout=30)
            if os.environ.get('CI_SOUND_INHERITANCE_FIXTURE') == '1':
                wait_for_new(wearer_log, r'Sound packet: id=fiskheroes:slow_motion_enable_cw, loop=false, volume=0\.4',
                             wearer_start, 30, 'one-shot keeps explicit false and inherits grandparent volume', wearer)
            time.sleep(1)
            subprocess.run(['xdotool', 'keydown', 'v'], check=True, timeout=30)
            try:
                wait_for_new(wearer_log, r'Playing pack sound definition fiskheroes:slow_motion_disable_cw as',
                             wearer_start, 60, 'wearer receives speed profile slow-motion disable sound', wearer)
            finally: subprocess.run(['xdotool', 'keyup', 'v'], check=True, timeout=30)
            time.sleep(1)
            if re.search(r'Playing pack sound definition fiskheroes:slow_motion_(?:enable|disable)_cw as',
                         observer_log.read_text(errors='replace')[observer_start:]):
                raise RuntimeError('ONLY_SELF slow-motion sound leaked to nearby observer')
            print('PASS: slow-motion sounds remain private to the wearer', flush=True)
            # Drive the actual server-side motion transitions and check both
            # layers of the RGB profile on the nearby observing client.
            command('fill -4 179 -4 20 179 25 minecraft:stone')
            # The earlier unsuited-V9 wall would otherwise hide the remote
            # player from the observer during the later suited trail check.
            command('fill -5 180 9 5 183 9 minecraft:air')
            command('fill -4 180 20 4 184 20 minecraft:stone')
            command('tp Partner 0.5 180 3.5 0 0')
            subprocess.run(['xdotool', 'keydown', 'c'], check=True, timeout=30)
            time.sleep(.4)
            subprocess.run(['xdotool', 'keyup', 'c'], check=True, timeout=30)
            wait_for(server_log, r'Ability modifier executed: hero=fiskheroes:savitar, key=SUPER_SPEED, modifier=fiskheroes:super_speed',
                     30, 'speed enabled on server', server)
            time.sleep(1)
            start = len(observer_log.read_text(errors='replace'))
            subprocess.run(['xdotool', 'keydown', 'w'], check=True, timeout=30)
            subprocess.run(['xdotool', 'keydown', 'Control_L'], check=True, timeout=30)
            try:
                for sound in ('super_speed_move_loop_cw', 'super_speed_move_loop_savitar_rgb',
                              'super_speed_sprint_savitar', 'super_speed_sprint_savitar_noise'):
                    wait_for_new(observer_log, r'Playing pack sound definition fiskheroes:' + sound + r' as',
                                 start, 60, 'observer receives ' + sound, observer)
                if os.environ.get('CI_SOUND_INHERITANCE_FIXTURE') == '1':
                    # Changing V9 while already moving must replace the suit's
                    # loop and change its RGB trail, not wait for a new MOVE edge.
                    v9_profile = len(observer_log.read_text(errors='replace'))
                    command('effect give Partner fiskheroes:velocity_nine 30 0 true')
                    wait_for_new(observer_log, r'Sound loop stopping: id=fiskheroes:super_speed_move_loop_savitar_rgb, entity=',
                                 v9_profile, 30, 'V9 retires the running suit-specific loop', observer)
                    wait_for_new(observer_log, r'Sound loop (?:started|resumed): id=fiskheroes:super_speed_move_loop_cw, entity=',
                                 v9_profile, 30, 'V9 selects its movement loop during an existing run', observer)
                    time.sleep(1)  # Original conversion reaches blue over ten ticks.
                    trail_start = len(observer_log.read_text(errors='replace'))
                    blue_trail = r'Velocity 9 trail rendered: player=Partner, trail=fiskheroes:builtin/lightning_rgb_6, color=0059FF, samples=[1-9]'
                    # Holding forward can leave Partner pressed against the wall
                    # before the observer has collected any new trail samples.
                    # Move through several visible positions while keeping the
                    # real movement keys held, as in the original single-TP probe.
                    for z in (4, 6, 8, 10, 12, 14):
                        command(f'tp Partner 0.5 180 {z} 0 0')
                        if re.search(blue_trail, observer_log.read_text(errors='replace')[trail_start:]):
                            break
                    wait_for_new(observer_log, blue_trail, trail_start, 60,
                                 'V9 converts the suited RGB trail to blue', observer)
                    v9_stop = len(observer_log.read_text(errors='replace'))
                    subprocess.run(['xdotool', 'keydown', 'c'], check=True, timeout=30)
                    time.sleep(.4)
                    subprocess.run(['xdotool', 'keyup', 'c'], check=True, timeout=30)
                    wait_for_new(observer_log, r'Playing pack sound definition fiskheroes:super_speed_stop_zoom as',
                                 v9_stop, 30, 'suited V9 emits the original stop sound', observer)
                    v9_restart = len(observer_log.read_text(errors='replace'))
                    subprocess.run(['xdotool', 'keydown', 'c'], check=True, timeout=30)
                    time.sleep(.4)
                    subprocess.run(['xdotool', 'keyup', 'c'], check=True, timeout=30)
                    wait_for_new(observer_log, r'Sound loop (?:started|resumed): id=fiskheroes:super_speed_move_loop_cw, entity=',
                                 v9_restart, 30, 'V9 loop restarts after toggling speed', observer)
                    restored = len(observer_log.read_text(errors='replace'))
                    command('effect clear Partner fiskheroes:velocity_nine')
                    wait_for_new(observer_log, r'Sound loop (?:started|resumed): id=fiskheroes:super_speed_move_loop_savitar_rgb, entity=',
                                 restored, 30, 'suit-specific movement loop returns after V9', observer)
                # Disabling while still moving must stop both loops and play the
                # profile's two stop cues (their condition requires !speeding).
                stop_start = len(observer_log.read_text(errors='replace'))
                subprocess.run(['xdotool', 'keydown', 'c'], check=True, timeout=30)
                time.sleep(.4)
                subprocess.run(['xdotool', 'keyup', 'c'], check=True, timeout=30)
                for sound in ('super_speed_move_loop_cw', 'super_speed_move_loop_savitar_rgb'):
                    wait_for_new(observer_log, r'Sound loop stopping: id=fiskheroes:' + sound + r', entity=',
                                 stop_start, 60, 'observer stops ' + sound, observer)
                for sound in ('super_speed_stop_savitar', 'super_speed_stop_savitar_noise'):
                    wait_for_new(observer_log, r'Playing pack sound definition fiskheroes:' + sound + r' as',
                                 stop_start, 60, 'observer receives ' + sound, observer)
                # Re-enable while the movement keys are still held, then change
                # suits without a movement-release event. Old profile loops must stop.
                if os.environ.get('CI_SOUND_INHERITANCE_FIXTURE') == '1':
                    # Keep tracking Partner, but start the next loop outside its
                    # four-block audio range; no later MOVE event will be sent.
                    command('execute at Partner run tp Dev ~12 ~ ~')
                restart_start = len(observer_log.read_text(errors='replace'))
                subprocess.run(['xdotool', 'keydown', 'c'], check=True, timeout=30)
                time.sleep(.4)
                subprocess.run(['xdotool', 'keyup', 'c'], check=True, timeout=30)
                for sound in ('super_speed_move_loop_cw', 'super_speed_move_loop_savitar_rgb'):
                    wait_for_new(observer_log, r'Sound loop (?:started|resumed): id=fiskheroes:' + sound + r', entity=',
                                 restart_start, 60, 'observer restarts ' + sound, observer)
                if os.environ.get('CI_SOUND_INHERITANCE_FIXTURE') == '1':
                    # The fixture shortens this loop's range to four blocks. Move
                    # across the audio boundary without stopping entity tracking.
                    wait_for_new(observer_log, r'Sound loop range: id=fiskheroes:super_speed_move_loop_savitar_rgb, entity=\d+, audible=false, volume=0\.0',
                                 restart_start, 30, 'new out-of-range loop is initially silent', observer)
                    near_start = len(observer_log.read_text(errors='replace'))
                    command('execute at Partner run tp Dev ~3 ~ ~')
                    wait_for_new(observer_log, r'Sound loop range: id=fiskheroes:super_speed_move_loop_savitar_rgb, entity=\d+, audible=true, volume=(?!0\.0(?:\s|$))',
                                 near_start, 30, 'initially out-of-range tracked loop becomes audible after moving closer', observer)
                    far_start = len(observer_log.read_text(errors='replace'))
                    command('execute at Partner run tp Dev ~12 ~ ~')
                    wait_for_new(observer_log, r'Sound loop range: id=fiskheroes:super_speed_move_loop_savitar_rgb, entity=\d+, audible=false, volume=0\.0',
                                 far_start, 30, 'out-of-range loop becomes silent', observer)
                    time.sleep(2)  # Longer than the loop fade-out; it must remain alive.
                    near_start = len(observer_log.read_text(errors='replace'))
                    command('execute at Partner run tp Dev ~3 ~ ~')
                    wait_for_new(observer_log, r'Sound loop range: id=fiskheroes:super_speed_move_loop_savitar_rgb, entity=\d+, audible=true, volume=(?!0\.0(?:\s|$))',
                                 near_start, 30, 'same loop becomes audible again within tracking range', observer)
                # The observer leaves tracking range and returns while Partner
                # continues moving, without another MOVE transition on the server.
                command('tp Dev 250 200 0 0 0')
                time.sleep(3)
                server_start = len(server_log.read_text(errors='replace'))
                returning_start = len(observer_log.read_text(errors='replace'))
                if os.environ.get('CI_SOUND_INHERITANCE_FIXTURE') == '1':
                    command('execute at Partner run tp Dev ~12 ~ ~')
                else:
                    command('tp Dev 3 180 2 0 0')
                for sound in ('super_speed_move_loop_cw', 'super_speed_move_loop_savitar_rgb'):
                    wait_for_new(server_log, r'Active loop synced: sound=fiskheroes:' + sound + r', subject=Partner, listener=Dev',
                                 server_start, 60, 'server resynchronizes active ' + sound, server)
                    wait_for_new(observer_log, r'Sound loop active: id=fiskheroes:' + sound + r', entity=',
                                 returning_start, 60, 'returning observer retains active ' + sound, observer)
                if os.environ.get('CI_SOUND_INHERITANCE_FIXTURE') == '1':
                    wait_for_new(observer_log, r'Sound loop range: id=fiskheroes:super_speed_move_loop_savitar_rgb, entity=\d+, audible=false, volume=0\.0',
                                 returning_start, 30, 'new tracker receives the loop silently outside range', observer)
                    near_start = len(observer_log.read_text(errors='replace'))
                    command('execute at Partner run tp Dev ~3 ~ ~')
                    wait_for_new(observer_log, r'Sound loop range: id=fiskheroes:super_speed_move_loop_savitar_rgb, entity=\d+, audible=true, volume=(?!0\.0(?:\s|$))',
                                 near_start, 30, 'new tracker hears synced loop on entering range', observer)
                switch_start = len(observer_log.read_text(errors='replace'))
                command('execute as Partner run suit fiskheroes:iron_man_mk50')
                for sound in ('super_speed_move_loop_cw', 'super_speed_move_loop_savitar_rgb'):
                    wait_for_new(observer_log, r'Sound loop stopping: id=fiskheroes:' + sound + r', entity=',
                                 switch_start, 60, 'suit change stops old profile ' + sound, observer)
            finally:
                subprocess.run(['xdotool', 'keyup', 'w'], check=True, timeout=30)
                subprocess.run(['xdotool', 'keyup', 'Control_L'], check=True, timeout=30)
                subprocess.run(['xdotool', 'keyup', 'c'], check=True, timeout=30)
            # Do not obstruct the long-range teleport exercised later in this test.
            command('fill -4 180 20 4 184 20 minecraft:air')
            command('tp Partner 0 180 4 180 0')
            # Reset the observer as well: the range test moved it beside the wall,
            # facing away from the quiver and the subsequent visual checks.
            command('tp Dev 0 180 0 0 0')
            command('execute as Dev run suit fiskheroes:the_flash')
            # Clear the remote suit so the original unsuited quiver control remains valid.
            command('item replace entity Partner armor.head with minecraft:air')
            command('item replace entity Partner armor.chest with minecraft:air')
            command('item replace entity Partner armor.legs with minecraft:air')
            command('item replace entity Partner armor.feet with minecraft:air')
            command('item replace entity Partner hotbar.1 with fiskheroes:quiver{Enchantments:[{id:"fiskheroes:capacity",lvl:1s}],QuiverInventory:{Size:5,Items:[{Slot:0,id:"fiskheroes:trick_arrow",Count:8b,tag:{ArrowType:"cactus"}},{Slot:1,id:"minecraft:arrow",Count:22b}]}}')
            wait_for(observer_log, r'Quiver appearance: wearer=Partner, equipped=true, foil=true, visible=true, hero=false',
                     60, 'observer sees enchanted quiver on unsuited player', observer)
            wait_for(observer_log, r'Quiver shafts rendered: wearer=Partner, counts=12000, total=3',
                     60, 'observer renders synchronized quiver arrow quantities', observer)
            start = len(observer_log.read_text(errors='replace'))
            command('item replace entity Partner weapon.mainhand with fiskheroes:quiver')
            wait_for_new(observer_log, r'Quiver appearance: wearer=Partner, equipped=true, foil=(true|false), visible=false, hero=false', start,
                         60, 'back-mounted quiver is hidden when held', observer)
            command('item replace entity Partner weapon.mainhand with fiskheroes:compound_bow')
            command('execute as Partner run suit fiskheroes:green_arrow')
            wait_for(observer_log, r'Quiver appearance: wearer=Partner, equipped=true, foil=true, visible=true, hero=true',
                     60, 'observer renders remote enchanted quiver with suit', observer)
            time.sleep(3)
            subprocess.run(['xdotool', 'windowfocus', '--sync', partner_window], check=True, timeout=30)
            subprocess.run(['xdotool', 'mousemove', '--window', partner_window, '640', '360', 'click', '1'], check=True, timeout=30)
            subprocess.run(['xdotool', 'mousedown', '3'], check=True, timeout=30)
            try:
                wait_for(observer_log, r'Compound bow nocked arrow rendered: type=cactus, wearer=Partner',
                         60, 'observer sees selected quiver arrow of remote player', observer)
            finally:
                subprocess.run(['xdotool', 'mouseup', '3'], check=True, timeout=30)
            start = len(observer_log.read_text(errors='replace'))
            command('item replace entity Partner hotbar.1 with minecraft:air')
            wait_for_new(observer_log, r'Quiver appearance: wearer=Partner, equipped=false, foil=false', start,
                         60, 'observer sees remote quiver removal', observer)
            start = len(observer_log.read_text(errors='replace'))
            command('item replace entity Partner hotbar.1 with fiskheroes:quiver')
            wait_for_new(observer_log, r'Quiver shafts rendered: wearer=Partner, counts=00000, total=0', start,
                         60, 'observer sees an empty quiver without ghost arrows', observer)
            command('item replace entity Partner hotbar.1 with minecraft:air')
            command('execute as Partner run suit fiskheroes:deadpool_xmen')
            command('item replace entity Dev weapon.mainhand with fiskheroes:desert_eagle')
            command('item replace entity Partner weapon.mainhand with fiskheroes:desert_eagle')
            command('execute at Dev if entity @a[name=Partner,distance=..8] run say LIVERY_PLAYERS_NEARBY')
            wait_for(server_log, r'\[Server\] LIVERY_PLAYERS_NEARBY', 30, 'both players are within render range', server)
            wait_for(observer_log, r'Held weapon livery: wearer=Dev, hero=fiskheroes:the_flash, item=fiskheroes:desert_eagle, context=FIRST_PERSON_RIGHT_HAND, texture=default',
                     90, 'observer without a livery uses the default', observer)
            wait_for(observer_log, r'Held weapon livery: wearer=Partner, hero=fiskheroes:deadpool_xmen, item=fiskheroes:desert_eagle, context=THIRD_PERSON_RIGHT_HAND, texture=fiskheroes:textures/heroes/deadpool_xmen_gun\.png',
                     90, 'observer sees remote Deadpool pistol livery', observer)
            subprocess.run(['xdotool', 'windowfocus', '--sync', partner_window], check=True, timeout=30)
            subprocess.run(['xdotool', 'mousemove', '--window', partner_window, '640', '360', 'click', '1'], check=True, timeout=30)
            command('item replace entity Partner weapon.mainhand with fiskheroes:desert_eagle{Dual:1b,Ammo:16}')
            subprocess.run(['xdotool', 'click', '1'], check=True, timeout=30)
            wait_for(observer_log, r'Gun shot effect rendered: wearer=Partner, hand=MAIN_HAND, context=THIRD_PERSON_RIGHT_HAND',
                     60, 'observer sees remote main-hand pistol shot', observer)
            time.sleep(1)
            subprocess.run(['xdotool', 'click', '1'], check=True, timeout=30)
            wait_for(observer_log, r'Gun shot effect rendered: wearer=Partner, hand=OFF_HAND, context=THIRD_PERSON_LEFT_HAND',
                     60, 'observer sees alternating remote offhand pistol shot', observer)
            subprocess.run(['xdotool', 'key', 'c'], check=True, timeout=30)
            wait_for(observer_log, r'Desert eagle reload pose: wearer=Partner, hand=MAIN_HAND, context=THIRD_PERSON_RIGHT_HAND',
                     60, 'observer sees remote main-hand reload', observer)
            wait_for(observer_log, r'Desert eagle reload pose: wearer=Partner, hand=OFF_HAND, context=THIRD_PERSON_LEFT_HAND',
                     60, 'observer sees remote offhand reload', observer)
            command('execute if data entity Partner SelectedItem.tag{Ammo:16} run say REMOTE_RELOAD_AMMO_OK')
            wait_for(server_log, r'\[Server\] REMOTE_RELOAD_AMMO_OK', 30, 'remote reload restores dual magazine', server)
            command('item replace entity Partner weapon.mainhand with fiskheroes:desert_eagle{Dual:1b,Ammo:0}')
            subprocess.run(['xdotool', 'mousedown', '1'], check=True, timeout=30)
            try:
                wait_for(observer_log, r'Remote aiming sustained: wearer=Partner',
                         60, 'observer preserves sustained remote aiming', observer)
            finally:
                subprocess.run(['xdotool', 'mouseup', '1'], check=True, timeout=30)
            wait_for(observer_log, r'Remote aiming released: wearer=Partner',
                     60, 'observer sees remote aiming release', observer)
            command('item replace entity Partner weapon.mainhand with fiskheroes:desert_eagle{Ammo:0}')
            subprocess.run(['xdotool', 'mousedown', '1'], check=True, timeout=30)
            subprocess.run(['xdotool', 'keydown', 'Shift_L'], check=True, timeout=30)
            try:
                wait_for(observer_log, r'Remote scope active: wearer=Partner',
                         60, 'observer sees remote scoped state', observer)
                subprocess.run(['xdotool', 'keyup', 'Shift_L'], check=True, timeout=30)
                wait_for(observer_log, r'Remote scope released: wearer=Partner',
                         60, 'observer sees remote scope release', observer)
            finally:
                subprocess.run(['xdotool', 'keyup', 'Shift_L'], check=True, timeout=30)
                subprocess.run(['xdotool', 'mouseup', '1'], check=True, timeout=30)
            command('execute as Partner run suit fiskheroes:wild_dog')
            command('item replace entity Partner weapon.mainhand with fiskheroes:beretta_93r{Dual:1b}')
            wait_for(observer_log, r'Held weapon livery: wearer=Partner, hero=fiskheroes:wild_dog, item=fiskheroes:beretta_93r, context=THIRD_PERSON_LEFT_HAND, texture=default',
                     90, 'observer sees remote dual Beretta offhand', observer)
            subprocess.run(['xdotool', 'click', '1'], check=True, timeout=30)
            wait_for(observer_log, r'Beretta shot effect rendered: wearer=Partner, hand=MAIN_HAND, context=THIRD_PERSON_RIGHT_HAND',
                     60, 'observer sees remote Beretta main-hand shot', observer)
            wait_for(observer_log, r'Beretta shot effect rendered: wearer=Partner, hand=OFF_HAND, context=THIRD_PERSON_LEFT_HAND',
                     60, 'observer sees remote Beretta offhand burst shot', observer)
            subprocess.run(['xdotool', 'key', 'c'], check=True, timeout=30)
            wait_for(observer_log, r'Beretta reload pose: wearer=Partner, hand=MAIN_HAND, context=THIRD_PERSON_RIGHT_HAND',
                     60, 'observer sees remote Beretta reload', observer)
            wait_for(observer_log, r'Beretta reload pose: wearer=Partner, hand=OFF_HAND, context=THIRD_PERSON_LEFT_HAND',
                     60, 'observer sees remote Beretta offhand reload', observer)
            command('execute unless data entity Partner Inventory[{Slot:-106b}] run say REMOTE_BERETTA_EMPTY_OK')
            wait_for(server_log, r'\[Server\] REMOTE_BERETTA_EMPTY_OK', 30, 'remote Beretta dual is visual only', server)
            command('item replace entity Partner weapon.offhand with minecraft:torch')
            wait_for(observer_log, r'Dual visual offhand: player=Partner, item=fiskheroes:beretta_93r, mode=occupied',
                     60, 'observer respects real offhand with Beretta', observer)
            command('item replace entity Partner weapon.offhand with minecraft:air')
            command('execute as Partner run suit fiskheroes:heatwave')
            command('item replace entity Partner weapon.mainhand with fiskheroes:heat_gun')
            wait_for(observer_log, r'Held weapon livery: wearer=Partner, hero=fiskheroes:heatwave, item=fiskheroes:heat_gun, context=THIRD_PERSON_RIGHT_HAND, texture=default',
                     90, 'observer sees remote Heat Gun', observer)
            start = len(observer_log.read_text(errors='replace'))
            subprocess.run(['xdotool', 'mousedown', '1'], check=True, timeout=30)
            try:
                wait_for_new(observer_log, r'Remote aiming sustained: wearer=Partner', start,
                             60, 'observer sees remote Heat Gun aim', observer)
                wait_for(observer_log, r'Heat gun flame emitted: wearer=Partner, samples=',
                         60, 'observer receives remote Heat Gun flame stream', observer)
            finally:
                subprocess.run(['xdotool', 'mouseup', '1'], check=True, timeout=30)
            wait_for_new(observer_log, r'Remote aiming released: wearer=Partner', start,
                         60, 'observer sees remote Heat Gun aim release', observer)
            command('tp Partner 0 180 4 0 0')
            command('execute as Partner run suit fiskheroes:captain_cold')
            command('item replace entity Partner weapon.mainhand with fiskheroes:cold_gun{GrenadeMode:1b}')
            wait_for(observer_log, r'Held weapon livery: wearer=Partner, hero=fiskheroes:captain_cold, item=fiskheroes:cold_gun, context=THIRD_PERSON_RIGHT_HAND, texture=default',
                     90, 'observer sees remote Cold Gun', observer)
            start = len(observer_log.read_text(errors='replace'))
            subprocess.run(['xdotool', 'mousedown', '1'], check=True, timeout=30)
            try:
                wait_for_new(observer_log, r'Cold ball rendered: entity=', start,
                             60, 'observer renders cold ball fired by the remote player', observer)
            finally:
                subprocess.run(['xdotool', 'mouseup', '1'], check=True, timeout=30)
            command('tp Partner 0 180 4 180 0')
            command('execute as Partner run suit fiskheroes:deadpool_xmen')
            command('item replace entity Partner weapon.mainhand with fiskheroes:katana{Dual:1b}')
            wait_for(observer_log, r'Held weapon livery: wearer=Partner, hero=fiskheroes:deadpool_xmen, item=fiskheroes:katana, context=THIRD_PERSON_LEFT_HAND, texture=fiskheroes:textures/heroes/deadpool_xmen_katana\.png',
                     90, 'observer sees remote dual katana in offhand', observer)
            command('execute unless data entity Partner Inventory[{Slot:-106b}] run say REMOTE_DUAL_EMPTY_OK')
            wait_for(server_log, r'\[Server\] REMOTE_DUAL_EMPTY_OK', 30, 'remote dual offhand does not change server inventory', server)
            command('item replace entity Partner weapon.offhand with minecraft:torch')
            wait_for(observer_log, r'Dual visual offhand: player=Partner, item=fiskheroes:katana, mode=occupied',
                     60, 'observer respects remote real offhand item', observer)
            command('item replace entity Partner weapon.offhand with minecraft:air')
            command('item replace entity Partner weapon.mainhand with fiskheroes:desert_eagle')
            command('item replace entity Partner weapon.mainhand with fiskheroes:tactical_tonfa{Dual:1b}')
            wait_for(observer_log, r'Tonfa hand pose: wearer=Partner, context=THIRD_PERSON_RIGHT_HAND, toggled=false',
                     60, 'observer sees remote tonfa initial pose', observer)
            subprocess.run(['xdotool', 'windowfocus', '--sync', partner_window], check=True, timeout=30)
            subprocess.run(['xdotool', 'mousemove', '--window', partner_window, '640', '360', 'click', '1'],
                           check=True, timeout=30)
            time.sleep(1)
            subprocess.run(['xdotool', 'click', '1'], check=True, timeout=30)
            wait_for(observer_log, r'Tonfa hand pose: wearer=Partner, context=THIRD_PERSON_RIGHT_HAND, toggled=true',
                     60, 'observer sees remote tonfa flip after swing', observer)
            command('item replace entity Partner weapon.mainhand with fiskheroes:bo_staff')
            time.sleep(2)
            subprocess.run(['xdotool', 'windowfocus', '--sync', partner_window], check=True, timeout=30)
            subprocess.run(['xdotool', 'click', '1'], check=True, timeout=30)
            wait_for(observer_log, r'Bo staff swing pose: wearer=Partner, context=THIRD_PERSON_RIGHT_HAND',
                     60, 'observer sees remote staff swing', observer)
            command('execute as Partner run suit fiskheroes:rupture')
            command('item replace entity Partner weapon.mainhand with fiskheroes:ruptures_scythe')
            subprocess.run(['xdotool', 'windowfocus', '--sync', partner_window], check=True, timeout=30)
            subprocess.run(['xdotool', 'keydown', 'c'], check=True, timeout=30)
            try:
                wait_for(observer_log, r'Scythe charge arcs: wearer=Partner, context=THIRD_PERSON_RIGHT_HAND',
                         60, 'observer sees remote scythe energy charge', observer)
            finally:
                subprocess.run(['xdotool', 'keyup', 'c'], check=True, timeout=30)
            command('item replace entity Partner weapon.mainhand with fiskheroes:desert_eagle')
            command('execute as Partner run suit fiskheroes:deathstroke_dceu')
            wait_for(observer_log, r'Held weapon livery: wearer=Partner, hero=fiskheroes:deathstroke_dceu, item=fiskheroes:desert_eagle, context=THIRD_PERSON_RIGHT_HAND, texture=fiskheroes:textures/heroes/deathstroke_dceu_gun\.png',
                     90, 'observer sees remote suit change update the pistol livery', observer)
            command('execute as Partner run suit fiskheroes:the_flash')
            wait_for(observer_log, r'Held weapon livery: wearer=Partner, hero=fiskheroes:the_flash, item=fiskheroes:desert_eagle, context=THIRD_PERSON_RIGHT_HAND, texture=default',
                     90, 'remote wearer without a livery restores the default', observer)
            if os.environ.get('CI_SONIC_MULTIPLAYER_FIXTURE') == '1':
                # Runner-only Canary pack uses a toggle loop gated by sneaking.
                # Start outside tracking range, then bring the observer in after
                # the cry is already active; no second SHOOT transition occurs.
                command('tp Dev 256 180 0 0 0')
                command('tp Partner 0 180 4 180 0')
                command('execute as Partner run suit fiskheroes:black_canary')
                command('item replace entity Partner weapon.mainhand with minecraft:air')
                subprocess.run(['xdotool', 'windowfocus', '--sync', partner_window], check=True, timeout=30)
                start = len(wearer_log.read_text(errors='replace'))
                subprocess.run(['xdotool', 'keydown', 'c'], check=True, timeout=30)
                time.sleep(.4)
                subprocess.run(['xdotool', 'keyup', 'c'], check=True, timeout=30)
                wait_for_new(wearer_log, r'Sound loop started: id=fiskheroes:ci_sonic_loop', start, 30,
                             'wearer starts the toggled sonic loop', wearer)
                start = len(observer_log.read_text(errors='replace'))
                command('tp Dev 3 180 0 0 0')
                wait_for_new(observer_log, r'Sound loop started: id=fiskheroes:ci_sonic_loop', start, 45,
                             'late observer receives the active sonic loop', observer)
                stop = len(observer_log.read_text(errors='replace'))
                subprocess.run(['xdotool', 'keydown', 'Shift_L'], check=True, timeout=30)
                try:
                    wait_for_new(observer_log, r'Sound loop stopping: id=fiskheroes:ci_sonic_loop', stop, 30,
                                 'dynamic modifier gate stops the tracked sonic loop', observer)
                    command('kill @e[type=fiskheroes:sonic_wave]')
                    command('execute unless entity @e[type=fiskheroes:sonic_wave] run say SONIC_DISABLED_NO_WAVES')
                    wait_for(server_log, r'\[Server\] SONIC_DISABLED_NO_WAVES', 30,
                             'disabled sonic modifier stops spawning waves', server)
                finally:
                    subprocess.run(['xdotool', 'keyup', 'Shift_L'], check=True, timeout=30)
                time.sleep(2)
                tail = observer_log.read_text(errors='replace')[stop:]
                if re.search(r'Sound loop (?:started|resumed): id=fiskheroes:ci_sonic_loop', tail):
                    raise RuntimeError('Re-enabling the modifier unexpectedly restarted the stopped cry')
                print('PASS: re-enabling the sonic modifier does not restart the cry', flush=True)
            # A long teleport must reach each endpoint's observers independently,
            # including a destination observer who is not tracking the caster.
            command('forceload add -16 -16 16 96')
            command('fill -4 179 74 4 179 83 minecraft:stone')
            command('fill -2 180 80 2 183 80 minecraft:stone')
            command('execute as Partner run suit fiskheroes:the_monitor')
            command('item replace entity Partner weapon.mainhand with minecraft:air')
            command('tp Dev 3 180 2 0 0')
            command('tp Partner 0 180 2 0 0')
            time.sleep(3)
            subprocess.run(['xdotool', 'windowfocus', '--sync', partner_window], check=True, timeout=30)
            subprocess.run(['xdotool', 'mousemove', '--window', partner_window, '640', '360', 'click', '1'], check=True, timeout=30)
            origin_start = len(observer_log.read_text(errors='replace'))
            subprocess.run(['xdotool', 'keydown', 'b'], check=True, timeout=30)
            time.sleep(.3)
            subprocess.run(['xdotool', 'keyup', 'b'], check=True, timeout=30)
            origin = r'Teleport burst emitted: endpoint=origin, firstPerson=false, center=.*count=200, cloud=fiskheroes:breach'
            wait_for_new(observer_log, origin, origin_start, 45, 'origin observer receives remote teleport burst', observer)
            time.sleep(2)
            observed = observer_log.read_text(errors='replace')[origin_start:]
            if len(re.findall(origin, observed)) != 1 or 'Teleport burst emitted: endpoint=destination' in observed:
                raise RuntimeError('Origin-only observer must receive exactly one origin burst, not the remote destination')
            command('tp Dev 3 180 78 180 0')
            command('tp Partner 0 180 2 0 0')
            time.sleep(3)
            dest_start = len(observer_log.read_text(errors='replace'))
            subprocess.run(['xdotool', 'keydown', 'b'], check=True, timeout=30)
            time.sleep(.3)
            subprocess.run(['xdotool', 'keyup', 'b'], check=True, timeout=30)
            dest = r'Teleport burst emitted: endpoint=destination, firstPerson=false, center=.*count=200, cloud=fiskheroes:breach'
            wait_for_new(observer_log, dest, dest_start, 45, 'destination observer receives untracked remote teleport burst', observer)
            time.sleep(2)
            observed = observer_log.read_text(errors='replace')[dest_start:]
            if len(re.findall(dest, observed)) != 1 or 'Teleport burst emitted: endpoint=origin' in observed:
                raise RuntimeError('Destination-only observer must receive exactly one destination burst, not the remote origin')
            print('PASS: separated endpoint observers receive one burst each without needing to track the caster', flush=True)
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
