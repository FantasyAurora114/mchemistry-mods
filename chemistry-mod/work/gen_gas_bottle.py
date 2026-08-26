#!/usr/bin/env python3
"""Generates the placeable gas collecting bottle (集气瓶) block assets: plain,
glass-plate covered and small-opening models from the user's BlockBench files,
plus the 45-degree gas-nozzle-in-trough model."""

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


def element_to_mc(el, texture):
    faces = {}
    for face, data in el.get("faces", {}).items():
        if data is None:
            continue
        entry = {"texture": texture}
        if "uv" in data:
            entry["uv"] = data["uv"]
        faces[face] = entry
    return {"from": list(el["from"]), "to": list(el["to"]), "shade": False, "faces": faces}


def flip_y(elements):
    """Return a y-flipped (upside-down) copy of block elements. The whole model
    is flipped around its own vertical centre, so the bottle stands in the same
    footprint with its mouth at the bottom (no blockstate x:180, which would
    rotate around the block corner and misalign the model)."""
    # Flip around the BOTTLE's own vertical range only: protruding parts (the
    # inserted glass nozzle) must not raise the whole bottle off the ground.
    bottle = [e for e in elements
              if any(f and f.get("texture") == "gas_bottle" for f in e.get("faces", {}).values())]
    ref = bottle if bottle else elements
    ys = [e["from"][1] for e in ref] + [e["to"][1] for e in ref]
    lo, hi = min(ys), max(ys)
    span = lo + hi
    out = []
    for el in elements:
        x1, y1, z1 = el["from"]
        x2, y2, z2 = el["to"]
        new_faces = {}
        for face, data in el.get("faces", {}).items():
            if data is None:
                continue
            entry = {"texture": data["texture"]}
            uv = data.get("uv")
            if uv:
                u1, v1, u2, v2 = uv
                if face in ("up", "down"):
                    entry["uv"] = list(uv)
                else:
                    entry["uv"] = [u1, 16 - v2, u2, 16 - v1]
            if "rotation" in data:
                entry["rotation"] = data["rotation"]
            new_face = "down" if face == "up" else ("up" if face == "down" else face)
            new_faces[new_face] = entry
        out.append({"from": [x1, span - y2, z1], "to": [x2, span - y1, z2],
                    "shade": el.get("shade", False), "faces": new_faces})
    return out


def main():
    tex_dir = os.path.join(ASSETS, "textures", "block")
    model_dir = os.path.join(ASSETS, "models", "block")
    state_dir = os.path.join(ASSETS, "blockstates")
    for d in (tex_dir, model_dir, state_dir):
        os.makedirs(d, exist_ok=True)

    plain = load_bb("集气瓶")
    plated = load_bb("盖上玻璃板的集气瓶")
    opened = load_bb("开了个小口的集气瓶")
    extract_texture(plain).save(os.path.join(tex_dir, "gas_bottle.png"))

    def model(elements):
        return {"ambientocclusion": False, "particle": "gas_bottle",
                "display": {"gui": {"rotation": [30, 45, 0], "translation": [0, 0, 0],
                                    "scale": [0.625, 0.625, 0.625]},
                            "fixed": {"rotation": [30, 45, 0], "translation": [0, 0, 0],
                                      "scale": [0.625, 0.625, 0.625]}},
                "textures": {"gas_bottle": "chemistry:block/gas_bottle",
                             "glass_tube": "chemistry:block/glass_tube"},
                "elements": elements}

    plain_els = [element_to_mc(el, "gas_bottle") for el in plain["elements"]]
    plated_els = [element_to_mc(el, "gas_bottle") for el in plated["elements"]]
    opened_els = [element_to_mc(el, "gas_bottle") for el in opened["elements"]]
    # Inserted nozzle: a straight glass tube through the bottle mouth. A short
    # stub sticks out above the mouth (where the rubber tube connects); the
    # tube runs down into the bottle and ends in a pointed tip pointing
    # straight down inside the bottle.
    def tube_box(fr, to):
        return {"from": fr, "to": to, "shade": False,
                "faces": {f: {"texture": "glass_tube", "uv": [0, 4, 3, 10]}
                          for f in ("north", "south", "east", "west", "up", "down")}}
    opened_els.append(tube_box([7.6, 1.0, 7.6], [8.0, 8.5, 8.0]))       # tube through mouth into the bottle
    opened_els.append(tube_box([7.73, 0.67, 7.73], [7.87, 1.0, 7.87]))  # pointed tip, straight down

    with open(os.path.join(model_dir, "gas_bottle.json"), "w", encoding="utf-8") as f:
        json.dump(model(plain_els), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "gas_bottle_plate.json"), "w", encoding="utf-8") as f:
        json.dump(model(plated_els), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "gas_bottle_opening.json"), "w", encoding="utf-8") as f:
        json.dump(model(opened_els), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "gas_bottle_inverted.json"), "w", encoding="utf-8") as f:
        json.dump(model(flip_y(plain_els)), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "gas_bottle_plate_inverted.json"), "w", encoding="utf-8") as f:
        json.dump(model(flip_y(plated_els)), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "gas_bottle_opening_inverted.json"), "w", encoding="utf-8") as f:
        json.dump(model(flip_y(opened_els)), f, ensure_ascii=False, indent=2)

    variants = {}
    for inverted in (False, True):
        for has_plate in (False, True):
            for nozzle_in in (False, True):
                if nozzle_in:
                    base = "gas_bottle_opening"
                elif has_plate:
                    base = "gas_bottle_plate"
                else:
                    base = "gas_bottle"
                if inverted:
                    base += "_inverted"
                key = (f"has_nozzle={str(nozzle_in).lower()},has_plate={str(has_plate).lower()},"
                       f"inverted={str(inverted).lower()}")
                variants[key] = {"model": f"chemistry:block/{base}"}
    with open(os.path.join(state_dir, "gas_collecting_bottle.json"), "w", encoding="utf-8") as f:
        json.dump({"variants": variants}, f, ensure_ascii=False, indent=2)

    # 45-degree gas nozzle tilted up out of the water, plus a vertical stub.
    trough_nozzle = {
        "ambientocclusion": False, "particle": "gas_bottle",
        "textures": {"gas_bottle": "chemistry:block/gas_bottle"},
        "elements": [
            {"from": [7.9, 2.0, 7.9], "to": [8.1, 3.2, 8.1], "shade": False,
             "faces": {f: {"texture": "gas_bottle", "uv": [0, 4, 3, 10]}
                       for f in ("north", "south", "east", "west", "up", "down")}},
            {"from": [7.8, 3.2, 7.8], "to": [8.2, 5.4, 8.2], "shade": False,
             "rotation": {"origin": [8.0, 3.2, 8.0], "axis": "z", "angle": 45},
             "faces": {f: {"texture": "gas_bottle", "uv": [0, 4, 3, 10]}
                       for f in ("north", "south", "east", "west", "up", "down")}},
        ],
    }
    with open(os.path.join(model_dir, "gas_nozzle_in_trough.json"), "w", encoding="utf-8") as f:
        json.dump(trough_nozzle, f, ensure_ascii=False, indent=2)

    print("gas bottle + trough nozzle assets generated")


if __name__ == "__main__":
    main()
