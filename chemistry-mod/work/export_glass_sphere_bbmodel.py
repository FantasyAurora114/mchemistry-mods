#!/usr/bin/env python3
"""Exports the glass sphere (玻璃球) block model as a BlockBench .bbmodel file
on the Desktop, so the user can open/edit it in BlockBench."""

import base64
import io
import json
import math
import os
import uuid

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")
DESK = "/Users/apple/Desktop"

R = 6.0 / (2.0 * math.pi)
LAYERS = 7


def main():
    tex_path = os.path.join(ASSETS, "textures", "block", "glass_sphere.png")
    img = Image.open(tex_path).convert("RGBA")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    source = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")

    texture = {
        "name": "玻璃球.png",
        "relative_path": "玻璃球.png",
        "folder": "",
        "namespace": "",
        "id": "0",
        "group": "",
        "scope": 0,
        "width": 16,
        "height": 16,
        "uv_width": 16,
        "uv_height": 16,
        "particle": True,
        "use_as_default": False,
        "layers_enabled": False,
        "sync_to_project": "",
        "file_format": "png",
        "render_mode": "default",
        "render_sides": "auto",
        "wrap_mode": "limited",
        "pbr_channel": "color",
        "fps": 7,
        "frame_time": 1,
        "frame_order_type": "loop",
        "frame_order": "",
        "frame_interpolate": False,
        "visible": True,
        "internal": True,
        "saved": True,
        "uuid": str(uuid.uuid4()),
        "source": source,
    }

    elements = []
    outliner = []
    h = 2.0 * R / LAYERS
    for i in range(LAYERS):
        y_low = -R + i * h
        y_high = y_low + h
        y_mid = (y_low + y_high) / 2.0
        w = math.sqrt(max(R * R - y_mid * y_mid, 0.0))
        if w < 1e-4:
            continue
        uid = str(uuid.uuid4())
        outliner.append(uid)
        el = {
            "name": "玻璃球",
            "box_uv": False,
            "render_order": "default",
            "rescale": False,
            "locked": False,
            "shade": False,
            "light_emission": 0,
            "export": True,
            "scope": 0,
            "allow_mirror_modeling": True,
            "from": [-w, y_low, -w],
            "to": [w, y_high, w],
            "autouv": 0,
            "color": 0,
            "origin": [0.0, 0.0, 0.0],
            "faces": {f: {"uv": [0, 0, 16, 16], "texture": 0}
                      for f in ("north", "south", "east", "west", "up", "down")},
            "type": "cube",
            "uuid": uid,
        }
        if i % 2 == 1:
            el["rotation"] = [0, 45, 0]
            el["origin"] = [0.0, 0.0, 0.0]
        else:
            el["rotation"] = [0, 0, 0]
            el["origin"] = [0.0, 0.0, 0.0]
        elements.append(el)

    bb = {
        "meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False},
        "name": "玻璃球",
        "parent": "",
        "java_block_version": "1.21.11",
        "ambientocclusion": True,
        "front_gui_light": False,
        "visible_box": [1, 1, 0],
        "variable_placeholders": "",
        "multi_file_ruleset": "",
        "variable_placeholder_buttons": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 16, "height": 16},
        "elements": elements,
        "groups": [],
        "outliner": outliner,
        "textures": [texture],
    }
    out = os.path.join(DESK, "玻璃球.bbmodel")
    with open(out, "w", encoding="utf-8") as f:
        json.dump(bb, f, ensure_ascii=False, indent=2)
    print("saved", out, "elements:", len(elements))


if __name__ == "__main__":
    main()
