#!/usr/bin/env python3
"""Generates tinted liquid bucket items (28 liquids) from the vanilla bucket."""

import json
import os
import sys
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "work")))
from gen_liquid_bottles import LIQUIDS  # noqa: E402


def tint(template_path, color):
    img = Image.open(template_path).convert("RGBA")
    out = Image.new("RGBA", img.size)
    src = img.load()
    dst = out.load()
    cr, cg, cb = (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = src[x, y]
            if a == 0:
                dst[x, y] = (0, 0, 0, 0)
                continue
            shade = 0.38 + 0.62 * (max(r, g, b) / 255.0)
            dst[x, y] = (int(cr * shade), int(cg * shade), int(cb * shade), a)
    return out


def main():
    bucket = "/tmp/mc_assets/assets/minecraft/textures/item/bucket.png"
    if not os.path.exists(bucket):
        print("vanilla bucket texture missing; run the jar extraction first")
        return
    for liquid in LIQUIDS:
        lid, _formula, en, zh, color, _alpha, _product = liquid
        img = tint(bucket, color)
        img.save(os.path.join(ASSETS, "textures", "item", f"liquid_{lid}_bucket.png"))
        for rel, data in (
            (f"models/item/liquid_{lid}_bucket.json", {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"chemistry:item/liquid_{lid}_bucket"},
            }),
            (f"items/liquid_{lid}_bucket.json", {
                "model": {"type": "minecraft:model",
                          "model": f"chemistry:item/liquid_{lid}_bucket"},
            }),
        ):
            path = os.path.join(ASSETS, rel)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
    for locale, suffix in (("zh_cn", "桶"), ("en_us", "Bucket")):
        path = os.path.join(ASSETS, "lang", locale + ".json")
        with open(path, encoding="utf-8") as f:
            lang = json.load(f)
        for liquid in LIQUIDS:
            lid, _formula, en, zh, color, _alpha, _product = liquid
            key = f"item.chemistry.liquid_{lid}_bucket"
            base = lang.get(f"item.chemistry.liquid_{lid}", zh if locale == "zh_cn" else en)
            lang[key] = base + suffix if locale == "zh_cn" else f"{base} {suffix}"
        with open(path, "w", encoding="utf-8") as f:
            json.dump(lang, f, ensure_ascii=False, indent=2)
    print("bucket assets generated for", len(LIQUIDS), "liquids")


if __name__ == "__main__":
    main()
