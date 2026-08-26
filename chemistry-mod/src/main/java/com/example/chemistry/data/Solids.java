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

    public static final List<Solid> ALL = List.of(
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
            new Solid("copper_sulfate_anhydrous", "CuSO4", "Anhydrous Copper Sulfate", "无水硫酸铜", 0xF0F0F0, SolidForm.POWDER, ""),
            new Solid("copper_sulfate_pentahydrate", "CuSO4.5H2O", "Copper Sulfate Pentahydrate", "胆矾", 0x48A8E0, SolidForm.POWDER, ""),
            new Solid("iron_sulfate", "FeSO4", "Iron(II) Sulfate", "硫酸亚铁", 0xD8E8D8, SolidForm.POWDER, ""),
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
            new Solid("calcium_phosphate", "Ca3(PO4)2", "Calcium Phosphate", "磷酸钙", 0xF0F0EC, SolidForm.POWDER, "")







    );

    private Solids() {
    }
}
