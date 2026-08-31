package com.example.mci.data;

import java.util.List;

import com.example.chemistry.api.ChemistryAPI;
import com.example.chemistry.api.Substances.SolidSubstance;
import com.example.chemistry.data.Reactions.Ingredient;
import com.example.chemistry.data.Reactions.Product;
import com.example.chemistry.data.Reactions.Reaction;

/** Generated MCI substance/reaction registrations (see work/gen_mci_data.py). */
public final class MciSubstances {

    private MciSubstances() {
    }

    /** Called from the MCI mod constructor (after MChemistry seeds the API). */
    public static void registerAll() {
        for (Reaction r : REACTIONS) {
            ChemistryAPI.registerReaction(r);
        }
    }

    public static final List<SolidSubstance> SOLIDS = List.of(
            new SolidSubstance("bauxite", "Al2O3", "Bauxite", "铝土矿", 0xC07A58, false, ""),
            new SolidSubstance("magnesite", "MgCO3", "Magnesite", "菱镁矿", 0xE0D8C8, false, ""),
            new SolidSubstance("sphalerite", "ZnS", "Sphalerite", "闪锌矿", 0xC8A868, false, ""),
            new SolidSubstance("cassiterite", "SnO2", "Cassiterite", "锡石", 0x989078, false, ""),
            new SolidSubstance("galena", "PbS", "Galena", "方铅矿", 0x9098A0, false, ""),
            new SolidSubstance("pentlandite", "FeNi9S8", "Pentlandite", "镍黄铁矿", 0xC8B878, false, ""),
            new SolidSubstance("cobaltite", "CoAsS", "Cobaltite", "辉钴矿", 0xB8A8C0, false, ""),
            new SolidSubstance("chromite", "FeCr2O4", "Chromite", "铬铁矿", 0x605850, false, ""),
            new SolidSubstance("pyrolusite", "MnO2", "Pyrolusite", "软锰矿", 0x504850, false, ""),
            new SolidSubstance("ilmenite", "FeTiO3", "Ilmenite", "钛铁矿", 0x584840, false, ""),
            new SolidSubstance("molybdenite", "MoS2", "Molybdenite", "辉钼矿", 0x8898A8, false, ""),
            new SolidSubstance("wolframite", "FeWO4", "Wolframite", "黑钨矿", 0x504858, false, ""),
            new SolidSubstance("argentite", "Ag2S", "Argentite", "辉银矿", 0x98A0A8, false, ""),
            new SolidSubstance("sperrylite", "PtAs2", "Sperrylite", "砷铂矿", 0xC8C8D8, false, ""),
            new SolidSubstance("spodumene", "LiAlSi2O6", "Spodumene", "锂辉石", 0xD8C8B8, false, ""),
            new SolidSubstance("barite", "BaSO4", "Barite", "重晶石", 0xE0E0D8, false, ""),
            new SolidSubstance("bismuthinite", "Bi2S3", "Bismuthinite", "辉铋矿", 0xA8A8C0, false, ""),
            new SolidSubstance("greenockite", "CdS", "Greenockite", "硫镉矿", 0xE8C040, false, ""),
            new SolidSubstance("stibnite", "Sb2S3", "Stibnite", "辉锑矿", 0x889098, false, ""),
            new SolidSubstance("borax", "Na2B4O7", "Borax", "硼砂", 0xE0E8E8, false, ""),
            new SolidSubstance("sulfur_ore", "S", "Sulfur Ore", "硫磺矿", 0xD8C830, false, ""),
            new SolidSubstance("apatite", "Ca5(PO4)3F", "Apatite", "磷灰石", 0x90C8A8, false, ""),
            new SolidSubstance("fluorite", "CaF2", "Fluorite", "萤石", 0xB8D0E8, false, ""),
            new SolidSubstance("sylvite", "KCl", "Sylvite", "钾石盐", 0xD8A0A0, false, ""),
            new SolidSubstance("halite", "NaCl", "Halite", "岩盐", 0xF0E8E0, false, ""),
            new SolidSubstance("limestone", "CaCO3", "Limestone", "石灰石", 0xC8C8C0, false, ""),
            new SolidSubstance("iron_ore", "Fe2O3", "Iron Ore", "铁矿石", 0xC87A5A, false, ""),
            new SolidSubstance("copper_ore", "Cu2O", "Copper Ore", "铜矿石", 0xE8A858, false, ""),
            new SolidSubstance("gold_ore", "Au", "Gold Ore", "金矿石", 0xF0D060, false, ""),
            new SolidSubstance("zinc_oxide", "ZnO", "Zinc Oxide", "氧化锌", 0xE8E8E0, true, ""),
            new SolidSubstance("lead_oxide", "PbO", "Lead(II) Oxide", "氧化铅", 0xE0C050, true, ""),
            new SolidSubstance("nickel_oxide", "NiO", "Nickel(II) Oxide", "氧化镍", 0x90C8A0, true, ""),
            new SolidSubstance("cobalt_oxide", "CoO", "Cobalt(II) Oxide", "氧化钴", 0x8090A8, true, ""),
            new SolidSubstance("chromium_oxide", "Cr2O3", "Chromium(III) Oxide", "三氧化二铬", 0x70A070, true, ""),
            new SolidSubstance("molybdenum_oxide", "MoO3", "Molybdenum(VI) Oxide", "三氧化钼", 0xD8E8F0, true, ""),
            new SolidSubstance("tungsten_oxide", "WO3", "Tungsten(VI) Oxide", "三氧化钨", 0xE8E8A0, true, ""),
            new SolidSubstance("bismuth_oxide", "Bi2O3", "Bismuth(III) Oxide", "三氧化二铋", 0xE8D8A0, true, ""),
            new SolidSubstance("cadmium_oxide", "CdO", "Cadmium Oxide", "氧化镉", 0xB88048, true, ""),
            new SolidSubstance("antimony_oxide", "Sb2O3", "Antimony(III) Oxide", "三氧化二锑", 0xF0F0F0, true, ""),
            new SolidSubstance("barium_sulfide", "BaS", "Barium Sulfide", "硫化钡", 0xE0E0D8, true, ""),
            new SolidSubstance("manganese_chloride", "MnCl2", "Manganese(II) Chloride", "氯化锰", 0xF0B8D8, true, ""),
            new SolidSubstance("tin", "Sn", "Tin", "锡", 0xD0D8D8, false, ""),
            new SolidSubstance("lead", "Pb", "Lead", "铅", 0x9098A0, false, ""),
            new SolidSubstance("nickel", "Ni", "Nickel", "镍", 0xC8D0D8, false, ""),
            new SolidSubstance("cobalt", "Co", "Cobalt", "钴", 0x98A8D0, false, ""),
            new SolidSubstance("chromium", "Cr", "Chromium", "铬", 0xD0D8E8, false, ""),
            new SolidSubstance("manganese", "Mn", "Manganese", "锰", 0xB8A8A0, false, ""),
            new SolidSubstance("titanium", "Ti", "Titanium", "钛", 0xB8C0C8, false, ""),
            new SolidSubstance("molybdenum", "Mo", "Molybdenum", "钼", 0x98A0A8, false, ""),
            new SolidSubstance("tungsten", "W", "Tungsten", "钨", 0x808890, false, ""),
            new SolidSubstance("platinum", "Pt", "Platinum", "铂", 0xD0D8E0, false, ""),
            new SolidSubstance("bismuth", "Bi", "Bismuth", "铋", 0xC8A8C0, false, ""),
            new SolidSubstance("cadmium", "Cd", "Cadmium", "镉", 0xD8D8B8, false, ""),
            new SolidSubstance("antimony", "Sb", "Antimony", "锑", 0xA8B0B8, false, ""),
            new SolidSubstance("boron", "B", "Boron", "硼", 0xA08868, false, "")
    );

