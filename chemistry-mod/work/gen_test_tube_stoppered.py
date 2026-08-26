#!/usr/bin/env python3
"""Generates the stoppered test-tube item assets (塞着橡胶塞的试管 and 套着
试管架的塞着橡胶塞的试管) from the user-drawn LibreSprite files, plus the
items JSONs for every test-tube x stopper x clamp variant."""

import json
import os
import struct
import zlib

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")
DESKTOP = os.path.expanduser("~/Desktop")


def parse_ase(path):
    data = open(path, "rb").read()
    if data[4:6] != b"\xe0\xa5":
        raise ValueError(f"not an ase file: {path}")
    nframes = struct.unpack_from("<H", data, 6)[0]
    pos = 128
    best = None
    for _ in range(nframes):
        fstart = pos
        fsize, _magic, _nchunks, _dur = struct.unpack_from("<IHHH", data, pos)
        pos += 16
        for _c in range(_nchunks):
            csize, ctype = struct.unpack_from("<IH", data, pos)
            body = pos + 6
            if ctype == 0x2005:
                layer_index, cx, cy, opacity, cel_type = struct.unpack_from("<HHhBB", data, body)
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
                            # LibreSprite stores pixels in RGBA order.
                            r, g, b, a = raw[(yy * w + xx) * 4 : (yy * w + xx) * 4 + 4]
                            px[xx, yy] = (r, g, b, a)
                    if best is None or (w, h) > (best[0].width, best[0].height):
                        best = (img, cx, cy)
            pos += csize
        pos = fstart + fsize
    return best


def canvas_from_ase(source):
    parsed = parse_ase(os.path.join(DESKTOP, source + ".ase"))
    if parsed is None:
        raise SystemExit(f"no cel found in {source}.ase")
    img, cx, cy = parsed
    canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    canvas.alpha_composite(img, (cx, cy))
    return canvas


def main():
    tex_dir = os.path.join(ASSETS, "textures", "item")
    model_dir = os.path.join(ASSETS, "models", "item")
    items_dir = os.path.join(ASSETS, "items")
    for d in (tex_dir, model_dir, items_dir):
        os.makedirs(d, exist_ok=True)

    stoppered = canvas_from_ase("塞着橡胶塞的试管")
    clamped = canvas_from_ase("套着试管架的塞着橡胶塞的试管")
    stoppered.save(os.path.join(tex_dir, "test_tube_stoppered.png"))
    clamped.save(os.path.join(tex_dir, "test_tube_clamped_stoppered.png"))

    # White liquid overlays (tinted at runtime via custom_model_data colour).
    def liquid(inner_x, y0, y1):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        for y in range(y0, y1):
            d.line([(inner_x, y), (inner_x + 1, y)], fill=(255, 255, 255, 255))
        return img

    liquid(7, 7, 12).save(os.path.join(tex_dir, "test_tube_stoppered_contents.png"))
    liquid(9, 9, 12).save(os.path.join(tex_dir, "test_tube_clamped_stoppered_contents.png"))

    def simple_model(name, texture):
        with open(os.path.join(model_dir, name + ".json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": f"chemistry:item/{texture}"}},
                      f, ensure_ascii=False, indent=2)

    for name in ("test_tube_stoppered", "test_tube_clamped_stoppered"):
        simple_model(name, name)
    simple_model("test_tube_stoppered_contents", "test_tube_stoppered_contents")
    simple_model("test_tube_clamped_stoppered_contents", "test_tube_clamped_stoppered_contents")

    def item_json(base_model, contents_model, path):
        with open(os.path.join(items_dir, path + ".json"), "w", encoding="utf-8") as f:
            json.dump({
                "model": {
                    "type": "minecraft:select",
                    "property": "minecraft:custom_model_data",
                    "cases": [{
                        "when": "filled",
                        "model": {
                            "type": "minecraft:composite",
                            "models": [
                                {"type": "minecraft:model", "model": f"chemistry:item/{base_model}"},
                                {"type": "minecraft:model", "model": f"chemistry:item/{contents_model}",
                                 "tints": [{"type": "minecraft:custom_model_data", "index": 0, "default": 16777215}]}
                            ]
                        }
                    }],
                    "fallback": {"type": "minecraft:model", "model": f"chemistry:item/{base_model}"}
                }
            }, f, ensure_ascii=False, indent=2)

    vessels = ["test_tube_5ml", "test_tube_10ml", "test_tube_50ml"]
    count = 0
    for v in vessels:
        for glass in ("", "_borosilicate"):
            for clamp in ("", "_clamped"):
                base = "test_tube_clamped_stoppered" if clamp else "test_tube_stoppered"
                contents = base + "_contents"
                for holes in (1, 2):
                    item_id = f"{v}{glass}{clamp}_stoppered_{holes}"
                    item_json(base, contents, item_id)
                    count += 1
    print(f"{count} stoppered test-tube item definitions generated")


if __name__ == "__main__":
    main()
