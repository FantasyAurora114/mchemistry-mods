#!/usr/bin/env python3
"""Generate placeholder handbook textures + layout reference images.

Placeholders go into the mod's assets (so the game runs before real art
arrives); layout references go to ~/Desktop/MChemistry/手册材质/ for the user
to draw against.
"""
import os
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ITEM_DIR = ROOT / "src/main/resources/assets/mchemistry/textures/item"
GUI_DIR = ROOT / "src/main/resources/assets/mchemistry/textures/gui/handbook"
DESKTOP = Path(os.path.expanduser("~/Desktop/MChemistry/手册材质"))

FONT_CANDIDATES = [
    "/System/Library/Fonts/STHeiti Medium.ttc",
    "/System/Library/Fonts/STHeiti Light.ttc",
    "/System/Library/Fonts/Hiragino Sans GB.ttc",
]


def font(size):
    for path in FONT_CANDIDATES:
        try:
            return ImageFont.truetype(path, size, index=0)
        except Exception:
            continue
    return ImageFont.load_default()


def dashed_rect(draw, x0, y0, x1, y1, fill, dash=8, width=2):
    """Draw a dashed rectangle outline."""
    gap = dash
    # top / bottom
    for sx in range(x0, x1, dash + gap):
        draw.line([(sx, y0), (min(sx + dash, x1), y0)], fill=fill, width=width)
        draw.line([(sx, y1), (min(sx + dash, x1), y1)], fill=fill, width=width)
    for sy in range(y0, y1, dash + gap):
        draw.line([(x0, sy), (x0, min(sy + dash, y1))], fill=fill, width=width)
        draw.line([(x1, sy), (x1, min(sy + dash, y1))], fill=fill, width=width)


def label(draw, xy, text, fnt, fill=(200, 40, 40)):
    draw.text(xy, text, font=fnt, fill=fill)


# --------------------------------------------------------------------------
# 1. item icon 16x16
# --------------------------------------------------------------------------
def gen_item_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # cover
    d.rectangle([1, 2, 14, 13], fill=(43, 58, 85, 255))
    d.rectangle([1, 2, 14, 2], fill=(200, 162, 74, 255))
    d.rectangle([1, 13, 14, 13], fill=(200, 162, 74, 255))
    d.rectangle([1, 2, 1, 13], fill=(200, 162, 74, 255))
    d.rectangle([14, 2, 14, 13], fill=(200, 162, 74, 255))
    # spine
    d.rectangle([3, 2, 4, 13], fill=(30, 42, 66, 255))
    # page edge
    d.rectangle([13, 4, 14, 11], fill=(235, 228, 204, 255))
    # flask glyph
    f = (159, 216, 240, 255)
    b = (88, 128, 160, 255)
    d.rectangle([7, 4, 9, 5], fill=f)
    d.point((7, 5), fill=f)
    d.point((9, 5), fill=f)
    d.polygon([(6, 6), (10, 6), (10, 7), (8, 11), (6, 7)], fill=f)
    d.rectangle([6, 11, 10, 11], fill=b)
    d.rectangle([6, 11, 10, 11], outline=b)
    img.save(ITEM_DIR / "handbook.png")


# --------------------------------------------------------------------------
# 2. cover background 256x200
# --------------------------------------------------------------------------
def gen_cover():
    img = Image.new("RGBA", (256, 200), (27, 42, 68, 255))
    d = ImageDraw.Draw(img)
    # outer frame
    d.rectangle([2, 2, 253, 197], outline=(138, 109, 59, 255), width=2)
    d.rectangle([6, 6, 249, 193], outline=(96, 116, 146, 255), width=1)
    # title plate
    d.rectangle([64, 8, 192, 30], fill=(38, 56, 92, 255))
    d.rectangle([64, 8, 192, 30], outline=(184, 148, 74, 255), width=1)
    # category cells
    cells = [
        (40, 44), (144, 44), (40, 78), (144, 78),
        (40, 112), (144, 112), (40, 146),
    ]
    for (x, y) in cells:
        d.rectangle([x, y, x + 76, y + 28], fill=(34, 51, 82, 255))
        d.rectangle([x, y, x + 76, y + 28], outline=(110, 127, 155, 255), width=1)
    # footer band
    d.line([(20, 186), (236, 186)], fill=(96, 116, 146, 255), width=1)
    img.save(GUI_DIR / "cover.png")


# --------------------------------------------------------------------------
# 3. page background 256x200
# --------------------------------------------------------------------------
def gen_page():
    img = Image.new("RGBA", (256, 200), (237, 228, 204, 255))
    d = ImageDraw.Draw(img)
    # header band
    d.rectangle([0, 0, 255, 30], fill=(226, 215, 187, 255))
    d.line([(0, 30), (255, 30)], fill=(203, 185, 143, 255), width=1)
    # outer frame
    d.rectangle([2, 2, 253, 197], outline=(138, 109, 59, 255), width=2)
    # search box slot
    d.rectangle([150, 7, 242, 23], fill=(255, 255, 255, 180))
    d.rectangle([150, 7, 242, 23], outline=(138, 109, 59, 255), width=1)
    # content area hint
    d.rectangle([20, 32, 236, 188], outline=(203, 185, 143, 255), width=1)
    img.save(GUI_DIR / "page.png")


