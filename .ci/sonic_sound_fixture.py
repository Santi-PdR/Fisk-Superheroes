#!/usr/bin/env python3
"""Add runner-only cues to exercise a pack-provided sonic loop and stop sound."""
import json
from pathlib import Path

power = Path('src/main/resources/assets/fiskheroes/data/powers/canary_cry.json')
config = json.loads(power.read_text())
config['modifiers']['fiskheroes:sonic_waves']['soundEvents'] = {
    'SHOOT': 'fiskheroes:ci_sonic_loop',
    'SHOOT_STOP': 'fiskheroes:ci_sonic_stop',
}
power.write_text(json.dumps(config, indent=2))
root = Path('src/main/resources/assets/fiskheroes/events/sounds')
(root / 'ci_sonic_loop.json').write_text(json.dumps({
    'sound': 'minecraft:block.note_block.harp', 'loop': True, 'fadeOut': 20,
}))
(root / 'ci_sonic_stop.json').write_text(json.dumps({
    'sound': 'minecraft:block.note_block.harp', 'loop': False,
}))
