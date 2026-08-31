#!/usr/bin/env python3
"""Generates the alcohol blowtorch (酒精喷灯) block models, item icons,
blockstate and lang entries from the user's BlockBench model on the Desktop.
The model is scaled to half size (x/z around 8.5, y halved) so it fits the
block like the alcohol lamp."""

import base64
import io
import json
import os

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "mchemistry")
DESK = "/Users/apple/Desktop"
NS = "mchemistry"


def load_bb(name):
    return json.load(open(os.path.join(DESK, name + ".bbmodel"), encoding="utf-8"))


def extract_texture(bb, index=0):
    src = bb["textures"][index]["source"]
    return Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA")


def scale_coord(v, center):
    return center + (v - center) / 2.0


def scale_element(el, texture):
    f = [scale_coord(v, 8.5) if i != 1 else v / 2.0 for i, v in enumerate(el["from"])]
    t = [scale_coord(v, 8.5) if i != 1 else v / 2.0 for i, v in enumerate(el["to"])]
    faces = {}
    for face, data in el.get("faces", {}).items():
        if data is None:
            continue
        entry = {"texture": texture}
        if "uv" in data:
            entry["uv"] = data["uv"]
        faces[face] = entry
    return {"from": f, "to": t, "shade": False, "faces": faces}


def main():
    tex_dir = os.path.join(ASSETS, "textures", "block")
    item_tex = os.path.join(ASSETS, "textures", "item")
    model_dir = os.path.join(ASSETS, "models", "block")
    item_model = os.path.join(ASSETS, "models", "item")
    items_dir = os.path.join(ASSETS, "items")
    state_dir = os.path.join(ASSETS, "blockstates")
    for d in (tex_dir, item_tex, model_dir, item_model, items_dir, state_dir):
        os.makedirs(d, exist_ok=True)

    bb = load_bb("酒精喷灯")
    extract_texture(bb).save(os.path.join(tex_dir, "alcohol_blowtorch.png"))

    def model(elements):
        return {
            "ambientocclusion": False,
            "particle": "alcohol_blowtorch",
            "textures": {
                "alcohol_blowtorch": f"{NS}:block/alcohol_blowtorch",
                "flame": f"{NS}:block/alcohol_lamp_flame",
            },
            "elements": elements,
        }

    base = [scale_element(el, "alcohol_blowtorch") for el in bb["elements"]]
    # Flame at the very top of the model: the scaled top tube is
    # x/z 7.75-8.25, y 2-4, so the flame rises from y 4 to ~5.6.
    flame = [
        {"from": [7.8, 4.0, 7.8], "to": [8.2, 5.6, 8.2],
         "shade": False, "faces": {"north": {"texture": "flame"}, "south": {"texture": "flame"}}},
        {"from": [7.8, 4.0, 7.8], "to": [8.2, 5.6, 8.2],
         "shade": False, "faces": {"east": {"texture": "flame"}, "west": {"texture": "flame"}}},
    ]
    # Cap: a small bronze cube covering the nozzle outlet.
    cap = {"from": [7.65, 1.9, 9.85], "to": [8.35, 2.55, 10.35],
           "shade": False,
           "faces": {f: {"texture": "alcohol_blowtorch"} for f in
                     ("north", "south", "east", "west", "up", "down")}}

    for name, elements in (("alcohol_blowtorch", base),
                           ("alcohol_blowtorch_lit", base + flame),
                           ("alcohol_blowtorch_capped", base + [cap])):
        with open(os.path.join(model_dir, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(model(elements), f, ensure_ascii=False, indent=2)

    variants = {}
    for lit in (False, True):
        for capped in (False, True):
            for stuck in (False, True):
                m = ("alcohol_blowtorch_capped" if capped
                     else "alcohol_blowtorch_lit" if lit else "alcohol_blowtorch")
                key = f"lit={str(lit).lower()},capped={str(capped).lower()},stuck={str(stuck).lower()}"
                variants[key] = {"model": f"{NS}:block/{m}"}
    with open(os.path.join(state_dir, "alcohol_blowtorch.json"), "w", encoding="utf-8") as f:
        json.dump({"variants": variants}, f, ensure_ascii=False, indent=2)

    # ---- Item icons: 16x16 bronze blowtorch pixel art ----
    BRONZE = (201, 164, 112, 255)
    BRONZE_L = (228, 185, 113, 255)
    BRONZE_D = (150, 120, 80, 255)
    OUTLINE = (105, 82, 52, 255)
    RED = (244, 39, 39, 255)
    YELLOW = (244, 234, 39, 255)

    def icon(lit=False, capped=False):
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        px = im.load()
        rect = [(x0, y0, x1, y1, color) for (x0, y0, x1, y1, color) in [
            (4, 13, 11, 15, BRONZE_D),      # base
            (7, 5, 9, 13, BRONZE),          # body
            (7, 3, 9, 5, BRONZE_L),         # top knob
            (9, 6, 13, 8, BRONZE),          # nozzle arm
            (13, 6, 14, 8, BRONZE_D),       # nozzle tip
        ]]
        if capped:
            rect.append((6, 2, 10, 4, BRONZE_D))
        for x0, y0, x1, y1, color in rect:
            for y in range(y0, y1 + 1):
                for x in range(x0, x1 + 1):
                    px[x, y] = color
        # outline
        for x in range(4, 12):
            px[x, 13] = OUTLINE
            px[x, 15] = OUTLINE
        for y in range(13, 16):
            px[4, y] = OUTLINE
            px[11, y] = OUTLINE
        for y in range(5, 14):
            px[7, y] = OUTLINE
            px[9, y] = OUTLINE
        if lit:
            for y in range(4, 7):
                for x in range(13, 16):
                    px[x, y] = RED if y < 6 else YELLOW
            px[14, 3] = YELLOW
        return im

    items = [
        ("alcohol_blowtorch", icon(False, False), "酒精喷灯", "Alcohol Blowtorch"),
        ("alcohol_blowtorch_lit", icon(True, False), "点燃的酒精喷灯", "Lit Alcohol Blowtorch"),
    ]
    zh = {}
    en = {}
    for item_id, img, zname, ename in items:
        img.save(os.path.join(item_tex, item_id + ".png"))
        with open(os.path.join(item_model, item_id + ".json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": f"{NS}:item/{item_id}"}},
                      f, ensure_ascii=False, indent=2)
        with open(os.path.join(items_dir, item_id + ".json"), "w", encoding="utf-8") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"{NS}:item/{item_id}"}},
                      f, ensure_ascii=False, indent=2)
        zh[f"item.{NS}.{item_id}"] = zname
        en[f"item.{NS}.{item_id}"] = ename
    zh[f"block.{NS}.alcohol_blowtorch"] = "酒精喷灯"
    en[f"block.{NS}.alcohol_blowtorch"] = "Alcohol Blowtorch"
    for locale, data in (("zh_cn", zh), ("en_us", en)):
        path = os.path.join(ASSETS, "lang", locale + ".json")
        lang = json.load(open(path, encoding="utf-8"))
        lang.update(data)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(lang, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("alcohol blowtorch assets generated")


if __name__ == "__main__":
    main()
