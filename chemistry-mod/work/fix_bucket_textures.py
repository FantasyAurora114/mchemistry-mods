#!/usr/bin/env python3
"""Fix every liquid bucket texture: keep the vanilla iron bucket greyscale and
tint only the liquid region inside the bucket mouth (the 32 pixels where
water_bucket.png differs from bucket.png). The old generator tinted the whole
texture, so the iron bucket part took on the liquid colour."""

import re
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src" / "main" / "resources" / "assets" / "mchemistry" / "textures" / "item"
LIQUIDS_JAVA = ROOT / "src" / "main" / "java" / "com" / "example" / "chemistry" / "data" / "Liquids.java"
VANILLA = Path(__file__).resolve().parent / "vanilla_bucket"


def load(p):
    return Image.open(p).convert("RGBA")


def main():
    bucket = load(VANILLA / "bucket.png")
    water = load(VANILLA / "water_bucket.png")
    pb, pw = bucket.load(), water.load()
    content = []
    for y in range(16):
        for x in range(16):
            if pb[x, y] != pw[x, y]:
                content.append((x, y))
    print(f"content region: {len(content)} pixels")

    src = LIQUIDS_JAVA.read_text(encoding="utf-8")
    liquids = re.findall(
        r'new Liquid\("(\w+)", "[^"]*", "[^"]*", "[^"]*", 0x([0-9A-Fa-f]{6})', src)
    print(f"parsed {len(liquids)} liquids from Liquids.java")

    for lid, hexcolor in liquids:
        color = int(hexcolor, 16)
        cr, cg, cb = (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF
        out = bucket.copy()
        po = out.load()
        for (x, y) in content:
            wpx = pw[x, y]
            if wpx[3] == 0:
                continue
            r, g, b = wpx[0], wpx[1], wpx[2]
            if r == g == b:
                # Bucket interior wall visible above the liquid: keep grey.
                po[x, y] = wpx
            else:
                shade = 0.38 + 0.62 * (max(r, g, b) / 255.0)
                po[x, y] = (int(cr * shade), int(cg * shade), int(cb * shade), 255)
        out.save(TEX / f"liquid_{lid}_bucket.png")
    print("OK: regenerated all liquid bucket textures")


if __name__ == "__main__":
    main()
