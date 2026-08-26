#!/usr/bin/env python3
"""Shared rubber-stopper geometry converted from the user's 橡胶塞.bbmodel.

The stopper is two cubes: a narrow end that goes into the tube mouth and a
wide flange that sits on the rim. Callers pass the mouth position/size; the
iron stand passes the tube's rotation/origin so the stopper swings with it.
"""

import base64
import io
import json
import os

from PIL import Image

DESK = "/Users/apple/Desktop"
STOPPER_BB = os.path.join(DESK, "橡胶塞.bbmodel")

# Face UVs copied verbatim from the user's model (texture 1 = xjs.png).
NARROW_UVS = {
    "north": [7, 2.33333, 8, 3],
    "east": [6, 2.33333, 7, 3],
    "south": [5, 2.33333, 6, 3],
    "west": [4, 6.33333, 5, 7],
    "up": [7, 9, 8, 10],
    "down": [8, 9, 9, 10],
}
WIDE_UVS = {
    "north": [7.33333, 2.33333, 9, 3],
    "east": [7.33333, 2.33333, 9, 3],
    "south": [6, 2.33333, 7.66667, 3],
    "west": [8, 2.33333, 9.66667, 3],
    "up": [7, 9, 8.66667, 10.66667],
    "down": [7, 9.33333, 8.66667, 11],
}


def rubber_texture():
    """The rubber material texture (xjs.png) from the user's bbmodel."""
    bb = json.load(open(STOPPER_BB, encoding="utf-8"))
    src = bb["textures"][1]["source"]
    return Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA")


def stopper_elements(narrow_from, narrow_to, wide_from, wide_to,
                     rotation=None, origin=None):
    """Two MC-style elements: narrow end (into the mouth) + wide flange."""
    els = []
    for f, t, uvs in ((narrow_from, narrow_to, NARROW_UVS),
                      (wide_from, wide_to, WIDE_UVS)):
        el = {
            "from": list(f),
            "to": list(t),
            "faces": {face: {"texture": "stopper", "uv": uv} for face, uv in uvs.items()},
        }
        if rotation:
            el["rotation"] = list(rotation)
            el["origin"] = list(origin)
        els.append(el)
    return els
