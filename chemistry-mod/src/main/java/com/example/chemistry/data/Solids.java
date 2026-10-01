package com.example.chemistry.data;

import java.util.List;

/**
 * Solid wide-mouth bottles (广口瓶) registered by the mod (generated).
 * form: POWDER (药匙/纸槽) or LUMP (镊子); openProduct: solid id the OPEN
 * form converts to after 2 in-game days (\"\" = none).
 */
public final class Solids {

    public enum SolidForm { POWDER, LUMP }

    public record Solid(String id, String formula, String english, String chinese, int color, SolidForm form, String openProduct) {
    }

    public static final List<Solid> ALL = java.util.stream.Stream.concat(FutureChemicals.ALL.stream().filter(c->c.phase().equals("SOLID")).map(c->new Solid(c.id(),c.formula(),c.english(),c.chinese(),c.color(),FutureElements.ALL.stream().anyMatch(e->e.id().equals(c.id()))?SolidForm.LUMP:SolidForm.POWDER,"")), java.util.stream.Stream.concat(BatchChemicals.ALL.stream().filter(c -> !c.id().equals("tin_iv_chloride") && !c.id().equals("ethylenediamine")).map(c -> new Solid(c.id(),c.formula(),c.english(),c.chinese(),c.color(),SolidForm.POWDER,"")), java.util.stream.Stream.concat(EdtaCompounds.ALL.stream().map(c -> new Solid(c.id(), c.formula(), c.english(), c.chinese(), c.color(), SolidForm.POWDER, "")), List.of(
            new Solid("sodium_silicate", "Na2SiO3", "Sodium Silicate", "硅酸钠", 0xE5E9DF, SolidForm.POWDER, ""),
            new Solid("zinc_nitrate", "Zn(NO3)2", "Zinc Nitrate", "硝酸锌", 0xE6EBE9, SolidForm.POWDER, ""),
            new Solid("crude_salt", "mixture", "Crude Salt", "粗盐", 0xC9C1A6, SolidForm.POWDER, ""),
            new Solid("marble", "CaCO3", "Marble", "大理石", 0xD8D8D0, SolidForm.LUMP, ""),
            new Solid("iron", "Fe", "Iron", "铁块", 0xC8C8D0, SolidForm.LUMP, ""),
            new Solid("iron_powder", "Fe", "Iron Powder", "铁粉", 0x909090, SolidForm.POWDER, ""),
            new Solid("copper", "Cu", "Copper", "铜块", 0xE8A858, SolidForm.LUMP, ""),
            new Solid("copper_powder", "Cu", "Copper Powder", "铜粉", 0xC88440, SolidForm.POWDER, ""),
            new Solid("aluminium", "Al", "Aluminium", "铝块", 0xD0D8E0, SolidForm.LUMP, ""),
            new Solid("aluminium_powder", "Al", "Aluminium Powder", "铝粉", 0xB8C0C8, SolidForm.POWDER, ""),
            new Solid("zinc", "Zn", "Zinc", "锌块", 0xB8C8D0, SolidForm.LUMP, ""),
            new Solid("zinc_powder", "Zn", "Zinc Powder", "锌粉", 0x98A8B0, SolidForm.POWDER, ""),
            new Solid("magnesium", "Mg", "Magnesium", "镁块", 0xD8D8D8, SolidForm.LUMP, ""),
            new Solid("magnesium_powder", "Mg", "Magnesium Powder", "镁粉", 0xC0C0C0, SolidForm.POWDER, ""),
            new Solid("sodium", "Na", "Sodium", "钠块", 0xD8D8E8, SolidForm.LUMP, ""),
            new Solid("sodium_powder", "Na", "Sodium Powder", "钠粉", 0xB8B8C8, SolidForm.POWDER, ""),
            new Solid("potassium", "K", "Potassium", "钾块", 0xD0D0E0, SolidForm.LUMP, ""),
            new Solid("calcium", "Ca", "Calcium", "钙块", 0xC8C8D0, SolidForm.LUMP, ""),
            new Solid("calcium_powder", "Ca", "Calcium Powder", "钙粉", 0xA8A8B8, SolidForm.POWDER, ""),
            new Solid("silver", "Ag", "Silver", "银块", 0xF0F0F8, SolidForm.LUMP, ""),
            new Solid("silver_powder", "Ag", "Silver Powder", "银粉", 0xD0D0D8, SolidForm.POWDER, ""),
            new Solid("barium", "Ba", "Barium", "钡块", 0xA8B0B8, SolidForm.LUMP, ""),
            new Solid("barium_powder", "Ba", "Barium Powder", "钡粉", 0x888F98, SolidForm.POWDER, ""),
            new Solid("lithium", "Li", "Lithium", "锂块", 0xC8C8D8, SolidForm.LUMP, ""),
            new Solid("lithium_powder", "Li", "Lithium Powder", "锂粉", 0xA8A8B8, SolidForm.POWDER, ""),
            new Solid("diamond", "C", "Diamond", "金刚石", 0xD8F0F8, SolidForm.LUMP, ""),
            new Solid("graphite", "C", "Graphite", "石墨粉", 0x505860, SolidForm.POWDER, ""),
            new Solid("charcoal", "C", "Charcoal", "木炭", 0x403838, SolidForm.LUMP, ""),
            new Solid("activated_carbon", "C", "Activated Carbon", "活性炭", 0x606868, SolidForm.POWDER, ""),
            new Solid("sulfur", "S", "Sulfur", "硫磺粉", 0xF0D848, SolidForm.POWDER, ""),
            new Solid("red_phosphorus", "P", "Red Phosphorus", "红磷", 0xC85038, SolidForm.POWDER, ""),
            new Solid("white_phosphorus", "P4", "White Phosphorus", "白磷", 0xF0F0D8, SolidForm.LUMP, ""),
            new Solid("iodine", "I2", "Iodine", "碘", 0x9070C8, SolidForm.LUMP, ""),
            new Solid("silicon", "Si", "Silicon", "硅块", 0x8898A8, SolidForm.LUMP, ""),
            new Solid("calcium_oxide", "CaO", "Calcium Oxide (Quicklime)", "生石灰", 0xE8E8E0, SolidForm.POWDER, ""),
            new Solid("magnesium_oxide", "MgO", "Magnesium Oxide", "氧化镁", 0xF0F0E8, SolidForm.POWDER, ""),
            new Solid("copper_ii_oxide", "CuO", "Copper(II) Oxide", "氧化铜", 0x403838, SolidForm.POWDER, ""),
            new Solid("copper_i_oxide", "Cu2O", "Copper(I) Oxide", "氧化亚铜", 0xB04830, SolidForm.POWDER, ""),
            new Solid("iron_iii_oxide", "Fe2O3", "Iron(III) Oxide (Iron Red)", "氧化铁", 0xB04030, SolidForm.POWDER, ""),
            new Solid("iron_ii_iii_oxide", "Fe3O4", "Iron(II,III) Oxide (Magnetite)", "四氧化三铁", 0x303030, SolidForm.POWDER, ""),
            new Solid("aluminium_oxide", "Al2O3", "Aluminium Oxide", "氧化铝", 0xE8E8F0, SolidForm.POWDER, ""),
            new Solid("silicon_dioxide", "SiO2", "Silicon Dioxide (Quartz)", "二氧化硅", 0xE0E0D8, SolidForm.POWDER, ""),
            new Solid("manganese_dioxide", "MnO2", "Manganese Dioxide", "二氧化锰", 0x383838, SolidForm.POWDER, ""),
            new Solid("phosphorus_pentoxide", "P2O5", "Phosphorus Pentoxide", "五氧化二磷", 0xF0F0E8, SolidForm.POWDER, ""),
            new Solid("boric_acid", "H3BO3", "Boric Acid", "硼酸", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_hydroxide", "NaOH", "Sodium Hydroxide (Caustic Soda)", "氢氧化钠", 0xE8E8F0, SolidForm.POWDER, "sodium_carbonate"),
            new Solid("potassium_hydroxide", "KOH", "Potassium Hydroxide", "氢氧化钾", 0xF0F0E8, SolidForm.POWDER, "potassium_carbonate"),
            new Solid("calcium_hydroxide", "Ca(OH)2", "Calcium Hydroxide (Slaked Lime)", "氢氧化钙", 0xF0F0EA, SolidForm.POWDER, "calcium_carbonate"),
            new Solid("barium_hydroxide", "Ba(OH)2", "Barium Hydroxide", "氢氧化钡", 0xF0F0EC, SolidForm.POWDER, ""),
            new Solid("magnesium_hydroxide", "Mg(OH)2", "Magnesium Hydroxide", "氢氧化镁", 0xF0F0EE, SolidForm.POWDER, ""),
            new Solid("aluminium_hydroxide", "Al(OH)3", "Aluminium Hydroxide", "氢氧化铝", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("iron_iii_hydroxide", "Fe(OH)3", "Iron(III) Hydroxide", "氢氧化铁", 0xB04828, SolidForm.POWDER, ""),
            new Solid("copper_ii_hydroxide", "Cu(OH)2", "Copper(II) Hydroxide", "氢氧化铜", 0x58A8C0, SolidForm.POWDER, ""),
            new Solid("iron_ii_hydroxide", "Fe(OH)2", "Iron(II) Hydroxide", "氢氧化亚铁", 0xD8E8E0, SolidForm.POWDER, ""),
            new Solid("sodium_chloride", "NaCl", "Sodium Chloride", "氯化钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_chloride", "KCl", "Potassium Chloride", "氯化钾", 0xF0F0EC, SolidForm.POWDER, ""),
            new Solid("calcium_chloride", "CaCl2", "Calcium Chloride", "氯化钙", 0xF0F0EA, SolidForm.POWDER, ""),
            new Solid("barium_chloride", "BaCl2", "Barium Chloride", "氯化钡", 0xF0F0E8, SolidForm.POWDER, ""),
            new Solid("iron_iii_chloride", "FeCl3", "Iron(III) Chloride", "氯化铁", 0xD8A030, SolidForm.POWDER, ""),
            new Solid("iron_ii_chloride", "FeCl2", "Iron(II) Chloride", "氯化亚铁", 0xC8E0D0, SolidForm.POWDER, ""),
            new Solid("silver_chloride", "AgCl", "Silver Chloride", "氯化银", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_carbonate", "Na2CO3", "Sodium Carbonate (Soda Ash)", "碳酸钠", 0xF0F0E8, SolidForm.POWDER, ""),
            new Solid("potassium_carbonate", "K2CO3", "Potassium Carbonate", "碳酸钾", 0xF0F0E8, SolidForm.POWDER, ""),
            new Solid("sodium_bicarbonate", "NaHCO3", "Sodium Bicarbonate", "碳酸氢钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("calcium_carbonate", "CaCO3", "Calcium Carbonate", "碳酸钙", 0xF0F0EA, SolidForm.POWDER, ""),
            new Solid("barium_carbonate", "BaCO3", "Barium Carbonate", "碳酸钡", 0xF0F0EC, SolidForm.POWDER, ""),
            new Solid("ammonium_bicarbonate", "NH4HCO3", "Ammonium Bicarbonate", "碳酸氢铵", 0xF0F0E8, SolidForm.POWDER, ""),
            new Solid("ammonium_chloride", "NH4Cl", "Ammonium Chloride", "氯化铵", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("acetamide", "CH3CONH2", "Acetamide", "乙酰胺", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_cyanide", "NaCN", "Sodium Cyanide", "氰化钠", 0xE8E8F0, SolidForm.POWDER, ""),
            new Solid("potassium_cyanide", "KCN", "Potassium Cyanide", "氰化钾", 0xF0E8F0, SolidForm.POWDER, ""),
            new Solid("sodium_formate", "HCOONa", "Sodium Formate", "甲酸钠", 0xE8E8E8, SolidForm.POWDER, ""),
            new Solid("sodium_acetate", "CH3COONa", "Sodium Acetate", "乙酸钠", 0xE8E8E8, SolidForm.POWDER, ""),

            // 羧酸盐（甲酸盐/乙酸盐）
            new Solid("potassium_formate", "HCOOK", "Potassium Formate", "甲酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_acetate", "CH3COOK", "Potassium Acetate", "乙酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("calcium_formate", "Ca(HCOO)2", "Calcium Formate", "甲酸钙", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("calcium_acetate", "Ca(CH3COO)2", "Calcium Acetate", "乙酸钙", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("ammonium_formate", "HCOONH4", "Ammonium Formate", "甲酸铵", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("ammonium_acetate", "CH3COONH4", "Ammonium Acetate", "乙酸铵", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("magnesium_acetate", "Mg(CH3COO)2", "Magnesium Acetate", "乙酸镁", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("copper_ii_acetate", "Cu(CH3COO)2", "Copper(II) Acetate", "乙酸铜", 0x58A8B8, SolidForm.POWDER, ""),
            new Solid("iron_iii_acetate", "Fe(CH3COO)3", "Iron(III) Acetate", "乙酸铁", 0xB05030, SolidForm.POWDER, ""),
            new Solid("silver_acetate", "CH3COOAg", "Silver Acetate", "乙酸银", 0xF0F0F0, SolidForm.POWDER, ""),

            // 草酸及其盐
            new Solid("oxalic_acid", "H2C2O4", "Oxalic Acid", "草酸", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_oxalate", "Na2C2O4", "Sodium Oxalate", "草酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_oxalate", "K2C2O4", "Potassium Oxalate", "草酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("calcium_oxalate", "CaC2O4", "Calcium Oxalate", "草酸钙", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("manganese_sulfate", "MnSO4", "Manganese Sulfate", "硫酸锰", 0xF0E8F0, SolidForm.POWDER, ""),

            new Solid("iodoform", "CHI3", "Iodoform", "碘仿", 0xE8D848, SolidForm.POWDER, ""),
            new Solid("benzoic_acid", "C6H5COOH", "Benzoic Acid", "苯甲酸", 0xF0F0F0, SolidForm.POWDER, ""),

            // 过氧化物与超氧化物
            new Solid("sodium_peroxide", "Na2O2", "Sodium Peroxide", "过氧化钠", 0xF0F0D8, SolidForm.POWDER, ""),
            new Solid("potassium_peroxide", "K2O2", "Potassium Peroxide", "过氧化钾", 0xF0F0D8, SolidForm.POWDER, ""),
            new Solid("calcium_peroxide", "CaO2", "Calcium Peroxide", "过氧化钙", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("barium_peroxide", "BaO2", "Barium Peroxide", "过氧化钡", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_superoxide", "KO2", "Potassium Superoxide", "超氧化钾", 0xE8E090, SolidForm.POWDER, ""),

            // 酸式盐
            new Solid("potassium_bicarbonate", "KHCO3", "Potassium Bicarbonate", "碳酸氢钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("calcium_bicarbonate", "Ca(HCO3)2", "Calcium Bicarbonate", "碳酸氢钙", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("magnesium_bicarbonate", "Mg(HCO3)2", "Magnesium Bicarbonate", "碳酸氢镁", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_bisulfate", "NaHSO4", "Sodium Bisulfate", "硫酸氢钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_bisulfate", "KHSO4", "Potassium Bisulfate", "硫酸氢钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_dihydrogen_phosphate", "NaH2PO4", "Sodium Dihydrogen Phosphate", "磷酸二氢钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("disodium_hydrogen_phosphate", "Na2HPO4", "Disodium Hydrogen Phosphate", "磷酸氢二钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_dihydrogen_phosphate", "KH2PO4", "Potassium Dihydrogen Phosphate", "磷酸二氢钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("dipotassium_hydrogen_phosphate", "K2HPO4", "Dipotassium Hydrogen Phosphate", "磷酸氢二钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_bisulfite", "NaHSO3", "Sodium Bisulfite", "亚硫酸氢钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("calcium_bisulfite", "Ca(HSO3)2", "Calcium Bisulfite", "亚硫酸氢钙", 0xF0F0F0, SolidForm.POWDER, ""),

            // 碱式盐
            new Solid("basic_copper_carbonate", "Cu2(OH)2CO3", "Basic Copper Carbonate", "碱式碳酸铜（铜绿）", 0x58B8A8, SolidForm.POWDER, ""),
            new Solid("basic_magnesium_carbonate", "Mg2(OH)2CO3", "Basic Magnesium Carbonate", "碱式碳酸镁", 0xF0F0F0, SolidForm.POWDER, ""),

            // 亚硫酸盐
            new Solid("sodium_sulfite", "Na2SO3", "Sodium Sulfite", "亚硫酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_sulfite", "K2SO3", "Potassium Sulfite", "亚硫酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("calcium_sulfite", "CaSO3", "Calcium Sulfite", "亚硫酸钙", 0xF0F0F0, SolidForm.POWDER, ""),

            // 硫代硫酸盐
            new Solid("sodium_thiosulfate", "Na2S2O3", "Sodium Thiosulfate", "硫代硫酸钠（大苏打）", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("magnesium_carbonate", "MgCO3", "Magnesium Carbonate", "碳酸镁", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_sulfate", "Na2SO4", "Sodium Sulfate", "硫酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_phosphate", "Na3PO4", "Sodium Phosphate", "磷酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_tetrathionate", "Na2S4O6", "Sodium Tetrathionate", "连四硫酸钠", 0xF0F0F0, SolidForm.POWDER, ""),

            // 醇盐
            new Solid("sodium_methoxide", "CH3ONa", "Sodium Methoxide", "甲醇钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_ethoxide", "C2H5ONa", "Sodium Ethoxide", "乙醇钠", 0xF0F0E8, SolidForm.POWDER, ""),
            new Solid("potassium_methoxide", "CH3OK", "Potassium Methoxide", "甲醇钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_ethoxide", "C2H5OK", "Potassium Ethoxide", "乙醇钾", 0xF0F0E8, SolidForm.POWDER, ""),

            // 中和反应配套盐（硝酸盐/硫酸铵/磷酸钾/氯化铜/卤化铵）
            new Solid("sodium_nitrate", "NaNO3", "Sodium Nitrate", "硝酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("calcium_nitrate", "Ca(NO3)2", "Calcium Nitrate", "硝酸钙", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("barium_nitrate", "Ba(NO3)2", "Barium Nitrate", "硝酸钡", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("magnesium_nitrate", "Mg(NO3)2", "Magnesium Nitrate", "硝酸镁", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("aluminium_nitrate", "Al(NO3)3", "Aluminium Nitrate", "硝酸铝", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("iron_iii_nitrate", "Fe(NO3)3", "Iron(III) Nitrate", "硝酸铁", 0xD8A080, SolidForm.POWDER, ""),
            new Solid("iron_ii_nitrate", "Fe(NO3)2", "Iron(II) Nitrate", "硝酸亚铁", 0xC8E0D0, SolidForm.POWDER, ""),
            new Solid("copper_ii_nitrate", "Cu(NO3)2", "Copper(II) Nitrate", "硝酸铜", 0x58A8B8, SolidForm.POWDER, ""),
            new Solid("ammonium_nitrate", "NH4NO3", "Ammonium Nitrate", "硝酸铵", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("ammonium_sulfate", "(NH4)2SO4", "Ammonium Sulfate", "硫酸铵", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_phosphate", "K3PO4", "Potassium Phosphate", "磷酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("copper_ii_chloride", "CuCl2", "Copper(II) Chloride", "氯化铜", 0x58B8A8, SolidForm.POWDER, ""),
            new Solid("ammonium_bromide", "NH4Br", "Ammonium Bromide", "溴化铵", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("ammonium_iodide", "NH4I", "Ammonium Iodide", "碘化铵", 0xF0F0F0, SolidForm.POWDER, ""),

            // 高温反应配套（亚硝酸盐/氧化钡）
            new Solid("sodium_nitrite", "NaNO2", "Sodium Nitrite", "亚硝酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_nitrite", "KNO2", "Potassium Nitrite", "亚硝酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("barium_oxide", "BaO", "Barium Oxide", "氧化钡", 0xF0F0E8, SolidForm.POWDER, ""),

            // 亚硝酸盐
            new Solid("calcium_nitrite", "Ca(NO2)2", "Calcium Nitrite", "亚硝酸钙", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("barium_nitrite", "Ba(NO2)2", "Barium Nitrite", "亚硝酸钡", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("ammonium_nitrite", "NH4NO2", "Ammonium Nitrite", "亚硝酸铵", 0xF0F0F0, SolidForm.POWDER, ""),

            // 次卤酸盐
            new Solid("sodium_hypochlorite", "NaClO", "Sodium Hypochlorite", "次氯酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_hypochlorite", "KClO", "Potassium Hypochlorite", "次氯酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("calcium_hypochlorite", "Ca(ClO)2", "Calcium Hypochlorite", "次氯酸钙（漂白粉）", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_hypobromite", "NaBrO", "Sodium Hypobromite", "次溴酸钠", 0xF0F0E8, SolidForm.POWDER, ""),
            new Solid("sodium_hypoiodite", "NaIO", "Sodium Hypoiodite", "次碘酸钠", 0xF0F0E8, SolidForm.POWDER, ""),

            // 卤酸盐
            new Solid("sodium_chlorate", "NaClO3", "Sodium Chlorate", "氯酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_bromate", "NaBrO3", "Sodium Bromate", "溴酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_bromate", "KBrO3", "Potassium Bromate", "溴酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_iodate", "NaIO3", "Sodium Iodate", "碘酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_iodate", "KIO3", "Potassium Iodate", "碘酸钾", 0xF0F0F0, SolidForm.POWDER, ""),

            // 高卤酸盐
            new Solid("potassium_perchlorate", "KClO4", "Potassium Perchlorate", "高氯酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_perchlorate", "NaClO4", "Sodium Perchlorate", "高氯酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_perbromate", "KBrO4", "Potassium Perbromate", "高溴酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_periodate", "KIO4", "Potassium Periodate", "高碘酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_periodate", "NaIO4", "Sodium Periodate", "高碘酸钠", 0xF0F0F0, SolidForm.POWDER, ""),

            // 氢化物
            new Solid("sodium_hydride", "NaH", "Sodium Hydride", "氢化钠", 0xE0E0E0, SolidForm.POWDER, ""),
            new Solid("potassium_hydride", "KH", "Potassium Hydride", "氢化钾", 0xE0E0E0, SolidForm.POWDER, ""),
            new Solid("calcium_hydride", "CaH2", "Calcium Hydride", "氢化钙", 0xD0D0D0, SolidForm.POWDER, ""),

            // 草酸-甘油法制甲酸中间体 / 重铬酸钾氧化产物
            new Solid("glyceryl_oxalate", "C5H6O5", "Glyceryl Oxalate", "草酸甘油酯", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("chromium_iii_sulfate", "Cr2(SO4)3", "Chromium(III) Sulfate", "硫酸铬", 0x58A878, SolidForm.POWDER, ""),

            new Solid("copper_sulfate_anhydrous", "CuSO4", "Anhydrous Copper Sulfate", "无水硫酸铜", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("copper_sulfate_pentahydrate", "CuSO4.5H2O", "Copper Sulfate Pentahydrate", "胆矾", 0x48A8E0, SolidForm.POWDER, ""),
            new Solid("iron_sulfate", "FeSO4", "Iron(II) Sulfate", "硫酸亚铁", 0xD8E8D8, SolidForm.POWDER, ""),
            new Solid("iron_sulfide", "FeS", "Iron(II) Sulfide", "硫化亚铁", 0x908070, SolidForm.LUMP, ""),
            new Solid("iron_sulfate_heptahydrate", "FeSO4.7H2O", "Iron(II) Sulfate Heptahydrate", "绿矾", 0xB8E0C8, SolidForm.POWDER, ""),
            new Solid("iron_iii_sulfate", "Fe2(SO4)3", "Iron(III) Sulfate", "硫酸铁", 0xE8C8A0, SolidForm.POWDER, ""),
            new Solid("magnesium_chloride", "MgCl2", "Magnesium Chloride", "氯化镁", 0xE0E0E8, SolidForm.POWDER, ""),
            new Solid("zinc_chloride", "ZnCl2", "Zinc Chloride", "氯化锌", 0xE0E0E0, SolidForm.POWDER, ""),
            new Solid("aluminium_chloride", "AlCl3", "Aluminium Chloride", "氯化铝", 0xE8E8E0, SolidForm.POWDER, ""),
            new Solid("magnesium_sulfate", "MgSO4", "Magnesium Sulfate", "硫酸镁", 0xE0E8E0, SolidForm.POWDER, ""),
            new Solid("zinc_sulfate", "ZnSO4", "Zinc Sulfate", "硫酸锌", 0xE0E8E8, SolidForm.POWDER, ""),
            new Solid("aluminium_sulfate", "Al2(SO4)3", "Aluminium Sulfate", "硫酸铝", 0xE8E8E8, SolidForm.POWDER, ""),
            new Solid("barium_sulfate", "BaSO4", "Barium Sulfate", "硫酸钡", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("calcium_sulfate", "CaSO4", "Calcium Sulfate", "硫酸钙", 0xF0F0EC, SolidForm.POWDER, ""),
            new Solid("potassium_nitrate", "KNO3", "Potassium Nitrate", "硝酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("silver_nitrate", "AgNO3", "Silver Nitrate", "硝酸银", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_permanganate", "KMnO4", "Potassium Permanganate", "高锰酸钾", 0x703088, SolidForm.POWDER, ""),
            new Solid("potassium_manganate", "K2MnO4", "Potassium Manganate", "锰酸钾", 0x305018, SolidForm.POWDER, ""),
            new Solid("potassium_chlorate", "KClO3", "Potassium Chlorate", "氯酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_dichromate", "K2Cr2O7", "Potassium Dichromate", "重铬酸钾", 0xD06018, SolidForm.POWDER, ""),
            new Solid("alum", "KAl(SO4)2.12H2O", "Alum", "明矾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("calcium_phosphate", "Ca3(PO4)2", "Calcium Phosphate", "磷酸钙", 0xF0F0EC, SolidForm.POWDER, ""),

            // 氰盐
            new Solid("calcium_cyanide", "Ca(CN)2", "Calcium Cyanide", "氰化钙", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("zinc_cyanide", "Zn(CN)2", "Zinc Cyanide", "氰化锌", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("copper_i_cyanide", "CuCN", "Copper(I) Cyanide", "氰化亚铜", 0xE8E8E8, SolidForm.POWDER, ""),
            new Solid("copper_ii_cyanide", "Cu(CN)2", "Copper(II) Cyanide", "氰化铜", 0xD0B060, SolidForm.POWDER, ""),
            new Solid("silver_cyanide", "AgCN", "Silver Cyanide", "氰化银", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("ammonium_cyanide", "NH4CN", "Ammonium Cyanide", "氰化铵", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_ferrocyanide", "K4[Fe(CN)6]", "Potassium Ferrocyanide", "亚铁氰化钾（黄血盐）", 0xF0E8A0, SolidForm.POWDER, ""),
            new Solid("potassium_ferricyanide", "K3[Fe(CN)6]", "Potassium Ferricyanide", "铁氰化钾（赤血盐）", 0xC84830, SolidForm.POWDER, ""),

            // 氰酸盐
            new Solid("sodium_cyanate", "NaOCN", "Sodium Cyanate", "氰酸钠", 0xE8E8E8, SolidForm.POWDER, ""),
            new Solid("potassium_cyanate", "KOCN", "Potassium Cyanate", "氰酸钾", 0xF0F0EC, SolidForm.POWDER, ""),
            new Solid("ammonium_cyanate", "NH4OCN", "Ammonium Cyanate", "氰酸铵", 0xF0F0F0, SolidForm.POWDER, ""),

            // 硫氰酸盐
            new Solid("sodium_thiocyanate", "NaSCN", "Sodium Thiocyanate", "硫氰酸钠", 0xE8E8E8, SolidForm.POWDER, ""),
            new Solid("potassium_thiocyanate", "KSCN", "Potassium Thiocyanate", "硫氰酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("ammonium_thiocyanate", "NH4SCN", "Ammonium Thiocyanate", "硫氰酸铵", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("iron_iii_thiocyanate", "Fe(SCN)3", "Iron(III) Thiocyanate", "硫氰酸铁", 0xA02020, SolidForm.POWDER, ""),
            new Solid("silver_thiocyanate", "AgSCN", "Silver Thiocyanate", "硫氰酸银", 0xF0F0F0, SolidForm.POWDER, ""),

            // 相关产物
            new Solid("urea", "CO(NH2)2", "Urea", "尿素", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_sulfate", "K2SO4", "Potassium Sulfate", "硫酸钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("prussian_blue", "Fe4[Fe(CN)6]3", "Prussian Blue", "普鲁士蓝", 0x1B2A6B, SolidForm.POWDER, ""),
            new Solid("turnbull_blue", "Fe3[Fe(CN)6]2", "Turnbull's Blue", "滕氏蓝", 0x24418F, SolidForm.POWDER, ""),

            // 汞及其化合物
            new Solid("mercury_ii_oxide", "HgO", "Mercury(II) Oxide", "氧化汞", 0xD84830, SolidForm.POWDER, ""),
            new Solid("mercury_i_chloride", "Hg2Cl2", "Mercury(I) Chloride", "氯化亚汞（甘汞）", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("mercury_ii_chloride", "HgCl2", "Mercury(II) Chloride", "氯化汞（升汞）", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("mercury_ii_sulfide", "HgS", "Mercury(II) Sulfide", "硫化汞（朱砂）", 0xC82828, SolidForm.POWDER, ""),
            new Solid("mercury_ii_nitrate", "Hg(NO3)2", "Mercury(II) Nitrate", "硝酸汞", 0xE8E8E8, SolidForm.POWDER, ""),
            new Solid("mercury_ii_sulfate", "HgSO4", "Mercury(II) Sulfate", "硫酸汞", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("mercury_ii_cyanide", "Hg(CN)2", "Mercury(II) Cyanide", "氰化汞", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("mercury_ii_iodide", "HgI2", "Mercury(II) Iodide", "碘化汞", 0xC82828, SolidForm.POWDER, ""),

            // 氟/溴/碘化物
            new Solid("sodium_fluoride", "NaF", "Sodium Fluoride", "氟化钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_fluoride", "KF", "Potassium Fluoride", "氟化钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("calcium_fluoride", "CaF2", "Calcium Fluoride", "氟化钙（萤石）", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_bromide", "NaBr", "Sodium Bromide", "溴化钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_bromide", "KBr", "Potassium Bromide", "溴化钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_iodide", "NaI", "Sodium Iodide", "碘化钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("potassium_iodide", "KI", "Potassium Iodide", "碘化钾", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("silver_bromide", "AgBr", "Silver Bromide", "溴化银", 0xE8E0C8, SolidForm.POWDER, ""),
            new Solid("silver_iodide", "AgI", "Silver Iodide", "碘化银", 0xE8E078, SolidForm.POWDER, ""),
            new Solid("calcium_carbide", "CaC2", "Calcium Carbide", "碳化钙（电石）", 0x989088, SolidForm.POWDER, "")

            ,
            // 高铁酸盐 / 高铜酸盐
            new Solid("potassium_ferrate", "K2FeO4", "Potassium Ferrate", "高铁酸钾", 0x4A2A6A, SolidForm.POWDER, ""),
            new Solid("sodium_ferrate", "Na2FeO4", "Sodium Ferrate", "高铁酸钠", 0x5A3A7A, SolidForm.POWDER, ""),
            new Solid("potassium_cuprate", "KCuO2", "Potassium Cuprate", "高铜酸钾", 0x2A3A5A, SolidForm.POWDER, ""),
            new Solid("sodium_cuprate", "NaCuO2", "Sodium Cuprate", "高铜酸钠", 0x3A4A6A, SolidForm.POWDER, ""),

            // 有机酸与危险化学品（柠檬酸/水杨酸/五氯化磷/氢化铝锂）
            new Solid("citric_acid", "C6H8O7", "Citric Acid", "柠檬酸", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("salicylic_acid", "C7H6O3", "Salicylic Acid", "水杨酸", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("phosphorus_pentachloride", "PCl5", "Phosphorus Pentachloride", "五氯化磷", 0xF0F0D0, SolidForm.POWDER, ""),
            new Solid("lithium_aluminium_hydride", "LiAlH4", "Lithium Aluminium Hydride", "氢化铝锂", 0xE8E8E8, SolidForm.POWDER, ""),

            // 更多有机酸
            new Solid("tartaric_acid", "C4H6O6", "Tartaric Acid", "酒石酸", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("malic_acid", "C4H6O5", "Malic Acid", "苹果酸", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("succinic_acid", "C4H6O4", "Succinic Acid", "琥珀酸", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("ascorbic_acid", "C6H8O6", "Ascorbic Acid (Vitamin C)", "抗坏血酸（维生素C）", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("gallic_acid", "C7H6O5", "Gallic Acid", "没食子酸", 0xF0F0E8, SolidForm.POWDER, ""),
            new Solid("stearic_acid", "C18H36O2", "Stearic Acid", "硬脂酸", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("palmitic_acid", "C16H32O2", "Palmitic Acid", "棕榈酸", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("adipic_acid", "C6H10O4", "Adipic Acid", "己二酸", 0xF0F0F0, SolidForm.POWDER, ""),

            // 更多危险化学品
            new Solid("sodium_borohydride", "NaBH4", "Sodium Borohydride", "硼氢化钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_azide", "NaN3", "Sodium Azide", "叠氮化钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("benzoyl_peroxide", "C14H10O4", "Benzoyl Peroxide", "过氧化苯甲酰", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("picric_acid", "C6H3N3O7", "Picric Acid", "苦味酸", 0xE8D040, SolidForm.POWDER, ""),

            // 有机酸盐 / 危险化学品水解产物（补全反应产物）
            new Solid("sodium_citrate", "Na3C6H5O7", "Sodium Citrate", "柠檬酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_tartrate", "Na2C4H4O6", "Sodium Tartrate", "酒石酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("sodium_salicylate", "NaC7H5O3", "Sodium Salicylate", "水杨酸钠", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("lithium_hydroxide", "LiOH", "Lithium Hydroxide", "氢氧化锂", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("chromium_iii_chloride", "CrCl3", "Chromium(III) Chloride", "氯化铬", 0x9060C0, SolidForm.POWDER, ""),
            new Solid("magnesium_citrate", "Mg3(C6H5O7)2", "Magnesium Citrate", "柠檬酸镁", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("acetylsalicylic_acid", "C9H8O4", "Acetylsalicylic Acid (Aspirin)", "乙酰水杨酸（阿司匹林）", 0xF0F0F0, SolidForm.POWDER, "")







    ).stream()))).toList();

    private Solids() {
    }
}
