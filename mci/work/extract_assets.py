"""Extract the ore + synthesis-tower assets from the complete jar into the
MCI project resources (namespace stays mchemistry)."""
import json
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_ores import INTERMEDIATES, METALS, ORES, VANILLA_ORES  # noqa: E402

SRC = Path("/tmp/mci_jar_extract")
DST = Path(__file__).resolve().parent.parent / "src/main/resources"
assets = SRC / "assets/mchemistry"
data = SRC / "data/mchemistry"
mcdata = SRC / "data/minecraft"

ore_ids = [o[0] for o in ORES]
vanilla_ids = [v[0] for v in VANILLA_ORES]
extra_ids = [m[0] for m in INTERMEDIATES] + [m[0] for m in METALS]
solid_ids = set(ore_ids + vanilla_ids + extra_ids)


def copy(p, rel):
    dst = DST / rel
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(p, dst)


count = 0
for sid in solid_ids:
    for prefix in ("solid", "open_solid", "loose"):
        n = f"{prefix}_{sid}" if prefix != "loose" else f"loose_{sid}"
        for sub in ("textures/item", "items", "models/item"):
            p = assets / sub / f"{n}.png" if "textures" in sub else assets / sub / f"{n}.json"
            if p.exists():
                copy(p, f"assets/mchemistry/{sub}/{p.name}")
                count += 1

for oid in ore_ids:
    for base in (f"ore_{oid}", f"deepslate_ore_{oid}"):
        for sub in ("textures/block", "models/block", "models/item", "items", "blockstates"):
            p = assets / sub / f"{base}.png" if "textures" in sub else assets / sub / f"{base}.json"
            if p.exists():
                copy(p, f"assets/mchemistry/{sub}/{p.name}")
                count += 1
        p = data / "loot_table/blocks" / f"{base}.json"
        if p.exists():
            copy(p, f"data/mchemistry/loot_table/blocks/{p.name}")
            count += 1
    for sub in ("worldgen/configured_feature", "worldgen/placed_feature"):
        p = data / sub / f"ore_{oid}.json"
        if p.exists():
            copy(p, f"data/mchemistry/{sub}/{p.name}")
            count += 1
    p = data / "neoforge/biome_modifier" / f"ore_{oid}.json"
    if p.exists():
        copy(p, f"data/mchemistry/neoforge/biome_modifier/{p.name}")
        count += 1

for v in VANILLA_ORES:
    for block in v[7]:
        p = mcdata / "loot_table/blocks" / f"{block}.json"
        if p.exists():
            copy(p, f"data/minecraft/loot_table/blocks/{p.name}")
            count += 1
for tag in ("mineable/pickaxe.json", "needs_iron_tool.json", "needs_diamond_tool.json"):
    p = mcdata / "tags/block" / tag
    if p.exists():
        copy(p, f"data/minecraft/tags/block/{tag}")
        count += 1

for p in list(assets.rglob("*synthesis_tower*")):
    if p.is_file():
        rel = str(p).split("/mchemistry/", 1)[1]
        copy(p, f"assets/mchemistry/{rel}")
        count += 1

for lang in ("zh_cn.json", "en_us.json"):
    d = json.loads((assets / "lang" / lang).read_text())
    keep = {}
    for k, v in d.items():
        if (k.startswith("block.mchemistry.ore_") or k.startswith("block.mchemistry.deepslate_ore_")
                or k.startswith("item.mchemistry.ore_") or k.startswith("item.mchemistry.deepslate_ore_")
                or k.startswith("item.mchemistry.solid_") or k.startswith("item.mchemistry.open_solid_")
                or k.startswith("item.mchemistry.loose_") or k.startswith("item.mchemistry.gas_canister")
                or k in ("block.mchemistry.synthesis_tower", "container.mchemistry.synthesis_tower")
                or k.startswith("itemGroup.mci.")):
            keep[k] = v
    for k in [k for k in keep if k.startswith("item.mchemistry.")
              and ("solid_" in k or "loose_" in k or "open_solid_" in k)]:
        if k.startswith("item.mchemistry.loose_"):
            sid = k[len("item.mchemistry.loose_"):]
        elif k.startswith("item.mchemistry.open_solid_"):
            sid = k[len("item.mchemistry.open_solid_"):]
        else:
            sid = k[len("item.mchemistry.solid_"):]
        if sid not in solid_ids:
            del keep[k]
    out = DST / "assets/mchemistry/lang" / lang
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(keep, ensure_ascii=False, indent=2) + "\n")
    print(lang, "keys:", len(keep))

print("copied files:", count)
