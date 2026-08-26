package com.example.chemistry.data;

import java.util.List;

/**
 * Liquid narrow-mouth bottles (细口瓶) registered by the mod (generated).
 * openProduct: liquid id the OPEN form converts to after 2 in-game days (\"\" = none).
 */
public final class Liquids {

    public record Liquid(String id, String formula, String english, String chinese, int color, String openProduct) {
    }

    public static final List<Liquid> ALL = List.of(
            new Liquid("water", "H2O", "Water", "水", 0x8FC8E8, ""),
            new Liquid("hydrogen_peroxide", "H2O2", "Hydrogen Peroxide Solution", "过氧化氢溶液", 0xD8E8F0, ""),
            new Liquid("sulfuric_acid_dilute", "H2SO4", "Dilute Sulfuric Acid", "稀硫酸", 0xE8F0E0, ""),
            new Liquid("sulfuric_acid_concentrated", "H2SO4", "Concentrated Sulfuric Acid", "浓硫酸", 0xF0E8C0, ""),
            new Liquid("hydrochloric_acid", "HCl", "Hydrochloric Acid Solution", "氯化氢的水溶液", 0xE8E8E8, ""),
            new Liquid("hydrochloric_acid_concentrated", "HCl", "Concentrated HCl Solution", "氯化氢的高浓度水溶液", 0xF0F0D8, ""),
            new Liquid("nitric_acid", "HNO3", "Nitric Acid Solution", "硝酸的水溶液", 0xE8F0E8, ""),
            new Liquid("nitric_acid_concentrated", "HNO3", "Concentrated Nitric Acid Solution", "硝酸的高浓度水溶液", 0xF0E8A0, ""),
            new Liquid("phosphoric_acid", "H3PO4", "Phosphoric Acid Solution", "磷酸的水溶液", 0xE8E8F0, ""),
            new Liquid("phosphoric_acid_concentrated", "H3PO4", "Pure Phosphoric Acid", "纯磷酸", 0xE0E8F0, ""),
            new Liquid("ammonia_water", "NH3.H2O", "Ammonia Water", "氨水", 0xE0F0E8, ""),
            new Liquid("ammonia_water_concentrated", "NH3.H2O", "Concentrated Ammonia Water", "浓氨水", 0xD8F0E8, ""),
            new Liquid("sodium_hydroxide_solution", "NaOH", "Sodium Hydroxide Solution", "氢氧化钠溶液", 0xD8E8F0, "sodium_carbonate_solution"),
            new Liquid("sodium_carbonate_solution", "Na2CO3", "Sodium Carbonate Solution", "碳酸钠溶液", 0xE0F0E0, ""),
            new Liquid("potassium_hydroxide_solution", "KOH", "Potassium Hydroxide Solution", "氢氧化钾溶液", 0xE0E0F0, "potassium_carbonate_solution"),
            new Liquid("potassium_carbonate_solution", "K2CO3", "Potassium Carbonate Solution", "碳酸钾溶液", 0xF0F0E0, ""),
            new Liquid("limewater_clear", "Ca(OH)2", "Clear Limewater", "澄清石灰水", 0xE8F8E8, "limewater_cloudy"),
            new Liquid("limewater_cloudy", "CaCO3", "Cloudy Limewater", "浑浊的石灰水", 0xF0F0EA, ""),
            new Liquid("sodium_chloride_solution", "NaCl", "Sodium Chloride Solution", "氯化钠溶液", 0xE8F0F0, ""),
            new Liquid("iron_chloride_solution", "FeCl3", "Iron(III) Chloride Solution", "氯化铁溶液", 0xD8A030, ""),
            new Liquid("copper_sulfate_solution", "CuSO4", "Copper Sulfate Solution", "硫酸铜溶液", 0x2E8FD8, ""),
            new Liquid("potassium_permanganate_solution", "KMnO4", "Potassium Permanganate Solution", "高锰酸钾溶液", 0x7A2FA8, ""),
            new Liquid("chlorine_water", "Cl2(aq)", "Chlorine Water", "氯水", 0xD0E04A, ""),
            new Liquid("bromine_water", "Br2(aq)", "Bromine Water", "溴水", 0xD87A1A, ""),
            new Liquid("iodine_water", "I2(aq)", "Iodine Water", "碘水", 0x8A5A24, ""),
            new Liquid("litmus_solution", "C7H7NO4", "Litmus Solution", "石蕊试液", 0x8A4AA8, ""),
            new Liquid("phenolphthalein_solution", "C20H14O4", "Phenolphthalein Solution", "酚酞试液", 0xF0E0F0, ""),
            new Liquid("methyl_orange_solution", "C14H14N3NaO3S", "Methyl Orange Solution", "甲基橙试液", 0xF0A030, "")
    );

    private Liquids() {
    }
}
