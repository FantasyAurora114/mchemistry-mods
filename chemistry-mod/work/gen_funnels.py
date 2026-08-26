#!/usr/bin/env python3
"""Builds the placeable long-stem funnel (长颈漏斗) and separatory funnel
(分液漏斗) block assets from the user's BlockBench models:

* long-stem funnel: single model.
* separatory funnel: piston (活塞) open/closed x stopper (瓶塞) present/absent.
  The no-stopper variants are the two user models with the topmost small cube
  (the glass stopper at y 15-16) removed.

Also renders improved 16x16 item icons for both funnels from the 3D models,
using the new glass texture (漏斗.png) from the models.
"""

import base64
import io
import itertools
import json
import math
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")
DESK = "/Users/apple/Desktop"


def load_bb(name):
    return json.load(open(os.path.join(DESK, name + ".bbmodel"), encoding="utf-8"))


def extract_texture(bb, index=1):
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


def block_model(elements, texture):
    return {
        "ambientocclusion": False,
        "particle": texture,
        "display": {
            "gui": {"rotation": [30, 45, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
            "fixed": {"rotation": [30, 45, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
            "ground": {"rotation": [30, 45, 0], "translation": [0, 2, 0], "scale": [0.8, 0.8, 0.8]},
        },
        "textures": {"funnel": texture},
        "elements": elements,
    }


def without_stopper(elements):
    """The user's two separatory-funnel models share the stopper cube at the
    very top (from [3, 15, 3] to [4, 16, 4]); drop it for the no-stopper state."""
    return [el for el in elements
            if el["from"] != [3, 15, 3] or el["to"] != [4, 16, 4]]


# ---------------------------------------------------------------------------
# Icon rendering: simple isometric projection with affine texture mapping.
# ---------------------------------------------------------------------------

def _affine_from_quads(src, dst):
    """Affine matrix mapping destination (icon) coords -> source (texture) coords."""
    # dst = M * src  (2D affine, 6 unknowns); solve with 3 corner pairs.
    (sx0, sy0), (sx1, sy1), (sx2, sy2) = src[0], src[1], src[2]
    (dx0, dy0), (dx1, dy1), (dx2, dy2) = dst[0], dst[1], dst[2]
    denom = dx0 * (dy1 - dy2) + dx1 * (dy2 - dy0) + dx2 * (dy0 - dy1)
    if abs(denom) < 1e-9:
        return None
    a = (sx0 * (dy1 - dy2) + sx1 * (dy2 - dy0) + sx2 * (dy0 - dy1)) / denom
    b = (sx0 * (dx2 - dx1) + sx1 * (dx0 - dx2) + sx2 * (dx1 - dx0)) / denom
    c = (sx0 * (dx1 * dy2 - dx2 * dy1) + sx1 * (dx2 * dy0 - dx0 * dy2)
         + sx2 * (dx0 * dy1 - dx1 * dy0)) / denom
    d = (sy0 * (dy1 - dy2) + sy1 * (dy2 - dy0) + sy2 * (dy0 - dy1)) / denom
    e = (sy0 * (dx2 - dx1) + sy1 * (dx0 - dx2) + sy2 * (dx1 - dx0)) / denom
    f = (sy0 * (dx1 * dy2 - dx2 * dy1) + sy1 * (dx2 * dy0 - dx0 * dy2)
         + sy2 * (dx0 * dy1 - dx1 * dy0)) / denom
    return [a, b, c, d, e, f]


def _draw_face(canvas, tex, uv, quad, size):
    u1, v1, u2, v2 = uv
    sw = max(2, int(math.ceil(u2 - u1)))
    sh = max(2, int(math.ceil(v2 - v1)))
    patch = tex.crop((int(u1), int(v1), int(u1) + sw, int(v1) + sh))
    src = [(0, 0), (sw, 0), (sw, sh), (0, sh)]
    matrix = _affine_from_quads(src, quad)
    if matrix is None:
        return
    mapped = patch.transform((size, size), Image.AFFINE, matrix,
                             resample=Image.BILINEAR, fillcolor=(0, 0, 0, 0))
    canvas.alpha_composite(mapped)


def render_icon(elements, texture, size=32):
    """Orthographic view from the north-west + top, so the funnel's stopcock
    handle (on the -Z side) is visible. Draws the north, west and up faces,
    auto-fitting the model into the icon canvas."""
    yaw = math.radians(225.0)
    pitch = math.radians(28.0)
    cy = math.cos(yaw)
    sy = math.sin(yaw)
    cp = math.cos(pitch)
    sp = math.sin(pitch)

    def raw(x, y, z):
        dx, dy, dz = x - 8.0, y - 3.5, z - 8.0
        x1 = dx * cy + dz * sy
        z1 = -dx * sy + dz * cy
        y2 = dy * cp - z1 * sp
        return x1, -y2

    # Bounding box of the projected model (all element corners).
    pts = []
    for el in elements:
        (x1, y1, z1), (x2, y2, z2) = el["from"], el["to"]
        for c in itertools.product((x1, x2), (y1, y2), (z1, z2)):
            pts.append(raw(*c))
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    cx, cy2 = (min(xs) + max(xs)) / 2.0, (min(ys) + max(ys)) / 2.0
    w, h = max(xs) - min(xs), max(ys) - min(ys)
    pad = 2.0
    scale = (size - 2.0 * pad) / max(w, h, 1e-6)

    def proj(x, y, z):
        px, py = raw(x, y, z)
        return (px - cx) * scale + size / 2.0, (py - cy2) * scale + size / 2.0

    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    # Painter's algorithm: farthest first. From the north-west, far = +x/+z.
    for el in sorted(elements, key=lambda e: e["to"][0] + e["to"][2], reverse=True):
        (x1, y1, z1), (x2, y2, z2) = el["from"], el["to"]
        faces = el.get("faces", {})
        if "up" in faces and (y2 - y1) > 0.01:
            u1, v1, u2, v2 = faces["up"]["uv"]
            quad = [proj(x1, y2, z1), proj(x2, y2, z1), proj(x2, y2, z2), proj(x1, y2, z2)]
            _draw_face(canvas, texture, (u1, v1, u2, v2), quad, size)
        # north face (-z, z1)
        if "north" in faces:
            u1, v1, u2, v2 = faces["north"]["uv"]
            quad = [proj(x1, y1, z1), proj(x2, y1, z1), proj(x2, y2, z1), proj(x1, y2, z1)]
            _draw_face(canvas, texture, (u1, v1, u2, v2), quad, size)
        # west face (-x, x1)
        if "west" in faces:
            u1, v1, u2, v2 = faces["west"]["uv"]
            quad = [proj(x1, y1, z2), proj(x1, y1, z1), proj(x1, y2, z1), proj(x1, y2, z2)]
            _draw_face(canvas, texture, (u1, v1, u2, v2), quad, size)
    return canvas


# Hand-drawn 16x16 pixel-art icons, in the same glass palette as the new
# funnel texture. '1' = light glass, '2' = mid glass, '3' = dark glass,
# 'g' = stopcock gray. Each row is exactly 16 characters.
LONG_STEM_ICON = [
    ".....233132.....",
    ".....233132.....",
    "......2312......",
    "......2312......",
    ".......23.......",
    ".......23.......",
    ".......23.......",
    ".......23.......",
    ".......23.......",
    ".......23.......",
    ".......23.......",
    ".......23.......",
    ".......23.......",
    ".......23.......",
    ".......23.......",
    "................",
]

SEPARATORY_ICON = [
    ".......22.......",
    ".......23.......",
    "......2312......",
    ".....233132.....",
    ".....233132.....",
    "......2312......",
    "......2312......",
    "...gggggg.......",
    "...gggggg.......",
    ".......23.......",
    ".......23.......",
    ".......23.......",
    ".......23.......",
    ".......23.......",
    ".......23.......",
    "................",
]

ICON_COLORS = {
    "1": (0xD4, 0xE5, 0xF7, 255),
    "2": (0xB3, 0xCF, 0xEC, 255),
    "3": (0x8B, 0xAD, 0xD0, 255),
    "g": (0xBA, 0xBA, 0xBA, 255),
}


def draw_pixel_icon(rows):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in ICON_COLORS:
                img.putpixel((x, y), ICON_COLORS[ch])
    return img


def main():
    tex_dir = os.path.join(ASSETS, "textures", "block")
    item_tex_dir = os.path.join(ASSETS, "textures", "item")
    model_dir = os.path.join(ASSETS, "models", "block")
    item_model_dir = os.path.join(ASSETS, "models", "item")
    state_dir = os.path.join(ASSETS, "blockstates")
    for d in (tex_dir, item_tex_dir, model_dir, item_model_dir, state_dir):
        os.makedirs(d, exist_ok=True)

    funnel_tex = extract_texture(load_bb("长颈漏斗"), 1)
    funnel_tex.save(os.path.join(tex_dir, "funnel.png"))
    texture = "chemistry:block/funnel"

    long_stem = load_bb("长颈漏斗")
    sep_closed = load_bb("分液漏斗（关闭 有瓶塞）")
    sep_open = load_bb("分液漏斗（打开 有瓶塞）")

    long_els = [element_to_mc(el, "funnel") for el in long_stem["elements"]]
    closed_els = [element_to_mc(el, "funnel") for el in sep_closed["elements"]]
    open_els = [element_to_mc(el, "funnel") for el in sep_open["elements"]]

    models = {
        "long_stem_funnel": long_els,
        "separatory_funnel_closed_stoppered": closed_els,
        "separatory_funnel_open_stoppered": open_els,
        "separatory_funnel_closed": without_stopper(closed_els),
        "separatory_funnel_open": without_stopper(open_els),
    }
    for name, els in models.items():
        with open(os.path.join(model_dir, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(block_model(els, texture), f, ensure_ascii=False, indent=2)

    with open(os.path.join(state_dir, "long_stem_funnel.json"), "w", encoding="utf-8") as f:
        json.dump({"variants": {"": {"model": "chemistry:block/long_stem_funnel"}}},
                  f, ensure_ascii=False, indent=2)

    sep_variants = {}
    for open_ in (False, True):
        for stopper in (False, True):
            if open_:
                base = "separatory_funnel_open"
            else:
                base = "separatory_funnel_closed"
            if stopper:
                base += "_stoppered"
            key = f"open={str(open_).lower()},has_stopper={str(stopper).lower()}"
            sep_variants[key] = {"model": f"chemistry:block/{base}"}
    with open(os.path.join(state_dir, "separatory_funnel.json"), "w", encoding="utf-8") as f:
        json.dump({"variants": sep_variants}, f, ensure_ascii=False, indent=2)

    # Compact "inserted in a rubber stopper" models, drawn by the iron-stand
    # renderer. The attached frame has +Y pointing OUT of the vessel mouth, so
    # the bowl sticks up and the stem goes down into the vessel.
    def glass_box(fr, to, uv):
        return {"from": list(fr), "to": list(to), "shade": False,
                "faces": {f: {"texture": "funnel", "uv": list(uv)}
                          for f in ("north", "south", "east", "west", "up", "down")}}

    funnel_in_stopper = {
        "ambientocclusion": False,
        "textures": {"funnel": texture},
        "elements": [
            glass_box([-0.2, -3.0, -0.2], [0.2, 0, 0.2], [0, 0, 3, 14]),        # stem in the vessel
            glass_box([-0.25, 0, -0.25], [0.25, 3.0, 0.25], [0, 0, 3, 14]),     # raised neck
            glass_box([-0.55, 3.0, -0.55], [0.55, 4.0, 0.55], [10, 3, 16, 7]),  # bowl neck
            glass_box([-1.1, 4.0, -1.1], [1.1, 5.0, 1.1], [10, 3, 16, 7]),      # bowl rim above the glass-tube head
        ],
    }
    separatory_in_stopper = {
        "ambientocclusion": False,
        "textures": {"funnel": texture},
        "elements": [
            glass_box([-0.2, -2.5, -0.2], [0.2, 0, 0.2], [0, 0, 3, 14]),         # stem in the vessel
            glass_box([-0.6, 0, -0.6], [0.6, 0.5, 0.6], [0, 14, 6, 16]),         # stopcock band (gray)
            glass_box([-1.05, 0, -0.2], [-0.6, 0.5, 0.2], [0, 14, 6, 16]),       # stopcock handle
            glass_box([-0.25, 0.5, -0.25], [0.25, 3.0, 0.25], [0, 0, 3, 14]),    # raised neck
            glass_box([-0.55, 3.0, -0.55], [0.55, 4.0, 0.55], [10, 3, 16, 7]),   # bowl neck
            glass_box([-1.1, 4.0, -1.1], [1.1, 5.0, 1.1], [10, 3, 16, 7]),       # bowl rim above the glass-tube head
            glass_box([-0.6, 5.0, -0.6], [0.6, 5.5, 0.6], [10, 3, 16, 7]),       # stopper neck
            glass_box([-0.4, 5.5, -0.4], [0.4, 5.9, 0.4], [3, 7, 10, 14]),       # stopper knob
        ],
    }
    with open(os.path.join(model_dir, "funnel_in_stopper.json"), "w", encoding="utf-8") as f:
        json.dump(funnel_in_stopper, f, ensure_ascii=False, indent=2)
    with open(os.path.join(model_dir, "separatory_funnel_in_stopper.json"), "w", encoding="utf-8") as f:
        json.dump(separatory_in_stopper, f, ensure_ascii=False, indent=2)

    # Improved item icons: rendered from the 3D models with the new glass texture.
    icons = {
        "long_stem_funnel": LONG_STEM_ICON,
        "separatory_funnel": SEPARATORY_ICON,
    }
    for item_id, rows in icons.items():
        icon = draw_pixel_icon(rows)
        icon.save(os.path.join(item_tex_dir, item_id + ".png"))
        with open(os.path.join(item_model_dir, item_id + ".json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": f"chemistry:item/{item_id}"}},
                      f, ensure_ascii=False, indent=2)
        icon.resize((64, 64), Image.NEAREST).save(os.path.join(DESK, item_id + "_icon_preview.png"))

    print("funnel assets generated")


if __name__ == "__main__":
    main()
