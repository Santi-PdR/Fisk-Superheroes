#!/usr/bin/env python3
"""Keep compact particle-smoke previews in the existing CI diagnostic log."""
import base64
import io
from pathlib import Path
from PIL import Image

for perspective in ('third', 'first'):
    path = Path(f'ci-arrow-killer-frost-smoke-{perspective}.png')
    if not path.exists():
        continue
    with Image.open(path) as image:
        image.thumbnail((640, 360))
        buffer = io.BytesIO()
        image.convert('RGB').save(buffer, format='JPEG', quality=70, optimize=True)
    print(f'PARTICLE_PREVIEW_{perspective.upper()}=' + base64.b64encode(buffer.getvalue()).decode('ascii'))
