#!/usr/bin/env python3
"""Validate the shipped hero-pack texture generators and their input sprites."""
import itertools
import json
from pathlib import Path
import re
import sys

ASSETS = Path(__file__).resolve().parents[1] / 'src/main/resources/assets'
ROOT = ASSETS / 'fiskheroes/textures/heroes'


def expand(template, getters):
    keys = sorted(set(re.findall(r'<([^>]+)>', template)))
    choices = []
    for key in keys:
        values = getters.get(key, {}).get('values')
        if isinstance(values, list) and values:
            choices.append(values)
        elif isinstance(values, dict) and 'min' in values and 'max' in values:
            choices.append(range(values['min'], values['max'] + 1))
        else:
            raise ValueError(f'cannot enumerate getter {key}')
    for combination in itertools.product(*choices):
        result = template
        for key, value in zip(keys, combination):
            result = result.replace(f'<{key}>', str(value))
        yield result


def sprite(location):
    namespace, sep, path = location.partition(':')
    if not sep:
        namespace, path = 'fiskheroes', namespace
    path = path.removesuffix('.png')
    return ASSETS / namespace / 'textures/heroes' / (path + '.png')


def main():
    errors = []
    refs = 0
    files = list(ROOT.rglob('*.tx.json'))
    for file in files:
        try:
            obj = json.loads(file.read_text(encoding='utf-8'))
            getters = obj.get('getters', {})
            texture = obj['texture']
            sprites = []
            if isinstance(texture, str):
                sprites.append(texture)
            elif isinstance(texture, dict):
                sprites.append(texture['in'])
                for operation in texture.get('transform', []):
                    op = operation['operation']
                    if op in ('OR', 'XOR'):
                        sprites.append(operation['supply'])
                    elif op == 'STITCH':
                        namespace, _, name = operation['supply'].partition(':')
                        path = ASSETS / namespace / 'textures/stitchers' / (name + '.json')
                        if not path.is_file():
                            errors.append(f'{file}: missing stitcher {path}')
                        else:
                            json.loads(path.read_text(encoding='utf-8'))
                    elif op != 'HUE':
                        errors.append(f'{file}: unsupported operation {op}')
            else:
                raise ValueError('texture must be a string or an object')
            for template in sprites:
                for value in expand(template, getters):
                    refs += 1
                    path = sprite(value)
                    if not path.is_file():
                        errors.append(f'{file}: missing input {path}')
        except (OSError, ValueError, KeyError, TypeError) as error:
            errors.append(f'{file}: {error}')
    if errors:
        print('\n'.join(errors), file=sys.stderr)
        return 1
    print(f'Suit texture verification passed: {len(files)} definitions, {refs} input sprites resolved.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
