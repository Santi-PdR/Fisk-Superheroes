#!/usr/bin/env python3
"""Check hero trail declarations, inheritance chains, and referenced trail resources."""

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets"
MODELS = {}
TRAILS = set()

for path in ROOT.glob("*/models/heroes/*.json"):
    key = f"{path.parts[-4]}:{path.stem}"
    MODELS[key] = json.loads(path.read_text())
for path in ROOT.glob("*/models/trails/*.json"):
    TRAILS.add(f"{path.parts[-4]}:{path.stem}")


def inherited_model(key, seen=()):
    if key in seen:
        raise ValueError(f"hero model parent cycle: {' -> '.join((*seen, key))}")
    model = MODELS.get(key)
    if model is None:
        return {}
    result = inherited_model(model["parent"], (*seen, key)) if "parent" in model else {}
    result = {**result, **model}
    if "custom" in model:
        custom = dict(result.get("custom", {}))
        custom.update(model["custom"])
        result["custom"] = custom
    return result


def merge_json(parent, child):
    result = dict(parent)
    for key, value in child.items():
        if isinstance(value, dict) and isinstance(result.get(key), dict):
            result[key] = merge_json(result[key], value)
        else:
            result[key] = value
    return result


def inherited_trail(key, seen=()):
    if key in seen:
        raise ValueError(f"trail parent cycle: {' -> '.join((*seen, key))}")
    path = ROOT / key.split(':')[0] / "models/trails" / f"{key.split(':')[1]}.json"
    data = json.loads(path.read_text())
    result = {}
    if "parent" in data:
        parent = data["parent"]
        if ":" not in parent:
            parent = f"{key.split(':', 1)[0]}:{parent}"
        result = inherited_trail(parent, (*seen, key))
    return merge_json(result, data)


errors = []
checked = 0
for hero in MODELS:
    effect = inherited_model(hero).get("custom", {}).get("fiskheroes:trail")
    if not isinstance(effect, dict) or "type" not in effect:
        continue
    checked += 1
    resource = effect["type"]
    if resource.startswith("builtin/lightning_rgb_") and re.fullmatch(r"builtin/lightning_rgb_\d+", resource):
        continue
    if ":" not in resource:
        resource = f"{hero.split(':', 1)[0]}:{resource}"
    if resource not in TRAILS:
        errors.append(f"{hero}: trail resource {resource!r} is missing")

for key, data in ((key, json.loads((ROOT / key.split(':')[0] / "models/trails" / f"{key.split(':')[1]}.json").read_text())) for key in TRAILS):
    parent = data.get("parent")
    seen = {key}
    while parent:
        if ":" not in parent:
            parent = f"{key.split(':', 1)[0]}:{parent}"
        if parent in seen:
            errors.append(f"{key}: trail parent cycle through {parent}")
            break
        seen.add(parent)
        if parent not in TRAILS:
            errors.append(f"{key}: parent resource {parent!r} is missing")
            break
        parent_data = json.loads((ROOT / parent.split(':')[0] / "models/trails" / f"{parent.split(':')[1]}.json").read_text())
        parent = parent_data.get("parent")

for key in TRAILS:
    trail = inherited_trail(key)
    texture = trail.get("particles", {}).get("texture")
    if not texture or texture.startswith('@'):
        continue
    namespace, path = texture.split(':', 1) if ':' in texture else (key.split(':', 1)[0], texture)
    if not (ROOT / namespace / path).is_file():
        errors.append(f"{key}: trail particle texture {texture!r} is missing")

if errors:
    raise SystemExit("Trail verification failed:\n" + "\n".join(errors))
print(f"Trail verification passed: {checked} hero trail declarations resolve; {len(TRAILS)} trail resources and parent chains checked.")
