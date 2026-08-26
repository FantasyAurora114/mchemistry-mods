#!/usr/bin/env python3
"""Wood splint (木条) + glowing splint (带火星的木条) item icons."""

import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")


def main():
    item_tex = os.path.join(ASSETS, "textures", "item")
    item_model = os.path.join(ASSETS, "models", "item")
    items_dir = os.path.join(ASSETS, "items")
    for d in (item_tex, item_model, items_dir):
        os.makedirs(d, exist_ok=True)

    # Wood stick (diagonal), browns; glowing tip red/orange.
    SPLINT = [
        "................",
        "................",
        "................",
        ".......###......",
        "......###.......",
        "......##........",
        ".....###........",
        ".....##.........",
        "....###.........",
        "....##..........",
        "...###..........",
        "...##...........",
        "..###...........",
        "..##............",
        "................",
        "................",
    ]
    GLOWING = [
        "................",
        "................",
        ".......RRR......",
        "......RRRR......",
        "......RR........",
        ".....###........",
        ".....##.........",
        "....###.........",
        "....##..........",
        "...###..........",
        "...##...........",
        "..###...........",
        "..##............",
        "................",
        "................",
        "................",
    ]
    colors = {"#": (0x8A, 0x63, 0x3A, 255), "R": (0xE8, 0x5A, 0x2A, 255)}

    def draw(rows):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in colors:
                    img.putpixel((x, y), colors[ch])
        return img

    for item_id, rows in (("splint", SPLINT), ("glowing_splint", GLOWING)):
        draw(rows).save(os.path.join(item_tex, item_id + ".png"))
        with open(os.path.join(item_model, item_id + ".json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": f"chemistry:item/{item_id}"}},
                      f, ensure_ascii=False, indent=2)
        with open(os.path.join(items_dir, item_id + ".json"), "w", encoding="utf-8") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"chemistry:item/{item_id}"}},
                      f, ensure_ascii=False, indent=2)
    print("splint assets generated")


if __name__ == "__main__":
    main()
