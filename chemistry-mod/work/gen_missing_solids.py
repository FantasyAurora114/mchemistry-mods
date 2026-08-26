"""Generate assets for the two newly added solids (potassium, potassium_carbonate)
by recolouring the existing sodium / sodium-carbonate textures."""
import json
import shutil
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/chemistry"
TEX = ROOT / "textures/item"
ITEMS = ROOT / "items"
MODELS = ROOT / "models/item"


def tint_bottle(src, dst, content_from, content_to):
    im = Image.open(TEX / src).convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a and (r, g, b) == content_from:
                px[x, y] = (*content_to, a)
    im.save(TEX / dst)


def tint_loose(src, dst, src_nominal, dst_nominal):
    im = Image.open(TEX / src).convert("RGBA")
    px = im.load()
    sr, sg, sb = src_nominal
    dr, dg, db = dst_nominal
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (min(255, r * dr // sr), min(255, g * dg // sg),
                            min(255, b * db // sb), a)
    im.save(TEX / dst)


def write_json(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False))


# Potassium: silver-grey metal (nominal 0xD0D0E0), based on sodium (0xD8D8E8).
# Sodium's bottle content pixel is 0x707078; scale it by the nominal ratio.
r = 112 / 216
potassium_content = (int(208 * r), int(208 * r), int(224 * r))
for src, dst in [("solid_sodium.png", "solid_potassium.png"),
                 ("open_solid_sodium.png", "open_solid_potassium.png")]:
    tint_bottle(src, dst, (112, 112, 120), potassium_content)
tint_loose("loose_sodium.png", "loose_potassium.png",
           (216, 216, 232), (208, 208, 224))

# Potassium carbonate: white powder, same nominal colour as sodium carbonate.
for name in ("solid", "open_solid", "loose"):
    shutil.copyfile(TEX / f"{name}_sodium_carbonate.png",
                    TEX / f"{name}_potassium_carbonate.png")

for solid in ("potassium", "potassium_carbonate"):
    for prefix in ("solid", "open_solid", "loose"):
        item_id = f"{prefix}_{solid}" if prefix != "loose" else f"loose_{solid}"
        write_json(ITEMS / f"{item_id}.json", {
            "model": {"type": "minecraft:model", "model": f"chemistry:item/{item_id}"}
        })
        write_json(MODELS / f"{item_id}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"chemistry:item/{item_id}"}
        })

print("OK: textures + item models generated")
