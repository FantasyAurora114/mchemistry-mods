#!/usr/bin/env python3
"""Generates the 1-hole / 2-hole rubber stopper (橡胶塞) item assets from the
user-drawn LibreSprite files on the Desktop (1孔橡胶塞.ase / 2孔橡胶塞.ase)."""

import json
import os
import struct
import zlib

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")
DESKTOP = os.path.expanduser("~/Desktop")


def parse_ase(path):
    """Parse a LibreSprite .ase and return the largest cel as RGBA."""
    data = open(path, "rb").read()
    if data[4:6] != b"\xe0\xa5":
        raise ValueError(f"not an ase file: {path}")
    nframes = struct.unpack_from("<H", data, 6)[0]
    pos = 128
    best = None
    for _ in range(nframes):
        fstart = pos
        fsize, _magic, _nchunks, _dur = struct.unpack_from("<IHHH", data, pos)
        pos += 16  # LibreSprite frame header is 16 bytes
        for _c in range(_nchunks):
            csize, ctype = struct.unpack_from("<IH", data, pos)
            body = pos + 6
            if ctype == 0x2005:  # cel
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
                    if best is None or (w, h) > (best.width, best.height):
                        best = (img, cx, cy)
            pos += csize
        pos = fstart + fsize
    return best


def main():
    item_tex = os.path.join(ASSETS, "textures", "item")
    item_model = os.path.join(ASSETS, "models", "item")
    items_dir = os.path.join(ASSETS, "items")
    for d in (item_tex, item_model, items_dir):
        os.makedirs(d, exist_ok=True)

    variants = [("rubber_stopper_1_hole", "1孔橡胶塞"), ("rubber_stopper_2_hole", "2孔橡胶塞")]
    for item_id, source in variants:
        parsed = parse_ase(os.path.join(DESKTOP, source + ".ase"))
        if parsed is None:
            raise SystemExit(f"no cel found in {source}.ase")
        img, cx, cy = parsed
        canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        canvas.alpha_composite(img, (cx, cy))
        canvas.save(os.path.join(item_tex, item_id + ".png"))
        with open(os.path.join(item_model, item_id + ".json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": f"chemistry:item/{item_id}"}},
                      f, ensure_ascii=False, indent=2)
        with open(os.path.join(items_dir, item_id + ".json"), "w", encoding="utf-8") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"chemistry:item/{item_id}"}},
                      f, ensure_ascii=False, indent=2)
        print(f"{item_id} <- {source}.ase ({img.width}x{img.height} @ {cx},{cy})")


if __name__ == "__main__":
    main()
