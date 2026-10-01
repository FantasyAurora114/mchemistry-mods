package com.example.chemistry.data;

import java.util.Map;

/**
 * Aqueous solutions: solvent is water, the listed solute crystallises out
 * when the water evaporates above 100 C (generated).
 */
public final class Solutions {

    public static final Map<String, String> SOLUTE = Map.ofEntries(
        Map.entry("iodine_water", "iodine"),
        Map.entry("nickel_hydroxide_solution","nickel_hydroxide"),
        Map.entry("zinc_hydroxide_solution","zinc_hydroxide"),
        Map.entry("copper_sulfate_solution", "copper_sulfate_anhydrous"),
        Map.entry("sodium_chloride_solution", "sodium_chloride"),
        Map.entry("silver_chloride_solution", "silver_chloride"),
        Map.entry("calcium_oxalate_solution", "calcium_oxalate"),
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
        String known = SOLUTE.get(liquidId);
        if (known != null) return known;
        if (!liquidId.endsWith("_solution")) return null;
        String base = liquidId.substring(0, liquidId.length() - "_solution".length());
        return com.example.chemistry.AqueousSolubility.hasCurve(base) ? base : null;
    }

    /** All nonvolatile aqueous solutes supported by the vessel system. */
    public static java.util.Set<String> allIds() {
        java.util.Set<String> ids = new java.util.TreeSet<>(SOLUTE.keySet());
        for (String solid : com.example.chemistry.AqueousSolubility.solidIds())
            ids.add(com.example.chemistry.AqueousSolubility.solutionId(solid));
        return ids;
    }

    public static boolean isStockReagent(String id) {
        return Liquids.ALL.stream().anyMatch(liquid -> liquid.id().equals(id));
    }

    /** Gameplay defaults, not measured concentrations. Keep them in one place. */
    public static double stockFraction(String id) {
        if(com.example.chemistry.solution.BatchChemistry.indicator(id))return .001;
        if(id.equals("dimethylglyoxime_solution"))return .0003;
        if(id.equals("lead_iodide_solution"))return .0005;
        if(id.equals("calcium_sulfate_solution"))return .001;
        double nominal = switch (id) {
            case "iodine_water" -> .0003;
            case "edta_solution" -> .0002;
            case "disodium_edta_solution" -> .02;
            case "tetrasodium_edta_solution" -> .10;
            case "calcium_disodium_edta_solution", "magnesium_disodium_edta_solution", "copper_disodium_edta_solution", "ferrous_disodium_edta_solution", "ferric_sodium_edta_solution" -> .05;
            case "sodium_hydroxide_solution", "potassium_hydroxide_solution" -> 0.15;
            case "potassium_permanganate_solution" -> 0.02;
            case "limewater_clear" -> 0.001;
            case "limewater_cloudy" -> 0.01;
            default -> 0.10;
        };
        double density = Math.max(1.0, ChemicalInfoProvider.densityOfLiquid(id));
        // A nonnegative additive solute volume must agree with the stock density.
        return Math.max(nominal, 1.0 - 1.0 / density + 0.01);
    }

    public static double soluteMlPerGram(String id) {
        double fraction = stockFraction(id);
        return Math.max(0.0, (1.0 / ChemicalInfoProvider.densityOfLiquid(id)
                - (1.0 - fraction)) / fraction);
    }

    private Solutions() {
    }
}
