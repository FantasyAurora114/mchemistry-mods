package com.example.chemistry.solution;

import java.util.Map;

/** Bounded ideal-concentration solver for a 1:1 salt. Pure solid activity is one. */
public final class SolubilityEquilibrium {
    public static SpeciesInventory solve(SpeciesInventory inventory, String solid,
            String cation, String anion, double ksp, double litres) {
        if (!Double.isFinite(ksp) || ksp <= 0 || !Double.isFinite(litres) || litres < 0) {
            throw new IllegalArgumentException("Invalid solubility equilibrium conditions");
        }
        if (litres == 0 || inventory.amount("water") <= 0) return inventory;
        double previous = inventory.amount(solid);
        double totalCation = inventory.amount(cation) + previous;
        double totalAnion = inventory.amount(anion) + previous;
        double target = 0;
        if (totalCation * totalAnion > ksp * litres * litres) {
            double lo = 0, hi = Math.min(totalCation, totalAnion);
            for (int i = 0; i < 80; i++) {
                double mid = lo + (hi - lo) * .5;
                if ((totalCation - mid) * (totalAnion - mid) > ksp * litres * litres) lo = mid;
                else hi = mid;
            }
            target = lo + (hi - lo) * .5;
        }
        Map<String, Integer> dissolved = Map.of(cation, 1, anion, 1);
        Map<String, Integer> residue = Map.of(solid, 1);
        double change = target - previous;
        if (change > 0) {
            change = Math.min(change, Math.min(inventory.amount(cation), inventory.amount(anion)));
            return inventory.transform(dissolved, residue, change);
        }
        return change < 0 ? inventory.transform(residue, dissolved, -change) : inventory;
    }

    private SolubilityEquilibrium() {}
}
