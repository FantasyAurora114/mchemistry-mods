#!/usr/bin/env python3
"""Redraws the synthesis tower GUI in vanilla 1.21.10 container style.

The vanilla style is: #FFFFFF/#555555 outer bevel, #C6C6C6 frame, #373737 inner
shadow line, #8B8B8B panel fill, and 18x18 slot sprites with #373737/#FFFFFF
bevels. Buttons use #AAAAAA/#6F6F6F/#565656.
"""

import os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "chemistry",
                   "textures", "gui", "synthesis_tower.png")

W, H = 176, 200

# Vanilla palette
C_OUTER_LIGHT = (0xFF, 0xFF, 0xFF, 255)
C_OUTER_DARK = (0x55, 0x55, 0x55, 255)
C_FRAME = (0xC6, 0xC6, 0xC6, 255)
C_SHADOW = (0x37, 0x37, 0x37, 255)
C_FILL = (0x8B, 0x8B, 0x8B, 255)
# Slot bevel (18x18 sprite): dark top/left, light bottom/right, fill base
C_SLOT_DARK = (0x37, 0x37, 0x37, 255)
C_SLOT_LIGHT = (0xFF, 0xFF, 0xFF, 255)
C_SLOT_BASE = (0x8B, 0x8B, 0x8B, 255)
# Button
C_BTN_BORDER = (0xAA, 0xAA, 0xAA, 255)
C_BTN_FILL = (0x6F, 0x6F, 0x6F, 255)
C_BTN_DARK = (0x56, 0x56, 0x56, 255)
# Gauge well
C_WELL_INNER = (0x2E, 0x2E, 0x2E, 255)


def make_slot() -> Image.Image:
    """18x18 vanilla slot sprite: 2px bevel, 16x16 base."""
    img = Image.new("RGBA", (18, 18), C_SLOT_BASE)
    px = img.load()
    for y in range(18):
        for x in range(18):
            if x < 2 or y < 2:
                px[x, y] = C_SLOT_DARK
            elif x >= 16 or y >= 16:
                px[x, y] = C_SLOT_LIGHT
            else:
                px[x, y] = C_SLOT_BASE
    return img


def draw_button(d: ImageDraw.ImageDraw, x0, y0, w, h):
    """Vanilla button: light border top/left, dark bottom/right, gray fill."""
    d.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=C_BTN_FILL)
    for x in range(x0, x0 + w):
        d.point((x, y0), C_BTN_BORDER)
        d.point((x, y0 + h - 1), C_BTN_DARK)
    for y in range(y0, y0 + h):
        d.point((x0, y), C_BTN_BORDER)
        d.point((x0 + w - 1, y), C_BTN_DARK)


def main() -> None:
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")

    # --- Frame ---
    d.rectangle([0, 0, W - 1, H - 1], fill=C_OUTER_DARK)          # bottom/right bevel base
    d.rectangle([2, 2, W - 3, H - 3], fill=C_FRAME)               # frame
    d.rectangle([6, 6, W - 7, H - 7], fill=C_SHADOW)              # inner shadow line
    d.rectangle([7, 7, W - 8, H - 8], fill=C_FILL)                # panel fill
    # top/left outer bevel
    d.rectangle([0, 0, 1, H - 1], fill=C_OUTER_LIGHT)
    d.rectangle([0, 0, W - 1, 1], fill=C_OUTER_LIGHT)

    slot = make_slot()
    def put_slot(x, y):
        img.paste(slot, (x, y))

    # --- Machine slots ---
    for x in (26, 44, 62, 80):
        put_slot(x, 26)
    for x in (116,):
        for y in (26, 44, 62, 80, 98):
            put_slot(x, y)

    # --- Config buttons (气/液/固), 18x12, at y=9 ---
    for x in (44, 62, 80):
        draw_button(d, x, 9, 18, 12)

    # --- Run button 48x12 at (52,62) ---
    draw_button(d, 52, 62, 48, 12)

    # --- Gauge wells (12x72) ---
    for gx in (140, 156):
        d.rectangle([gx, 8, gx + 11, 79], fill=C_WELL_INNER)
        d.rectangle([gx, 8, gx + 11, 8], fill=C_SHADOW)      # top
        d.rectangle([gx, 8, gx, 79], fill=C_SHADOW)          # left
        d.rectangle([gx + 11, 8, gx + 11, 79], fill=C_SLOT_LIGHT)  # right
        d.rectangle([gx, 79, gx + 11, 79], fill=C_SLOT_LIGHT)      # bottom

    # --- Player inventory 4x9 slots ---
    for y in (120, 138, 156, 174):
        for c in range(9):
            put_slot(8 + c * 18, y)

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT)
    print("saved", OUT, img.size)


if __name__ == "__main__":
    main()
