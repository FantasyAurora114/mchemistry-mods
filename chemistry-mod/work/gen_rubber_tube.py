#!/usr/bin/env python3
"""Generates the rubber tube (橡胶管) textures, the unit line model, and the
item definitions. The block-entity renderer stretches the unit model between
two anchor points."""

import json
import os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")


def main():
    tex_dir = os.path.join(ASSETS, "textures", "block")
    model_dir = os.path.join(ASSETS, "models", "block")
    items_dir = os.path.join(ASSETS, "items")
    state_dir = os.path.join(ASSETS, "blockstates")
    for d in (tex_dir, model_dir, items_dir, state_dir):
        os.makedirs(d, exist_ok=True)

    # Rubber palette: #D9A441 base, #F0D070 highlight, #A87A28 shadow.
    base = (0xD9, 0xA4, 0x41)
    light = (0xF0, 0xD0, 0x70)
    dark = (0xA8, 0x7A, 0x28)

    # Side texture: rubber with horizontal ridges (stretched along the tube).
    side = Image.new("RGBA", (16, 16), base + (255,))
    d = ImageDraw.Draw(side)
    for y in range(0, 16, 4):
        d.line([(0, y), (15, y)], fill=dark + (255,))
        d.line([(0, y + 1), (15, y + 1)], fill=light + (255,))
    side.save(os.path.join(tex_dir, "rubber_tube_side.png"))

    # End texture: rubber ring with a dark hole.
    end = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    de = ImageDraw.Draw(end)
    de.ellipse([0, 0, 15, 15], fill=base + (255,), outline=dark + (255,), width=2)
    de.ellipse([3, 3, 12, 12], fill=dark + (255,))
    de.ellipse([5, 5, 10, 10], fill=(0x20, 0x18, 0x08, 255))
    end.save(os.path.join(tex_dir, "rubber_tube_end.png"))

    # Unit line model: 1x1x1 box; +Z/-Z faces show the end ring (hole).
    line = {
        "ambientocclusion": False,
        "particle": "side",
        "display": {"none": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}},
        "textures": {
            "side": "chemistry:block/rubber_tube_side",
            "end": "chemistry:block/rubber_tube_end",
        },
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16], "shade": False, "faces": {
                "up": {"uv": [0, 0, 16, 16], "texture": "end"},
                "down": {"uv": [0, 0, 16, 16], "texture": "end"},
                "north": {"uv": [0, 0, 16, 16], "texture": "side"},
                "south": {"uv": [0, 0, 16, 16], "texture": "side"},
                "east": {"uv": [0, 0, 16, 16], "texture": "side"},
                "west": {"uv": [0, 0, 16, 16], "texture": "side"}}}
        ],
    }
    with open(os.path.join(model_dir, "rubber_tube_line.json"), "w", encoding="utf-8") as f:
        json.dump(line, f, ensure_ascii=False, indent=2)

    with open(os.path.join(items_dir, "rubber_tube_line.json"), "w", encoding="utf-8") as f:
        json.dump({"model": {"type": "minecraft:model", "model": "chemistry:block/rubber_tube_line"}},
                  f, ensure_ascii=False, indent=2)
    with open(os.path.join(state_dir, "rubber_tube_link.json"), "w", encoding="utf-8") as f:
        json.dump({"variants": {"": {"model": "chemistry:block/rubber_tube_line"}}},
                  f, ensure_ascii=False, indent=2)

    print("rubber tube assets generated")


if __name__ == "__main__":
    main()
