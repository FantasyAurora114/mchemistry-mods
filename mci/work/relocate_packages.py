"""Relocate all MCI classes from com.example.chemistry.* to com.example.mci.*
so the two mods do not split the same Java packages (JPMS module conflict).
References to the CORE mod's classes (com.example.chemistry.api / .item /
.registry.ModItems / .storage.ChemUnits / ...) are left untouched."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/java"

# MCI class simple names -> new package suffix under com.example.mci
MCI_CLASSES = {
    "ChemistryMod": "",
    "ChemistryModClient": "",
    "SynthesisTowerBlock": "block",
    "SynthesisTowerBlockEntity": "blockentity",
    "SynthesisTowerMenu": "menu",
    "SynthesisTowerScreen": "client",
    "ModClientEvents": "client",
    "GasCanisterItem": "item",
    "GasCanisterHelper": "item",
    "ChemGasTank": "storage",
    "TowerFluidTank": "storage",
    "ChemistryNetworking": "network",
    "MciSubstances": "data",
    "Ores": "data",
    "ModBlocks": "registry",
    "MciItems": "registry",
    "ModBlockEntities": "registry",
    "ModMenus": "registry",
    "ModCapabilities": "registry",
    "ModCreativeTabs": "registry",
}

new_pkg = {c: f"com.example.mci" + (f".{s}" if s else "") for c, s in MCI_CLASSES.items()}
old_full = {c: "com.example.chemistry" + (f".{s}" if s else "") + f".{c}"
            for c, s in MCI_CLASSES.items()}

changed = 0
for p in ROOT.rglob("*.java"):
    src = p.read_text()
    m = re.search(r"^package ([\w.]+);", src, re.M)
    if not m:
        continue
    is_mci_file = any(p.name == f"{c}.java" for c in MCI_CLASSES)
    if not is_mci_file:
        continue
    cls = next(c for c in MCI_CLASSES if p.name == f"{c}.java")
    # 1) own package declaration (idempotent: skip if already relocated)
    if not src.startswith(f"package {new_pkg[cls]};"):
        src = re.sub(r"^package [\w.]+;", f"package {new_pkg[cls]};", src, count=1)
    # 2) references to other MCI classes (imports + fully-qualified uses)
    for c, suffix in MCI_CLASSES.items():
        if c == cls:
            continue
        src = src.replace(old_full[c], new_pkg[c] + "." + c)
    p.write_text(src)
    changed += 1
    print("relocated", p.name, "->", new_pkg[cls])

print("files changed:", changed)
