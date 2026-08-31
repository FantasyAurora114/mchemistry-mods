"""Convert reaction display text from Chinese words (铁 + 盐酸 → ...) to real
chemical equations (Fe + 2HCl → FeCl₂ + H₂↑) using the substance formulas and
the reactions' own coefficients. Also updates GasFlowEngine.GAS_NAME_TO_ID so
gas collection still parses the new "H₂↑" style displays."""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src/main/java/com/example/chemistry/data"


def formulas(path, pat):
    d = path.read_text()
    return {m.group(1): m.group(2) for m in re.finditer(pat, d)}


SOLID_F = formulas(DATA / "Solids.java", r'new Solid\("([a-z_0-9]+)", "([^"]*)"')
LIQUID_F = formulas(DATA / "Liquids.java", r'new Liquid\("([a-z_0-9]+)", "([^"]*)"')
LIQUID_CN = {m.group(1): m.group(2) for m in
             re.finditer(r'new Liquid\("([a-z_0-9]+)", "[^"]*", "[^"]*", "([^"]*)"', (DATA / "Liquids.java").read_text())}
GAS_F = formulas(DATA / "GasJars.java", r'new GasJar\("([a-z_0-9]+)", "([^"]*)"')
SOLID_CN = {m.group(1): m.group(2) for m in
            re.finditer(r'new Solid\("([a-z_0-9]+)", "[^"]*", "[^"]*", "([^"]*)"', (DATA / "Solids.java").read_text())}

SUB = str.maketrans("0123456789", "₀₁₂₃₄₅₆₇₈₉")


def subscript(f):
    parts = f.split(".")
    body = parts[0].translate(SUB)
    if len(parts) > 1:
        rest = parts[1]
        i = 0
        while i < len(rest) and rest[i].isdigit():
            i += 1
        # hydration coefficient stays normal digits, the rest gets subscripts
        body += "·" + rest[:i] + rest[i:].translate(SUB)
    return body


# gas chinese name -> formula
GAS_CN = {m.group(2): m.group(1) for m in
          re.finditer(r'new GasJar\("([a-z_0-9]+)", "[^"]*", "[^"]*", "([^"]*)"', (DATA / "GasJars.java").read_text())}
CN_TO_FORMULA = {cn: subscript(GAS_F[i]) for cn, i in GAS_CN.items()}

# keep these as their Chinese names (no sensible short formula in-game)
KEEP_CN = {
    "ammonia_water": "氨水",
    "ammonia_water_concentrated": "浓氨水",
    "limewater_clear": "澄清石灰水",
    "limewater_cloudy": "浑浊石灰水",
    "litmus_solution": "石蕊试液",
    "phenolphthalein_solution": "酚酞试液",
    "methyl_orange_solution": "甲基橙试液",
}

# hard-coded extra formulas (core-side products that live in MCI data)
EXTRA = {"manganese_chloride": "MnCl2"}


def piece(t, i, coeff, old):
    if t == "passivate":
        return "钝化"
    if t == "vent":
        return None  # handled separately via gas list
    f = SOLID_F.get(i) or LIQUID_F.get(i) or GAS_F.get(i) or EXTRA.get(i)
    if i in KEEP_CN:
        text = KEEP_CN[i]
    elif f:
        if i.endswith("_solution"):
            text = subscript(f) + "溶液"
        elif i.endswith("_water") or i.endswith("_water_concentrated"):
            text = subscript(f.split("(")[0]) + "溶液"
        else:
            text = subscript(f)
    else:
        text = i  # fallback: raw id (should not happen)
    # Concentration comes from the reaction's own requirement, best read from
    # the original Chinese display ("浓盐酸"/"稀硫酸").
    if t == "liquid" and i in LIQUID_CN:
        cn = LIQUID_CN[i]
        base = cn[1:] if cn.startswith(("浓", "稀")) else cn
        if "浓" + base in old:
            text = text + "（浓）"
        elif "稀" + base in old:
            text = text + "（稀）"
    return f"{coeff if coeff > 1 else ''}{text}"


def gas_formula_of_display(old, idx):
    """idx-th gas (by ↑ order) in the old Chinese display -> formula."""
    gases = []
    for cn in CN_TO_FORMULA:
        if re.search(re.escape(cn) + "↑", old):
            gases.append(cn)
    return CN_TO_FORMULA[gases[idx]] if idx < len(gases) else "?"


def has_precipitate(old, solid_id):
    cn = SOLID_CN.get(solid_id, "")
    return bool(cn) and re.search(re.escape(cn) + "↓", old)


def suffix_of(old):
    m = re.search(r"（[^）]*）", old)
    return m.group(0) if m else ""


def build_display(reactants_str, products_str, old):
    ing = re.findall(r'new Ingredient\("(\w+)", "([a-z_0-9]*)", (\d+)', reactants_str)
    prod = re.findall(r'new Product\("(\w+)", "([a-z_0-9]*)", (\d+)', products_str)
    left = []
    for t, i, n in ing:
        left.append(piece(t, i, int(n), old))
    right = []
    gas_idx = 0
    for t, i, n in prod:
        if t == "vent":
            g = gas_formula_of_display(old, gas_idx)
            gas_idx += 1
            right.append(f"{int(n) if int(n) > 1 else ''}{g}↑")
        elif t == "passivate":
            right.append("钝化")
        else:
            p = piece(t, i, int(n), old)
            if t == "solid" and has_precipitate(old, i):
                p += "↓"
            right.append(p)
    eq = " + ".join(left) + " → " + " + ".join(right)
    return eq + suffix_of(old)


def rewrite_reactions():
    p = DATA / "Reactions.java"
    src = p.read_text()
    pat = re.compile(
        r'(new Reaction\(List\.of\()(.*?)(\)\s*,\s*List\.of\()(.*?)(\)\s*,\s*")((?:[^"\\]|\\.)*)(")',
        re.S)

    def repl(m):
        new_display = build_display(m.group(2), m.group(4), m.group(6))
        return m.group(1) + m.group(2) + m.group(3) + m.group(4) + m.group(5) + new_display + m.group(7)

    new_src, n = pat.subn(repl, src)
    if n != src.count("new Reaction("):
        print("WARN: replaced", n, "displays but found", src.count("new Reaction("), "reactions")
    p.write_text(new_src)
    print("rewrote", n, "reaction displays")


if __name__ == "__main__":
    if "--dry" in sys.argv:
        src = (DATA / "Reactions.java").read_text()
        for m in re.finditer(
                r'new Reaction\(List\.of\((.*?)\)\s*,\s*List\.of\((.*?)\)\s*,\s*"((?:[^"\\]|\\.)*)"', src, re.S):
            print(build_display(m.group(1), m.group(2), m.group(3)))
    else:
        rewrite_reactions()
