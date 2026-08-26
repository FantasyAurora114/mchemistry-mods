"""Full-mod consistency audit: registrations vs assets, lang keys, models,
textures, duplicates, reaction ids, molar masses."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java/com/example/chemistry"
ASSETS = ROOT / "src/main/resources/assets/chemistry"

problems = []


def add(cat, msg):
    problems.append((cat, msg))


# ---- 1. Collect registered item / block ids from Java ----
java = {p: p.read_text() for p in SRC.rglob("*.java")}
all_java = "\n".join(java.values())

item_ids = set(re.findall(r'register(?:Simple)?Item\("([a-z0-9_]+)"', all_java))
item_ids |= set(re.findall(r'register(?:Simple)?BlockItem\("([a-z0-9_]+)"', all_java))
item_ids -= {"element_", "loose_", "liquid_", "solid_", "open_"}
block_ids = set(re.findall(r'registerBlock\("([^"]+)"', all_java))
items_json = {p.stem for p in (ASSETS / "items").glob("*.json")}

# ---- 2. Every registered item needs an items/<id>.json ----
for iid in sorted(item_ids):
    if iid not in items_json:
        add("item-asset", f"item '{iid}' registered but no items/{iid}.json")

# ---- 3. Item model targets exist ----
for p in (ASSETS / "items").glob("*.json"):
    d = json.loads(p.read_text())
    refs = []

    def walk(node):
        if isinstance(node, dict):
            for k, v in node.items():
                if k == "model" and isinstance(v, str) and v.startswith("chemistry:"):
                    refs.append(v)
                else:
                    walk(v)
        elif isinstance(node, list):
            for v in node:
                walk(v)

    walk(d)
    if not refs:
        add("item-model", f"items/{p.stem}.json has no model reference")
        continue
    for ref in refs:
        path = ref.split(":", 1)[1]
        if not (ASSETS / "models" / f"{path}.json").exists():
            add("item-model", f"items/{p.stem}.json -> missing models/{path}.json")

# ---- 4. Blockstate files exist for every block ----
bs_dir = ASSETS / "blockstates"
for bid in sorted(block_ids):
    if not (bs_dir / f"{bid}.json").exists():
        add("blockstate", f"block '{bid}' has no blockstate file")

# ---- 5. Blockstate model variants exist ----
for p in bs_dir.glob("*.json"):
    d = json.loads(p.read_text())
    for variant in d.get("variants", {}).values():
        model = variant.get("model")
        if not model:
            continue
        ns, path = model.split(":", 1)
        if ns != "chemistry" or "builtin" in path:
            continue
        if not (ASSETS / "models" / f"{path}.json").exists():
            add("blockstate-model", f"{p.stem}: variant -> missing models/{path}.json")

# ---- 6. Model texture references exist ----
for p in (ASSETS / "models").rglob("*.json"):
    d = json.loads(p.read_text())
    for tex in (d.get("textures") or {}).values():
        if not str(tex).startswith("chemistry:"):
            continue
        path = str(tex).split(":", 1)[1]
        if not (ASSETS / "textures" / f"{path}.png").exists():
            add("model-texture", f"{p}: missing textures/{path}.png")

# ---- 7. Lang: every translatable "chemistry.*" key used in Java ----
lang_zh = json.loads((ASSETS / "lang/zh_cn.json").read_text())
used = set(re.findall(r'translatable\("(chemistry\.[^"]+)"', all_java))
used |= set(re.findall(r'Component\.translatable\("(chemistry\.[^"]+)"', all_java))
for key in sorted(used):
    if key not in lang_zh:
        add("lang", f"missing zh_cn key: {key}")

# ---- 8. Duplicate registrations ----
instruments = set(re.findall(r'"([a-z_]+)"', (SRC / "data/Instruments.java").read_text()))
dup = item_ids & instruments
for d in sorted(dup):
    add("dup", f"item '{d}' also in Instruments.ALL")

# ---- 9. Reaction ids vs substances ----
reactions_src = (SRC / "data/Reactions.java").read_text()
solids = set(re.findall(r'new Solid\("([^"]+)"', (SRC / "data/Solids.java").read_text()))
liquids = set(re.findall(r'new Liquid\("([^"]+)"', (SRC / "data/Liquids.java").read_text()))
gases = set(re.findall(r'new GasJar\("([^"]+)"', (SRC / "data/GasJars.java").read_text()))
pools = {"solid": solids, "liquid": liquids, "gas": gases}
for m in re.finditer(r'(?:new (?:Ingredient|Product))\("(\w+)", "([^"]+)"', reactions_src):
    t, i = m.groups()
    if t in ("vent", "passivate") or i == "":
        continue
    if i not in pools.get(t, set()):
        add("reaction", f"unknown {t} id '{i}' in reactions")

# ---- 10. Molar mass / info coverage ----
prov = (SRC / "data/ChemicalInfoProvider.java").read_text()
for t, pool in (("solid", solids), ("liquid", liquids), ("gas", gases)):
    for i in pool:
        if f'"{t}_{i}"' not in prov:
            add("info", f"no info/molar entry for {t}_{i}")

# ---- 11. Open-product chains (openProduct ids exist) ----
for m in re.finditer(r'new Solid\("([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)", 0x[0-9A-Fa-f]+, \w+, "([^"]*)"\)', (SRC / "data/Solids.java").read_text()):
    sid, prod = m.group(1), m.group(5)
    if prod and prod not in solids:
        add("open-product", f"solid '{sid}' openProduct '{prod}' missing")
for m in re.finditer(r'new Liquid\("([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)", 0x[0-9A-Fa-f]+, "([^"]*)"\)', (SRC / "data/Liquids.java").read_text()):
    lid, prod = m.group(1), m.group(5)
    if prod and prod not in liquids:
        add("open-product", f"liquid '{lid}' openProduct '{prod}' missing")

# ---- 12. Item models referenced by lang-free item json that use layer0 textures ----
for p in (ASSETS / "models/item").glob("*.json"):
    d = json.loads(p.read_text())
    if d.get("parent") == "minecraft:item/generated":
        tex = (d.get("textures") or {}).get("layer0", "")
        if str(tex).startswith("chemistry:"):
            path = str(tex).split(":", 1)[1]
            if not (ASSETS / "textures" / f"{path}.png").exists():
                add("item-texture", f"models/item/{p.stem}.json -> missing {path}.png")

print(f"total problems: {len(problems)}")
for cat, msg in sorted(problems):
    print(f"[{cat}] {msg}")
