package com.example.chemistry.data;

import java.util.Map;

/**
 * Aqueous solutions: solvent is water, the listed solute crystallises out
 * when the water evaporates above 100 C (generated).
 */
public final class Solutions {

    public static final Map<String, String> SOLUTE = Map.ofEntries(
        Map.entry("copper_sulfate_solution", "copper_sulfate_anhydrous"),
        Map.entry("sodium_chloride_solution", "sodium_chloride"),
        Map.entry("sodium_carbonate_solution", "sodium_carbonate"),
        Map.entry("potassium_carbonate_solution", "potassium_carbonate"),
        Map.entry("sodium_hydroxide_solution", "sodium_hydroxide"),
        Map.entry("potassium_hydroxide_solution", "potassium_hydroxide"),
        Map.entry("iron_chloride_solution", "iron_iii_chloride"),
        Map.entry("potassium_permanganate_solution", "potassium_permanganate"),
        Map.entry("limewater_clear", "calcium_hydroxide"),
        Map.entry("limewater_cloudy", "calcium_carbonate")
    );

    public static String soluteOf(String liquidId) {
        return SOLUTE.get(liquidId);
    }

    private Solutions() {
    }
}
