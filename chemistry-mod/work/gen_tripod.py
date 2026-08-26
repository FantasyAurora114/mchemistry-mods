#!/usr/bin/env python3
"""Builds the chemistry tripod (三脚架) and clay triangle (泥三角) assets.

The top is a perfect equilateral triangle (side 6, centred on the block) and
the three legs form a 75-degree angle with the ground (15 degrees from
vertical, splayed outward). Minecraft block models cannot rotate by 60/15
degrees, so the tripod block is invisible and the renderer draws the bars and
legs with arbitrary pose-stack rotations; this script only provides the
metal texture, the bar/leg part models and the item icon.
"""

import base64
import io
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")
DESK = "/Users/apple/Desktop"


def bar_element(fr, to, texture):
    return {"from": list(fr), "to": list(to), "shade": False,
            "faces": {f: {"texture": texture, "uv": [0, 0, 16, 16]}
                      for f in ("north", "south", "east", "west", "up", "down")}}


def main():
    tex_dir = os.path.join(ASSETS, "textures", "block")
    item_tex_dir = os.path.join(ASSETS, "textures", "item")
    model_dir = os.path.join(ASSETS, "models", "block")
    item_model_dir = os.path.join(ASSETS, "models", "item")
    items_dir = os.path.join(ASSETS, "items")
    for d in (tex_dir, item_tex_dir, model_dir, item_model_dir, items_dir):
        os.makedirs(d, exist_ok=True)

    # Metal texture: subtle gray gradient so the bars read as round metal.
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            v = 168 - int((x + y) * 1.4)
            img.putpixel((x, y), (v, v, v + 6, 255))
    img.save(os.path.join(tex_dir, "tripod.png"))

    # Bar: 6 long along +X, 0.5 square, CENTRED at the origin (the renderer
    # translates to each side's midpoint, so the bar must span -3..+3).
    bar = {"ambientocclusion": False, "textures": {"tripod": "chemistry:block/tripod"},
           "elements": [bar_element([-3, -0.25, -0.25], [3, 0.25, 0.25], "tripod")]}
    # Leg: hangs DOWN from the origin (top of the leg), 11 long, 0.5 square.
    leg = {"ambientocclusion": False, "textures": {"tripod": "chemistry:block/tripod"},
           "elements": [bar_element([-0.25, -11, -0.25], [0.25, 0, 0.25], "tripod")]}
    with open(os.path.join(model_dir, "tripod_bar.json"), "w", encoding="utf-8") as f:
        json.dump(bar, f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "tripod_leg.json"), "w", encoding="utf-8") as f:
        json.dump(leg, f, ensure_ascii=False, indent=2)

    # Clay triangle (泥三角): the user's unit model completed into a triangle.
    # One unit = a 3-long clay tube (1x1) with a wire at each end reaching the
    # triangle corners (unit total length 6 = the tripod's side length), drawn
    # along +X and CENTRED at the origin; the renderer rotates it 0/120/240
    # degrees and places it on the three sides of the tripod triangle.
    clay_bb = json.load(open(os.path.join(DESK, "泥三角单元.bbmodel"), encoding="utf-8"))
    src = clay_bb["textures"][1]["source"]
    Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA").save(
        os.path.join(tex_dir, "clay_triangle.png"))

    def clay_box(fr, to, uv):
        return {"from": list(fr), "to": list(to), "shade": False,
                "faces": {f: {"texture": "clay_triangle", "uv": list(uv)}
                          for f in ("north", "south", "east", "west", "up", "down")}}

    clay_unit = {
        "ambientocclusion": False,
        "textures": {"clay_triangle": "chemistry:block/clay_triangle"},
        "elements": [
            clay_box([-1.5, -0.5, -0.5], [1.5, 0.5, 0.5], [3, 3, 6, 6]),      # clay tube
            clay_box([-3, -0.333, -0.333], [-1.5, 0.333, 0.333], [0, 0, 16, 16]),   # wire
            clay_box([1.5, -0.333, -0.333], [3, 0.333, 0.333], [0, 0, 16, 16]),     # wire
        ],
    }
    with open(os.path.join(model_dir, "clay_triangle_unit.json"), "w", encoding="utf-8") as f:
        json.dump(clay_unit, f, ensure_ascii=False, indent=2)

    # The invisible tripod block still needs valid blockstate variants for all
    # of its properties.
    state_dir = os.path.join(ASSETS, "blockstates")
    os.makedirs(state_dir, exist_ok=True)
    variants = {}
    for clay in (False, True):
        for lamp in (False, True):
            for lit in (False, True):
                for vessel in (False, True):
                    key = (f"has_clay_triangle={str(clay).lower()},has_lamp={str(lamp).lower()},"
                           f"has_vessel={str(vessel).lower()},lamp_lit={str(lit).lower()}")
                    variants[key] = {"model": "chemistry:block/tripod_bar"}
    with open(os.path.join(state_dir, "tripod.json"), "w", encoding="utf-8") as f:
        json.dump({"variants": variants}, f, ensure_ascii=False, indent=2)

    # Item icon: a small tripod (triangle top + three legs), metal gray.
    # 'm' = mid gray, 'd' = dark gray.
    TRIPOD_ICON = [
        "................",
        ".....mmmmmm.....",
        ".....mmmmmm.....",
        "......mmmm......",
        ".......mm.......",
        ".......mm.......",
        "......m..m......",
        ".....m....m.....",
        "....m......m....",
        "....m......m....",
        "...m........m...",
        "...m........m...",
        "..m..........m..",
        "..m..........m..",
        "................",
        "................",
    ]
    CLAY_TRIANGLE_ICON = [
        "................",
        ".......bb.......",
        "......b..b......",
        ".....b....b.....",
        "....bbbbbbbb....",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    colors = {"m": (0x9A, 0x9A, 0x9A, 255), "d": (0x66, 0x66, 0x66, 255),
              "b": (0x8B, 0xAD, 0xD0, 255)}
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(TRIPOD_ICON):
        for x, ch in enumerate(row):
            if ch in colors:
                icon.putpixel((x, y), colors[ch])
    icon.save(os.path.join(item_tex_dir, "tripod.png"))
    with open(os.path.join(item_model_dir, "tripod.json"), "w", encoding="utf-8") as f:
        json.dump({"parent": "minecraft:item/generated",
                   "textures": {"layer0": "chemistry:item/tripod"}},
                  f, ensure_ascii=False, indent=2)
    with open(os.path.join(items_dir, "tripod.json"), "w", encoding="utf-8") as f:
        json.dump({"model": {"type": "minecraft:model", "model": "chemistry:item/tripod"}},
                  f, ensure_ascii=False, indent=2)
    icon.resize((64, 64), Image.NEAREST).save(
        os.path.join(os.path.expanduser("~/Desktop"), "tripod_icon_preview.png"))

    clay_icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(CLAY_TRIANGLE_ICON):
        for x, ch in enumerate(row):
            if ch in colors:
                clay_icon.putpixel((x, y), colors[ch])
    clay_icon.save(os.path.join(item_tex_dir, "clay_triangle.png"))
    with open(os.path.join(item_model_dir, "clay_triangle.json"), "w", encoding="utf-8") as f:
        json.dump({"parent": "minecraft:item/generated",
                   "textures": {"layer0": "chemistry:item/clay_triangle"}},
                  f, ensure_ascii=False, indent=2)
    with open(os.path.join(items_dir, "clay_triangle.json"), "w", encoding="utf-8") as f:
        json.dump({"model": {"type": "minecraft:model", "model": "chemistry:item/clay_triangle"}},
                  f, ensure_ascii=False, indent=2)
    clay_icon.resize((64, 64), Image.NEAREST).save(
        os.path.join(os.path.expanduser("~/Desktop"), "clay_triangle_icon_preview.png"))
    print("tripod assets generated")


if __name__ == "__main__":
    main()
