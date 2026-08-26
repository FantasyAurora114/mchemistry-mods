#!/usr/bin/env python3
"""Builds a small glass sphere (玻璃球) block model with circumference 6
(radius = 6 / 2pi ~= 0.955 model units). The sphere is approximated with
7 layered boxes; alternate layers are rotated 45 degrees around Y so the
silhouette reads as round (octagonal) instead of square. The texture is a
light-blue glass with a highlight.
"""

import json
import math
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")

R = 6.0 / (2.0 * math.pi)
LAYERS = 7


def main():
    tex_dir = os.path.join(ASSETS, "textures", "block")
    model_dir = os.path.join(ASSETS, "models", "block")
    os.makedirs(tex_dir, exist_ok=True)
    os.makedirs(model_dir, exist_ok=True)

    # Light-blue glass texture with a diagonal highlight.
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            d = (x - 5) ** 2 + (y - 5) ** 2
            if d > 110:
                continue  # transparent outside the circle
            if d < 12:
                img.putpixel((x, y), (0xD4, 0xE5, 0xF7, 255))  # highlight
            elif d < 45:
                img.putpixel((x, y), (0xB3, 0xCF, 0xEC, 255))  # mid
            else:
                img.putpixel((x, y), (0x8B, 0xAD, 0xD0, 255))  # edge
    img.save(os.path.join(tex_dir, "glass_sphere.png"))

    h = 2.0 * R / LAYERS
    elements = []
    for i in range(LAYERS):
        y_low = -R + i * h
        y_high = y_low + h
        # Width at the layer's middle (best stepped-sphere silhouette).
        y_mid = (y_low + y_high) / 2.0
        w = math.sqrt(max(R * R - y_mid * y_mid, 0.0))
        if w < 1e-4:
            continue
        el = {
            "from": [-w, y_low, -w],
            "to": [w, y_high, w],
            "shade": False,
            "faces": {f: {"texture": "glass_sphere", "uv": [0, 0, 16, 16]}
                      for f in ("north", "south", "east", "west", "up", "down")},
        }
        # Alternate layers rotate 45 degrees around Y for a round outline.
        if i % 2 == 1:
            el["rotation"] = {"origin": [0.0, 0.0, 0.0], "axis": "y", "angle": 45}
        elements.append(el)

    model = {
        "ambientocclusion": False,
        "textures": {"glass_sphere": "chemistry:block/glass_sphere"},
        "elements": elements,
    }
    with open(os.path.join(model_dir, "glass_sphere.json"), "w", encoding="utf-8") as f:
        json.dump(model, f, ensure_ascii=False, indent=2)

    print(f"glass sphere generated: R={R:.4f}, layers={len(elements)}")


if __name__ == "__main__":
    main()
