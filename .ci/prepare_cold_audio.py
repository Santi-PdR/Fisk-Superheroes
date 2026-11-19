#!/usr/bin/env python3
"""Seed a tiny CI-only cache with the upstream Cold Gun recordings."""
import json
import os
import sys
import zipfile
from pathlib import Path

archive = Path(sys.argv[1])
with zipfile.ZipFile(archive) as z:
    manifest = json.loads(z.read('sounds.json'))
    events = {key: value for key, value in manifest.items() if key.startswith('item.coldgun.')}
    if 'item.coldgun.beam.loop' not in events:
        raise RuntimeError('Upstream sound archive lacks Cold Gun loop')
    if os.environ.get('CI_COLD_LONG_START') == '1':
        if 'item.coldgun.beam.start' not in events:
            raise RuntimeError('Upstream sound archive lacks Cold Gun start')
        # Runner-only: use the eight-second loop recording as a non-looping start
        # so the real audio engine can observe movement and an early release.
        events['item.coldgun.beam.start']['sounds'] = events['item.coldgun.beam.loop']['sounds']
    recordings = [name for name in z.namelist() if name.startswith('sounds/item/cold_gun/') and name.endswith('.ogg')]
    for run in sys.argv[2:]:
        root = Path(run) / 'fiskheroes_sounds'
        sounds = root / 'pack/assets/fiskheroes/sounds'
        sounds.mkdir(parents=True, exist_ok=True)
        index = []
        for name in recordings:
            relative = name[len('sounds/'):]
            target = sounds / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(z.read(name))
            index.append('fiskheroes:' + relative[:-4].replace('/', '.') + '=' + relative)
        (root / 'sounds.index').write_text('\n'.join(index) + '\n')
        (root / 'sound-events.json').write_text(json.dumps(events))
