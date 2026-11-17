#!/usr/bin/env python3
"""Use runner-only sound parents to test inheritance through real client playback."""
import json
from pathlib import Path

root = Path('src/main/resources/assets/fiskheroes/events/sounds')
(root / 'ci_sound_root.json').write_text(json.dumps({
    'sound': 'fiskheroes:modifier.slowmotion.enable.cw',
    'loop': True, 'volume': 0.4, 'fadeOut': 11,
}))
(root / 'ci_sound_middle.json').write_text(json.dumps({
    'parent': 'fiskheroes:ci_sound_root', 'sound': 'fiskheroes:modifier.slowmotion.enable.cw',
}))
path = root / 'slow_motion_enable_cw.json'
child = json.loads(path.read_text())
child['parent'] = 'fiskheroes:ci_sound_middle'
child['loop'] = False
child['fadeOut'] = 0
path.write_text(json.dumps(child, indent=2))

# A short range lets the multiplayer test cross the audio boundary without
# leaving entity tracking range. This change exists only on the CI runner.
manifest_path = Path('src/main/resources/heropack.json')
manifest = json.loads(manifest_path.read_text())
manifest['sounds']['range']['super_speed_move_loop_savitar_rgb'] = {'distance': 4.0, 'target': 'ALL_IN_RANGE'}
manifest_path.write_text(json.dumps(manifest, indent=2))
