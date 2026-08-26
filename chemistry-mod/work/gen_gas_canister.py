#!/usr/bin/env python3
"""Generates the portable gas canister textures, item model, and lang keys."""

import json
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry")


def make_base() -> Image.Image:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Canister body outline (x 3..12, y 2..13), metal greys.
    light = (190, 196, 204, 255)
    mid = (148, 154, 164, 255)
    dark = (104, 110, 120, 255)
    for y in range(2, 14):
        for x in range(3, 13):
            if x == 3 or x == 12:
                img.putpixel((x, y), dark)
            elif y == 2 or y == 13:
                img.putpixel((x, y), mid)
            elif x == 4:
                img.putpixel((x, y), (124, 130, 140, 255))
            elif x in (10, 11):
                img.putpixel((x, y), mid)
            else:
                # window, leave transparent
                pass
    # Top lip
    for x in range(4, 12):
        img.putpixel((x, 1), light)
    # Bottom rim
    for x in range(4, 12):
        img.putpixel((x, 14), dark)
    # Slight highlight on the top-left of the body
    for x in range(4, 6):
        img.putpixel((x, 3), light)
    return img


def make_contents() -> Image.Image:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # Window inside the canister body; tinted by the item model.
    for y in range(4, 12):
        for x in range(5, 11):
            img.putpixel((x, y), (255, 255, 255, 255))
    return img


def write_png(name: str, img: Image.Image) -> None:
    path = os.path.join(ASSETS, "textures", "item", name + ".png")
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


def write_json(rel: str, data) -> None:
    path = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print("wrote", os.path.relpath(path, ROOT))


def main() -> None:
    write_png("gas_canister", make_base())
    write_png("gas_canister_contents", make_contents())

    write_json("models/item/gas_canister.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "chemistry:item/gas_canister"},
    })
    write_json("models/item/gas_canister_contents.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "chemistry:item/gas_canister_contents"},
    })
    write_json("items/gas_canister.json", {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:custom_model_data",
            "cases": [
                {
                    "when": "filled",
                    "model": {
                        "type": "minecraft:composite",
                        "models": [
                            {"type": "minecraft:model", "model": "chemistry:item/gas_canister"},
                            {
                                "type": "minecraft:model",
                                "model": "chemistry:item/gas_canister_contents",
                                "tints": [
                                    {
                                        "type": "minecraft:custom_model_data",
                                        "index": 0,
                                        "default": 16777215,
                                    }
                                ],
                            },
                        ],
                    },
                }
            ],
            "fallback": {"type": "minecraft:model", "model": "chemistry:item/gas_canister"},
        }
    })

    for lang, name in (("zh_cn", "气体罐"), ("en_us", "Gas Canister")):
        path = os.path.join(ASSETS, "lang", lang + ".json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data["item.chemistry.gas_canister"] = name
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print("updated", os.path.relpath(path, ROOT))


if __name__ == "__main__":
    main()
