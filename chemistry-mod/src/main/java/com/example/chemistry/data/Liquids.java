package com.example.chemistry.data;

import java.util.List;

/**
 * Liquid narrow-mouth bottles (细口瓶) registered by the mod (generated).
 * openProduct: liquid id the OPEN form converts to after 2 in-game days (\"\" = none).
 */
public final class Liquids {

    public record Liquid(String id, String formula, String english, String chinese, int color, String openProduct) {
    }

    public static final List<Liquid> ALL = java.util.stream.Stream.concat(FutureChemicals.ALL.stream().filter(c->c.phase().equals("LIQUID")||(c.phase().equals("SOLID")&&c.ceiling()>0)).map(c->new Liquid(c.id()+(c.phase().equals("SOLID")?"_solution":""),c.formula(),c.english()+(c.phase().equals("SOLID")?" Solution":""),c.chinese()+(c.phase().equals("SOLID")?"溶液":""),c.color(),"")), java.util.stream.Stream.concat(BatchChemicals.ALL.stream().filter(c -> c.ceiling()>0).map(c -> new Liquid(c.id()+"_solution",c.formula(),c.english()+" Solution",c.chinese()+"溶液",c.color(),"")), java.util.stream.Stream.concat(EdtaCompounds.ALL.stream().map(c -> new Liquid(c.id() + "_solution", c.formula(), c.english() + " solution", c.chinese() + "溶液", c.color(), "")), List.of(
            new Liquid("chromium_chloride_solution","CrCl3","Chromium Chloride Solution","氯化铬溶液",0x688C75,""),
            new Liquid("chromium_sulfate_solution","Cr2(SO4)3","Chromium Sulfate Solution","硫酸铬溶液",0x688C75,""),
            new Liquid("potassium_dichromate_solution","K2Cr2O7","Potassium Dichromate Solution","重铬酸钾溶液",0xB8752E,""),
            new Liquid("zinc_chloride_solution","ZnCl2","Zinc Chloride Solution","氯化锌溶液",0xC4DCE2,""),
            new Liquid("magnesium_sulfate_solution","MgSO4","Magnesium Sulfate Solution","硫酸镁溶液",0xC4DCE2,""),
            new Liquid("calcium_sulfate_solution","CaSO4","Calcium Sulfate Solution","硫酸钙溶液",0xC4DCE2,""),
            new Liquid("potassium_iodide_solution","KI","Potassium Iodide Solution","碘化钾溶液",0xC4DCE2,""),
            new Liquid("sodium_silicate_solution", "Na2SiO3(aq)", "Sodium Silicate Solution", "硅酸钠溶液", 0xC4DCE2, ""),
            new Liquid("zinc_sulfate_solution", "ZnSO4(aq)", "Zinc Sulfate Solution", "硫酸锌溶液", 0xC4DCE2, ""),
            new Liquid("zinc_nitrate_solution", "Zn(NO3)2(aq)", "Zinc Nitrate Solution", "硝酸锌溶液", 0xC4DCE2, ""),
            new Liquid("crude_saltwater", "mixture", "Crude Saltwater", "粗盐水", 0xA6AC94, ""),
            new Liquid("water", "H2O", "Water", "水", 0x8FC8E8, ""),
            new Liquid("hydrogen_peroxide", "H2O2", "Hydrogen Peroxide Solution", "过氧化氢溶液", 0xD8E8F0, ""),
            new Liquid("sulfuric_acid_dilute", "H2SO4", "Dilute Sulfuric Acid", "稀硫酸", 0xE8F0E0, ""),
            new Liquid("sulfuric_acid_concentrated", "H2SO4", "Concentrated Sulfuric Acid", "浓硫酸", 0xF0E8C0, ""),
            new Liquid("hydrochloric_acid", "HCl", "Hydrochloric Acid Solution", "盐酸", 0xE8E8E8, ""),
            new Liquid("hydrochloric_acid_concentrated", "HCl", "Concentrated HCl Solution", "浓盐酸", 0xF0F0D8, ""),
            new Liquid("nitric_acid", "HNO3", "Nitric Acid Solution", "硝酸", 0xE8F0E8, ""),
            new Liquid("nitric_acid_concentrated", "HNO3", "Concentrated Nitric Acid Solution", "浓硝酸", 0xF0E8A0, ""),
            new Liquid("phosphoric_acid", "H3PO4", "Phosphoric Acid Solution", "磷酸", 0xE8E8F0, ""),
            new Liquid("phosphoric_acid_concentrated", "H3PO4", "Pure Phosphoric Acid", "浓磷酸", 0xE0E8F0, ""),
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
            new Liquid("methyl_orange_solution", "C14H14N3NaO3S", "Methyl Orange Solution", "甲基橙试液", 0xF0A030, ""),
            new Liquid("formic_acid", "HCOOH", "Formic Acid", "甲酸", 0xE8E8D8, ""),
            new Liquid("acetic_acid", "CH3COOH", "Acetic Acid", "乙酸", 0xE8E8E0, ""),
            new Liquid("hydrocyanic_acid", "HCN", "Hydrocyanic Acid", "氢氰酸", 0xD8E8E8, ""),
            new Liquid("formamide", "HCONH2", "Formamide", "甲酰胺", 0xE8F0E8, ""),
            new Liquid("cyanic_acid", "HOCN", "Cyanic Acid", "氰酸", 0xE0F0F0, ""),
            new Liquid("thiocyanic_acid", "HSCN", "Thiocyanic Acid", "硫氰酸", 0xF0F0E8, ""),
            new Liquid("mercury", "Hg", "Mercury", "汞", 0xD0D8E0, ""),

            // 卤素单质与氢卤酸
            new Liquid("bromine", "Br2", "Bromine", "液溴", 0xC84A12, ""),
            new Liquid("hydrofluoric_acid", "HF", "Hydrofluoric Acid", "氢氟酸", 0xE8F0F0, ""),
            new Liquid("hydrobromic_acid", "HBr", "Hydrobromic Acid", "氢溴酸", 0xE8E8D8, ""),
            new Liquid("hydroiodic_acid", "HI", "Hydroiodic Acid", "氢碘酸", 0xE8D8A0, ""),

            // 醇与卤代烃
            new Liquid("ethanol", "C2H5OH", "Anhydrous Ethanol", "无水乙醇", 0xE8E8E8, ""),
            new Liquid("ethanol_95", "C2H5OH", "95% Ethanol", "95%乙醇", 0xE8F0F0, ""),
            new Liquid("ethanol_75", "C2H5OH", "75% Ethanol", "75%乙醇", 0xE8F0E8, ""),
            new Liquid("methanol", "CH3OH", "Methanol", "甲醇", 0xE8E8E8, ""),
            new Liquid("dichloromethane", "CH2Cl2", "Dichloromethane", "二氯甲烷", 0xE8E8E8, ""),
            new Liquid("chloroform", "CHCl3", "Chloroform", "氯仿", 0xE8E8E8, ""),
            new Liquid("carbon_tetrachloride", "CCl4", "Carbon Tetrachloride", "四氯化碳", 0xE8E8E8, ""),
            new Liquid("chloroethane", "C2H5Cl", "Chloroethane", "氯乙烷", 0xE8E8E8, ""),
            new Liquid("bromoethane", "C2H5Br", "Bromoethane", "溴乙烷", 0xE0D8C0, ""),
            new Liquid("iodomethane", "CH3I", "Iodomethane", "碘甲烷", 0xE0C8A0, ""),
            new Liquid("dibromoethane", "C2H4Br2", "1,2-Dibromoethane", "1,2-二溴乙烷", 0xE0D8C0, ""),
            new Liquid("dibromoethylene", "C2H2Br2", "1,2-Dibromoethylene", "1,2-二溴乙烯", 0xE0D0B8, ""),

            // 有机溶剂
            new Liquid("acetone", "CH3COCH3", "Acetone", "丙酮", 0xE8E8E8, ""),
            new Liquid("diethyl_ether", "(C2H5)2O", "Diethyl Ether", "乙醚", 0xE8E8E8, ""),
            new Liquid("ethyl_acetate", "CH3COOC2H5", "Ethyl Acetate", "乙酸乙酯", 0xE8F0E8, ""),
            new Liquid("benzene", "C6H6", "Benzene", "苯", 0xE8E8E0, ""),
            new Liquid("toluene", "C6H5CH3", "Toluene", "甲苯", 0xE8E8E0, ""),
            new Liquid("acetic_anhydride", "(CH3CO)2O", "Acetic Anhydride", "乙酸酐", 0xE8E8E8, ""),
            new Liquid("methyl_ethyl_ether", "CH3OC2H5", "Methyl Ethyl Ether", "甲乙醚", 0xE8E8E8, ""),

            // 次卤酸/卤酸/高卤酸
            new Liquid("hypochlorous_acid", "HClO", "Hypochlorous Acid", "次氯酸", 0xE8F0F0, ""),
            new Liquid("hypobromous_acid", "HBrO", "Hypobromous Acid", "次溴酸", 0xE8E8D8, ""),
            new Liquid("hypoiodous_acid", "HIO", "Hypoiodous Acid", "次碘酸", 0xE8E0C8, ""),
            new Liquid("chloric_acid", "HClO3", "Chloric Acid", "氯酸", 0xE8F0E8, ""),
            new Liquid("bromic_acid", "HBrO3", "Bromic Acid", "溴酸", 0xE8E8D8, ""),
            new Liquid("iodic_acid", "HIO3", "Iodic Acid", "碘酸", 0xE8E0C8, ""),
            new Liquid("perchloric_acid", "HClO4", "Perchloric Acid", "高氯酸", 0xE8F0E8, ""),
            new Liquid("perbromic_acid", "HBrO4", "Perbromic Acid", "高溴酸", 0xE8E8D8, ""),
            new Liquid("periodic_acid", "HIO4", "Periodic Acid", "高碘酸", 0xE8E0C8, ""),
            new Liquid("hydrazine", "N2H4", "Hydrazine", "肼", 0xE0E8E8, ""),

            // 酯类与硫酸氢乙酯
            new Liquid("methyl_formate", "HCOOCH3", "Methyl Formate", "甲酸甲酯", 0xE8E8E8, ""),
            new Liquid("ethyl_formate", "HCOOC2H5", "Ethyl Formate", "甲酸乙酯", 0xE8E8E8, ""),
            new Liquid("methyl_acetate", "CH3COOCH3", "Methyl Acetate", "乙酸甲酯", 0xE8E8E8, ""),
            new Liquid("ethyl_hydrogen_sulfate", "C2H5HSO4", "Ethyl Hydrogen Sulfate", "硫酸氢乙酯", 0xE8E8E0, ""),
            new Liquid("acetaldehyde", "CH3CHO", "Acetaldehyde", "乙醛", 0xE8E8E8, ""),
            new Liquid("glycerol", "C3H8O3", "Glycerol", "甘油", 0xE8F0F0, "")
    ).stream()))).toList();

    private Liquids() {
    }
}
