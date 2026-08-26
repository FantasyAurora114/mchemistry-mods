package com.example.chemistry.data;

import java.util.Map;

/**
 * Substance variants: different forms of the same substance react the same
 * way but at different speeds (generated).
 */
public final class SubstanceVariants {

    public static final Map<String, String> CANONICAL = Map.ofEntries(
        Map.entry("hydrochloric_acid", "hydrochloric_acid"),
        Map.entry("hydrochloric_acid_concentrated", "hydrochloric_acid"),
        Map.entry("sulfuric_acid_dilute", "sulfuric_acid_dilute"),
        Map.entry("sulfuric_acid_concentrated", "sulfuric_acid_dilute"),
        Map.entry("nitric_acid", "nitric_acid"),
        Map.entry("nitric_acid_concentrated", "nitric_acid"),
        Map.entry("phosphoric_acid", "phosphoric_acid"),
        Map.entry("phosphoric_acid_concentrated", "phosphoric_acid"),
        Map.entry("ammonia_water", "ammonia_water"),
        Map.entry("ammonia_water_concentrated", "ammonia_water"),
        Map.entry("calcium_carbonate", "calcium_carbonate"),
        Map.entry("marble", "calcium_carbonate"),
        Map.entry("charcoal", "charcoal"),
        Map.entry("graphite", "charcoal"),
        Map.entry("activated_carbon", "charcoal"),
        Map.entry("iron", "iron"),
        Map.entry("iron_powder", "iron"),
        Map.entry("copper", "copper"),
        Map.entry("copper_powder", "copper"),
        Map.entry("aluminium", "aluminium"),
        Map.entry("aluminium_powder", "aluminium"),
        Map.entry("zinc", "zinc"),
        Map.entry("zinc_powder", "zinc"),
        Map.entry("magnesium", "magnesium"),
        Map.entry("magnesium_powder", "magnesium"),
        Map.entry("sodium", "sodium"),
        Map.entry("sodium_powder", "sodium"),
        Map.entry("potassium", "potassium"),
        Map.entry("calcium", "calcium"),
        Map.entry("calcium_powder", "calcium"),
        Map.entry("silver", "silver"),
        Map.entry("silver_powder", "silver"),
        Map.entry("barium", "barium"),
        Map.entry("barium_powder", "barium"),
        Map.entry("lithium", "lithium"),
        Map.entry("lithium_powder", "lithium")
    );

    public static final Map<String, Double> SPEED = Map.ofEntries(
        Map.entry("hydrochloric_acid", 1.0),
        Map.entry("hydrochloric_acid_concentrated", 3.0),
        Map.entry("sulfuric_acid_dilute", 1.0),
        Map.entry("sulfuric_acid_concentrated", 3.0),
        Map.entry("nitric_acid", 1.0),
        Map.entry("nitric_acid_concentrated", 3.0),
        Map.entry("phosphoric_acid", 1.0),
        Map.entry("phosphoric_acid_concentrated", 2.0),
        Map.entry("ammonia_water", 1.0),
        Map.entry("ammonia_water_concentrated", 2.0),
        Map.entry("calcium_carbonate", 1.0),
        Map.entry("marble", 0.8),
        Map.entry("charcoal", 1.0),
        Map.entry("graphite", 1.2),
        Map.entry("activated_carbon", 1.5),
        Map.entry("iron", 1.0),
        Map.entry("iron_powder", 4.0),
        Map.entry("copper", 1.0),
        Map.entry("copper_powder", 4.0),
        Map.entry("aluminium", 1.0),
        Map.entry("aluminium_powder", 4.0),
        Map.entry("zinc", 1.0),
        Map.entry("zinc_powder", 4.0),
        Map.entry("magnesium", 1.0),
        Map.entry("magnesium_powder", 4.0),
        Map.entry("sodium", 1.0),
        Map.entry("sodium_powder", 4.0),
        Map.entry("potassium", 1.0),
        Map.entry("calcium", 1.0),
        Map.entry("calcium_powder", 4.0),
        Map.entry("silver", 1.0),
        Map.entry("silver_powder", 4.0),
        Map.entry("barium", 1.0),
        Map.entry("barium_powder", 4.0),
        Map.entry("lithium", 1.0),
        Map.entry("lithium_powder", 4.0)
    );

    public static String canonicalOf(String id) {
        return CANONICAL.getOrDefault(id, id);
    }

    public static double speedOf(String id) {
        return SPEED.getOrDefault(id, 1.0);
    }

    private SubstanceVariants() {
    }
}
