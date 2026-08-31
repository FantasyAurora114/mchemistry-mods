from PIL import Image
import os

src = "/Users/apple/Documents/Codex/2026-08-21/wo/outputs/chemistry-mod" \
      "/src/main/resources/assets/mchemistry/textures/item/handbook.png"
dest_dir = os.path.expanduser("~/Desktop/MChemistry/手册材质")

icon = Image.open(src).convert("RGBA")
icon.save(os.path.join(dest_dir, "手册图标.png"))

# 8x preview on a soft dark panel so the transparent outline is visible
scale = 8
big = icon.resize((16 * scale, 16 * scale), Image.NEAREST)
pad = 24
canvas = Image.new("RGBA", (16 * scale + pad * 2, 16 * scale + pad * 2), (36, 42, 54, 255))
canvas.paste(big, (pad, pad), big)
canvas.save(os.path.join(dest_dir, "手册图标_预览.png"))

print("saved ->", os.path.join(dest_dir, "手册图标.png"))
print("saved ->", os.path.join(dest_dir, "手册图标_预览.png"))
