#!/usr/bin/env python3
"""Generates the water trough (水槽) block assets: empty + water-filled models
from the user's 空水槽.bbmodel, plus a blue-tinted water surface texture
derived from the vanilla water_still frame."""

import base64
import io
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")
DESK = "/Users/apple/Desktop"


def element_to_mc(el, texture):
    faces = {}
    for face, data in el.get("faces", {}).items():
        if data is None or face == "down":
            continue
        entry = {"texture": texture}
        if "uv" in data:
            entry["uv"] = data["uv"]
        faces[face] = entry
    mc = {"from": list(el["from"]), "to": list(el["to"]), "shade": False, "faces": faces}
    rot = el.get("rotation")
    if rot and rot != [0, 0, 0]:
        mc["rotation"] = {"origin": el.get("origin", [8, 8, 8]), "axis": "z", "angle": rot[2]}
    return mc


def pick_axis_rotation(rot):
    """BlockBench 3-axis rotations -> one MC axis in [-45,45] (best fit)."""
    candidates = sorted(
        ((abs(rot[0]), "x", rot[0]), (abs(rot[1]), "y", rot[1]), (abs(rot[2]), "z", rot[2])),
        reverse=True)
    for _, axis, angle in candidates:
        if -45 <= angle <= 45:
            return axis, angle
    _, axis, angle = candidates[0]
    return axis, max(-45, min(45, angle))


def rotate_point(p, origin, axis, angle):
    """Rotate p around origin by angle degrees around x/y/z."""
    a = math.radians(angle)
    c = math.cos(a)
    s = math.sin(a)
    x = p[0] - origin[0]
    y = p[1] - origin[1]
    z = p[2] - origin[2]
    if axis == "x":
        return (origin[0] + x, origin[1] + y * c - z * s, origin[2] + y * s + z * c)
    if axis == "y":
        return (origin[0] + x * c + z * s, origin[1] + y, origin[2] - x * s + z * c)
    return (origin[0] + x * c - y * s, origin[1] + x * s + y * c, origin[2] + z)


def rotated_aabb(f, t, origin, axis, angle):
    pts = []
    for x in (f[0], t[0]):
        for y in (f[1], t[1]):
            for z in (f[2], t[2]):
                p = rotate_point((x, y, z), origin, axis, angle)
                pts.append(p)
    return (min(p[0] for p in pts), min(p[1] for p in pts), min(p[2] for p in pts),
            max(p[0] for p in pts), max(p[1] for p in pts), max(p[2] for p in pts))


def generate_ice_cubes(texture):
    """Scatter ~26 ice cubes of varying sizes inside the trough; every rotated
    AABB is verified to stay inside the outer faces (x 3..14, z 3..14, y 0..4)."""
    random.seed(7)
    cubes = []
    axes = ["x", "y", "z"]

    def try_add(cx, cy, cz, size, height, max_angle):
        f = [cx - size / 2.0, cy, cz - size / 2.0]
        t = [cx + size / 2.0, cy + height, cz + size / 2.0]
        origin = [(f[0] + t[0]) / 2.0, (f[1] + t[1]) / 2.0, (f[2] + t[2]) / 2.0]
        angles = sorted({0.0, random.uniform(-max_angle, max_angle)}, key=abs, reverse=True)
        for angle in angles:
            axis = random.choice(axes)
            bb = rotated_aabb(f, t, origin, axis, angle)
            if bb[0] >= 3.1 and bb[3] <= 13.9 and bb[2] >= 3.1 and bb[5] <= 13.9 \
                    and bb[1] >= 1.0 and bb[4] <= 4.0:
                mc = {"from": [round(v, 3) for v in f], "to": [round(v, 3) for v in t],
                      "shade": False,
                      "faces": {face: {"texture": texture, "uv": [0, 0, 16, 16]}
                                for face in ("north", "south", "east", "west", "up", "down")}}
                if angle != 0:
                    mc["rotation"] = {"origin": [round(v, 3) for v in origin],
                                      "axis": axis, "angle": round(angle, 1)}
                cubes.append(mc)
                return True
        return False

    # Bottom layer: a 3x3 grid of big chunks with jitter.
    for i in range(3):
        for j in range(3):
            cx = 5.3 + i * 3.2 + random.uniform(-0.35, 0.35)
            cz = 5.3 + j * 3.2 + random.uniform(-0.35, 0.35)
            try_add(cx, 1.2, cz, random.uniform(2.6, 3.0), random.uniform(1.8, 2.2), 10)

    # Extra bottom chunks in the gaps between the big grid cells.
    for _ in range(6):
        cx = random.uniform(5.0, 12.0)
        cz = random.uniform(5.0, 12.0)
        try_add(cx, 1.15, cz, random.uniform(1.8, 2.4), random.uniform(1.4, 1.8), 15)

    # Upper layer: medium cubes filling gaps (keep tops under the rim).
    for _ in range(10):
        cx = random.uniform(5.2, 11.8)
        cz = random.uniform(5.2, 11.8)
        try_add(cx, random.uniform(2.5, 2.9), cz, random.uniform(1.4, 2.0),
                random.uniform(1.0, 1.3), 25)

    # Pile of small cubes near the top.
    for _ in range(8):
        cx = random.uniform(5.4, 11.6)
        cz = random.uniform(5.4, 11.6)
        try_add(cx, random.uniform(3.0, 3.5), cz, random.uniform(0.9, 1.5),
                random.uniform(0.6, 0.9), 30)

    return cubes


