#!/usr/bin/env python3
"""Builds assets for the NB-lab vessels and distillation parts:

* Standalone models (used by the iron stand / tripod renderers) for the
  round-bottom flask, Erlenmeyer flask, crucible, evaporating dish, straight
  condenser, receiver adapters and thermometer, from the user's BlockBench
  models.
* Tinted contents-box models for the four vessels (drawn by the renderers).
* 16x16 item icons (hand-drawn pixel art) + dynamic "filled" icons for the
  vessels, so the item shows its liquid colour.
"""

import base64
import io
import itertools
import json
import math
import os
import struct
import zlib

from PIL import Image

import stopper_common

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")
DESK = "/Users/apple/Desktop"


def load_bb(name):
    return json.load(open(os.path.join(DESK, name + ".bbmodel"), encoding="utf-8"))


def extract_texture(bb, index):
    src = bb["textures"][index]["source"]
    return Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA")


def _rotate(p, origin, angles):
    x, y, z = p
    ox, oy, oz = origin
    for axis, deg in (("x", angles[0]), ("y", angles[1]), ("z", angles[2])):
        if deg == 0:
            continue
        a = math.radians(deg)
        dx, dy, dz = x - ox, y - oy, z - oz
        if axis == "x":
            ny = dy * math.cos(a) - dz * math.sin(a)
            nz = dy * math.sin(a) + dz * math.cos(a)
            x, y, z = ox + dx, oy + ny, oz + nz
        elif axis == "y":
            nx = dx * math.cos(a) + dz * math.sin(a)
            nz = -dx * math.sin(a) + dz * math.cos(a)
            x, y, z = ox + nx, oy + dy, oz + nz
        else:
            nx = dx * math.cos(a) - dy * math.sin(a)
            ny = dx * math.sin(a) + dy * math.cos(a)
            x, y, z = ox + nx, oy + ny, oz + dz
    return (x, y, z)


def element_to_mc(el, texture):
    faces = {}
    for face, data in el.get("faces", {}).items():
        if data is None:
            continue
        entry = {"texture": texture}
        if "uv" in data:
            uv = list(data["uv"])
            # The flask necks use a mostly-transparent texture region on the
            # sides; remap them to the visible glass band like the other parts.
            if face in ("north", "south", "east", "west") and uv == [10, 7, 16, 15]:
                uv = [10, 3, 16, 7]
            entry["uv"] = uv
        faces[face] = entry
    rot = el.get("rotation")
    if rot and any(rot):
        origin = el.get("origin", [0, 0, 0])
        corners = list(itertools.product((el["from"][0], el["to"][0]),
                                         (el["from"][1], el["to"][1]),
                                         (el["from"][2], el["to"][2])))
        rotated = [_rotate(c, origin, rot) for c in corners]
        fr = [min(c[i] for c in rotated) for i in range(3)]
        to = [max(c[i] for c in rotated) for i in range(3)]
        return {"from": [round(v, 3) for v in fr], "to": [round(v, 3) for v in to],
                "shade": False, "faces": faces}
    return {"from": list(el["from"]), "to": list(el["to"]), "shade": False, "faces": faces}


def standalone(elements, textures, particle=None):
    model = {"ambientocclusion": False, "textures": textures, "elements": elements}
    if particle:
        model["particle"] = particle
    return model


def parse_ase(path):
    data = open(path, "rb").read()
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
                            px[xx, yy] = tuple(raw[(yy * w + xx) * 4 : (yy * w + xx) * 4 + 4])
                    if best is None or (w * h) > (best[0].size[0] * best[0].size[1]):
                        best = (img, cx, cy)
            pos = body + csize - 6
        pos = fstart + fsize
    if best is None:
        raise SystemExit(f"no cel found in {path}")
    return best


def pixel_icon(rows, colors):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colors:
                img.putpixel((x, y), colors[ch])
    return img


