#!/usr/bin/env python3
"""Builds the iron stand (铁架台) block models from the user's eight direction
files. The base + clamp stay fixed (the clamp only slides down when the tube
mouth points lower-left / lower-right); the tube comes from each direction
file, with 90-degree steps baked into the geometry and the remaining <=45
degrees kept as an MC element rotation (MC limit). Three states per direction:
no tube / empty tube / filled tube."""

import base64
import io
import json
import os
from PIL import Image

import stopper_common

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")
DESK = "/Users/apple/Desktop"

DIRECTIONS = {
    0: "带有试管的铁架台",
    1: "带有试管的铁架台45",
    2: "带有试管的铁架台90",
    3: "带有试管的铁架台135",
    4: "带有试管的铁架台180",
    5: "带有试管的铁架台-135",
    6: "带有试管的铁架台-90",
    7: "带有试管的铁架台-45",
}

CLAMP_DROP_ROTS = {3, 5}  # mouth points lower-left / lower-right
CLAMP_DROP = 2.0
CONTENTS_FROM = [8.3333, 7.3333, 8.3333]
CONTENTS_TO = [9.3333, 8.6667, 9.3333]


def load_bb(name):
    return json.load(open(os.path.join(DESK, name + ".bbmodel"), encoding="utf-8"))


def extract_texture(bb, index):
    src = bb["textures"][index]["source"]
    return Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA")


def faces_to_mc(faces, texture):
    out = {}
    for face, data in faces.items():
        if face == "down" or data is None:
            continue
        entry = {"texture": texture}
        if "uv" in data:
            entry["uv"] = data["uv"]
        out[face] = entry
    return out


def bake(from_, to, origin, k):
    ox, oy = origin[0], origin[1]
    pts = [(from_[0], from_[1]), (to[0], from_[1]), (from_[0], to[1]), (to[0], to[1])]
    for _ in range(k % 4):
        pts = [(ox - (y - oy), oy + (x - ox)) for x, y in pts]
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    # Rotation happens in the x-y plane; z stays. Keep [x, y, z] order.
    return ([round(min(xs), 3), round(min(ys), 3), from_[2]],
            [round(max(xs), 3), round(max(ys), 3), to[2]])


