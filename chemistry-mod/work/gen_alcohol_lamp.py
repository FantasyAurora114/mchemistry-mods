#!/usr/bin/env python3
"""Generates the alcohol lamp (酒精灯) block models, flame texture, item
icons, and the blockstate from the user's BlockBench models and LibreSprite
icons on the Desktop."""

import base64
import io
import json
import math
import os
import struct
import zlib

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")
DESK = "/Users/apple/Desktop"


def load_bb(name):
    return json.load(open(os.path.join(DESK, name + ".bbmodel"), encoding="utf-8"))


def extract_texture(bb, index=0):
    src = bb["textures"][index]["source"]
    return Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA")


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
                            # LibreSprite stores pixels in RGBA order.
                            r, g, b, a = raw[(yy * w + xx) * 4 : (yy * w + xx) * 4 + 4]
                            px[xx, yy] = (r, g, b, a)
                    if best is None or (w, h) > (best[0].width, best[0].height):
                        best = (img, cx, cy)
            pos += csize
        pos = fstart + fsize
    return best


def element_to_mc(el, texture):
    f = el["from"]
    t = el["to"]
    faces = {}
    for face, data in el.get("faces", {}).items():
        if data is None or face == "down":
            continue
        entry = {"texture": texture}
        if "uv" in data:
            entry["uv"] = data["uv"]
        faces[face] = entry
    mc = {"from": list(f), "to": list(t), "shade": False, "faces": faces}
    rot = el.get("rotation")
    if rot and rot != [0, 0, 0]:
        origin = el.get("origin", [8, 8, 8])
        mc["rotation"] = {"origin": list(origin), "axis": "z", "angle": rot[2]}
    return mc


def scale_coord(v, center):
    return center + (v - center) / 2.0


def scale_element(el, texture):
    """Shrink the element to half size: x/z around 8.5, y around the ground."""
    f = [scale_coord(v, 8.5) if i != 1 else v / 2.0 for i, v in enumerate(el["from"])]
    t = [scale_coord(v, 8.5) if i != 1 else v / 2.0 for i, v in enumerate(el["to"])]
    faces = {}
    for face, data in el.get("faces", {}).items():
        if data is None or face == "down":
            continue
        entry = {"texture": texture}
        if "uv" in data:
            entry["uv"] = data["uv"]
        faces[face] = entry
    mc = {"from": f, "to": t, "shade": False, "faces": faces}
    rot = el.get("rotation")
    if rot and rot != [0, 0, 0]:
        origin = el.get("origin", [8, 8, 8])
        scaled_origin = [scale_coord(origin[0], 8.5), origin[1] / 2.0, scale_coord(origin[2], 8.5)]
        mc["rotation"] = {"origin": scaled_origin, "axis": "z", "angle": rot[2]}
    return mc


def scale_flame(fr, to):
    return ([scale_coord(fr[0], 8.5), fr[1] / 2.0, scale_coord(fr[2], 8.5)],
            [scale_coord(to[0], 8.5), to[1] / 2.0, scale_coord(to[2], 8.5)])


def flame_texture():
    """Teardrop flame, tip up, orange with a yellow core, smooth alpha."""
    S = 4
    big = Image.new("RGBA", (16 * S, 16 * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    # outer teardrop
    d.ellipse([3 * S, 7 * S, 13 * S, 16 * S], fill=(255, 106, 0, 255))
    d.polygon([(8 * S, 0), (2.5 * S, 9 * S), (13.5 * S, 9 * S)], fill=(255, 106, 0, 255))
    # inner yellow core
    d.ellipse([5.5 * S, 9 * S, 10.5 * S, 15 * S], fill=(255, 224, 102, 255))
    d.polygon([(8 * S, 2 * S), (4.5 * S, 10 * S), (11.5 * S, 10 * S)], fill=(255, 224, 102, 255))
    img = big.resize((16, 16), Image.LANCZOS)
    px = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            # fade the tip and edges
            if a > 0:
                px[x, y] = (r, g, b, 255)
    return img


def main():
    tex_dir = os.path.join(ASSETS, "textures", "block")
    item_tex = os.path.join(ASSETS, "textures", "item")
    model_dir = os.path.join(ASSETS, "models", "block")
    item_model = os.path.join(ASSETS, "models", "item")
    items_dir = os.path.join(ASSETS, "items")
    state_dir = os.path.join(ASSETS, "blockstates")
    for d in (tex_dir, item_tex, model_dir, item_model, items_dir, state_dir):
        os.makedirs(d, exist_ok=True)

    unlit_bb = load_bb("酒精灯")
    capped_bb = load_bb("盖帽的酒精灯")
    extract_texture(unlit_bb).save(os.path.join(tex_dir, "alcohol_lamp.png"))
    flame_texture().save(os.path.join(tex_dir, "alcohol_lamp_flame.png"))

    def model(elements):
        return {
            "ambientocclusion": False,
            "particle": "alcohol_lamp",
            "textures": {
                "alcohol_lamp": "chemistry:block/alcohol_lamp",
                "flame": "chemistry:block/alcohol_lamp_flame",
            },
            "elements": elements,
        }

    unlit = [scale_element(el, "alcohol_lamp") for el in unlit_bb["elements"]]
    capped = [scale_element(el, "alcohol_lamp") for el in capped_bb["elements"]]
    flame = [
        {"from": scale_flame([7.95, 8, 8.45], [8.85, 11.8, 8.55])[0],
         "to": scale_flame([7.95, 8, 8.45], [8.85, 11.8, 8.55])[1],
         "shade": False,
         "faces": {"north": {"texture": "flame"}, "south": {"texture": "flame"}}},
        {"from": scale_flame([8.45, 8, 7.95], [8.55, 11.8, 8.85])[0],
         "to": scale_flame([8.45, 8, 7.95], [8.55, 11.8, 8.85])[1],
         "shade": False,
         "faces": {"east": {"texture": "flame"}, "west": {"texture": "flame"}}},
    ]
    with open(os.path.join(model_dir, "alcohol_lamp.json"), "w", encoding="utf-8") as f:
        json.dump(model(unlit), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "alcohol_lamp_lit.json"), "w", encoding="utf-8") as f:
        json.dump(model(unlit + flame), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "alcohol_lamp_capped.json"), "w", encoding="utf-8") as f:
        json.dump(model(capped), f, ensure_ascii=False, indent=2)

    variants = {}
    for lit in (False, True):
        for capped_ in (False, True):
            for stuck in (False, True):
                if capped_:
                    m = "alcohol_lamp_capped"
                elif lit:
                    m = "alcohol_lamp_lit"
                else:
                    m = "alcohol_lamp"
                key = (f"lit={str(lit).lower()},capped={str(capped_).lower()},"
                       f"stuck={str(stuck).lower()}")
                variants[key] = {"model": f"chemistry:block/{m}"}
    with open(os.path.join(state_dir, "alcohol_lamp.json"), "w", encoding="utf-8") as f:
        json.dump({"variants": variants}, f, ensure_ascii=False, indent=2)

    icons = [
        ("alcohol_lamp", "未点燃的酒精灯"),
        ("alcohol_lamp_lit", "点燃的酒精灯"),
        ("alcohol_lamp_capped", "盖帽的酒精灯"),
        ("alcohol_lamp_cap", "酒精灯帽"),
    ]
    for item_id, source in icons:
        parsed = parse_ase(os.path.join(DESK, source + ".ase"))
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
    print("alcohol lamp assets generated")


if __name__ == "__main__":
    main()