    public static final List<Reaction> REACTIONS = List.of(
            new Reaction(List.of(new Ingredient("solid", "bauxite", 2), new Ingredient("solid", "charcoal", 3)), List.of(new Product("solid", "aluminium", 2), new Product("vent", "", 3)), "2Al₂O₃ + 3C → 2Al + 3CO₂↑（高温）", 0.4, 1200, "", 0),
            new Reaction(List.of(new Ingredient("solid", "magnesite", 1)), List.of(new Product("solid", "magnesium_oxide", 1), new Product("vent", "", 1)), "MgCO₃ → MgO + CO₂↑（高温）", 0.6, 700, "", 0),
            new Reaction(List.of(new Ingredient("solid", "magnesium_oxide", 1), new Ingredient("solid", "charcoal", 1)), List.of(new Product("solid", "magnesium", 1), new Product("vent", "", 1)), "MgO + C → Mg + CO₂↑（高温）", 0.4, 1600, "", 0),
            new Reaction(List.of(new Ingredient("solid", "sphalerite", 1)), List.of(new Product("solid", "zinc_oxide", 1), new Product("vent", "", 1)), "ZnS → ZnO + SO₂↑（高温）", 0.6, 900, "", 0),
            new Reaction(List.of(new Ingredient("solid", "zinc_oxide", 1), new Ingredient("solid", "charcoal", 1)), List.of(new Product("solid", "zinc", 1), new Product("vent", "", 1)), "ZnO + C → Zn + CO₂↑（高温）", 0.5, 1200, "", 0),
            new Reaction(List.of(new Ingredient("solid", "cassiterite", 1), new Ingredient("solid", "charcoal", 1)), List.of(new Product("solid", "tin", 1), new Product("vent", "", 1)), "SnO₂ + C → Sn + CO₂↑（高温）", 0.6, 1000, "", 0),
            new Reaction(List.of(new Ingredient("solid", "galena", 1)), List.of(new Product("solid", "lead_oxide", 1), new Product("vent", "", 1)), "PbS → PbO + SO₂↑（高温）", 0.6, 900, "", 0),
            new Reaction(List.of(new Ingredient("solid", "lead_oxide", 1), new Ingredient("solid", "charcoal", 1)), List.of(new Product("solid", "lead", 1), new Product("vent", "", 1)), "PbO + C → Pb + CO₂↑（高温）", 0.5, 1100, "", 0),
            new Reaction(List.of(new Ingredient("solid", "pentlandite", 1)), List.of(new Product("solid", "nickel_oxide", 9), new Product("vent", "", 8)), "FeNi₉S₈ → 9NiO + 8SO₂↑（高温）", 0.6, 900, "", 0),
            new Reaction(List.of(new Ingredient("solid", "nickel_oxide", 1), new Ingredient("solid", "charcoal", 1)), List.of(new Product("solid", "nickel", 1), new Product("vent", "", 1)), "NiO + C → Ni + CO₂↑（高温）", 0.5, 1100, "", 0),
            new Reaction(List.of(new Ingredient("solid", "cobaltite", 1)), List.of(new Product("solid", "cobalt_oxide", 1), new Product("vent", "", 2)), "CoAsS → CoO + 2SO₂↑（高温）", 0.5, 900, "", 0),
            new Reaction(List.of(new Ingredient("solid", "cobalt_oxide", 1), new Ingredient("solid", "charcoal", 1)), List.of(new Product("solid", "cobalt", 1), new Product("vent", "", 1)), "CoO + C → Co + CO₂↑（高温）", 0.5, 1100, "", 0),
            new Reaction(List.of(new Ingredient("solid", "chromite", 2), new Ingredient("solid", "charcoal", 3)), List.of(new Product("solid", "chromium", 2), new Product("solid", "iron", 1), new Product("vent", "", 3)), "2FeCr₂O₄ + 3C → 2Cr + Fe + 3CO₂↑（高温）", 0.4, 1500, "", 0),
            new Reaction(List.of(new Ingredient("solid", "pyrolusite", 1), new Ingredient("liquid", "hydrochloric_acid", 4)), List.of(new Product("solid", "manganese_chloride", 1), new Product("vent", "", 1), new Product("liquid", "water", 2)), "MnO₂ + 4HCl → MnCl₂ + Cl₂↑ + 2H₂O", 1.0, 20, "", 0),
            new Reaction(List.of(new Ingredient("solid", "manganese_chloride", 1), new Ingredient("solid", "sodium", 2)), List.of(new Product("solid", "manganese", 1), new Product("solid", "sodium_chloride", 2)), "MnCl₂ + 2Na → Mn + 2NaCl（高温）", 0.5, 800, "", 0),
            new Reaction(List.of(new Ingredient("solid", "ilmenite", 2), new Ingredient("solid", "charcoal", 3)), List.of(new Product("solid", "titanium", 2), new Product("solid", "iron", 1), new Product("vent", "", 3)), "2FeTiO₃ + 3C → 2Ti + Fe + 3CO₂↑（高温）", 0.4, 1600, "", 0),
            new Reaction(List.of(new Ingredient("solid", "molybdenite", 1)), List.of(new Product("solid", "molybdenum_oxide", 1), new Product("vent", "", 2)), "MoS₂ → MoO₃ + 2SO₂↑（高温）", 0.5, 700, "", 0),
            new Reaction(List.of(new Ingredient("solid", "molybdenum_oxide", 1), new Ingredient("solid", "charcoal", 1)), List.of(new Product("solid", "molybdenum", 1), new Product("vent", "", 1)), "MoO₃ + C → Mo + CO₂↑（高温）", 0.5, 1200, "", 0),
            new Reaction(List.of(new Ingredient("solid", "wolframite", 1), new Ingredient("solid", "charcoal", 3)), List.of(new Product("solid", "tungsten", 1), new Product("solid", "iron", 1), new Product("vent", "", 3)), "FeWO₄ + 3C → W + Fe + 3CO₂↑（高温）", 0.3, 1600, "", 0),
            new Reaction(List.of(new Ingredient("solid", "argentite", 1)), List.of(new Product("solid", "silver", 2), new Product("vent", "", 1)), "Ag₂S → 2Ag + SO₂↑（高温）", 0.6, 800, "", 0),
            new Reaction(List.of(new Ingredient("solid", "sperrylite", 1)), List.of(new Product("solid", "platinum", 1), new Product("vent", "", 2)), "PtAs₂ → Pt + 2?↑（高温）", 0.5, 1000, "", 0),
            new Reaction(List.of(new Ingredient("solid", "spodumene", 2), new Ingredient("solid", "charcoal", 2)), List.of(new Product("solid", "lithium", 2), new Product("solid", "aluminium_oxide", 1), new Product("solid", "silicon_dioxide", 4), new Product("vent", "", 2)), "2LiAlSi₂O₆ + 2C → 2Li + Al₂O₃ + 4SiO₂ + 2CO₂↑（高温）", 0.4, 1500, "", 0),
            new Reaction(List.of(new Ingredient("solid", "barite", 1), new Ingredient("solid", "charcoal", 4)), List.of(new Product("solid", "barium_sulfide", 1), new Product("vent", "", 4)), "BaSO₄ + 4C → BaS + 4CO₂↑（高温）", 0.5, 1000, "", 0),
            new Reaction(List.of(new Ingredient("solid", "barium_sulfide", 1), new Ingredient("liquid", "hydrochloric_acid", 2)), List.of(new Product("solid", "barium_chloride", 1), new Product("vent", "", 1)), "BaS + 2HCl → BaCl₂ + H₂S↑", 1.0, 20, "", 0),
            new Reaction(List.of(new Ingredient("solid", "bismuthinite", 1)), List.of(new Product("solid", "bismuth_oxide", 1), new Product("vent", "", 3)), "Bi₂S₃ → Bi₂O₃ + 3SO₂↑（高温）", 0.5, 800, "", 0),
            new Reaction(List.of(new Ingredient("solid", "bismuth_oxide", 1), new Ingredient("solid", "charcoal", 1)), List.of(new Product("solid", "bismuth", 2), new Product("vent", "", 1)), "Bi₂O₃ + C → 2Bi + CO₂↑（高温）", 0.5, 1000, "", 0),
            new Reaction(List.of(new Ingredient("solid", "greenockite", 1)), List.of(new Product("solid", "cadmium_oxide", 1), new Product("vent", "", 1)), "CdS → CdO + SO₂↑（高温）", 0.5, 800, "", 0),
            new Reaction(List.of(new Ingredient("solid", "cadmium_oxide", 1), new Ingredient("solid", "charcoal", 1)), List.of(new Product("solid", "cadmium", 1), new Product("vent", "", 1)), "CdO + C → Cd + CO₂↑（高温）", 0.5, 1000, "", 0),
            new Reaction(List.of(new Ingredient("solid", "stibnite", 1)), List.of(new Product("solid", "antimony_oxide", 1), new Product("vent", "", 3)), "Sb₂S₃ → Sb₂O₃ + 3SO₂↑（高温）", 0.5, 800, "", 0),
            new Reaction(List.of(new Ingredient("solid", "antimony_oxide", 1), new Ingredient("solid", "charcoal", 1)), List.of(new Product("solid", "antimony", 2), new Product("vent", "", 1)), "Sb₂O₃ + C → 2Sb + CO₂↑（高温）", 0.5, 1000, "", 0),
            new Reaction(List.of(new Ingredient("solid", "borax", 1), new Ingredient("liquid", "hydrochloric_acid", 2)), List.of(new Product("solid", "boric_acid", 4), new Product("liquid", "sodium_chloride_solution", 2)), "Na₂B₄O₇ + 2HCl → 4H₃BO₃ + 2NaCl溶液", 1.0, 20, "", 0),
            new Reaction(List.of(new Ingredient("solid", "boric_acid", 2), new Ingredient("solid", "magnesium", 3)), List.of(new Product("solid", "boron", 2), new Product("solid", "magnesium_oxide", 3)), "2H₃BO₃ + 3Mg → 2B + 3MgO（高温）", 0.5, 900, "", 0),
            new Reaction(List.of(new Ingredient("solid", "sulfur_ore", 1)), List.of(new Product("solid", "sulfur", 1)), "S → S（高温升华提纯）", 1.0, 400, "", 0),
            new Reaction(List.of(new Ingredient("solid", "apatite", 1), new Ingredient("liquid", "sulfuric_acid_concentrated", 3)), List.of(new Product("liquid", "phosphoric_acid", 1), new Product("solid", "calcium_sulfate", 1), new Product("vent", "", 1)), "Ca₅(PO₄)₃F + 3H₂SO₄（浓） → H₃PO₄ + CaSO₄ + ?↑（微热）", 0.8, 80, "", 0),
            new Reaction(List.of(new Ingredient("liquid", "phosphoric_acid", 2), new Ingredient("solid", "charcoal", 1)), List.of(new Product("solid", "white_phosphorus", 1), new Product("vent", "", 1)), "2H₃PO₄ + C → P₄ + CO↑（高温）", 0.4, 1500, "", 0),
            new Reaction(List.of(new Ingredient("solid", "fluorite", 1), new Ingredient("liquid", "sulfuric_acid_concentrated", 1)), List.of(new Product("solid", "calcium_sulfate", 1), new Product("vent", "", 2)), "CaF₂ + H₂SO₄（浓） → CaSO₄ + 2?↑（高温）", 0.6, 200, "", 0),
            new Reaction(List.of(new Ingredient("solid", "sylvite", 2)), List.of(new Product("solid", "potassium", 2), new Product("vent", "", 1)), "2KCl → 2K + Cl₂↑（高温电解）", 0.5, 800, "", 0),
            new Reaction(List.of(new Ingredient("solid", "halite", 2)), List.of(new Product("solid", "sodium", 2), new Product("vent", "", 1)), "2NaCl → 2Na + Cl₂↑（高温电解）", 0.5, 800, "", 0),
            new Reaction(List.of(new Ingredient("solid", "limestone", 1)), List.of(new Product("solid", "calcium_oxide", 1), new Product("vent", "", 1)), "CaCO₃ → CaO + CO₂↑（高温）", 0.6, 900, "", 0),
            new Reaction(List.of(new Ingredient("solid", "iron_ore", 2), new Ingredient("solid", "charcoal", 3)), List.of(new Product("solid", "iron", 2), new Product("vent", "", 3)), "2Fe₂O₃ + 3C → 2Fe + 3CO₂↑（高温）", 0.5, 1000, "", 0),
            new Reaction(List.of(new Ingredient("solid", "copper_ore", 1), new Ingredient("solid", "charcoal", 1)), List.of(new Product("solid", "copper", 2), new Product("vent", "", 1)), "Cu₂O + C → 2Cu + CO₂↑（高温）", 0.5, 900, "", 0),
            new Reaction(List.of(new Ingredient("solid", "gold_ore", 1)), List.of(new Product("solid", "gold", 1)), "Au → gold（高温）", 0.5, 600, "", 0)
    );
}
