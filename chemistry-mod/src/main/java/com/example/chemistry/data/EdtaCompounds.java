package com.example.chemistry.data;

import java.util.List;

/** Explicit anhydrous formulas; hydrate variants are not silently treated as anhydrous salts. */
public final class EdtaCompounds {
    public record Compound(String id, String formula, String chinese, String english, int color,
            int sodium, int protons, String metal, double molarMass, double solubility) { }
    public static final List<Compound> ALL = List.of(
            new Compound("edta", "C10H16N2O8", "乙二胺四乙酸（EDTA）", "EDTA", 0xF2F2E8, 0, 4, "", 292.244, .05),
            new Compound("disodium_edta", "C10H14N2Na2O8", "EDTA二钠（无水）", "Disodium EDTA (anhydrous)", 0xF2F2E8, 2, 2, "", 336.208, 10),
            new Compound("tetrasodium_edta", "C10H12N2Na4O8", "EDTA四钠（无水）", "Tetrasodium EDTA (anhydrous)", 0xF2F2E8, 4, 0, "", 380.172, 50),
            new Compound("calcium_disodium_edta", "C10H12CaN2Na2O8", "EDTA钙二钠", "Calcium disodium EDTA", 0xE9EFE8, 2, 0, "calcium", 374.270, 20),
            new Compound("magnesium_disodium_edta", "C10H12MgN2Na2O8", "EDTA镁二钠", "Magnesium disodium EDTA", 0xE9EFE8, 2, 0, "magnesium", 358.497, 20),
            new Compound("copper_disodium_edta", "C10H12CuN2Na2O8", "EDTA铜二钠", "Copper disodium EDTA", 0x355EC0, 2, 0, "copper_ii", 397.738, 20),
            new Compound("ferrous_disodium_edta", "C10H12FeN2Na2O8", "EDTA亚铁二钠", "Iron(II) disodium EDTA", 0xBEC7A2, 2, 0, "iron_ii", 390.037, 20),
            new Compound("ferric_sodium_edta", "C10H12FeN2NaO8", "EDTA铁钠", "Iron(III) sodium EDTA", 0xBEB45B, 1, 0, "iron_iii", 367.047, 20));
    private EdtaCompounds() { }
}
