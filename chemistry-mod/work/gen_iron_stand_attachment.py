#!/usr/bin/env python3
"""Builds the iron-stand heating attachments (铁圈 / 石棉网 / 陶土网):

* The clay-gauze (陶土网) and iron-ring (铁圈) meshes are taken from the
  user's with-clay-net / with-ring iron-stand BlockBench models (the thin
  wire elements can be unclipped from the stand).
* The asbestos gauze (石棉网) is the same mesh shape in white.
* Generates the three item icons and item model JSONs.
"""

import base64
import io
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")
DESK = "/Users/apple/Desktop"


def load_bb(name):
    return json.load(open(os.path.join(DESK, name + ".bbmodel"), encoding="utf-8"))


def extract_texture(bb, index=0):
    src = bb["textures"][index]["source"]
    return Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA")


def wire_to_mc(el, texture):
    faces = {}
    for face, data in el.get("faces", {}).items():
        if data is None:
            continue
        entry = {"texture": texture}
        if "uv" in data:
            entry["uv"] = data["uv"]
        faces[face] = entry
    return {"from": list(el["from"]), "to": list(el["to"]), "shade": False, "faces": faces}


def attachment_model(elements, textures):
    return {"ambientocclusion": False, "textures": textures, "elements": elements}


def main():
    tex_dir = os.path.join(ASSETS, "textures", "block")
    item_tex_dir = os.path.join(ASSETS, "textures", "item")
    model_dir = os.path.join(ASSETS, "models", "block")
    item_model_dir = os.path.join(ASSETS, "models", "item")
    items_dir = os.path.join(ASSETS, "items")
    for d in (tex_dir, item_tex_dir, model_dir, item_model_dir, items_dir):
        os.makedirs(d, exist_ok=True)

    with_net = load_bb("带有陶土网的铁架台")
    with_ring = load_bb("带有铁圈的铁架台")
    # The wire elements start at index 9 in both files.
    clay_wires = [wire_to_mc(el, "iron_stand") for el in with_net["elements"][9:]]
    ring_wires = [wire_to_mc(el, "iron_stand") for el in with_ring["elements"][9:]]

    # Asbestos gauze: same mesh shape, white wires.
    asbestos_wires = [wire_to_mc(el, "asbestos_gauze") for el in with_net["elements"][9:]]
    Image.new("RGBA", (16, 16), (0xF4, 0xF4, 0xF4, 255)).save(
        os.path.join(tex_dir, "asbestos_gauze.png"))

    iron_tex = {"iron_stand": "chemistry:block/iron_stand"}
    asbestos_tex = {"asbestos_gauze": "chemistry:block/asbestos_gauze"}
    models = {
        "iron_stand_attachment_ring": (ring_wires, iron_tex),
        "iron_stand_attachment_clay": (clay_wires, iron_tex),
        "iron_stand_attachment_asbestos": (asbestos_wires, asbestos_tex),
    }
    for name, (els, tex) in models.items():
        with open(os.path.join(model_dir, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(attachment_model(els, tex), f, ensure_ascii=False, indent=2)

    # Item icons: 16x16 pixel art.
    # 'm' = mid gray, 'd' = dark gray, 'w' = white.
    RING_ICON = [
        "................",
        "................",
        "................",
        "....mmmmmmmm....",
        "....mm....mm....",
        "....mm....mm....",
        "....mm....mm....",
        "....mm....mm....",
        "....mm....mm....",
        "....mm....mm....",
        "....mm....mm....",
        "....mmmmmmmm....",
        "................",
        "................",
        "................",
        "................",
    ]
    GAUZE_ICON = [
        "................",
        "................",
        "................",
        "................",
        "....m.m.m.m....",
        "................",
        "....m.m.m.m....",
        "................",
        "....m.m.m.m....",
        "................",
        "....m.m.m.m....",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    ASBESTOS_ICON = [row.replace("m", "w") for row in GAUZE_ICON]

    colors = {
        "m": (0x90, 0x90, 0x90, 255),
        "w": (0xF4, 0xF4, 0xF4, 255),
    }
    icons = {
        "iron_ring": RING_ICON,
        "clay_gauze": GAUZE_ICON,
        "asbestos_gauze": ASBESTOS_ICON,
    }
    for item_id, rows in icons.items():
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in colors:
                    img.putpixel((x, y), colors[ch])
        img.save(os.path.join(item_tex_dir, item_id + ".png"))
        with open(os.path.join(item_model_dir, item_id + ".json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": f"chemistry:item/{item_id}"}},
                      f, ensure_ascii=False, indent=2)
        with open(os.path.join(items_dir, item_id + ".json"), "w", encoding="utf-8") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"chemistry:item/{item_id}"}},
                      f, ensure_ascii=False, indent=2)
        img.resize((64, 64), Image.NEAREST).save(os.path.join(DESK, item_id + "_icon_preview.png"))

    print("iron stand attachment assets generated")


if __name__ == "__main__":
    main()
