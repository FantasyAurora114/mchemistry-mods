package com.example.chemistry.api;

/**
 * Concentration of a solution. The same substance at different concentrations
 * (e.g. 稀硫酸 vs 浓硫酸) is one substance; only the concentration differs.
 *
 * @param key         stable id, e.g. "dilute", "concentrated", "standard"
 * @param displayName Chinese display name, e.g. "稀硫酸"
 * @param speed       reaction speed multiplier for this concentration
 */
public record Concentration(String key, String displayName, double speed) {

    public static final Concentration STANDARD = new Concentration("standard", "标准", 1.0);
}