def write_item(item_id, base_icon, contents_icon=None):
    item_tex = os.path.join(ASSETS, "textures", "item")
    item_model = os.path.join(ASSETS, "models", "item")
    items_dir = os.path.join(ASSETS, "items")
    for d in (item_tex, item_model, items_dir):
        os.makedirs(d, exist_ok=True)
    base_icon.save(os.path.join(item_tex, item_id + ".png"))
    if contents_icon is not None:
        contents_icon.save(os.path.join(item_tex, item_id + "_contents.png"))
        with open(os.path.join(item_model, item_id + "_contents.json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": f"chemistry:item/{item_id}_contents"}},
                      f, ensure_ascii=False, indent=2)
        registry = {
            "model": {
                "type": "minecraft:select",
                "property": "minecraft:custom_model_data",
                "cases": [
                    {
                        "when": "filled",
                        "model": {
                            "type": "minecraft:composite",
                            "models": [
                                {"type": "minecraft:model", "model": f"chemistry:item/{item_id}"},
                                {"type": "minecraft:model", "model": f"chemistry:item/{item_id}_contents",
                                 "tints": [{"type": "minecraft:custom_model_data", "index": 0,
                                            "default": 16777215}]},
                            ],
                        },
                    }
                ],
                "fallback": {"type": "minecraft:model", "model": f"chemistry:item/{item_id}"},
            }
        }
    else:
        registry = {"model": {"type": "minecraft:model", "model": f"chemistry:item/{item_id}"}}
    with open(os.path.join(item_model, item_id + ".json"), "w", encoding="utf-8") as f:
        json.dump({"parent": "minecraft:item/generated",
                   "textures": {"layer0": f"chemistry:item/{item_id}"}},
                  f, ensure_ascii=False, indent=2)
    with open(os.path.join(items_dir, item_id + ".json"), "w", encoding="utf-8") as f:
        json.dump(registry, f, ensure_ascii=False, indent=2)
    base_icon.resize((64, 64), Image.NEAREST).save(
        os.path.join(DESK, item_id + "_icon_preview.png"))


GLASS = {"1": (0xD4, 0xE5, 0xF7, 255), "2": (0xB3, 0xCF, 0xEC, 255),
         "3": (0x8B, 0xAD, 0xD0, 255), "R": (0xD2, 0x4A, 0x43, 255),
         "m": (0x9A, 0x9A, 0x9A, 255)}