def main():
    tex_dir = os.path.join(ASSETS, "textures", "block")
    model_dir = os.path.join(ASSETS, "models", "block")
    state_dir = os.path.join(ASSETS, "blockstates")
    for d in (tex_dir, model_dir, state_dir):
        os.makedirs(d, exist_ok=True)

    bb = json.load(open(os.path.join(DESK, "空水槽.bbmodel"), encoding="utf-8"))
    src = bb["textures"][1]["source"]
    trough_tex = Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA")
    trough_tex.save(os.path.join(tex_dir, "water_trough.png"))

    ice_bb = json.load(open(os.path.join(DESK, "装满冰块的水槽.bbmodel"), encoding="utf-8"))
    ice_src = ice_bb["textures"][2]["source"]
    Image.open(io.BytesIO(base64.b64decode(ice_src.split(",", 1)[1]))).convert("RGBA") \
        .save(os.path.join(tex_dir, "water_trough_ice.png"))

    # Blue-tinted water surface from the first vanilla water_still frame.
    water = Image.open(os.path.join(os.path.dirname(__file__), "water_still.png")).convert("RGBA")
    frame = water.crop((0, 0, 16, 16))
    px = frame.load()
    # Vanilla water colour (~ #3F76E4).
    tint = (0x3F / 255.0, 0x76 / 255.0, 0xE4 / 255.0)
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            px[x, y] = (int(r * tint[0]), int(g * tint[1]), int(b * tint[2]), a)
    frame.save(os.path.join(tex_dir, "water_trough_water.png"))

    trough = [element_to_mc(el, "water_trough") for el in bb["elements"]]
    water_box = {"from": [4.05, 1.05, 4.05], "to": [12.95, 3.85, 12.95], "shade": False,
                 "faces": {f: {"texture": "water_trough_water"}
                           for f in ("north", "south", "east", "west", "up", "down")}}
    ice_cubes = generate_ice_cubes("water_trough_ice")

    def model(elements):
        return {"ambientocclusion": False, "particle": "water_trough",
                "display": {"gui": {"rotation": [30, 45, 0], "translation": [0, 0, 0],
                                    "scale": [0.625, 0.625, 0.625]},
                            "fixed": {"rotation": [30, 45, 0], "translation": [0, 0, 0],
                                      "scale": [0.625, 0.625, 0.625]}},
                "textures": {"water_trough": "chemistry:block/water_trough",
                             "water_trough_water": "chemistry:block/water_trough_water",
                             "water_trough_ice": "chemistry:block/water_trough_ice",
                             "glass_tube": "chemistry:block/glass_tube"},
                "elements": elements}

    with open(os.path.join(model_dir, "water_trough.json"), "w", encoding="utf-8") as f:
        json.dump(model(trough), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "water_trough_water.json"), "w", encoding="utf-8") as f:
        json.dump(model(trough + [water_box]), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "water_trough_ice.json"), "w", encoding="utf-8") as f:
        json.dump(model(trough + ice_cubes), f, ensure_ascii=False, indent=2)

    # 45-degree gas nozzle standing in the water, one model per direction:
    # a single straight tube tilted 45 degrees upward.
    dirs = {
        "n": ("x", -45),
        "s": ("x", 45),
        "e": ("z", -45),
        "w": ("z", 45),
    }
    for suffix, (axis, angle) in dirs.items():
        nozzle = {"from": [7.9, 2.5, 7.9], "to": [8.1, 5.5, 8.1], "shade": False,
               "rotation": {"origin": [8.0, 2.5, 8.0], "axis": axis, "angle": angle},
               "faces": {f: {"texture": "glass_tube", "uv": [0, 4, 3, 10]}
                         for f in ("north", "south", "east", "west", "up", "down")}}
        with open(os.path.join(model_dir, f"water_trough_water_nozzle_{suffix}.json"),
                  "w", encoding="utf-8") as f:
            json.dump(model(trough + [water_box, nozzle]), f, ensure_ascii=False, indent=2)

    variants = {}
    for fill, base in (("empty", "water_trough"), ("water", "water_trough_water"),
                       ("ice", "water_trough_ice")):
        for nozzle in ("none", "north", "south", "east", "west"):
            if fill == "water" and nozzle != "none":
                model_name = f"water_trough_water_nozzle_{nozzle[0]}"
            else:
                model_name = base
            variants[f"filled={fill},nozzle={nozzle}"] = {"model": f"chemistry:block/{model_name}"}
    with open(os.path.join(state_dir, "water_trough.json"), "w", encoding="utf-8") as f:
        json.dump({"variants": variants}, f, ensure_ascii=False, indent=2)

    print("water trough assets generated")


if __name__ == "__main__":
    main()
