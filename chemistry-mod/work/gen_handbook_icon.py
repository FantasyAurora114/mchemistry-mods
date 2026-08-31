#!/usr/bin/env python3
"""Turn knowledge_book.png into the handbook item icon:
light-blue cover, no middle binding line, page-edge surroundings recolored
to the cover tone. Writes textures/item/handbook.png and a desktop copy."""
import os
from pathlib import Path

from PIL import Image

SRC = Path("/tmp/vanilla_assets/assets/minecraft/textures/item/knowledge_book.png")
OUT = Path("/Users/apple/Documents/Codex/2026-08-21/wo/outputs/chemistry-mod"
           "/src/main/resources/assets/mchemistry/textures/item/handbook.png")
DESK = Path(os.path.expanduser("~/Desktop/MChemistry/手册材质/原版参考/handbook_icon_淡蓝.png"))

# 知识之书 -> 淡蓝手册封面
PALETTE = {
    (42, 89, 42, 255): (53, 108, 160, 255),      # C 深绿轮廓/装订线 -> 深蓝
    (61, 122, 61, 255): (88, 152, 208, 255),     # M 中绿 -> 中蓝
    (71, 142, 71, 255): (122, 180, 228, 255),    # L 绿 -> 亮蓝
    (86, 173, 86, 255): (156, 204, 240, 255),    # H 浅绿(书皮主色) -> 淡蓝
    (27, 54, 27, 255): (22, 52, 79, 255),        # D 最深绿 -> 最深蓝
    (22, 16, 5, 255): (14, 36, 56, 255),         # O 深棕描边/阴影 -> 深蓝描边
    (183, 183, 183, 255): (156, 204, 240, 255),  # W 页边浅灰 -> 书皮淡蓝
    (214, 214, 214, 255): (156, 204, 240, 255),  # w 页边白 -> 书皮淡蓝
    (153, 153, 153, 255): (156, 204, 240, 255),  # G 页边灰 -> 书皮淡蓝
    (91, 91, 91, 255): (156, 204, 240, 255),     # g 页边深灰 -> 书皮淡蓝
}


def dump_map(px):
    cmap = {
        (42, 89, 42, 255): "C", (61, 122, 61, 255): "M", (71, 142, 71, 255): "L",
        (86, 173, 86, 255): "H", (27, 54, 27, 255): "D", (22, 16, 5, 255): "O",
        (183, 183, 183, 255): "W", (214, 214, 214, 255): "w",
        (153, 153, 153, 255): "G", (91, 91, 91, 255): "g",
    }
    for y in range(16):
        print("".join(cmap.get(px[x, y], ".") for x in range(16)))


def main():
    im = Image.open(SRC).convert("RGBA")
    px = im.load()
    print("== knowledge_book layout ==")
    dump_map(px)
    for y in range(16):
        for x in range(16):
            c = px[x, y]
            if c in PALETTE:
                px[x, y] = PALETTE[c]
    # 装订线：去掉封面中央纵向的深色折痕（C 色），替换为两侧的书皮淡蓝。
    for y in range(1, 14):
        for x in range(4, 12):
            if px[x, y] == (53, 108, 160, 255):
                # 若上下或左右是淡蓝/亮蓝，说明这是封面内部的折痕而非外轮廓
                around = [px[x - 1, y], px[x + 1, y], px[x, y - 1], px[x, y + 1]]
                if any(a in ((156, 204, 240, 255), (122, 180, 228, 255)) for a in around):
                    px[x, y] = (156, 204, 240, 255)
    im.save(OUT)
    DESK.parent.mkdir(parents=True, exist_ok=True)
    im.save(DESK)
    print("== handbook icon (淡蓝) ==")
    dump_map(px)
    print("saved ->", OUT)
    print("saved ->", DESK)


if __name__ == "__main__":
    main()
