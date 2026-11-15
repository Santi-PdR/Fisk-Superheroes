#!/usr/bin/env python3
"""Verify every directly registered Fisk item has a readable model and local overrides."""
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fiskheroes'
ITEMS = ROOT / 'src/main/java/com/fiskmods/heroes/common/item/ModItems.java'


def main():
    source = ITEMS.read_text(encoding='utf-8')
    names = set(re.findall(r'REGISTRY\.register\(\s*"([a-z0-9_]+)"', source))
    names.update(re.findall(r'registerGadget\(\s*"([a-z0-9_]+)"', source))
    errors = []
    visited = set()

    def check(model):
        if model in visited:
            return
        visited.add(model)
        file = ASSETS / 'models' / (model + '.json')
        if not file.is_file():
            errors.append(f'missing model {model}')
            return
        try:
            data = json.loads(file.read_text(encoding='utf-8'))
        except (OSError, ValueError) as exc:
            errors.append(f'invalid model {model}: {exc}')
            return
        if not isinstance(data, dict) or not isinstance(data.get('parent'), str):
            errors.append(f'model {model} has no parent')
            return
        parent = data['parent']
        if parent.startswith('fiskheroes:'):
            check(parent.removeprefix('fiskheroes:'))
        for override in data.get('overrides', []):
            target = override.get('model', '') if isinstance(override, dict) else ''
            if target.startswith('fiskheroes:'):
                check(target.removeprefix('fiskheroes:'))

    if not names:
        errors.append('no item registrations found')
    for name in sorted(names):
        check('item/' + name)
    if errors:
        print('Item model verification FAILED:', *errors, sep='\n- ')
        return 1
    print(f'Item model verification passed: {len(names)} registered items and {len(visited)} local models checked.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