def main():
    tex_dir = os.path.join(ASSETS, "textures", "block")
    model_dir = os.path.join(ASSETS, "models", "block")
    for d in (tex_dir, model_dir):
        os.makedirs(d, exist_ok=True)

    # Reuse the funnel glass texture for all new glassware.
    texture = "chemistry:block/funnel"

    # Standalone models from the user's BlockBench files.
    def bb_model(name, tex=texture):
        bb = load_bb(name)
        els = [element_to_mc(el, "funnel") for el in bb["elements"]]
        return standalone(els, {"funnel": texture}, particle=texture)

    vessels = {
        "round_bottom_flask": "圆底烧瓶",
        "erlenmeyer_flask": "锥形瓶",
        "crucible": "坩埚",
        "evaporating_dish": "蒸发皿",
    }
    for item_id, src in vessels.items():
        with open(os.path.join(model_dir, item_id + ".json"), "w", encoding="utf-8") as f:
            json.dump(bb_model(src), f, ensure_ascii=False, indent=2)

    with open(os.path.join(model_dir, "straight_condenser.json"), "w", encoding="utf-8") as f:
        json.dump(bb_model("直型冷凝器"), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "receiver_adapter_straight.json"), "w", encoding="utf-8") as f:
        json.dump(bb_model("直牛角管"), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "receiver_adapter_bent.json"), "w", encoding="utf-8") as f:
        json.dump(bb_model("弯牛角管"), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "thermometer.json"), "w", encoding="utf-8") as f:
        json.dump(bb_model("温度计"), f, ensure_ascii=False, indent=2)

    # Rubber stopper that sits on a flask mouth when the flask is sealed:
    # the same tapered shape (narrow end into the mouth, wide flange on the
    # rim) as the test-tube stoppers.
    vessel_stopper = standalone(
        stopper_common.stopper_elements(
            [-0.55, -0.9, -0.55], [0.55, 0, 0.55],   # narrow end into the mouth
            [-0.8, 0, -0.8], [0.8, 0.7, 0.8]),       # wide flange on the rim
        {"stopper": "chemistry:block/rubber_stopper"})
    with open(os.path.join(model_dir, "vessel_stopper.json"), "w", encoding="utf-8") as f:
        json.dump(vessel_stopper, f, ensure_ascii=False, indent=2)

    # The invisible ground-placed vessel block still needs a blockstate variant.
    state_dir = os.path.join(ASSETS, "blockstates")
    os.makedirs(state_dir, exist_ok=True)
    with open(os.path.join(state_dir, "placed_vessel.json"), "w", encoding="utf-8") as f:
        json.dump({"variants": {"": {"model": "chemistry:block/erlenmeyer_flask"}}},
                  f, ensure_ascii=False, indent=2)

    # Tinted contents boxes for the four vessels (drawn by the renderers).
    contents = {
        "round_bottom_flask": ([6.4, 1.2, 6.4], [10.6, 3.6, 10.6]),
        "erlenmeyer_flask": ([6.4, 0.2, 6.4], [10.6, 3.8, 10.6]),
        "crucible": ([6.3, 0.2, 7.3], [8.7, 5.2, 9.7]),
        "evaporating_dish": ([6.3, 0.2, 7.3], [8.7, 1.5, 9.7]),
    }
    contents_tex = os.path.join(tex_dir, "vessel_contents.png")
    Image.new("RGBA", (16, 16), (255, 255, 255, 255)).save(contents_tex)
    for item_id, (fr, to) in contents.items():
        model = {
            "ambientocclusion": False,
            "textures": {"contents": "chemistry:block/vessel_contents"},
            "elements": [{
                "from": list(fr), "to": list(to), "shade": False,
                "faces": {f: {"texture": "contents", "tintindex": 0}
                          for f in ("north", "south", "east", "west", "up", "down")},
            }],
        }
        with open(os.path.join(model_dir, item_id + "_contents.json"), "w", encoding="utf-8") as f:
            json.dump(model, f, ensure_ascii=False, indent=2)

    # Item icons: hand-drawn pixel art.
    ROUND_BOTTOM_FLASK = [
        ".......##.......",
        ".......##.......",
        ".......##.......",
        "......2332......",
        ".....233132.....",
        "....23311332....",
        "....23311332....",
        "....23311332....",
        "....23311332....",
        ".....233132.....",
        ".....233132.....",
        "......2332......",
        ".......22.......",
        "................",
        "................",
        "................",
    ]
    ERLENMEYER = [
        "................",
        "......2332......",
        "......2332......",
        ".....233132.....",
        ".....233132.....",
        "....23311332....",
        "....23311332....",
        "...233111332....",
        "...233111332....",
        "..2333111332....",
        "..2331111332....",
        "..2333111332....",
        "...23311332.....",
        "....233332......",
        "................",
        "................",
    ]
    CRUCIBLE = [
        "................",
        "................",
        "................",
        "..2333333332....",
        "..2333333332....",
        "...23333332.....",
        "...23333332.....",
        "....233332......",
        "....233332......",
        "....233332......",
        ".....2332.......",
        ".....2332.......",
        "................",
        "................",
        "................",
        "................",
    ]
    EVAPORATING_DISH = [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".2333333333332..",
        ".2333333333332..",
        "..23333333332...",
        "..23333333332...",
        "...233333332....",
        "....2333332.....",
        "................",
        "................",
    ]
    CONDENSER = [
        ".....2332.......",
        ".....2332.......",
        ".....2332.......",
        "....233332......",
        "....233332......",
        "....233332......",
        "....233332......",
        "....233332......",
        "....233332......",
        "....233332......",
        "....233332......",
        "....233332......",
        ".....2332.......",
        ".....2332.......",
        ".....2332.......",
        ".....2332.......",
    ]
    RECEIVER_STRAIGHT = [
        "................",
        ".....2332.......",
        ".....2332.......",
        ".....2332.......",
        "....233332......",
        "....233332......",
        "....233332......",
        "....233332......",
        "....233332......",
        "....233332......",
        ".....2332.......",
        ".....2332.......",
        ".....2332.......",
        "................",
        "................",
        "................",
    ]
    RECEIVER_BENT = [
        "................",
        ".....2332.......",
        ".....2332.......",
        "....233332......",
        "....233332......",
        "....233332......",
        "....233332......",
        "....233332......",
        "...2333332......",
        "..233333332.....",
        ".23333333332....",
        "..233333332.....",
        "...2333332......",
        "................",
        "................",
        "................",
    ]
    THERMOMETER = [
        "................",
        ".....RRRR.......",
        ".....RRRR.......",
        ".....RRRR.......",
        "......RR........",
        "......RR........",
        "......RR........",
        "......RR........",
        "......RR........",
        "......RR........",
        "......RR........",
        "......RR........",
        "......RR........",
        "......RR........",
        "......RR........",
        "................",
    ]

    contents_fills = {
        "round_bottom_flask": [
            "................",
            "................",
            "................",
            "................",
            "................",
            "....hhhhhhhh....",
            "....hhhhhhhh....",
            "....hhhhhhhh....",
            "....hhhhhhhh....",
            ".....hhhhhh.....",
            ".....hhhhhh.....",
            "......hhhh......",
            "................",
            "................",
            "................",
            "................",
        ],
        "erlenmeyer_flask": [
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "....hhhhhhhh....",
            "....hhhhhhhh....",
            "...hhhhhhhhhh...",
            "...hhhhhhhhhh...",
            "..hhhhhhhhhhhh..",
            "..hhhhhhhhhhhh..",
            "..hhhhhhhhhhhh..",
            "...hhhhhhhhhh...",
            "................",
            "................",
        ],
        "crucible": [
            "................",
            "................",
            "................",
            "..hhhhhhhhhhhh..",
            "..hhhhhhhhhhhh..",
            "...hhhhhhhhhh...",
            "...hhhhhhhhhh...",
            "....hhhhhhhh....",
            "....hhhhhhhh....",
            "....hhhhhhhh....",
            ".....hhhhhh.....",
            ".....hhhhhh.....",
            "................",
            "................",
            "................",
            "................",
        ],
        "evaporating_dish": [
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            ".hhhhhhhhhhhhhh.",
            ".hhhhhhhhhhhhhh.",
            "..hhhhhhhhhhhh..",
            "..hhhhhhhhhhhh..",
            "...hhhhhhhhhh...",
            "....hhhhhhhh....",
            "................",
            "................",
        ],
    }

    for item_id, rows in {
        "round_bottom_flask": ROUND_BOTTOM_FLASK,
        "erlenmeyer_flask": ERLENMEYER,
        "crucible": CRUCIBLE,
        "evaporating_dish": EVAPORATING_DISH,
        "straight_condenser": CONDENSER,
        "receiver_adapter_straight": RECEIVER_STRAIGHT,
        "receiver_adapter_bent": RECEIVER_BENT,
        "thermometer": THERMOMETER,
    }.items():
        icon = pixel_icon(rows, GLASS)
        fill = pixel_icon(contents_fills.get(item_id, []), {"h": (255, 255, 255, 255)})
        write_item(item_id, icon, fill if item_id in contents_fills else None)

    # Crucible tongs icon comes from the user's Aseprite texture.
    img, cx, cy = parse_ase(os.path.join(DESK, "坩埚钳.ase"))
    canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    canvas.alpha_composite(img, (cx, cy))
    write_item("crucible_tongs", canvas)

    print("nb vessel assets generated")


if __name__ == "__main__":
    main()
