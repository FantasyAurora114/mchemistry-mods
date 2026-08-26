#!/usr/bin/env python3
"""Converts the user's BlockBench test-tube models into a placed-block model
(empty + filled-with-tinted-contents) and extracts the tube texture."""

import base64
import io
import json
import os
from PIL import Image

import stopper_common

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")

EMPTY_BB = "/Users/apple/Desktop/试管.bbmodel"
FILLED_BB = "/Users/apple/Desktop/装满物品的试管.bbmodel"

SHIFT = 5  # centre the 5-wide tube in the 16-wide block


def load_bb(path):
    return json.load(open(path, encoding="utf-8"))


def element_to_mc(el, texture, tint=False):
    f = el["from"]
    t = el["to"]
    faces = {}
    for face, data in el.get("faces", {}).items():
        if face == "down" or data is None:
            continue
        entry = {"texture": texture}
        if "uv" in data:
            entry["uv"] = data["uv"]
        if tint:
            entry["tintindex"] = 0
        faces[face] = entry
    return {
        "from": [f[0] + SHIFT, f[1], f[2] + SHIFT],
        "to": [t[0] + SHIFT, t[1], t[2] + SHIFT],
        "faces": faces,
    }


def extract_texture(bb):
    src = bb["textures"][0]["source"]
    png = base64.b64decode(src.split(",", 1)[1])
    return Image.open(io.BytesIO(png)).convert("RGBA")


def main():
    empty = load_bb(EMPTY_BB)
    filled = load_bb(FILLED_BB)

    tex_dir = os.path.join(ASSETS, "textures", "block")
    model_dir = os.path.join(ASSETS, "models", "block")
    state_dir = os.path.join(ASSETS, "blockstates")
    os.makedirs(tex_dir, exist_ok=True)
    os.makedirs(model_dir, exist_ok=True)
    os.makedirs(state_dir, exist_ok=True)

    extract_texture(empty).save(os.path.join(tex_dir, "test_tube.png"))
    stopper_common.rubber_texture().save(os.path.join(tex_dir, "rubber_stopper.png"))
    # White contents layer; the box samples the [3,3]-[6,7] region and is tinted.
    Image.new("RGBA", (16, 16), (255, 255, 255, 255)).save(
        os.path.join(tex_dir, "test_tube_contents.png"))

    empty_els = [element_to_mc(e, "test_tube") for e in empty["elements"]]
    filled_els = [element_to_mc(e, "test_tube") for e in filled["elements"][:-1]]
    # Last element of the filled model is the contents box -> tinted layer.
    filled_els.append(element_to_mc(filled["elements"][-1], "contents", tint=True))

    # Rubber stopper on the tube mouth (rim at y=15): narrow end into the
    # opening, wide flange on top.
    stopper = stopper_common.stopper_elements(
        [6.5, 15, 6.5], [8.5, 15.667, 8.5],
        [6, 15.667, 6], [9, 16, 9])

    def model(elements):
        return {
            "ambientocclusion": False,
            "particle": "test_tube",
            "textures": {
                "test_tube": "chemistry:block/test_tube",
                "contents": "chemistry:block/test_tube_contents",
                "stopper": "chemistry:block/rubber_stopper",
            },
            "elements": elements,
        }

    with open(os.path.join(model_dir, "placed_test_tube.json"), "w", encoding="utf-8") as f:
        json.dump(model(empty_els), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "placed_test_tube_filled.json"), "w", encoding="utf-8") as f:
        json.dump(model(filled_els), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "placed_test_tube_stoppered.json"), "w", encoding="utf-8") as f:
        json.dump(model(empty_els + stopper), f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "placed_test_tube_stoppered_filled.json"), "w", encoding="utf-8") as f:
        json.dump(model(filled_els + stopper), f, ensure_ascii=False, indent=2)

    with open(os.path.join(state_dir, "placed_test_tube.json"), "w", encoding="utf-8") as f:
        json.dump({
            "variants": {
                "filled=false,stoppered=false": {"model": "chemistry:block/placed_test_tube"},
                "filled=true,stoppered=false": {"model": "chemistry:block/placed_test_tube_filled"},
                "filled=false,stoppered=true": {"model": "chemistry:block/placed_test_tube_stoppered"},
                "filled=true,stoppered=true": {"model": "chemistry:block/placed_test_tube_stoppered_filled"},
            }
        }, f, ensure_ascii=False, indent=2)

    print("placed test tube assets generated")


if __name__ == "__main__":
    main()