# --------------------------------------------------------------------------
# 5. layout references (2x, 512x400) for the user
# --------------------------------------------------------------------------
def gen_layout_refs():
    DESKTOP.mkdir(parents=True, exist_ok=True)
    f_big = font(22)
    f_small = font(16)

    # cover layout
    img = Image.new("RGB", (512, 400), (36, 48, 70))
    d = ImageDraw.Draw(img)
    red = (230, 70, 60)
    label(d, (24, 8), "化学手册封面布局（cover.png  256x200，此处为 2 倍放大）", f_small, (240, 230, 200))
    dashed_rect(d, 128, 22, 384, 60, red, dash=10)
    label(d, (140, 28), "标题牌（深色，文字由代码绘制）", f_big, red)
    cells = [
        (80, 94), (288, 94), (80, 162), (288, 162),
        (80, 230), (288, 230), (80, 298),
    ]
    names = ["元素周期表", "固体物质", "液体物质", "气体物质", "实验仪器", "化学反应", "操作指南"]
    for (x, y), name in zip(cells, names):
        dashed_rect(d, x, y, x + 152, y + 56, red, dash=10)
        label(d, (x + 12, y + 10), f"{name}（格子：图标+文字）", f_big, red)
        label(d, (x + 12, y + 34), "图标用游戏内真实物品", f_small, (250, 200, 100))
    dashed_rect(d, 40, 376, 472, 390, red, dash=8)
    label(d, (48, 378), "页脚（版本号文字）", f_small, red)
    img.save(DESKTOP / "布局参考-封面.png")

    # page layout (list)
    img = Image.new("RGB", (512, 400), (226, 214, 186))
    d = ImageDraw.Draw(img)
    label(d, (24, 8), "内容页布局（page.png  256x200，此处为 2 倍放大）", f_small, (90, 70, 40))
    dashed_rect(d, 16, 22, 48, 54, red, dash=6)
    label(d, (20, 60), "返回按钮", f_small, red)
    dashed_rect(d, 60, 22, 250, 54, red, dash=8)
    label(d, (70, 28), "分类标题文字（代码绘制）", f_big, red)
    dashed_rect(d, 300, 20, 484, 52, red, dash=8)
    label(d, (310, 26), "搜索框（放大镜+文字）", f_big, red)
    dashed_rect(d, 40, 70, 472, 376, red, dash=10)
    label(d, (52, 78), "条目列表：每行 = 16x16 物品图标 + 名称 + 灰色副标题", f_big, red)
    for i in range(7):
        y = 104 + i * 40
        d.rectangle([56, y, 464, y + 34], outline=(150, 120, 70), width=1)
    dashed_rect(d, 480, 70, 492, 376, red, dash=6)
    label(d, (456, 384), "滚动条", f_small, red)
    img.save(DESKTOP / "布局参考-内容页.png")

    # entry layout
    img = Image.new("RGB", (512, 400), (226, 214, 186))
    d = ImageDraw.Draw(img)
    label(d, (24, 8), "物质详情页布局（page.png 同款背景，2 倍放大）", f_small, (90, 70, 40))
    dashed_rect(d, 16, 22, 48, 54, red, dash=6)
    label(d, (20, 60), "返回", f_small, red)
    dashed_rect(d, 60, 22, 380, 54, red, dash=8)
    label(d, (70, 28), "物质名称（右上方灰色为化学式）", f_big, red)
    dashed_rect(d, 40, 78, 104, 142, red, dash=6)
    label(d, (36, 146), "大图标 32x32", f_small, red)
    dashed_rect(d, 120, 76, 300, 314, red, dash=10)
    label(d, (132, 84), "信息字段（每行 14px）", f_big, red)
    fields = ["分子量", "密度", "熔点", "沸点", "毒性", "腐蚀性", "爆炸性", "pH", "外观/气味"]
    for i, f in enumerate(fields):
        label(d, (132, 106 + i * 28), f"{f}：数值（左灰标签，右深色数值）", f_small, red)
    dashed_rect(d, 312, 76, 472, 196, red, dash=10)
    label(d, (322, 84), "相关反应（点击进入反应页）", f_big, red)
    label(d, (322, 122), "滚轮可滚动，最多同时显示 2 条", f_small, red)
    img.save(DESKTOP / "布局参考-条目页.png")


def main():
    ITEM_DIR.mkdir(parents=True, exist_ok=True)
    GUI_DIR.mkdir(parents=True, exist_ok=True)
    gen_item_icon()
    gen_cover()
    gen_page()
    gen_layout_refs()
    print("ok ->", ITEM_DIR)
    print("ok ->", GUI_DIR)
    print("ok ->", DESKTOP)


if __name__ == "__main__":
    main()
