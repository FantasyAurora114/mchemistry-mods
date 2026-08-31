#!/usr/bin/env python3
"""Generates the Erlenmeyer flask (锥形瓶) models from the user's new
BlockBench models on the Desktop: 空锥形瓶new (empty) and 满锥形瓶new (full).

MC block models only support axis-aligned boxes, so the tapered cone body and
the liquid frustum are approximated as stacked boxes. The models are scaled so
the neck top lands at model-y 9.0 (the same as the old model), keeping the
stopper / tube anchors / condenser aligned. Textures: 侧面.png (flat glass
blue, downscaled), 底部侧边/侧边/new1 pass through, chunbai (white liquid)
becomes the tinted contents layer."""

import base64
import io
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "mchemistry")
DESK = "/Users/apple/Desktop"
NS = "mchemistry"

Y_SCALE = 9.0 / 10.5  # new model mouth at 10.5 -> 9.0
SLICES = 8


def load_bb(name):
    return json.load(open(os.path.join(DESK, name + ".bbmodel"), encoding="utf-8"))


def texture(bb, index):
    src = bb["textures"][index]["source"]
    return Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA")


def sy(v):
    return round(v * Y_SCALE, 4)


def box_el(fr, to, faces):
    return {"from": fr, "to": to, "shade": False, "faces": faces}


def side_faces(tex_key, uv=(0, 0, 16, 16)):
    return {f: {"texture": tex_key, "uv": list(uv)} for f in
            ("north", "south", "east", "west")}


def box_element_uvs(el, tex_scale):
    """Convert a BlockBench box element (centred at 0) to MC coords centred at
    8.5, with the per-face UVs converted from BlockBench pixel coordinates to
    the MC 0..16 grid (scale = 16 / textureSize)."""
    fr = el["from"]
    to = el["to"]
    mc_from = [fr[0] + 8.5, sy(fr[1]), fr[2] + 8.5]
    mc_to = [to[0] + 8.5, sy(to[1]), to[2] + 8.5]
    faces = {}
    for face, data in el.get("faces", {}).items():
        if data is None:
            continue
        entry = {"texture": data["texture"]}
        if "uv" in data:
            s = tex_scale.get(data["texture"], 1.0)
            entry["uv"] = [v * s for v in data["uv"]]
        faces[face] = entry
    return {"from": mc_from, "to": mc_to, "shade": False, "faces": faces}


def slices(y0, y1, w0, w1, tex_key, top_face=False):
    """Stacked boxes approximating a linearly tapering frustum centred at 8.5."""
    els = []
    for i in range(SLICES):
        t = i / SLICES
        t1 = (i + 1) / SLICES
        a = sy(y0 + (y1 - y0) * t)
        b = sy(y0 + (y1 - y0) * t1)
        wa = w0 + (w1 - w0) * t
        wb = w0 + (w1 - w0) * t1
        # Average width of the slice keeps the silhouette smooth.
        w = (wa + wb) / 2.0
        half = w / 2.0
        faces = side_faces(tex_key)
        if top_face and i == SLICES - 1:
            faces["up"] = {"texture": tex_key, "uv": [0, 0, 16, 16]}
        els.append(box_el(
            [8.5 - half, a, 8.5 - half],
            [8.5 + half, b, 8.5 + half],
            faces))
    return els


def main():
    tex_dir = os.path.join(ASSETS, "textures", "block")
    model_dir = os.path.join(ASSETS, "models", "block")
    os.makedirs(tex_dir, exist_ok=True)
    os.makedirs(model_dir, exist_ok=True)

    empty = load_bb("空锥形瓶new")
    full = load_bb("满锥形瓶new")

    # ---- textures ----
    # Solid glass blue used for the four smooth corner lines.
    glass = Image.new("RGBA", (16, 16), (93, 143, 194, 255))
    gpx = glass.load()
    for y in range(4):
        for x in range(16):
            gpx[x, y] = (179, 207, 236, 255)
    glass.save(os.path.join(tex_dir, "erlenmeyer_glass.png"))
    texture(empty, 4).save(os.path.join(tex_dir, "erlenmeyer_bottom.png"))
    texture(empty, 5).save(os.path.join(tex_dir, "erlenmeyer_side.png"))
    texture(empty, 2).save(os.path.join(tex_dir, "erlenmeyer_top.png"))

    def el_tex(name):
        return {"erlenmeyer_glass": f"{NS}:block/erlenmeyer_glass",
                "erlenmeyer_bottom": f"{NS}:block/erlenmeyer_bottom",
                "erlenmeyer_side": f"{NS}:block/erlenmeyer_side",
                "erlenmeyer_top": f"{NS}:block/erlenmeyer_top",
                "contents": f"{NS}:block/vessel_contents"}[name]

    def write_model(name, textures, elements):
        data = {"ambientocclusion": False, "particle": "erlenmeyer_glass",
                "textures": textures, "elements": elements}
        with open(os.path.join(model_dir, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    # ---- empty flask model ----
    tex_scale = {2: 1.0, 4: 1.0, 5: 16.0 / 32.0}  # 侧边.png is 32x32
    empty_elements = []
    for el in empty["elements"]:
        if el.get("from") is None:
            continue  # 瓶身 mesh -> approximated below
        mc = box_element_uvs(el, tex_scale)
        # remap texture indices to named keys
        for face, data in mc["faces"].items():
            t = data["texture"]
            data["texture"] = {2: "erlenmeyer_top", 4: "erlenmeyer_bottom",
                               5: "erlenmeyer_side"}.get(t, "erlenmeyer_glass")
        empty_elements.append(mc)
    # The smooth taper body is drawn by ErlenmeyerRenderer (custom renderer);
    # the model keeps only the base and neck boxes.
    write_model("erlenmeyer_flask",
                {k: el_tex(k) for k in ("erlenmeyer_glass", "erlenmeyer_bottom",
                                        "erlenmeyer_side", "erlenmeyer_top")},
                empty_elements)

    # ---- contents (liquid) model: only the white chunbai (texture 6) parts ----
    contents = []
    for el in full["elements"]:
        if el.get("from") is None:
            continue
        face_tex = [d["texture"] for d in el.get("faces", {}).values()
                    if d is not None]
        if not face_tex or 6 not in face_tex:
            continue  # glass body / neck belongs to the empty model
        mc = box_element_uvs(el, {6: 1.0})
        for face, data in mc["faces"].items():
            data["texture"] = "contents"
            data["tintindex"] = 0
        contents.append(mc)
    # Liquid frustum: w 4.5 at y1.5 -> w 1.5 at y6.5 (scaled).
    contents.extend(slices(1.5, 6.5, 4.5, 1.5, "contents", top_face=True))
    for el in contents:
        for face, data in el["faces"].items():
            if "tintindex" not in data:
                data["texture"] = "contents"
                data["tintindex"] = 0
    write_model("erlenmeyer_flask_contents",
                {"contents": el_tex("contents")}, contents)

    print("new erlenmeyer flask models generated")


if __name__ == "__main__":
    main()
