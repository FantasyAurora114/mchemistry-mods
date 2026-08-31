"""Generate MCI Java data: MciSubstances (ChemistryAPI.registerSolid/
registerReaction calls) and Ores (runtime block registration data)."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_ores import CHAINS, INTERMEDIATES, METALS, ORES, VANILLA_ORES, hexc

JAVA = Path(__file__).resolve().parent.parent / "src/main/java/com/example/mci"
JAVA.mkdir(parents=True, exist_ok=True)

# Reuse the core mod's equation generator (substance formulas come from the
# core data classes; ore formulas are injected below).
sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "chemistry-mod" / "work"))
import gen_equations as ge

for o in ORES:
    ge.EXTRA[o[0]] = o[3]
for v in VANILLA_ORES:
    ge.EXTRA[v[0]] = v[3]
for m in INTERMEDIATES + METALS:
    ge.EXTRA[m[0]] = m[3]


def solid_lines():
    out = []
    for o in ORES:
        oid, cn, en, formula, color = o[0], o[1], o[2], o[3], o[4]
        out.append(f'            new SolidSubstance("{oid}", "{formula}", "{en}", "{cn}", {hexc(color)}, false, ""),')
    for v in VANILLA_ORES:
        vid, cn, en, formula, color = v[0], v[1], v[2], v[3], v[4]
        out.append(f'            new SolidSubstance("{vid}", "{formula}", "{en}", "{cn}", {hexc(color)}, false, ""),')
    for m in INTERMEDIATES:
        mid, cn, en, formula, color = m[0], m[1], m[2], m[3], m[4]
        out.append(f'            new SolidSubstance("{mid}", "{formula}", "{en}", "{cn}", {hexc(color)}, true, ""),')
    for m in METALS:
        mid, cn, en, formula, color = m[0], m[1], m[2], m[3], m[4]
        out.append(f'            new SolidSubstance("{mid}", "{formula}", "{en}", "{cn}", {hexc(color)}, false, ""),')
    return out


def reaction_lines():
    out = []
    for oid, *_ in ORES:
        for chain in CHAINS[oid]:
            reactants, products, display, speed, temp, catalyst, pressure = chain
            ing = ", ".join(f'new Ingredient("{t}", "{i}", {n})' for t, i, n in reactants)
            prod = ", ".join(f'new Product("{t}", "{i}", {n})' for t, i, n in products)
            eq = ge.build_display(ing, prod, display)
            out.append(f'            new Reaction(List.of({ing}), List.of({prod}), "{eq}", '
                       f'{speed}, {temp}, "{catalyst}", {pressure}),')
    for v in VANILLA_ORES:
        for chain in v[8]:
            reactants, products, display, speed, temp, catalyst, pressure = chain
            ing = ", ".join(f'new Ingredient("{t}", "{i}", {n})' for t, i, n in reactants)
            prod = ", ".join(f'new Product("{t}", "{i}", {n})' for t, i, n in products)
            eq = ge.build_display(ing, prod, display)
            out.append(f'            new Reaction(List.of({ing}), List.of({prod}), "{eq}", '
                       f'{speed}, {temp}, "{catalyst}", {pressure}),')
    return out


def write_mci_substances():
    solids = "\n".join(solid_lines())
    if solids.rstrip().endswith(","):
        solids = solids.rstrip()[:-1]
    reactions = "\n".join(reaction_lines())
    if reactions.rstrip().endswith(","):
        reactions = reactions.rstrip()[:-1]
    src = f"""package com.example.mci.data;

import java.util.List;

import com.example.chemistry.api.ChemistryAPI;
import com.example.chemistry.api.Substances.SolidSubstance;
import com.example.chemistry.data.Reactions.Ingredient;
import com.example.chemistry.data.Reactions.Product;
import com.example.chemistry.data.Reactions.Reaction;

/** Generated MCI substance/reaction registrations (see work/gen_mci_data.py). */
public final class MciSubstances {{

    private MciSubstances() {{
    }}

    /** Called from the MCI mod constructor (after MChemistry seeds the API). */
    public static void registerAll() {{
        for (Reaction r : REACTIONS) {{
            ChemistryAPI.registerReaction(r);
        }}
    }}

    public static final List<SolidSubstance> SOLIDS = List.of(
{solids}
    );

    public static final List<Reaction> REACTIONS = List.of(
{reactions}
    );
}}
"""
    (JAVA / "data" / "MciSubstances.java").write_text(src)
    print("MciSubstances.java written,", len(solid_lines()), "solids,", len(reaction_lines()), "reactions")


def write_ores():
    lines = []
    for o in ORES:
        oid, hard = o[0], o[6]
        lines.append(f'        new OreDef("{oid}", {hard}F),')
    joined = "\n".join(lines).rstrip()
    if joined.endswith(","):
        joined = joined[:-1]
    src = f"""package com.example.mci.data;

import java.util.List;

/** Generated ore table (see work/gen_mci_data.py). */
public final class Ores {{

    public record OreDef(String id, float hardness) {{
    }}

    public static final List<OreDef> ALL = List.of(
{joined}
    );

    private Ores() {{
    }}
}}
"""
    (JAVA / "data" / "Ores.java").write_text(src)
    print("Ores.java written,", len(ORES), "ores")


if __name__ == "__main__":
    write_mci_substances()
    write_ores()
