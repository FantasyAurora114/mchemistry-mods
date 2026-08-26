"""Convert the user's Blockbench models (washing bottle, glass tubes) into
Java block models. Bakes element rotations (90 deg) into axis-aligned boxes."""
import base64
import io
import json
import math
from pathlib import Path

from PIL import Image

DESK = Path("/Users/apple/Desktop")
ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/chemistry"
TEX = ROOT / "textures/block"
MODEL = ROOT / "models/block"


def load_bb(name):
    return json.load(open(DESK / name))


def extract_textures(bb, names):
    """names: map texture index -> output filename."""
    out = {}
    texs = bb.get("textures", [])
    if isinstance(texs, dict):
        texs = list(texs.values())
    for i, t in enumerate(texs):
        if i not in names:
            continue
        src = t.get("source", "") if isinstance(t, dict) else ""
        if str(src).startswith("data:image/png;base64,"):
            im = Image.open(io.BytesIO(base64.b64decode(src.split("base64,", 1)[1])))
            im.save(TEX / names[i])
            out[i] = names[i].replace(".png", "")
    return out


def rot_x(p, o, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    return [o[0] + x, o[1] + y * c - z * s, o[2] + y * s + z * c]


def bake_rot90(elem):
    """Rotate element by [90,0,0] about origin; return axis-aligned box."""
    o = elem["origin"]
    pts = []
    for x in (elem["from"][0], elem["to"][0]):
        for y in (elem["from"][1], elem["to"][1]):
            for z in (elem["from"][2], elem["to"][2]):
                pts.append(rot_x([x, y, z], o, 90))
    frm = [min(p[i] for p in pts) for i in range(3)]
    to = [max(p[i] for p in pts) for i in range(3)]
    return frm, to


# +X->east -X->west +Y->up -Y->down +Z->south -Z->north
DIR_TO_FACE = {(1, 0, 0): "east", (-1, 0, 0): "west", (0, 1, 0): "up",
               (0, -1, 0): "down", (0, 0, 1): "south", (0, 0, -1): "north"}
FACE_NORMAL = {v: k for k, v in DIR_TO_FACE.items()}


def mapped_face(old_face, deg):
    """For a 90-deg X rotation, map an original face name to the new one."""
    n = FACE_NORMAL[old_face]
    new = rot_x([n[0], n[1], n[2]], [0, 0, 0], deg)
    key = tuple(round(v) for v in new)
    return DIR_TO_FACE.get(key, old_face)


def emit_element(elem, tex_map, shift=(0, 0, 0), bake_rot=False):
    if bake_rot and elem.get("rotation") == [90, 0, 0]:
        frm, to = bake_rot90(elem)
        faces = {}
        old_faces = elem.get("faces") or {}
        for old_dir in old_faces:
            new_dir = mapped_face(old_dir, 90)
            faces[new_dir] = dict(old_faces[old_dir])
    else:
        frm, to = elem["from"], elem["to"]
        faces = dict(elem.get("faces") or {})
    out = {
        "from": [round(frm[i] + shift[i], 3) for i in range(3)],
        "to": [round(to[i] + shift[i], 3) for i in range(3)],
        "shade": False,
        "faces": {},
    }
    for d, f in faces.items():
        entry = {"texture": tex_map[f["texture"]]}
        if "uv" in f:
            entry["uv"] = f["uv"]
        if f.get("rotation"):
            entry["rotation"] = f["rotation"]
        out["faces"][d] = entry
    return out


def write_model(path, elements, tex_names):
    textures = {name.split("/")[-1]: f"chemistry:block/{name}" for name in tex_names}
    data = {"ambientocclusion": False, "particle": textures.get("wash_bottle_body"),
            "textures": textures, "elements": elements}
    (MODEL / path).write_text(json.dumps(data, ensure_ascii=False))


# ---- Washing bottle (empty): 3 elements, no rotations ----
bb = load_bb("洗气瓶.bbmodel")
tex_map = extract_textures(bb, {
    1: "wash_bottle_top.png", 2: "wash_bottle_body.png",
    3: "wash_bottle_neck.png", 4: "glass_tube_arm.png",
    5: "glass_tube_inner.png",
})
elems_empty = [emit_element(e, tex_map) for e in bb["elements"]]
write_model("wash_bottle.json", elems_empty, ["wash_bottle_top", "wash_bottle_body",
                                              "wash_bottle_neck"])

# ---- Washing bottle (assembled): bake the two 90-deg arms ----
bb2 = load_bb("洗气瓶（插满）.bbmodel")
tex_map2 = extract_textures(bb2, {
    1: "wash_bottle_top.png", 2: "wash_bottle_body.png",
    3: "wash_bottle_neck.png", 4: "glass_tube_arm.png",
    5: "glass_tube_inner.png",
})
elems_full = [emit_element(e, tex_map2, bake_rot=True) for e in bb2["elements"]]
write_model("wash_bottle_full.json", elems_full, ["wash_bottle_top", "wash_bottle_body",
                                                  "wash_bottle_neck", "glass_tube_arm",
                                                  "glass_tube_inner"])

# ---- Glass tubes: bake arm, shift stopper to origin, swing arm to -X ----
def convert_tube(src, dst, stem_y_from):
    bb3 = load_bb(src)
    tex_map3 = extract_textures(bb3, {0: "glass_tube_arm.png", 1: "glass_tube_inner.png"})
    # elem 0 = arm (rot 90), elem 1 = vertical stem
    arm_frm, arm_to = bake_rot90(bb3["elements"][0])
    # Center on the stem (x 8.5, z 7.75), put the stopper plane at y 0,
    # then swing the arm from -Z to -X: (x, z) -> (z, -x).
    def center_swing(p):
        xc = p[0] - 8.5
        yc = p[1] - 9.0
        zc = p[2] - 7.75
        return [zc, yc, -xc]
    a = center_swing(arm_frm)
    b = center_swing(arm_to)
    arm_frm = [min(a[0], b[0]), min(a[1], b[1]), min(a[2], b[2])]
    arm_to = [max(a[0], b[0]), max(a[1], b[1]), max(a[2], b[2])]
    arm = {
        "from": [round(arm_frm[0], 3), round(arm_frm[1], 3), round(arm_frm[2], 3)],
        "to": [round(arm_to[0], 3), round(arm_to[1], 3), round(arm_to[2], 3)],
        "shade": False,
        "faces": {},
    }
    for d, f in (bb3["elements"][0].get("faces") or {}).items():
        entry = {"texture": tex_map3[f["texture"]]}
        if "uv" in f:
            entry["uv"] = f["uv"]
        if f.get("rotation"):
            entry["rotation"] = f["rotation"]
        arm["faces"][d] = entry
    stem = bb3["elements"][1]
    stem = {
        "from": [stem["from"][0] - 8.5, stem_y_from, stem["from"][2] - 7.75],
        "to": [stem["to"][0] - 8.5, 0.0, stem["to"][2] - 7.75],
        "shade": False,
        "faces": {},
    }
    for d, f in (bb3["elements"][1].get("faces") or {}).items():
        entry = {"texture": tex_map3[f["texture"]]}
        if "uv" in f:
            entry["uv"] = f["uv"]
        if f.get("rotation"):
            entry["rotation"] = f["rotation"]
        stem["faces"][d] = entry
    write_model(dst, [arm, stem], ["glass_tube_arm", "glass_tube_inner"])


convert_tube("新90度玻璃导管.bbmodel", "glass_tube_right_angle.json", -2.0)
convert_tube("新90度长玻璃导管.bbmodel", "glass_tube_right_angle_long.json", -5.0)

# ---- Bottle water column (排水法集气): translucent blue, scaled in the renderer ----
im = Image.new("RGBA", (16, 16), (0x8F, 0xC8, 0xE8, 210))
im.save(TEX / "bottle_water.png")
water_model = {
    "ambientocclusion": False,
    "particle": "bottle_water",
    "textures": {"bottle_water": "chemistry:block/bottle_water"},
    "elements": [{
        "from": [6.8, 0, 6.8],
        "to": [10.2, 5.5, 10.2],
        "shade": False,
        "faces": {
            d: {"texture": "bottle_water", "uv": [0, 0, 16, 16]}
            for d in ("north", "south", "east", "west", "up", "down")
        },
    }],
}
(MODEL / "bottle_water.json").write_text(json.dumps(water_model, ensure_ascii=False))

print("OK: washing bottle + glass tubes + bottle water written")
