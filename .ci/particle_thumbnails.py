#!/usr/bin/env python3
"""Keep compact particle-smoke previews in the existing CI diagnostic log."""
import base64
import io
from pathlib import Path
from PIL import Image

for label, filename in (('THIRD', 'killer-frost-smoke-third'), ('FIRST', 'killer-frost-smoke-first'),
                        ('CHAIN_FIRST', 'chain-first'), ('CHAIN_THIRD', 'chain-third'), ('CHAIN_MOVED', 'chain-moved'),
                        ('MONITOR', 'monitor-aura'), ('BREACH', 'breach-cloud'), ('BREACH_FIRST', 'breach-first'),
                        ('FIRE_FIRST', 'firestorm-first'), ('FIRE_THIRD', 'firestorm-third'),
                        ('FIRE_INCOMPLETE', 'firestorm-incomplete'), ('FIRE_INCOMPLETE_FIRST', 'firestorm-incomplete-first'), ('HEAT_THIRD', 'heat-third'), ('HEAT_FIRST', 'heat-first'), ('MYSTERIO_BEAM_FIRST', 'mysterio-beam-first'), ('MYSTERIO_BEAM_THIRD', 'mysterio-beam-third'), ('MK50_FIRST', 'mk50-first'), ('MK50_FIRST_BEFORE', 'mk50-first-before'), ('MK50_THIRD', 'mk50-third')):
    path = Path(f'ci-arrow-{filename}.png')
    if not path.exists():
        continue
    with Image.open(path) as image:
        image.thumbnail((640, 360))
        buffer = io.BytesIO()
        image.convert('RGB').save(buffer, format='JPEG', quality=70, optimize=True)
    print(f'PARTICLE_PREVIEW_{label}=' + base64.b64encode(buffer.getvalue()).decode('ascii'))
