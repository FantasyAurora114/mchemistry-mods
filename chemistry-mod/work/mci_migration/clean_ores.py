"""Remove every generated ore/intermediate/metal entry from the Java data
files so gen_ores.py can regenerate them exactly once (the earlier runs
inserted the ore block multiple times)."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_ores import CHAINS, INTERMEDIATES, METALS, ORES, VANILLA_ORES  # noqa: E402

JAVA = Path(__file__).resolve().parent.parent / "src/main/java/com/example/chemistry"
IDS = ({o[0] for o in ORES} | {v[0] for v in VANILLA_ORES}
       | {m[0] for m in METALS} | {m[0] for m in INTERMEDIATES})
ORE_DISPLAYS = ({chain[2] for o in ORES for chain in CHAINS[o[0]]}
                | {chain[2] for v in VANILLA_ORES for chain in v[8]})


def filter_lines(path, pred):
    src = path.read_text()
    lines = src.splitlines(keepends=True)
    kept = [ln for ln in lines if not pred(ln)]
    path.write_text("".join(kept))
    print(f"{path.name}: removed {len(lines) - len(kept)} lines")


def drop_lone_commas(path):
    src = path.read_text()
    lines = src.splitlines(keepends=True)
    kept = [ln for ln in lines if ln.strip() != ","]
    if len(kept) != len(lines):
        path.write_text("".join(kept))
        print(f"{path.name}: dropped {len(lines) - len(kept)} lone comma lines")


def strip_anchor_commas(path, marker):
    """After the generated rows are removed the anchor row becomes the LAST
    list element, so any accumulated trailing commas on it must go (Java does
    not allow a trailing comma in List.of)."""
    src = path.read_text()
    lines = src.splitlines(keepends=True)
    out = [ln.rstrip("\n").rstrip(",") + "\n" if marker in ln else ln for ln in lines]
    path.write_text("".join(out))
    print(f"{path.name}: stripped trailing commas on anchor rows")


# Solids.java: drop the generated new Solid("<ore/...>", ...) rows.
filter_lines(JAVA / "data" / "Solids.java",
             lambda ln: any(f'new Solid("{i}",' in ln for i in IDS))
drop_lone_commas(JAVA / "data" / "Solids.java")
strip_anchor_commas(JAVA / "data" / "Solids.java", 'new Solid("calcium_phosphate"')

# ChemicalInfoProvider: drop CHEMICALS / MOLAR_MASS / BOILING_POINT rows.
filter_lines(JAVA / "data" / "ChemicalInfoProvider.java",
             lambda ln: any(f'"solid_{i}"' in ln for i in IDS))

# Reactions.java: drop generated ore-processing reactions by their display text
# (some use only pre-existing substances like Mg/MgO/white phosphorus, so an
# id-based filter cannot see them).
def is_ore_reaction(ln):
    return "new Reaction(" in ln and any(d in ln for d in ORE_DISPLAYS)


filter_lines(JAVA / "data" / "Reactions.java", is_ore_reaction)
drop_lone_commas(JAVA / "data" / "Reactions.java")
strip_anchor_commas(JAVA / "data" / "Reactions.java", "氮气 + 氧气 → 一氧化氮")

# Ores.java is overwritten by the generator each run; remove the stale copy.
ores_java = JAVA / "data" / "Ores.java"
if ores_java.exists():
    ores_java.unlink()
    print("Ores.java removed")

print("cleanup done")
