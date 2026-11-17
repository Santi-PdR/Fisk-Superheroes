#!/usr/bin/env python3
"""Create a runner-only selector that exposes the miniature getter context."""
import json
import shutil
from pathlib import Path

root = Path('src/main/resources/assets/fiskheroes')
model_path = root / 'models/heroes/the_flash.json'
model = json.loads(model_path.read_text())
model['resources']['layer1'] = 'fiskheroes:ci_mini_display.tx.json'
model_path.write_text(json.dumps(model, indent=2))

textures = root / 'textures/heroes'
definition = {
    'texture': 'fiskheroes:ci_mini_display_<DISPLAY>_<OPEN>_<TIMER>',
    'getters': {
        'DISPLAY': {'key': "entity.is('DISPLAY')", 'values': ['true', 'false']},
        'OPEN': {'key': 'fiskheroes:mask_open', 'values': ['true', 'false']},
        'TIMER': {'key': "entity.getInterpolatedData('fiskheroes:mask_open_timer2') > 0", 'values': ['true', 'false']},
    },
}
(textures / 'ci_mini_display.tx.json').write_text(json.dumps(definition))
shutil.copyfile(textures / 'the_flash_layer1.png', textures / 'ci_mini_display_true_false_false.png')
