#!/usr/bin/env python3
"""Rubber tube v2 assets: the user's recolored unprocessed-tube icon (plus a
wet darkened variant), and the gas nozzle (导气嘴) items."""

import json
import os
import struct
import zlib

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")
DESK = os.path.expanduser("~/Desktop")


def parse_ase(path):
    data = open(path, "rb").read()
    if data[4:6] != b"\xe0\xa5":
        raise ValueError(f"not an ase file: {path}")
    nframes = struct.unpack_from("<H", data, 6)[0]
    pos = 128
    best = None
    for _ in range(nframes):
        fstart = pos
        fsize, _m, _nchunks, _d = struct.unpack_from("<IHHH", data, pos)
        pos += 16
        for _c in range(_nchunks):
            csize, ctype = struct.unpack_from("<IH", data, pos)
            body = pos + 6
            if ctype == 0x2005:
                _layer, cx, cy, _op, _ct = struct.unpack_from("<HHhBB", data, body)
                rest = data[body + 8 : body + csize - 6]
                idx = rest.find(b"\x78")
                if idx >= 0:
                    w = struct.unpack_from("<H", rest, idx - 4)[0]
                    h = struct.unpack_from("<H", rest, idx - 2)[0]
                    raw = zlib.decompress(rest[idx:])
                    img = Image.new("RGBA", (w, h))
                    px = img.load()
                    for yy in range(h):
                        for xx in range(w):
                            r, g, b, a = raw[(yy * w + xx) * 4 : (yy * w + xx) * 4 + 4]
                            px[xx, yy] = (r, g, b, a)
                    if best is None or (w, h) > (best[0].width, best[0].height):
                        best = (img, cx, cy)
            pos += csize
        pos = fstart + fsize
    return best


def main():
    tex_dir = os.path.join(ASSETS, "textures", "item")
    model_dir = os.path.join(ASSETS, "models", "item")
    items_dir = os.path.join(ASSETS, "items")
    for d in (tex_dir, model_dir, items_dir):
        os.makedirs(d, exist_ok=True)

    # Rubber tube icon: the user's recolored unprocessed-tube texture.
    src = os.path.join(DESK, "未处理的橡胶管（橡胶色）.png")
    dry = Image.open(src).convert("RGBA")
    dry.save(os.path.join(tex_dir, "rubber_tube.png"))

    # Wet variant keeps the same icon (no darkening).
    dry.save(os.path.join(tex_dir, "rubber_tube_wet.png"))

    def simple_model(name, texture):
        with open(os.path.join(model_dir, name + ".json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": f"chemistry:item/{texture}"}},
                      f, ensure_ascii=False, indent=2)

    simple_model("rubber_tube", "rubber_tube")
    simple_model("rubber_tube_wet", "rubber_tube_wet")
    with open(os.path.join(items_dir, "rubber_tube.json"), "w", encoding="utf-8") as f:
        json.dump({"model": {
            "type": "minecraft:select",
            "property": "minecraft:custom_model_data",
            "cases": [{"when": "wet",
                       "model": {"type": "minecraft:model", "model": "chemistry:item/rubber_tube_wet"}}],
            "fallback": {"type": "minecraft:model", "model": "chemistry:item/rubber_tube"}
        }}, f, ensure_ascii=False, indent=2)

    for item_id, source in (("gas_nozzle", "导气嘴"), ("gas_nozzle_tubed", "导气嘴（接橡胶管）")):
        parsed = parse_ase(os.path.join(DESK, source + ".ase"))
        if parsed is None:
            raise SystemExit(f"no cel found in {source}.ase")
        img, cx, cy = parsed
        canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        canvas.alpha_composite(img, (cx, cy))
        canvas.save(os.path.join(tex_dir, item_id + ".png"))
        simple_model(item_id, item_id)
        with open(os.path.join(items_dir, item_id + ".json"), "w", encoding="utf-8") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"chemistry:item/{item_id}"}},
                      f, ensure_ascii=False, indent=2)
    print("rubber tube v2 + gas nozzle assets generated")


if __name__ == "__main__":
    main()
