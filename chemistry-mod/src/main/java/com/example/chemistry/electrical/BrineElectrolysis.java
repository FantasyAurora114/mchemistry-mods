package com.example.chemistry.electrical;

import com.example.chemistry.item.LabVesselItem;
import net.minecraft.world.item.ItemStack;

/** Ideal separated chlor-alkali cell; dissolved masses are grams, not solution volume. */
public final class BrineElectrolysis {
    public static final double SALT_MOLAR = 58.44;
    public static final double BASE_MOLAR = 39.997;
    // Gameplay boundary: dilute chloride selectivity and mixed-gas evolution are not simulated.
    public static final double MIN_SALT_WATER_RATIO = 0.20;
    public record Result(double charge, double waterGrams, double saltGrams,
                         double baseGrams, double gasMoles) {}

    public static double salt(ItemStack stack) {
        return LabVesselItem.getContents(stack).stream()
                .filter(e -> e.type().equals("liquid") && e.id().equals("sodium_chloride_solution"))
                .mapToDouble(LabVesselItem.Entry::amount).sum();
    }

    public static boolean supported(ItemStack stack) {
        return salt(stack) > 0 && LabVesselItem.getContents(stack).stream().allMatch(e ->
                e.type().equals("liquid") && (e.id().equals("water")
                || e.id().equals("sodium_chloride_solution") || e.id().equals("sodium_hydroxide_solution")));
    }

    public static Result calculate(double current, double seconds, double water, double salt,
                                   double hydrogenSpace, double chlorineSpace, double totalSpace) {
        if (!Double.isFinite(current + seconds + water + salt + hydrogenSpace + chlorineSpace + totalSpace)
                || current <= 0 || seconds <= 0 || water <= 0 || salt <= 0) return new Result(0,0,0,0,0);
        double n = Math.min(current * seconds / (2 * WaterElectrolysis.FARADAY),
                Math.min(water / (2 * WaterElectrolysis.WATER_MOLAR), salt / (2 * SALT_MOLAR)));
        n = Math.min(n, (salt - MIN_SALT_WATER_RATIO * water)
                / (2 * SALT_MOLAR - MIN_SALT_WATER_RATIO * 2 * WaterElectrolysis.WATER_MOLAR));
        // Reserve all gas volume conservatively; consumed liquid need not free space in this step.
        n = Math.max(0, Math.min(n, Math.min(Math.min(hydrogenSpace, chlorineSpace)
                / WaterElectrolysis.GAS_ML_PER_MOLE, totalSpace / (2 * WaterElectrolysis.GAS_ML_PER_MOLE))));
        return new Result(n * 2 * WaterElectrolysis.FARADAY, n * 2 * WaterElectrolysis.WATER_MOLAR,
                n * 2 * SALT_MOLAR, n * 2 * BASE_MOLAR, n);
    }
    private BrineElectrolysis() {}
}
