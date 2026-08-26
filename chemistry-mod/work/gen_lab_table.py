#!/usr/bin/env python3
"""Builds the 实验台 (lab bench) block from the user's BlockBench model:
thick tabletop (black top, light bottom, four blue sides) + four corner legs."""

import base64
import io
import json
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")

BB = "/Users/apple/Desktop/实验台.bbmodel"


def main():
    bb = json.load(open(BB, encoding="utf-8"))
    src = bb["textures"][0]["source"]
    img = Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA")
    assert img.size == (48, 48)
    px = img.load()

    def tile(tx, ty):
        return img.crop((tx * 16, ty * 16, tx * 16 + 16, ty * 16 + 16))

    tex_dir = os.path.join(ASSETS, "textures", "block")
    model_dir = os.path.join(ASSETS, "models", "block")
    item_model_dir = os.path.join(ASSETS, "models", "item")
    items_dir = os.path.join(ASSETS, "items")
    state_dir = os.path.join(ASSETS, "blockstates")
    for d in (tex_dir, model_dir, item_model_dir, items_dir, state_dir):
        os.makedirs(d, exist_ok=True)

    tile(1, 1).save(os.path.join(tex_dir, "lab_table_top.png"))       # black top
    tile(0, 2).save(os.path.join(tex_dir, "lab_table_bottom.png"))    # light bottom
    tile(1, 0).save(os.path.join(tex_dir, "lab_table_north.png"))
    tile(1, 2).save(os.path.join(tex_dir, "lab_table_south.png"))
    tile(0, 1).save(os.path.join(tex_dir, "lab_table_west.png"))
    tile(2, 1).save(os.path.join(tex_dir, "lab_table_east.png"))
    # Legs: solid dark strip, matching the table's dark frame colour.
    Image.new("RGBA", (16, 16), (0x28, 0x28, 0x28, 255)).save(
        os.path.join(tex_dir, "lab_table_leg.png"))

    face = {"uv": [0, 0, 16, 16]}
    leg_face = {"uv": [0, 0, 16, 16], "texture": "leg"}

    def leg_element(x0, z0):
        # Top at 14.98: just under the plate (no coplanar faces, no z-fight).
        return {"from": [x0, 0, z0], "to": [x0 + 2, 14.98, z0 + 2], "faces": {
            # No up face: it is coplanar with the base plate and would z-fight.
            "down": dict(leg_face),
            "north": dict(leg_face), "south": dict(leg_face),
            "east": dict(leg_face), "west": dict(leg_face)}}

    model = {
        "parent": "block/block",
        "textures": {
            "up": "chemistry:block/lab_table_top",
            "down": "chemistry:block/lab_table_bottom",
            "north": "chemistry:block/lab_table_north",
            "south": "chemistry:block/lab_table_south",
            "east": "chemistry:block/lab_table_east",
            "west": "chemistry:block/lab_table_west",
            "leg": "chemistry:block/lab_table_leg",
            "particle": "chemistry:block/lab_table_north",
        },
        "elements": [
            # Thick tabletop (slightly inset), black top at y=15.
            {"from": [1, 2, 1], "to": [15, 15, 15], "faces": {
                "up": dict(face, texture="up"),
                "down": dict(face, texture="down"),
                "north": dict(face, texture="north"),
                "south": dict(face, texture="south"),
                "east": dict(face, texture="east"),
                "west": dict(face, texture="west")}},
            # Four tall legs.
            leg_element(0, 0),
            leg_element(0, 14),
            leg_element(14, 14),
            leg_element(14, 0),
            # Bottom base plate.
            {"from": [0, 14.98, 0], "to": [16, 16, 16], "faces": {
                "up": dict(face, texture="down"),
                "down": dict(face, texture="down"),
                "north": dict(face, texture="north"),
                "south": dict(face, texture="south"),
                "east": dict(face, texture="east"),
                "west": dict(face, texture="west")}},
        ],
    }
    with open(os.path.join(model_dir, "lab_table.json"), "w", encoding="utf-8") as f:
        json.dump(model, f, ensure_ascii=False, indent=2)
    with open(os.path.join(item_model_dir, "lab_table.json"), "w", encoding="utf-8") as f:
        json.dump({"parent": "chemistry:block/lab_table"}, f, ensure_ascii=False, indent=2)
    with open(os.path.join(items_dir, "lab_table.json"), "w", encoding="utf-8") as f:
        json.dump({"model": {"type": "minecraft:model", "model": "chemistry:item/lab_table"}},
                  f, ensure_ascii=False, indent=2)
    with open(os.path.join(state_dir, "lab_table.json"), "w", encoding="utf-8") as f:
        json.dump({"variants": {"": {"model": "chemistry:block/lab_table"}}},
                  f, ensure_ascii=False, indent=2)

    print("lab table (bbmodel) generated")


if __name__ == "__main__":
    main()
