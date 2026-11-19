#!/usr/bin/env python3
"""Add runner-only cues to exercise a pack-provided sonic loop and stop sound."""
import json
import sys
from pathlib import Path

power = Path('src/main/resources/assets/fiskheroes/data/powers/canary_cry.json')
config = json.loads(power.read_text())
config['modifiers']['fiskheroes:sonic_waves']['soundEvents'] = {
    'SHOOT': 'fiskheroes:ci_sonic_loop',
    'SHOOT_STOP': 'fiskheroes:ci_sonic_stop',
}
if '--toggle-and-gate' in sys.argv:
    config['modifiers']['fiskheroes:sonic_waves']['isToggle'] = True
    hero = Path('src/main/resources/assets/fiskheroes/data/heroes/black_canary.js')
    text = hero.read_text()
    if '    hero.addPowers("fiskheroes:canary_cry");' not in text:
        raise RuntimeError('Black Canary fixture could not locate the power declaration')
    text = text.replace('    hero.addPowers("fiskheroes:canary_cry");',
                        '    hero.addPowers("fiskheroes:canary_cry");\n'
                        '    hero.setModifierEnabled((entity, modifier) => '
                        'modifier.name() != "fiskheroes:sonic_waves" || !entity.isSneaking());')
    hero.write_text(text)
power.write_text(json.dumps(config, indent=2))
root = Path('src/main/resources/assets/fiskheroes/events/sounds')
(root / 'ci_sonic_loop.json').write_text(json.dumps({
    'sound': 'minecraft:block.note_block.harp', 'loop': True, 'fadeOut': 20,
}))
(root / 'ci_sonic_stop.json').write_text(json.dumps({
    'sound': 'minecraft:block.note_block.harp', 'loop': False,
}))
