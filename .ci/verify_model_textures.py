#!/usr/bin/env python3
"""Check that every in-mod-namespace texture used by a model is packaged."""

import json
from pathlib import Path
import sys


ASSETS = Path(__file__).resolve().parents[1] / "src/main/resources/assets/fiskheroes"
MODELS = ASSETS / "models"


def main() -> int:
    errors: list[str] = []
    checked = 0

    for model in sorted(MODELS.rglob("*.json")):
        try:
            document = json.loads(model.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            errors.append(f"{model}: cannot parse model JSON: {error}")
            continue

        textures = document.get("textures", {})
        if not isinstance(textures, dict):
            errors.append(f"{model}: 'textures' must be a JSON object")
            continue

        for name, location in textures.items():
            # Texture aliases ("#layer0") are resolved by the model itself.
            if not isinstance(location, str) or location.startswith("#"):
                continue
            namespace, separator, path = location.partition(":")
            if not separator or namespace != "fiskheroes":
                continue

            checked += 1
            texture = ASSETS / "textures" / f"{path}.png"
            if not texture.is_file():
                errors.append(f"{model}: texture '{name}' points to missing {texture}")

    if errors:
        print("Model texture verification FAILED:")
        print("\n".join(f"- {error}" for error in errors))
        return 1

    print(f"Model texture verification passed: {checked} FiskHeroes texture references resolved.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