def decompose(angle):
    a = angle % 360
    k = int(a // 90)
    return k, a - k * 90


def element_to_mc(el, texture, shade):
    f, t = el["from"], el["to"]
    rot = el.get("rotation")
    mc = {"from": list(f), "to": list(t), "shade": shade,
          "faces": faces_to_mc(el.get("faces", {}), texture)}
    if rot and rot != [0, 0, 0]:
        origin = el.get("origin", [8.5, 9.5, 10])
        k, delta = decompose(rot[2])
        f, t = bake(f, t, origin, k)
        mc["from"], mc["to"] = f, t
        if delta != 0:
            mc["rotation"] = {"origin": list(origin), "axis": "z", "angle": delta}
    return mc


def main():
    tex_dir = os.path.join(ASSETS, "textures", "block")
    model_dir = os.path.join(ASSETS, "models", "block")
    state_dir = os.path.join(ASSETS, "blockstates")
    for d in (tex_dir, model_dir, state_dir):
        os.makedirs(d, exist_ok=True)

    base_bb = load_bb(DIRECTIONS[0])
    extract_texture(base_bb, 0).save(os.path.join(tex_dir, "iron_stand.png"))
    extract_texture(base_bb, 1).save(os.path.join(tex_dir, "test_tube.png"))
    stopper_common.rubber_texture().save(os.path.join(tex_dir, "rubber_stopper.png"))
    Image.new("RGBA", (16, 16), (255, 255, 255, 255)).save(
        os.path.join(tex_dir, "test_tube_contents.png"))

    # Glass delivery tubes (straight + 90 deg) that insert into the rubber
    # stopper on the stand. The texture and UVs come from the user's model; the
    # 90-degree bend is pre-baked (element rotations are limited to +/-45).
    glass_bb = load_bb("直玻璃管")
    extract_texture(glass_bb, 0).save(os.path.join(tex_dir, "glass_tube.png"))

    SIDE_UV = [0, 4, 3, 10]
    END_UV = [3, 4, 6, 7]

    def tube_model(elements):
        return {"ambientocclusion": False, "textures": {"glass_tube": "chemistry:block/glass_tube"},
                "elements": elements}

    def glass_box(f, t):
        return {"from": list(f), "to": list(t), "shade": False,
                "faces": {"north": {"texture": "glass_tube", "uv": SIDE_UV},
                          "south": {"texture": "glass_tube", "uv": SIDE_UV},
                          "east": {"texture": "glass_tube", "uv": SIDE_UV},
                          "west": {"texture": "glass_tube", "uv": SIDE_UV},
                          "up": {"texture": "glass_tube", "uv": END_UV},
                          "down": {"texture": "glass_tube", "uv": END_UV}}}

    w = 0.4  # tube cross-section (thinner so 2-hole instruments don't overlap)
    half = w / 2.0
    straight = [glass_box([-half, 0, -half], [half, 4, half])]
    # Right-angle: the stub goes INTO the mouth (-Y) and sticks out a little
    # above the stopper (+Y); the delivery arm runs along -X, which the
    # renderer's Z-rotation maps to the downward perpendicular.
    right_angle = [glass_box([-half, -2.0, -half], [half, 1.0, half]),
                   glass_box([-2.5, 1.0, -half], [0, 1.6, half])]
    with open(os.path.join(model_dir, "glass_tube_straight.json"), "w", encoding="utf-8") as f:
        json.dump(tube_model(straight), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "glass_tube_right_angle.json"), "w", encoding="utf-8") as f:
        json.dump(tube_model(right_angle), f, ensure_ascii=False, indent=2)

    # Inserted dropper from the user's model: glass tip + body + red bulb.
    # Centred on the tube axis (x 6.5, z 7.5 in the source).
    bulb_tex = Image.new("RGBA", (16, 16), (0xD2, 0x4A, 0x43, 255))
    bp = bulb_tex.load()
    for y in range(16):
        for x in range(16):
            if x < 3:
                bp[x, y] = (0xF0, 0x7A, 0x6A, 255)
            elif x > 12:
                bp[x, y] = (0xA0, 0x30, 0x28, 255)
    bulb_tex.save(os.path.join(tex_dir, "dropper_bulb.png"))

    def centered_glass(f, t):
        return {"from": [f[0] - 6.5, f[1], f[2] - 7.5],
                "to": [t[0] - 6.5, t[1], t[2] - 7.5],
                "shade": False,
                "faces": {"north": {"texture": "glass_tube", "uv": SIDE_UV},
                          "south": {"texture": "glass_tube", "uv": SIDE_UV},
                          "east": {"texture": "glass_tube", "uv": SIDE_UV},
                          "west": {"texture": "glass_tube", "uv": SIDE_UV},
                          "up": {"texture": "glass_tube", "uv": END_UV},
                          "down": {"texture": "glass_tube", "uv": END_UV}}}

    dropper = {
        "ambientocclusion": False,
        "textures": {"glass_tube": "chemistry:block/glass_tube",
                     "dropper_bulb": "chemistry:block/dropper_bulb"},
        "elements": [
            centered_glass([6.3333, 0, 7.3333], [6.66663, 0.5, 7.66663]),
            centered_glass([6, 0.5, 7], [7, 2.5, 8]),
            {"from": [5.8 - 6.5, 2.5, 6.8 - 7.5], "to": [7.2 - 6.5, 3.3, 8.2 - 7.5],
             "shade": False,
             "faces": {face: {"texture": "dropper_bulb", "uv": [0, 0, 16, 16]}
                       for face in ("north", "south", "east", "west", "up", "down")}},
        ],
    }
    with open(os.path.join(model_dir, "glass_tube_dropper.json"), "w", encoding="utf-8") as f:
        json.dump(dropper, f, ensure_ascii=False, indent=2)

    textures = {
        "iron_stand": "chemistry:block/iron_stand",
        "test_tube": "chemistry:block/test_tube",
        "contents": "chemistry:block/test_tube_contents",
        "stopper": "chemistry:block/rubber_stopper",
    }

    variants = {}
    for r, file in DIRECTIONS.items():
        bb = load_bb(file)

        base = [element_to_mc(el, "iron_stand", True) for el in bb["elements"][:6]]
        # Clamp rotates with the tube in the direction files (both around the
        # shared pivot); with the bake order fixed it stays attached correctly.
        clamp = [element_to_mc(el, "iron_stand", True) for el in bb["elements"][11:]]
        tube = [element_to_mc(el, "test_tube", False) for el in bb["elements"][6:11]]

        # Contents box: same transform as the tube (rotation + drop).
        rot = bb["elements"][6].get("rotation")
        origin = bb["elements"][6].get("origin", [8.5, 9.5, 10])
        contents = {"from": CONTENTS_FROM, "to": CONTENTS_TO, "shade": False,
                    "faces": {f: {"texture": "contents", "tintindex": 0}
                              for f in ("north", "south", "east", "west", "up", "down")}}
        if rot and rot != [0, 0, 0]:
            k, delta = decompose(rot[2])
            contents["from"], contents["to"] = bake(CONTENTS_FROM, CONTENTS_TO, origin, k)
            if delta != 0:
                contents["rotation"] = {"origin": list(origin), "axis": "z", "angle": delta}

        # Rubber stopper on the tube mouth (rim at y=12), swinging with the tube.
        # The narrow end is inserted below the rim so it stays plugged in when
        # the tube is tilted.
        stopper = [element_to_mc(el, "stopper", False) for el in stopper_common.stopper_elements(
            [8.3333, 11.333, 8.3333], [9.3333, 12, 9.3333],
            [8, 12, 8], [9.6667, 12.6667, 9.6667],
            rotation=rot, origin=origin)]

        def model(elements):
            return {"ambientocclusion": False, "particle": "iron_stand",
                    "display": {"gui": {"rotation": [30, 45, 0], "translation": [0, 0, 0],
                                        "scale": [0.625, 0.625, 0.625]},
                                "fixed": {"rotation": [30, 45, 0], "translation": [0, 0, 0],
                                          "scale": [0.625, 0.625, 0.625]}},
                    "textures": textures, "elements": elements}

        with open(os.path.join(model_dir, f"iron_stand_{r}.json"), "w", encoding="utf-8") as f:
            json.dump(model(base + clamp), f, ensure_ascii=False, indent=2)
        with open(os.path.join(model_dir, f"iron_stand_{r}_tube.json"), "w", encoding="utf-8") as f:
            json.dump(model(base + clamp + tube), f, ensure_ascii=False, indent=2)
        with open(os.path.join(model_dir, f"iron_stand_{r}_filled.json"), "w", encoding="utf-8") as f:
            json.dump(model(base + clamp + tube + [contents]), f, ensure_ascii=False, indent=2)
        with open(os.path.join(model_dir, f"iron_stand_{r}_tube_stoppered.json"), "w", encoding="utf-8") as f:
            json.dump(model(base + clamp + tube + stopper), f, ensure_ascii=False, indent=2)
        with open(os.path.join(model_dir, f"iron_stand_{r}_filled_stoppered.json"), "w", encoding="utf-8") as f:
            json.dump(model(base + clamp + tube + [contents] + stopper), f, ensure_ascii=False, indent=2)
        # Part models for the block-entity renderer.
        with open(os.path.join(model_dir, f"iron_stand_{r}_tube_only.json"), "w", encoding="utf-8") as f:
            json.dump(model(tube), f, ensure_ascii=False, indent=2)
        with open(os.path.join(model_dir, f"iron_stand_{r}_contents.json"), "w", encoding="utf-8") as f:
            json.dump(model([contents]), f, ensure_ascii=False, indent=2)
        with open(os.path.join(model_dir, f"iron_stand_{r}_stopper.json"), "w", encoding="utf-8") as f:
            json.dump(model(stopper), f, ensure_ascii=False, indent=2)

    # The block is invisible and drawn by the renderer; every property combo
    # maps to the same placeholder model so the baker stays quiet.
    # MC orders block-state properties alphabetically when matching variant
    # keys, so emit them in the canonical order.
    # MC orders block-state properties alphabetically when matching variant
    # keys, so emit them in the canonical order.
    facings = ("north", "south", "east", "west")
    for facing in facings:
        for r in DIRECTIONS:
            for has_tube in (False, True):
                for has_contents in (False, True):
                    for has_stopper in (False, True):
                        for has_lamp in (False, True):
                            for lamp_lit in (False, True):
                                key = (f"facing={facing},has_contents={str(has_contents).lower()},"
                                       f"has_lamp={str(has_lamp).lower()},has_stopper={str(has_stopper).lower()},"
                                       f"has_tube={str(has_tube).lower()},lamp_lit={str(lamp_lit).lower()},"
                                       f"rotation={r}")
                                variants[key] = {"model": "chemistry:block/iron_stand_0"}

    with open(os.path.join(state_dir, "iron_stand.json"), "w", encoding="utf-8") as f:
        json.dump({"variants": variants}, f, ensure_ascii=False, indent=2)

    print("iron stand parts + placeholder blockstate generated:", len(variants), "variants")


if __name__ == "__main__":
    main()
