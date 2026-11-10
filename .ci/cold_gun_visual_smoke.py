#!/usr/bin/env python3
"""Inspect Captain Cold's weapon and exercise its server-authoritative modes."""
import os
import signal
import subprocess
import time
from pathlib import Path
from tonfa_visual_smoke import magenta, wait_for, xdo
from PIL import Image


def main():
    env = dict(os.environ, LIBGL_ALWAYS_SOFTWARE='1', ALSOFT_DRIVERS='null',
               JAVA_TOOL_OPTIONS='-Dfiskheroes.ciColdBallDiagnostics=true')
    server_log, client_log = Path('ci-cold-gun-server.log'), Path('ci-cold-gun-client.log')
    common = ['--no-daemon', '--console=plain', '-Dorg.gradle.jvmargs=-Xmx512m',
              '-Dorg.gradle.workers.max=1', '-I', '.ci/client_identity.gradle']
    processes = []
    with server_log.open('w') as sout, client_log.open('w') as cout:
        try:
            server = subprocess.Popen(['./gradlew', 'runServer', '-PciRunDir=run-cold-gun-server', *common],
                                      stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT,
                                      text=True, env=env, start_new_session=True)
            processes.append(server)
            wait_for(server_log, r'Done \(', 360, 'dedicated server ready', server)
            client = subprocess.Popen(['./gradlew', 'runClient', '-PciRunDir=run-cold-gun-client',
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
                server.stdin.write(line + '\n')
                server.stdin.flush()
                time.sleep(1.5)

            def screenshot(name):
                time.sleep(2)
                subprocess.run(['scrot', 'ci-cold-gun-' + name + '.png'], check=True, timeout=30)
                print('Captured:', name, flush=True)

            command('difficulty peaceful')
            command('gamemode creative Dev')
            command('time set day')
            command('tp Dev 0 185 0 0 0')
            command('fill -5 179 -5 5 179 5 minecraft:stone')
            command('tp Dev 0.5 180 0.5 0 0')
            command('execute as Dev run suit fiskheroes:captain_cold')
            command('item replace entity Dev weapon.mainhand with fiskheroes:cold_gun')
            wait_for(client_log, r'First-person cold gun arm rendered: RIGHT', 60, 'first-person gun arm', client)
            screenshot('first')
            xdo('key', 'F5')
            xdo('key', 'F5')
            screenshot('third-front')
            xdo('key', 'F5')
            xdo('key', 'e')
            screenshot('inventory')
            xdo('key', 'e')
            # Isolate the projectile against a dark backdrop so its blue shell is
            # verified independently from the gun's magenta diagnostic texture.
            command('fill -2 180 5 2 184 5 minecraft:black_concrete')
            screenshot('ball-before')
            command('summon fiskheroes:cold_ball 0.5 181.62 2.5 {NoGravity:1b}')
            wait_for(client_log, r'Cold ball rendered: entity=', 60, 'cold ball rendered by real client', client)
            screenshot('ball-visible')
            before_ball = Image.open('ci-cold-gun-ball-before.png').convert('RGB')
            after_ball = Image.open('ci-cold-gun-ball-visible.png').convert('RGB')
            region = (580, 300, 700, 430)
            def blue(image):
                return sum(1 for r, g, b in image.crop(region).get_flattened_data()
                           if b > 130 and g > 80 and b > r * 1.3 and g > r * 1.15)
            gain = blue(after_ball) - blue(before_ball)
            if gain < 20: raise RuntimeError(f'Cold ball blue shell missing from screenshot: {gain} pixels')
            print(f'PASS: cold ball blue shell adds {gain} pixels', flush=True)
            command('kill @e[type=fiskheroes:cold_ball]')
            command('fill -2 180 5 2 184 5 minecraft:air')
            command('setblock 0 181 4 minecraft:water')
            xdo('mousedown', '1')
            try:
                wait_for(client_log, r'Cold gun first-person aiming: arm=RIGHT', 60, 'gun aims in first person', client)
                screenshot('aimed')
            finally:
                xdo('mouseup', '1')
            # Flowing water may reach the player before the shot; the first
            # intercepted fluid, not necessarily the source, must freeze.
            for z in range(1, 5):
                command(f'execute if block 0 181 {z} minecraft:ice run say COLD_WATER_OK')
            wait_for(server_log, r'COLD_WATER_OK', 30, 'cold gun freezes water', server)
            xdo('keydown', 'Shift_L')
            try:
                time.sleep(0.6)
                xdo('click', '3')
                time.sleep(0.3)
            finally:
                xdo('keyup', 'Shift_L')
            command('execute if data entity Dev SelectedItem.tag{GrenadeMode:1b} run say COLD_GRENADE_MODE_OK')
            wait_for(server_log, r'COLD_GRENADE_MODE_OK', 30, 'sneak-use switches to grenade mode', server)
            screenshot('grenade')
            command('fill -4 180 1 4 183 5 minecraft:air')
            command('tp Dev 0.5 180 0.5 0 10')
            command('summon minecraft:cow 0.5 180 4.5 {NoAI:1b,Silent:1b,Tags:["cold_ball_test"]}')
            xdo('click', '1')
            command('data get entity @e[type=minecraft:cow,tag=cold_ball_test,limit=1]')
            command('execute as @e[type=minecraft:cow,tag=cold_ball_test,limit=1] if data entity @s ActiveEffects[{Id:2}] run say COLD_BALL_IMPACT_OK')
            wait_for(server_log, r'COLD_BALL_IMPACT_OK', 30, 'cold ball explodes and slows a target on impact', server)
            command('execute as @e[type=minecraft:cow,tag=cold_ball_test,limit=1] unless data entity @s {Health:10.0f} run say COLD_BALL_DAMAGE_OK')
            wait_for(server_log, r'COLD_BALL_DAMAGE_OK', 30, 'cold ball impact damages the target', server)
            command('kill @e[type=minecraft:cow,tag=cold_ball_test]')
            command('tp Dev 0.5 180 0.5 0 0')
            # Point-blank creative mining must be suppressed even when holding attack.
            command('setblock 0 181 2 minecraft:stone')
            xdo('mousedown', '1')
            try:
                time.sleep(1.5)
            finally:
                xdo('mouseup', '1')
            command('execute if block 0 181 2 minecraft:stone run say GUN_NO_MINING_OK')
            wait_for(server_log, r'GUN_NO_MINING_OK', 30, 'gun attack does not mine blocks in creative', server)
            command('setblock 0 181 2 minecraft:air')
            command('tp Dev 0.5 180 0.5 0 10')
            command('summon minecraft:cow 0.5 180 2.5 {NoAI:1b,Silent:1b}')
            command('execute as Dev at @s if entity @e[type=minecraft:cow,distance=..4,nbt={Health:10.0f}] run say MELEE_TARGET_READY')
            wait_for(server_log, r'MELEE_TARGET_READY', 30, 'full-health melee target is present', server)
            command('item replace entity Dev weapon.mainhand with fiskheroes:desert_eagle{Ammo:0}')
            xdo('click', '1')
            command('execute as Dev at @s if entity @e[type=minecraft:cow,distance=..4,nbt={Health:10.0f}] run say GUN_NO_MELEE_OK')
            wait_for(server_log, r'GUN_NO_MELEE_OK', 30, 'empty gun does not add a vanilla melee hit', server)
            command('item replace entity Dev weapon.mainhand with minecraft:air')
            xdo('click', '1')
            command('execute as Dev at @s unless entity @e[type=minecraft:cow,distance=..4,nbt={Health:10.0f}] run say UNARMED_MELEE_OK')
            wait_for(server_log, r'UNARMED_MELEE_OK', 30, 'unarmed vanilla attack still works', server)
            command('item replace entity Dev weapon.offhand with fiskheroes:cold_gun')
            screenshot('offhand')
            first, third, icon, aimed, offhand = (Image.open(f'ci-cold-gun-{name}.png') for name in
                                                   ('first', 'third-front', 'inventory', 'aimed', 'offhand'))
            if any(im.size != (1280, 720) for im in (first, third, icon, aimed, offhand)):
                raise RuntimeError('Unexpected screenshot dimensions')
            if magenta(first, (700, 350, 900, 550)) < 500 or magenta(third, (580, 380, 620, 450)) < 100:
                raise RuntimeError('Cold gun is missing from first- or third-person hand')
            slot = magenta(icon, (461, 448, 496, 484))
            around = magenta(icon, (450, 430, 505, 495))
            if slot < 50 or around - slot > 5:
                raise RuntimeError(f'Cold gun icon clips outside inventory slot: {slot=}, {around=}')
            if magenta(offhand, (350, 350, 580, 550)) < 500:
                raise RuntimeError('Real offhand cold gun is missing')
            def center_x(image):
                pts = [(x, y) for y in range(350, 555) for x in range(600, 950)
                       if (lambda c: c[0] > 90 and c[2] > 90 and c[1] < 50 and abs(c[0] - c[2]) < 40)(image.getpixel((x, y))[:3])]
                if len(pts) < 500: raise RuntimeError('Cold gun is missing from aim frame')
                return sum(x for x, _ in pts) / len(pts)
            before, after = center_x(first), center_x(aimed)
            if before - after < 15: raise RuntimeError(f'Cold gun did not move toward center when aiming: {before}, {after}')
            print(f'PASS: first/third/offhand poses, contained icon, and aiming movement: {before:.1f} -> {after:.1f}', flush=True)
        finally:
            for process in reversed(processes):
                if process.poll() is None:
                    try: os.killpg(process.pid, signal.SIGTERM)
                    except ProcessLookupError: pass
            for process in reversed(processes):
                try: process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()


if __name__ == '__main__': main()
