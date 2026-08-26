package com.example.chemistry.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tooltip chemistry data for every chemical item (generated).
 * Keys are item id prefixes; lookup also matches open_ variants and
 * per-form suffixes (e.g. element_iron_ingot -> element_iron).
 */
public final class ChemicalInfoProvider {

    public record ChemicalInfo(String formula, String molarMass, String toxicity, String corrosiveness,
            String explosiveness, String ph, String density, String appearance, String odour,
            String melting, String boiling) {
    }

    public static final Map<String, ChemicalInfo> CHEMICALS = new LinkedHashMap<>();
    public static final Map<String, Double> MOLAR_MASS = new LinkedHashMap<>();
    public static final Map<String, Double> DENSITY = new LinkedHashMap<>();
    public static final Map<String, Integer> BOILING_POINT = new LinkedHashMap<>();

    static {
        CHEMICALS.put("compound_salt", new ChemicalInfo("NaCl", "58.44", "none", "none", "none", "7.0|neutral", "2.16 g/cm³", "白色晶体", "无味", "801°C", "1465°C"));
        CHEMICALS.put("compound_water", new ChemicalInfo("H₂O", "18.02", "none", "none", "none", "7.0|neutral", "1.00 g/cm³", "无色透明液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("dropper_bottle_ammonia_water", new ChemicalInfo("NH₃·H₂O", "35.05", "low", "moderate", "flammable", "11.5|alkaline", "0.91 g/cm³", "无色透明液体", "刺激性气味", "-58°C", "35°C"));
        CHEMICALS.put("dropper_bottle_ammonia_water_concentrated", new ChemicalInfo("NH₃·H₂O", "35.05", "high", "strong", "flammable", "13.0|strong_alkaline", "0.88 g/cm³", "无色液体", "强烈刺激性气味", "-58°C", "35°C"));
        CHEMICALS.put("dropper_bottle_bromine_water", new ChemicalInfo("Br₂(aq)", "159.81", "high", "strong", "oxidizer", "3.5|acid", "1.00 g/cm³", "橙黄色液体", "刺激性气味", "0°C", "100°C"));
        CHEMICALS.put("dropper_bottle_chlorine_water", new ChemicalInfo("Cl₂(aq)", "70.90", "high", "strong", "oxidizer", "2.5|acid", "1.00 g/cm³", "淡黄绿色液体", "刺激性气味", "0°C", "100°C"));
        CHEMICALS.put("dropper_bottle_copper_sulfate_solution", new ChemicalInfo("CuSO₄", "159.60", "medium", "weak", "none", "4.0|acid", "1.10 g/cm³", "蓝色透明液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("dropper_bottle_hydrochloric_acid", new ChemicalInfo("HCl", "36.46", "medium", "strong", "none", "1.0|strong_acid", "1.05 g/cm³", "无色透明液体", "刺激性气味", "-27°C", "110°C"));
        CHEMICALS.put("dropper_bottle_hydrochloric_acid_concentrated", new ChemicalInfo("HCl", "36.46", "high", "strong", "none", "<1|strong_acid", "1.18 g/cm³", "无色液体（冒白雾）", "刺激性气味", "-27°C", "48°C"));
        CHEMICALS.put("dropper_bottle_hydrogen_peroxide", new ChemicalInfo("H₂O₂", "34.01", "low", "moderate", "oxidizer", "4.5|acid", "1.11 g/cm³", "无色透明液体", "无味", "-0.4°C", "150°C"));
        CHEMICALS.put("dropper_bottle_iodine_water", new ChemicalInfo("I₂(aq)", "253.81", "medium", "weak", "none", "5.0|acid", "1.00 g/cm³", "浅褐色液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("dropper_bottle_iron_chloride_solution", new ChemicalInfo("FeCl₃", "162.19", "medium", "moderate", "none", "2.5|acid", "1.10 g/cm³", "棕黄色液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("dropper_bottle_limewater_clear", new ChemicalInfo("Ca(OH)₂", "74.09", "low", "moderate", "none", "12.4|strong_alkaline", "1.00 g/cm³", "无色透明液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("dropper_bottle_limewater_cloudy", new ChemicalInfo("CaCO₃", "100.09", "none", "none", "none", "9.0|alkaline", "1.00 g/cm³", "乳白色浑浊液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("dropper_bottle_litmus_solution", new ChemicalInfo("C₇H₇NO₄", "169.14", "low", "none", "none", "n/a", "1.00 g/cm³", "紫色液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("dropper_bottle_methyl_orange_solution", new ChemicalInfo("C₁4H₁4N₃NaO₃S", "327.33", "low", "none", "none", "n/a", "1.00 g/cm³", "橙黄色液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("dropper_bottle_nitric_acid", new ChemicalInfo("HNO₃", "63.01", "high", "strong", "none", "1.0|strong_acid", "1.08 g/cm³", "无色透明液体", "刺激性气味", "-42°C", "121°C"));
        CHEMICALS.put("dropper_bottle_nitric_acid_concentrated", new ChemicalInfo("HNO₃", "63.01", "extreme", "strong", "oxidizer", "<1|strong_acid", "1.42 g/cm³", "淡黄色液体", "刺激性气味", "-42°C", "83°C"));
        CHEMICALS.put("dropper_bottle_phenolphthalein_solution", new ChemicalInfo("C₂0H₁4O₄", "318.33", "low", "none", "none", "n/a", "1.00 g/cm³", "无色透明液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("dropper_bottle_phosphoric_acid", new ChemicalInfo("H₃PO₄", "97.99", "medium", "strong", "none", "1.5|strong_acid", "1.08 g/cm³", "无色透明液体", "无味", "42°C", "158°C"));
        CHEMICALS.put("dropper_bottle_phosphoric_acid_concentrated", new ChemicalInfo("H₃PO₄", "97.99", "high", "strong", "none", "<1|strong_acid", "1.68 g/cm³", "无色黏稠液体", "无味", "42°C", "213°C"));
        CHEMICALS.put("dropper_bottle_potassium_carbonate_solution", new ChemicalInfo("K₂CO₃", "138.20", "low", "weak", "none", "11.5|alkaline", "1.10 g/cm³", "无色透明液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("dropper_bottle_potassium_hydroxide_solution", new ChemicalInfo("KOH", "56.11", "high", "strong", "none", "14.0|strong_alkaline", "1.15 g/cm³", "无色透明液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("dropper_bottle_potassium_permanganate_solution", new ChemicalInfo("KMnO₄", "158.03", "medium", "moderate", "oxidizer", "7.0|neutral", "1.00 g/cm³", "紫红色液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("dropper_bottle_sodium_carbonate_solution", new ChemicalInfo("Na₂CO₃", "105.99", "low", "weak", "none", "11.5|alkaline", "1.10 g/cm³", "无色透明液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("dropper_bottle_sodium_chloride_solution", new ChemicalInfo("NaCl", "58.44", "none", "none", "none", "7.0|neutral", "1.05 g/cm³", "无色透明液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("dropper_bottle_sodium_hydroxide_solution", new ChemicalInfo("NaOH", "40.00", "high", "strong", "none", "13.5|strong_alkaline", "1.15 g/cm³", "无色透明液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("dropper_bottle_sulfuric_acid_concentrated", new ChemicalInfo("H₂SO₄", "98.07", "extreme", "strong", "none", "<1|strong_acid", "1.84 g/cm³", "无色油状液体", "无味", "10°C", "337°C"));
        CHEMICALS.put("dropper_bottle_sulfuric_acid_dilute", new ChemicalInfo("H₂SO₄", "98.07", "high", "strong", "none", "1.0|strong_acid", "1.08 g/cm³", "无色透明液体", "无味", "-20°C", "110°C"));
        CHEMICALS.put("dropper_bottle_water", new ChemicalInfo("H₂O", "18.02", "none", "none", "none", "7.0|neutral", "1.00 g/cm³", "无色透明液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("element_aluminium", new ChemicalInfo("Al", "26.98", "none", "none", "flammable", "n/a", "2.70 g/cm³", "银白色金属", "无味", "660°C", "2519°C"));
        CHEMICALS.put("element_antimony", new ChemicalInfo("Sb", "121.76", "medium", "none", "none", "n/a", "6.68 g/cm³", "银灰色金属", "无味", "631°C", "1587°C"));
        CHEMICALS.put("element_argon", new ChemicalInfo("Ar", "39.95", "none", "none", "none", "n/a", "1.784 g/L", "无色气体", "无味", "-189°C", "-186°C"));
        CHEMICALS.put("element_arsenic", new ChemicalInfo("As", "74.92", "high", "none", "none", "n/a", "5.73 g/cm³", "灰黑色固体", "无味", "817°C", "614°C"));
        CHEMICALS.put("element_barium", new ChemicalInfo("Ba", "137.33", "high", "moderate", "water_flammable", "n/a", "3.62 g/cm³", "银白色金属", "无味", "727°C", "1897°C"));
        CHEMICALS.put("element_beryllium", new ChemicalInfo("Be", "9.01", "extreme", "none", "none", "n/a", "1.85 g/cm³", "灰白色金属", "无味", "1287°C", "2470°C"));
        CHEMICALS.put("element_bismuth", new ChemicalInfo("Bi", "208.98", "low", "none", "none", "n/a", "9.78 g/cm³", "银白色金属（微粉）", "无味", "271°C", "1564°C"));
        CHEMICALS.put("element_boron", new ChemicalInfo("B", "10.81", "low", "none", "none", "n/a", "2.34 g/cm³", "黑褐色固体", "无味", "2076°C", "3927°C"));
        CHEMICALS.put("element_bromine", new ChemicalInfo("Br₂", "159.81", "high", "strong", "none", "n/a", "3.12 g/cm³", "红棕色液体", "刺激性气味", "-7°C", "59°C"));
        CHEMICALS.put("element_cadmium", new ChemicalInfo("Cd", "112.41", "high", "none", "none", "n/a", "8.65 g/cm³", "银白色金属", "无味", "321°C", "767°C"));
        CHEMICALS.put("element_calcium", new ChemicalInfo("Ca", "40.08", "low", "moderate", "water_flammable", "n/a", "1.54 g/cm³", "银白色金属", "无味", "842°C", "1484°C"));
        CHEMICALS.put("element_carbon", new ChemicalInfo("C", "12.01", "none", "none", "flammable", "n/a", "2.27 g/cm³", "灰黑色固体", "无味", "3550°C", "3642°C"));
        CHEMICALS.put("element_chlorine", new ChemicalInfo("Cl₂", "70.90", "extreme", "strong", "oxidizer", "n/a", "3.21 g/L", "黄绿色气体", "刺激性气味", "-101°C", "-34°C"));
        CHEMICALS.put("element_chromium", new ChemicalInfo("Cr", "52.00", "medium", "none", "none", "n/a", "7.19 g/cm³", "银白色金属", "无味", "1907°C", "2671°C"));
        CHEMICALS.put("element_cobalt", new ChemicalInfo("Co", "58.93", "medium", "none", "none", "n/a", "8.90 g/cm³", "银灰色金属", "无味", "1495°C", "2927°C"));
        CHEMICALS.put("element_copper", new ChemicalInfo("Cu", "63.55", "low", "none", "none", "n/a", "8.96 g/cm³", "紫红色金属", "无味", "1084°C", "2562°C"));
        CHEMICALS.put("element_fluorine", new ChemicalInfo("F₂", "38.00", "extreme", "strong", "oxidizer", "n/a", "1.696 g/L", "淡黄色气体", "刺激性气味", "-220°C", "-188°C"));
        CHEMICALS.put("element_germanium", new ChemicalInfo("Ge", "72.63", "low", "none", "none", "n/a", "5.32 g/cm³", "灰白色固体", "无味", "938°C", "2833°C"));
        CHEMICALS.put("element_gold", new ChemicalInfo("Au", "196.97", "none", "none", "none", "n/a", "19.32 g/cm³", "金黄色金属", "无味", "1064°C", "2856°C"));
        CHEMICALS.put("element_helium", new ChemicalInfo("He", "4.00", "none", "none", "none", "n/a", "0.1785 g/L", "无色气体", "无味", "-272°C", "-269°C"));
        CHEMICALS.put("element_hydrogen", new ChemicalInfo("H₂", "2.02", "none", "none", "flammable_high", "n/a", "0.0899 g/L", "无色气体", "无味", "-259°C", "-253°C"));
        CHEMICALS.put("element_iodine", new ChemicalInfo("I₂", "253.81", "medium", "weak", "none", "n/a", "4.93 g/cm³", "紫黑色固体", "特殊气味", "114°C", "184°C"));
        CHEMICALS.put("element_iron", new ChemicalInfo("Fe", "55.84", "none", "none", "none", "n/a", "7.87 g/cm³", "银白色金属", "无味", "1538°C", "2861°C"));
        CHEMICALS.put("element_krypton", new ChemicalInfo("Kr", "83.80", "none", "none", "none", "n/a", "3.75 g/L", "无色气体", "无味", "-157°C", "-153°C"));
        CHEMICALS.put("element_lead", new ChemicalInfo("Pb", "207.20", "high", "none", "none", "n/a", "11.34 g/cm³", "银灰色金属", "无味", "327°C", "1749°C"));
        CHEMICALS.put("element_lithium", new ChemicalInfo("Li", "6.94", "low", "strong", "water_flammable", "n/a", "0.534 g/cm³", "银白色金属", "无味", "180°C", "1342°C"));
        CHEMICALS.put("element_magnesium", new ChemicalInfo("Mg", "24.30", "none", "none", "flammable", "n/a", "1.738 g/cm³", "银白色金属", "无味", "650°C", "1090°C"));
        CHEMICALS.put("element_manganese", new ChemicalInfo("Mn", "54.94", "low", "none", "none", "n/a", "7.44 g/cm³", "灰白色金属", "无味", "1246°C", "2061°C"));
        CHEMICALS.put("element_mercury", new ChemicalInfo("Hg", "200.59", "high", "none", "none", "n/a", "13.53 g/cm³", "银白色液体", "无味", "-39°C", "357°C"));
        CHEMICALS.put("element_molybdenum", new ChemicalInfo("Mo", "95.95", "low", "none", "none", "n/a", "10.28 g/cm³", "银灰色金属", "无味", "2623°C", "4639°C"));
        CHEMICALS.put("element_neon", new ChemicalInfo("Ne", "20.18", "none", "none", "none", "n/a", "0.900 g/L", "无色气体", "无味", "-249°C", "-246°C"));
        CHEMICALS.put("element_nickel", new ChemicalInfo("Ni", "58.69", "medium", "none", "none", "n/a", "8.91 g/cm³", "银白色金属", "无味", "1455°C", "2913°C"));
        CHEMICALS.put("element_nitrogen", new ChemicalInfo("N₂", "28.01", "none", "none", "none", "n/a", "1.251 g/L", "无色气体", "无味", "-210°C", "-196°C"));
        CHEMICALS.put("element_oxygen", new ChemicalInfo("O₂", "32.00", "none", "none", "oxidizer", "n/a", "1.429 g/L", "无色气体", "无味", "-218°C", "-183°C"));
        CHEMICALS.put("element_phosphorus", new ChemicalInfo("P", "30.97", "medium", "weak", "flammable", "n/a", "1.82 g/cm³", "深色固体", "无味", "44°C", "280°C"));
        CHEMICALS.put("element_platinum", new ChemicalInfo("Pt", "195.08", "none", "none", "none", "n/a", "21.45 g/cm³", "银白色金属", "无味", "1768°C", "3825°C"));
        CHEMICALS.put("element_potassium", new ChemicalInfo("K", "39.10", "low", "strong", "water_flammable", "n/a", "0.862 g/cm³", "银白色金属", "无味", "63°C", "759°C"));
        CHEMICALS.put("element_radon", new ChemicalInfo("Rn", "222.00", "high", "none", "none", "n/a", "9.73 g/L", "无色气体", "无味", "-71°C", "-62°C"));
        CHEMICALS.put("element_selenium", new ChemicalInfo("Se", "78.97", "high", "none", "none", "n/a", "4.81 g/cm³", "灰色固体", "无味", "221°C", "685°C"));
        CHEMICALS.put("element_silicon", new ChemicalInfo("Si", "28.09", "none", "none", "none", "n/a", "2.33 g/cm³", "灰黑色固体", "无味", "1414°C", "3265°C"));
        CHEMICALS.put("element_silver", new ChemicalInfo("Ag", "107.87", "low", "none", "none", "n/a", "10.49 g/cm³", "银白色金属", "无味", "962°C", "2162°C"));
        CHEMICALS.put("element_sodium", new ChemicalInfo("Na", "22.99", "low", "strong", "water_flammable", "n/a", "0.971 g/cm³", "银白色金属", "无味", "98°C", "883°C"));
        CHEMICALS.put("element_sulfur", new ChemicalInfo("S", "32.06", "low", "none", "flammable", "n/a", "2.07 g/cm³", "淡黄色固体", "无味", "115°C", "445°C"));
        CHEMICALS.put("element_tin", new ChemicalInfo("Sn", "118.71", "low", "none", "none", "n/a", "7.31 g/cm³", "银白色金属", "无味", "232°C", "2602°C"));
        CHEMICALS.put("element_titanium", new ChemicalInfo("Ti", "47.87", "none", "none", "none", "n/a", "4.54 g/cm³", "银白色金属", "无味", "1668°C", "3287°C"));
        CHEMICALS.put("element_tungsten", new ChemicalInfo("W", "183.84", "none", "none", "none", "n/a", "19.25 g/cm³", "银灰色金属", "无味", "3422°C", "5555°C"));
        CHEMICALS.put("element_uranium", new ChemicalInfo("U", "238.03", "high", "none", "none", "n/a", "19.05 g/cm³", "银白色金属", "无味", "1135°C", "4131°C"));
        CHEMICALS.put("element_vanadium", new ChemicalInfo("V", "50.94", "medium", "none", "none", "n/a", "6.11 g/cm³", "银灰色金属", "无味", "1910°C", "3407°C"));
        CHEMICALS.put("element_xenon", new ChemicalInfo("Xe", "131.29", "none", "none", "none", "n/a", "5.90 g/L", "无色气体", "无味", "-112°C", "-108°C"));
        CHEMICALS.put("element_zinc", new ChemicalInfo("Zn", "65.38", "low", "none", "flammable", "n/a", "7.14 g/cm³", "银白色金属（微蓝）", "无味", "420°C", "907°C"));
        CHEMICALS.put("gas_collecting_bottle_ammonia", new ChemicalInfo("NH₃", "17.03", "medium", "strong", "flammable", "n/a", "0.77 g/L", "无色气体", "强烈刺激性气味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_argon", new ChemicalInfo("Ar", "39.95", "none", "none", "none", "n/a", "1.78 g/L", "无色气体", "无味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_carbon_dioxide", new ChemicalInfo("CO₂", "44.01", "none", "none", "none", "n/a", "1.98 g/L", "无色气体", "无味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_carbon_monoxide", new ChemicalInfo("CO", "28.01", "extreme", "none", "flammable_high", "n/a", "1.25 g/L", "无色气体", "无味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_chlorine", new ChemicalInfo("Cl₂", "70.90", "extreme", "strong", "oxidizer", "n/a", "3.21 g/L", "黄绿色气体", "刺激性气味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_helium", new ChemicalInfo("He", "4.00", "none", "none", "none", "n/a", "0.179 g/L", "无色气体", "无味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_hydrogen", new ChemicalInfo("H₂", "2.02", "none", "none", "flammable_high", "n/a", "0.0899 g/L", "无色气体", "无味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_hydrogen_chloride", new ChemicalInfo("HCl", "36.46", "high", "strong", "none", "n/a", "1.63 g/L", "无色气体（冒白雾）", "刺激性气味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_hydrogen_sulfide", new ChemicalInfo("H₂S", "34.08", "extreme", "moderate", "flammable", "n/a", "1.52 g/L", "无色气体", "臭鸡蛋气味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_krypton", new ChemicalInfo("Kr", "83.80", "none", "none", "none", "n/a", "3.75 g/L", "无色气体", "无味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_neon", new ChemicalInfo("Ne", "20.18", "none", "none", "none", "n/a", "0.90 g/L", "无色气体", "无味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_nitric_oxide", new ChemicalInfo("NO", "30.01", "high", "moderate", "none", "n/a", "1.34 g/L", "无色气体", "无味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_nitrogen", new ChemicalInfo("N₂", "28.01", "none", "none", "none", "n/a", "1.25 g/L", "无色气体", "无味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_nitrogen_dioxide", new ChemicalInfo("NO₂", "46.01", "high", "strong", "oxidizer", "n/a", "2.05 g/L", "红棕色气体", "刺激性气味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_oxygen", new ChemicalInfo("O₂", "32.00", "none", "none", "oxidizer", "n/a", "1.43 g/L", "无色气体", "无味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_sulfur_dioxide", new ChemicalInfo("SO₂", "64.06", "high", "strong", "none", "n/a", "2.93 g/L", "无色气体", "刺激性气味", "—", "—"));
        CHEMICALS.put("gas_collecting_bottle_xenon", new ChemicalInfo("Xe", "131.29", "none", "none", "none", "n/a", "5.90 g/L", "无色气体", "无味", "—", "—"));
        CHEMICALS.put("graphite", new ChemicalInfo("C", "12.01", "none", "none", "flammable", "n/a", "2.27 g/cm³", "灰黑色粉末", "无味", "3550°C", "3642°C"));
        CHEMICALS.put("liquid_ammonia_water", new ChemicalInfo("NH₃·H₂O", "35.05", "low", "moderate", "flammable", "11.5|alkaline", "0.91 g/cm³", "无色透明液体", "刺激性气味", "-58°C", "35°C"));
        CHEMICALS.put("liquid_ammonia_water_concentrated", new ChemicalInfo("NH₃·H₂O", "35.05", "high", "strong", "flammable", "13.0|strong_alkaline", "0.88 g/cm³", "无色液体", "强烈刺激性气味", "-58°C", "35°C"));
        CHEMICALS.put("liquid_bromine_water", new ChemicalInfo("Br₂(aq)", "159.81", "high", "strong", "oxidizer", "3.5|acid", "1.00 g/cm³", "橙黄色液体", "刺激性气味", "0°C", "100°C"));
        CHEMICALS.put("liquid_chlorine_water", new ChemicalInfo("Cl₂(aq)", "70.90", "high", "strong", "oxidizer", "2.5|acid", "1.00 g/cm³", "淡黄绿色液体", "刺激性气味", "0°C", "100°C"));
        CHEMICALS.put("liquid_copper_sulfate_solution", new ChemicalInfo("CuSO₄", "159.60", "medium", "weak", "none", "4.0|acid", "1.10 g/cm³", "蓝色透明液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("liquid_hydrochloric_acid", new ChemicalInfo("HCl", "36.46", "medium", "strong", "none", "1.0|strong_acid", "1.05 g/cm³", "无色透明液体", "刺激性气味", "-27°C", "110°C"));
        CHEMICALS.put("liquid_hydrochloric_acid_concentrated", new ChemicalInfo("HCl", "36.46", "high", "strong", "none", "<1|strong_acid", "1.18 g/cm³", "无色液体（冒白雾）", "刺激性气味", "-27°C", "48°C"));
        CHEMICALS.put("liquid_hydrogen_peroxide", new ChemicalInfo("H₂O₂", "34.01", "low", "moderate", "oxidizer", "4.5|acid", "1.11 g/cm³", "无色透明液体", "无味", "-0.4°C", "150°C"));
        CHEMICALS.put("liquid_iodine_water", new ChemicalInfo("I₂(aq)", "253.81", "medium", "weak", "none", "5.0|acid", "1.00 g/cm³", "浅褐色液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("liquid_iron_chloride_solution", new ChemicalInfo("FeCl₃", "162.19", "medium", "moderate", "none", "2.5|acid", "1.10 g/cm³", "棕黄色液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("liquid_limewater_clear", new ChemicalInfo("Ca(OH)₂", "74.09", "low", "moderate", "none", "12.4|strong_alkaline", "1.00 g/cm³", "无色透明液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("liquid_limewater_cloudy", new ChemicalInfo("CaCO₃", "100.09", "none", "none", "none", "9.0|alkaline", "1.00 g/cm³", "乳白色浑浊液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("liquid_litmus_solution", new ChemicalInfo("C₇H₇NO₄", "169.14", "low", "none", "none", "n/a", "1.00 g/cm³", "紫色液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("liquid_methyl_orange_solution", new ChemicalInfo("C₁4H₁4N₃NaO₃S", "327.33", "low", "none", "none", "n/a", "1.00 g/cm³", "橙黄色液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("liquid_nitric_acid", new ChemicalInfo("HNO₃", "63.01", "high", "strong", "none", "1.0|strong_acid", "1.08 g/cm³", "无色透明液体", "刺激性气味", "-42°C", "121°C"));
        CHEMICALS.put("liquid_nitric_acid_concentrated", new ChemicalInfo("HNO₃", "63.01", "extreme", "strong", "oxidizer", "<1|strong_acid", "1.42 g/cm³", "淡黄色液体", "刺激性气味", "-42°C", "83°C"));
        CHEMICALS.put("liquid_phenolphthalein_solution", new ChemicalInfo("C₂0H₁4O₄", "318.33", "low", "none", "none", "n/a", "1.00 g/cm³", "无色透明液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("liquid_phosphoric_acid", new ChemicalInfo("H₃PO₄", "97.99", "medium", "strong", "none", "1.5|strong_acid", "1.08 g/cm³", "无色透明液体", "无味", "42°C", "158°C"));
        CHEMICALS.put("liquid_phosphoric_acid_concentrated", new ChemicalInfo("H₃PO₄", "97.99", "high", "strong", "none", "<1|strong_acid", "1.68 g/cm³", "无色黏稠液体", "无味", "42°C", "213°C"));
        CHEMICALS.put("liquid_potassium_carbonate_solution", new ChemicalInfo("K₂CO₃", "138.20", "low", "weak", "none", "11.5|alkaline", "1.10 g/cm³", "无色透明液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("liquid_potassium_hydroxide_solution", new ChemicalInfo("KOH", "56.11", "high", "strong", "none", "14.0|strong_alkaline", "1.15 g/cm³", "无色透明液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("liquid_potassium_permanganate_solution", new ChemicalInfo("KMnO₄", "158.03", "medium", "moderate", "oxidizer", "7.0|neutral", "1.00 g/cm³", "紫红色液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("liquid_sodium_carbonate_solution", new ChemicalInfo("Na₂CO₃", "105.99", "low", "weak", "none", "11.5|alkaline", "1.10 g/cm³", "无色透明液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("liquid_sodium_chloride_solution", new ChemicalInfo("NaCl", "58.44", "none", "none", "none", "7.0|neutral", "1.05 g/cm³", "无色透明液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("liquid_sodium_hydroxide_solution", new ChemicalInfo("NaOH", "40.00", "high", "strong", "none", "13.5|strong_alkaline", "1.15 g/cm³", "无色透明液体", "无味", "0°C", "105°C"));
        CHEMICALS.put("liquid_sulfuric_acid_concentrated", new ChemicalInfo("H₂SO₄", "98.07", "extreme", "strong", "none", "<1|strong_acid", "1.84 g/cm³", "无色油状液体", "无味", "10°C", "337°C"));
        CHEMICALS.put("liquid_sulfuric_acid_dilute", new ChemicalInfo("H₂SO₄", "98.07", "high", "strong", "none", "1.0|strong_acid", "1.08 g/cm³", "无色透明液体", "无味", "-20°C", "110°C"));
        CHEMICALS.put("liquid_water", new ChemicalInfo("H₂O", "18.02", "none", "none", "none", "7.0|neutral", "1.00 g/cm³", "无色透明液体", "无味", "0°C", "100°C"));
        CHEMICALS.put("ozone", new ChemicalInfo("O₃", "48.00", "high", "none", "oxidizer", "n/a", "2.14 g/L", "淡蓝色气体", "特殊气味", "-193°C", "-112°C"));
        CHEMICALS.put("red_phosphorus", new ChemicalInfo("P", "30.97", "low", "weak", "flammable", "n/a", "2.34 g/cm³", "红棕色粉末", "无味", "590°C", "280°C"));
        CHEMICALS.put("solid_activated_carbon", new ChemicalInfo("C", "12.01", "none", "none", "flammable", "n/a", "0.50 g/cm³", "黑色颗粒/粉末", "无味", "3550°C", "3642°C"));
        CHEMICALS.put("solid_alum", new ChemicalInfo("KAl(SO₄)₂·12H₂O", "474.37", "medium", "weak", "none", "4.5|acid", "1.76 g/cm³", "白色晶体", "无味", "200°C", "200°C"));
        CHEMICALS.put("solid_aluminium", new ChemicalInfo("Al", "26.98", "none", "none", "none", "n/a", "2.70 g/cm³", "银白色金属块", "无味", "660°C", "2519°C"));
        CHEMICALS.put("solid_aluminium_chloride", new ChemicalInfo("AlCl₃", "133.33", "medium", "strong", "none", "3.0|acid", "2.44 g/cm³", "白色或浅黄色粉末", "无味", "190°C", "178°C"));
        CHEMICALS.put("solid_aluminium_hydroxide", new ChemicalInfo("Al(OH)₃", "78.00", "none", "none", "none", "8.0|alkaline", "2.42 g/cm³", "白色粉末", "无味", "300°C", "300°C"));
        CHEMICALS.put("solid_aluminium_oxide", new ChemicalInfo("Al₂O₃", "101.96", "none", "none", "none", "n/a", "3.97 g/cm³", "白色粉末", "无味", "2072°C", "2977°C"));
        CHEMICALS.put("solid_aluminium_powder", new ChemicalInfo("Al", "26.98", "none", "none", "flammable", "n/a", "2.70 g/cm³", "银灰色粉末", "无味", "660°C", "2519°C"));
        CHEMICALS.put("solid_aluminium_sulfate", new ChemicalInfo("Al₂(SO₄)₃", "342.13", "medium", "moderate", "none", "3.5|acid", "2.67 g/cm³", "白色粉末", "无味", "770°C", "770°C"));
        CHEMICALS.put("solid_ammonium_bicarbonate", new ChemicalInfo("NH₄HCO₃", "79.06", "low", "weak", "none", "8.0|alkaline", "1.59 g/cm³", "白色粉末", "无味", "36°C", "36°C"));
        CHEMICALS.put("solid_barium", new ChemicalInfo("Ba", "137.33", "high", "moderate", "water_flammable", "n/a", "3.62 g/cm³", "银白色金属块", "无味", "727°C", "1897°C"));
        CHEMICALS.put("solid_barium_carbonate", new ChemicalInfo("BaCO₃", "197.33", "high", "none", "none", "n/a", "4.29 g/cm³", "白色粉末", "无味", "811°C", "1450°C"));
        CHEMICALS.put("solid_barium_chloride", new ChemicalInfo("BaCl₂", "208.23", "high", "moderate", "none", "7.0|neutral", "3.86 g/cm³", "白色晶体", "无味", "962°C", "1560°C"));
        CHEMICALS.put("solid_barium_hydroxide", new ChemicalInfo("Ba(OH)₂", "171.34", "high", "strong", "none", "13.0|strong_alkaline", "2.18 g/cm³", "白色固体", "无味", "408°C", "780°C"));
        CHEMICALS.put("solid_barium_powder", new ChemicalInfo("Ba", "137.33", "high", "moderate", "water_flammable", "n/a", "3.62 g/cm³", "灰白色粉末", "无味", "727°C", "1897°C"));
        CHEMICALS.put("solid_barium_sulfate", new ChemicalInfo("BaSO₄", "233.38", "none", "none", "none", "n/a", "4.50 g/cm³", "白色粉末", "无味", "1580°C", "1600°C"));
        CHEMICALS.put("solid_boric_acid", new ChemicalInfo("H₃BO₃", "61.83", "low", "weak", "none", "5.0|acid", "1.44 g/cm³", "白色结晶粉末", "无味", "171°C", "300°C"));
        CHEMICALS.put("solid_calcium", new ChemicalInfo("Ca", "40.08", "low", "moderate", "water_flammable", "n/a", "1.54 g/cm³", "银白色金属块", "无味", "842°C", "1484°C"));
        CHEMICALS.put("solid_calcium_carbonate", new ChemicalInfo("CaCO₃", "100.09", "none", "none", "none", "9.0|alkaline", "2.71 g/cm³", "白色粉末", "无味", "825°C", "899°C"));
        CHEMICALS.put("solid_calcium_chloride", new ChemicalInfo("CaCl₂", "110.98", "low", "weak", "none", "7.0|neutral", "2.15 g/cm³", "白色固体", "无味", "772°C", "1935°C"));
        CHEMICALS.put("solid_calcium_hydroxide", new ChemicalInfo("Ca(OH)₂", "74.09", "medium", "strong", "none", "12.4|strong_alkaline", "2.21 g/cm³", "白色粉末", "无味", "580°C", "580°C"));
        CHEMICALS.put("solid_calcium_oxide", new ChemicalInfo("CaO", "56.08", "medium", "strong", "none", "12.5|strong_alkaline", "3.35 g/cm³", "白色块状/粉末", "无味", "2572°C", "2850°C"));
        CHEMICALS.put("solid_calcium_phosphate", new ChemicalInfo("Ca₃(PO₄)₂", "310.17", "none", "none", "none", "n/a", "3.14 g/cm³", "白色粉末", "无味", "1670°C", "1600°C"));













        CHEMICALS.put("solid_calcium_powder", new ChemicalInfo("Ca", "40.08", "low", "moderate", "water_flammable", "n/a", "1.54 g/cm³", "灰白色粉末", "无味", "842°C", "1484°C"));
        CHEMICALS.put("solid_calcium_sulfate", new ChemicalInfo("CaSO₄", "136.13", "none", "none", "none", "7.0|neutral", "2.96 g/cm³", "白色粉末", "无味", "1450°C", "1450°C"));
        CHEMICALS.put("solid_charcoal", new ChemicalInfo("C", "12.01", "none", "none", "flammable", "n/a", "0.40 g/cm³", "黑色块状", "无味", "3550°C", "3642°C"));
        CHEMICALS.put("solid_copper", new ChemicalInfo("Cu", "63.55", "low", "none", "none", "n/a", "8.96 g/cm³", "紫红色金属块", "无味", "1084°C", "2562°C"));
        CHEMICALS.put("solid_copper_i_oxide", new ChemicalInfo("Cu₂O", "143.09", "medium", "none", "none", "n/a", "6.00 g/cm³", "砖红色粉末", "无味", "1235°C", "1800°C"));
        CHEMICALS.put("solid_copper_ii_hydroxide", new ChemicalInfo("Cu(OH)₂", "97.56", "medium", "none", "none", "7.0|neutral", "3.37 g/cm³", "蓝色粉末", "无味", "100°C", "100°C"));
        CHEMICALS.put("solid_copper_ii_oxide", new ChemicalInfo("CuO", "79.55", "medium", "none", "none", "n/a", "6.31 g/cm³", "黑色粉末", "无味", "1326°C", "2000°C"));
        CHEMICALS.put("solid_copper_powder", new ChemicalInfo("Cu", "63.55", "low", "none", "flammable", "n/a", "8.96 g/cm³", "红棕色粉末", "无味", "1084°C", "2562°C"));
        CHEMICALS.put("solid_copper_sulfate_anhydrous", new ChemicalInfo("CuSO₄", "159.60", "medium", "moderate", "none", "4.0|acid", "3.60 g/cm³", "白色粉末", "无味", "560°C", "650°C"));
        CHEMICALS.put("solid_copper_sulfate_pentahydrate", new ChemicalInfo("CuSO₄·5H₂O", "249.68", "medium", "weak", "none", "4.0|acid", "2.29 g/cm³", "蓝色晶体", "无味", "110°C", "150°C"));
        CHEMICALS.put("solid_diamond", new ChemicalInfo("C", "12.01", "none", "none", "none", "n/a", "3.52 g/cm³", "无色透明晶体", "无味", "3550°C", "3642°C"));
        CHEMICALS.put("solid_graphite", new ChemicalInfo("C", "12.01", "none", "none", "flammable", "n/a", "2.27 g/cm³", "灰黑色粉末", "无味", "3550°C", "3642°C"));
        CHEMICALS.put("solid_iodine", new ChemicalInfo("I₂", "253.81", "medium", "weak", "none", "n/a", "4.93 g/cm³", "紫黑色固体", "无味", "114°C", "184°C"));
        CHEMICALS.put("solid_iron", new ChemicalInfo("Fe", "55.84", "none", "none", "none", "n/a", "7.87 g/cm³", "银白色金属块", "无味", "1538°C", "2861°C"));
        CHEMICALS.put("solid_iron_ii_chloride", new ChemicalInfo("FeCl₂", "126.75", "medium", "weak", "none", "4.0|acid", "3.16 g/cm³", "浅绿色固体", "无味", "677°C", "1023°C"));
        CHEMICALS.put("solid_iron_ii_hydroxide", new ChemicalInfo("Fe(OH)₂", "89.86", "low", "none", "none", "7.0|neutral", "3.40 g/cm³", "白色粉末", "无味", "500°C", "500°C"));
        CHEMICALS.put("solid_iron_ii_iii_oxide", new ChemicalInfo("Fe₃O₄", "231.53", "low", "none", "none", "n/a", "5.17 g/cm³", "黑色粉末", "无味", "1597°C", "2623°C"));
        CHEMICALS.put("solid_iron_iii_chloride", new ChemicalInfo("FeCl₃", "162.19", "medium", "moderate", "none", "2.5|acid", "2.90 g/cm³", "棕黄色固体", "无味", "304°C", "315°C"));
        CHEMICALS.put("solid_iron_iii_hydroxide", new ChemicalInfo("Fe(OH)₃", "106.87", "low", "none", "none", "7.0|neutral", "3.40 g/cm³", "红褐色粉末", "无味", "500°C", "500°C"));
        CHEMICALS.put("solid_iron_iii_oxide", new ChemicalInfo("Fe₂O₃", "159.69", "low", "none", "none", "n/a", "5.24 g/cm³", "红棕色粉末", "无味", "1565°C", "1565°C"));
        CHEMICALS.put("solid_iron_iii_sulfate", new ChemicalInfo("Fe₂(SO₄)₃", "399.86", "medium", "moderate", "none", "2.5|acid", "3.10 g/cm³", "淡黄色粉末", "无味", "480°C", "480°C"));
        CHEMICALS.put("solid_iron_powder", new ChemicalInfo("Fe", "55.84", "none", "none", "flammable", "n/a", "7.87 g/cm³", "灰黑色粉末", "无味", "1538°C", "2861°C"));
        CHEMICALS.put("solid_iron_sulfate", new ChemicalInfo("FeSO₄", "151.90", "medium", "weak", "none", "4.5|acid", "3.65 g/cm³", "浅绿色粉末", "无味", "671°C", "671°C"));
        CHEMICALS.put("solid_iron_sulfate_heptahydrate", new ChemicalInfo("FeSO₄·7H₂O", "278.01", "medium", "weak", "none", "4.5|acid", "1.90 g/cm³", "浅绿色晶体", "无味", "150°C", "150°C"));
        CHEMICALS.put("solid_lithium", new ChemicalInfo("Li", "6.94", "low", "strong", "water_flammable", "n/a", "0.53 g/cm³", "银白色金属块", "无味", "180°C", "1342°C"));
        CHEMICALS.put("solid_lithium_powder", new ChemicalInfo("Li", "6.94", "low", "strong", "water_flammable", "n/a", "0.53 g/cm³", "银灰色粉末", "无味", "180°C", "1342°C"));
        CHEMICALS.put("solid_magnesium", new ChemicalInfo("Mg", "24.30", "none", "none", "flammable", "n/a", "1.74 g/cm³", "银白色金属块", "无味", "650°C", "1090°C"));
        CHEMICALS.put("solid_magnesium_chloride", new ChemicalInfo("MgCl₂", "95.21", "low", "weak", "none", "5.5|acid", "2.32 g/cm³", "白色结晶粉末", "无味", "714°C", "1412°C"));
        CHEMICALS.put("solid_magnesium_hydroxide", new ChemicalInfo("Mg(OH)₂", "58.32", "low", "weak", "none", "10.0|alkaline", "2.34 g/cm³", "白色粉末", "无味", "350°C", "350°C"));
        CHEMICALS.put("solid_magnesium_oxide", new ChemicalInfo("MgO", "40.30", "low", "weak", "none", "10.0|alkaline", "3.58 g/cm³", "白色粉末", "无味", "2852°C", "3600°C"));
        CHEMICALS.put("solid_magnesium_powder", new ChemicalInfo("Mg", "24.30", "none", "none", "flammable", "n/a", "1.74 g/cm³", "银灰色粉末", "无味", "650°C", "1090°C"));
        CHEMICALS.put("solid_magnesium_sulfate", new ChemicalInfo("MgSO₄", "120.36", "low", "none", "none", "6.5|neutral", "2.66 g/cm³", "白色结晶粉末", "无味", "1124°C", "1124°C"));
        CHEMICALS.put("solid_manganese_dioxide", new ChemicalInfo("MnO₂", "86.94", "medium", "none", "oxidizer", "n/a", "5.03 g/cm³", "黑色粉末", "无味", "535°C", "535°C"));
        CHEMICALS.put("solid_marble", new ChemicalInfo("CaCO₃", "100.09", "none", "none", "none", "9.0|alkaline", "2.71 g/cm³", "白色致密块状", "无味", "825°C", "899°C"));
        CHEMICALS.put("solid_phosphorus_pentoxide", new ChemicalInfo("P₂O₅", "141.94", "high", "strong", "none", "1.0|strong_acid", "2.39 g/cm³", "白色粉末", "无味", "340°C", "360°C"));
        CHEMICALS.put("solid_potassium_chlorate", new ChemicalInfo("KClO₃", "122.55", "medium", "weak", "explosive", "7.0|neutral", "2.32 g/cm³", "白色晶体", "无味", "356°C", "400°C"));
        CHEMICALS.put("solid_potassium_chloride", new ChemicalInfo("KCl", "74.55", "none", "none", "none", "7.0|neutral", "1.98 g/cm³", "白色晶体", "无味", "770°C", "1420°C"));
        CHEMICALS.put("solid_potassium_dichromate", new ChemicalInfo("K₂Cr₂O₇", "294.18", "high", "strong", "oxidizer", "4.0|acid", "2.68 g/cm³", "橙红色晶体", "无味", "398°C", "500°C"));
        CHEMICALS.put("solid_potassium_hydroxide", new ChemicalInfo("KOH", "56.11", "high", "strong", "none", "14.0|strong_alkaline", "2.04 g/cm³", "白色固体", "无味", "360°C", "1327°C"));
        CHEMICALS.put("solid_potassium_manganate", new ChemicalInfo("K₂MnO₄", "197.13", "high", "moderate", "oxidizer", "7.0|neutral", "2.78 g/cm³", "墨绿色晶体", "无味", "190°C", "190°C"));
        CHEMICALS.put("solid_potassium_nitrate", new ChemicalInfo("KNO₃", "101.10", "medium", "none", "oxidizer", "7.0|neutral", "2.11 g/cm³", "白色晶体", "无味", "334°C", "400°C"));
        CHEMICALS.put("solid_potassium_permanganate", new ChemicalInfo("KMnO₄", "158.03", "medium", "moderate", "oxidizer", "7.0|neutral", "2.70 g/cm³", "紫黑色晶体", "无味", "240°C", "240°C"));
        CHEMICALS.put("solid_red_phosphorus", new ChemicalInfo("P", "30.97", "low", "weak", "flammable", "n/a", "2.34 g/cm³", "红棕色粉末", "无味", "590°C", "280°C"));
        CHEMICALS.put("solid_silicon", new ChemicalInfo("Si", "28.09", "none", "none", "none", "n/a", "2.33 g/cm³", "灰黑色固体", "无味", "1414°C", "3265°C"));
        CHEMICALS.put("solid_silicon_dioxide", new ChemicalInfo("SiO₂", "60.08", "none", "none", "none", "n/a", "2.65 g/cm³", "白色/无色粉末", "无味", "1710°C", "2230°C"));
        CHEMICALS.put("solid_silver", new ChemicalInfo("Ag", "107.87", "low", "none", "none", "n/a", "10.49 g/cm³", "银白色金属块", "无味", "962°C", "2162°C"));
        CHEMICALS.put("solid_silver_chloride", new ChemicalInfo("AgCl", "143.32", "low", "none", "none", "n/a", "5.56 g/cm³", "白色粉末", "无味", "455°C", "1547°C"));
        CHEMICALS.put("solid_silver_nitrate", new ChemicalInfo("AgNO₃", "169.87", "high", "strong", "oxidizer", "7.0|neutral", "4.35 g/cm³", "白色晶体", "无味", "212°C", "444°C"));
        CHEMICALS.put("solid_silver_powder", new ChemicalInfo("Ag", "107.87", "low", "none", "none", "n/a", "10.49 g/cm³", "银灰色粉末", "无味", "962°C", "2162°C"));
        CHEMICALS.put("solid_sodium", new ChemicalInfo("Na", "22.99", "low", "strong", "water_flammable", "n/a", "0.97 g/cm³", "银白色金属块", "无味", "98°C", "883°C"));
        CHEMICALS.put("solid_potassium", new ChemicalInfo("K", "39.10", "low", "strong", "water_flammable", "n/a", "0.86 g/cm³", "银白色金属块", "无味", "63°C", "759°C"));
        CHEMICALS.put("solid_sodium_bicarbonate", new ChemicalInfo("NaHCO₃", "84.01", "low", "none", "none", "8.3|alkaline", "2.20 g/cm³", "白色粉末", "无味", "270°C", "270°C"));
        CHEMICALS.put("solid_sodium_carbonate", new ChemicalInfo("Na₂CO₃", "105.99", "low", "weak", "none", "11.5|alkaline", "2.54 g/cm³", "白色粉末", "无味", "851°C", "1600°C"));
        CHEMICALS.put("solid_potassium_carbonate", new ChemicalInfo("K₂CO₃", "138.21", "low", "weak", "none", "11.5|alkaline", "2.43 g/cm³", "白色粉末", "无味", "891°C", "1300°C"));
        CHEMICALS.put("solid_sodium_chloride", new ChemicalInfo("NaCl", "58.44", "none", "none", "none", "7.0|neutral", "2.16 g/cm³", "白色晶体/粉末", "无味", "801°C", "1465°C"));
        CHEMICALS.put("solid_sodium_hydroxide", new ChemicalInfo("NaOH", "40.00", "high", "strong", "none", "14.0|strong_alkaline", "2.13 g/cm³", "白色块状/粉末", "无味", "318°C", "1388°C"));
        CHEMICALS.put("solid_sodium_powder", new ChemicalInfo("Na", "22.99", "low", "strong", "water_flammable", "n/a", "0.97 g/cm³", "银白色粉末", "无味", "98°C", "883°C"));
        CHEMICALS.put("solid_sulfur", new ChemicalInfo("S", "32.06", "low", "none", "flammable", "n/a", "2.07 g/cm³", "淡黄色粉末", "无味", "115°C", "445°C"));
        CHEMICALS.put("solid_white_phosphorus", new ChemicalInfo("P₄", "123.90", "extreme", "strong", "self_ignite", "n/a", "1.83 g/cm³", "白色蜡状固体", "无味", "44°C", "280°C"));
        CHEMICALS.put("solid_zinc", new ChemicalInfo("Zn", "65.38", "low", "none", "none", "n/a", "7.14 g/cm³", "银白色金属块", "无味", "420°C", "907°C"));
        CHEMICALS.put("solid_zinc_chloride", new ChemicalInfo("ZnCl₂", "136.28", "medium", "moderate", "none", "4.0|acid", "2.91 g/cm³", "白色结晶粉末", "无味", "290°C", "732°C"));
        CHEMICALS.put("solid_zinc_powder", new ChemicalInfo("Zn", "65.38", "low", "none", "flammable", "n/a", "7.14 g/cm³", "灰白色粉末", "无味", "420°C", "907°C"));
        CHEMICALS.put("solid_zinc_sulfate", new ChemicalInfo("ZnSO₄", "161.44", "medium", "moderate", "none", "4.5|acid", "3.54 g/cm³", "白色粉末", "无味", "680°C", "740°C"));
        CHEMICALS.put("white_phosphorus", new ChemicalInfo("P₄", "123.90", "extreme", "strong", "self_ignite", "n/a", "1.83 g/cm³", "白色蜡状固体", "大蒜气味", "44°C", "280°C"));
        MOLAR_MASS.put("liquid_water", 18.02);
        DENSITY.put("liquid_water", 1.0);
        BOILING_POINT.put("liquid_water", 100);
        MOLAR_MASS.put("liquid_hydrogen_peroxide", 34.01);
        DENSITY.put("liquid_hydrogen_peroxide", 1.11);
        BOILING_POINT.put("liquid_hydrogen_peroxide", 150);
        MOLAR_MASS.put("liquid_sulfuric_acid_dilute", 98.07);
        DENSITY.put("liquid_sulfuric_acid_dilute", 1.08);
        BOILING_POINT.put("liquid_sulfuric_acid_dilute", 110);
        MOLAR_MASS.put("liquid_sulfuric_acid_concentrated", 98.07);
        DENSITY.put("liquid_sulfuric_acid_concentrated", 1.84);
        BOILING_POINT.put("liquid_sulfuric_acid_concentrated", 337);
        MOLAR_MASS.put("liquid_hydrochloric_acid", 36.46);
        DENSITY.put("liquid_hydrochloric_acid", 1.05);
        BOILING_POINT.put("liquid_hydrochloric_acid", 110);
        MOLAR_MASS.put("liquid_hydrochloric_acid_concentrated", 36.46);
        DENSITY.put("liquid_hydrochloric_acid_concentrated", 1.18);
        BOILING_POINT.put("liquid_hydrochloric_acid_concentrated", 48);
        MOLAR_MASS.put("liquid_nitric_acid", 63.01);
        DENSITY.put("liquid_nitric_acid", 1.08);
        BOILING_POINT.put("liquid_nitric_acid", 121);
        MOLAR_MASS.put("liquid_nitric_acid_concentrated", 63.01);
        DENSITY.put("liquid_nitric_acid_concentrated", 1.42);
        BOILING_POINT.put("liquid_nitric_acid_concentrated", 83);
        MOLAR_MASS.put("liquid_phosphoric_acid", 97.99);
        DENSITY.put("liquid_phosphoric_acid", 1.08);
        BOILING_POINT.put("liquid_phosphoric_acid", 158);
        MOLAR_MASS.put("liquid_phosphoric_acid_concentrated", 97.99);
        DENSITY.put("liquid_phosphoric_acid_concentrated", 1.68);
        BOILING_POINT.put("liquid_phosphoric_acid_concentrated", 213);
        MOLAR_MASS.put("liquid_ammonia_water", 35.05);
        DENSITY.put("liquid_ammonia_water", 0.91);
        BOILING_POINT.put("liquid_ammonia_water", 35);
        MOLAR_MASS.put("liquid_ammonia_water_concentrated", 35.05);
        DENSITY.put("liquid_ammonia_water_concentrated", 0.88);
        BOILING_POINT.put("liquid_ammonia_water_concentrated", 35);
        MOLAR_MASS.put("liquid_sodium_hydroxide_solution", 40.0);
        DENSITY.put("liquid_sodium_hydroxide_solution", 1.15);
        BOILING_POINT.put("liquid_sodium_hydroxide_solution", 105);
        MOLAR_MASS.put("liquid_sodium_carbonate_solution", 105.99);
        DENSITY.put("liquid_sodium_carbonate_solution", 1.1);
        BOILING_POINT.put("liquid_sodium_carbonate_solution", 105);
        MOLAR_MASS.put("liquid_potassium_hydroxide_solution", 56.11);
        DENSITY.put("liquid_potassium_hydroxide_solution", 1.15);
        BOILING_POINT.put("liquid_potassium_hydroxide_solution", 105);
        MOLAR_MASS.put("liquid_potassium_carbonate_solution", 138.2);
        DENSITY.put("liquid_potassium_carbonate_solution", 1.1);
        BOILING_POINT.put("liquid_potassium_carbonate_solution", 105);
        MOLAR_MASS.put("liquid_limewater_clear", 74.09);
        DENSITY.put("liquid_limewater_clear", 1.0);
        BOILING_POINT.put("liquid_limewater_clear", 100);
        MOLAR_MASS.put("liquid_limewater_cloudy", 100.09);
        DENSITY.put("liquid_limewater_cloudy", 1.0);
        BOILING_POINT.put("liquid_limewater_cloudy", 100);
        MOLAR_MASS.put("liquid_sodium_chloride_solution", 58.44);
        DENSITY.put("liquid_sodium_chloride_solution", 1.05);
        BOILING_POINT.put("liquid_sodium_chloride_solution", 105);
        MOLAR_MASS.put("liquid_iron_chloride_solution", 162.19);
        DENSITY.put("liquid_iron_chloride_solution", 1.1);
        BOILING_POINT.put("liquid_iron_chloride_solution", 105);
        MOLAR_MASS.put("liquid_copper_sulfate_solution", 159.6);
        DENSITY.put("liquid_copper_sulfate_solution", 1.1);
        BOILING_POINT.put("liquid_copper_sulfate_solution", 105);
        MOLAR_MASS.put("liquid_potassium_permanganate_solution", 158.03);
        DENSITY.put("liquid_potassium_permanganate_solution", 1.0);
        BOILING_POINT.put("liquid_potassium_permanganate_solution", 105);
        MOLAR_MASS.put("liquid_chlorine_water", 70.9);
        DENSITY.put("liquid_chlorine_water", 1.0);
        BOILING_POINT.put("liquid_chlorine_water", 100);
        MOLAR_MASS.put("liquid_bromine_water", 159.81);
        DENSITY.put("liquid_bromine_water", 1.0);
        BOILING_POINT.put("liquid_bromine_water", 100);
        MOLAR_MASS.put("liquid_iodine_water", 253.81);
        DENSITY.put("liquid_iodine_water", 1.0);
        BOILING_POINT.put("liquid_iodine_water", 100);
        MOLAR_MASS.put("liquid_litmus_solution", 169.14);
        DENSITY.put("liquid_litmus_solution", 1.0);
        BOILING_POINT.put("liquid_litmus_solution", 100);
        MOLAR_MASS.put("liquid_phenolphthalein_solution", 318.33);
        DENSITY.put("liquid_phenolphthalein_solution", 1.0);
        BOILING_POINT.put("liquid_phenolphthalein_solution", 100);
        MOLAR_MASS.put("liquid_methyl_orange_solution", 327.33);
        DENSITY.put("liquid_methyl_orange_solution", 1.0);
        BOILING_POINT.put("liquid_methyl_orange_solution", 100);
        MOLAR_MASS.put("gas_oxygen", 32.0);
        MOLAR_MASS.put("gas_hydrogen", 2.02);
        MOLAR_MASS.put("gas_nitrogen", 28.01);
        MOLAR_MASS.put("gas_chlorine", 70.9);
        MOLAR_MASS.put("gas_helium", 4.0);
        MOLAR_MASS.put("gas_neon", 20.18);
        MOLAR_MASS.put("gas_argon", 39.95);
        MOLAR_MASS.put("gas_krypton", 83.8);
        MOLAR_MASS.put("gas_xenon", 131.29);
        MOLAR_MASS.put("gas_carbon_dioxide", 44.01);
        MOLAR_MASS.put("gas_carbon_monoxide", 28.01);
        MOLAR_MASS.put("gas_sulfur_dioxide", 64.06);
        MOLAR_MASS.put("gas_nitric_oxide", 30.01);
        MOLAR_MASS.put("gas_nitrogen_dioxide", 46.01);
        MOLAR_MASS.put("gas_ammonia", 17.03);
        MOLAR_MASS.put("gas_hydrogen_chloride", 36.46);
        MOLAR_MASS.put("gas_hydrogen_sulfide", 34.08);
        MOLAR_MASS.put("solid_marble", 100.09);
        BOILING_POINT.put("solid_marble", 899);
        MOLAR_MASS.put("solid_iron", 55.84);
        BOILING_POINT.put("solid_iron", 2861);
        MOLAR_MASS.put("solid_iron_powder", 55.84);
        BOILING_POINT.put("solid_iron_powder", 2861);
        MOLAR_MASS.put("solid_copper", 63.55);
        BOILING_POINT.put("solid_copper", 2562);
        MOLAR_MASS.put("solid_copper_powder", 63.55);
        BOILING_POINT.put("solid_copper_powder", 2562);
        MOLAR_MASS.put("solid_aluminium", 26.98);
        BOILING_POINT.put("solid_aluminium", 2519);
        MOLAR_MASS.put("solid_aluminium_powder", 26.98);
        BOILING_POINT.put("solid_aluminium_powder", 2519);
        MOLAR_MASS.put("solid_zinc", 65.38);
        BOILING_POINT.put("solid_zinc", 907);
        MOLAR_MASS.put("solid_zinc_powder", 65.38);
        BOILING_POINT.put("solid_zinc_powder", 907);
        MOLAR_MASS.put("solid_magnesium", 24.3);
        BOILING_POINT.put("solid_magnesium", 1090);
        MOLAR_MASS.put("solid_magnesium_powder", 24.3);
        BOILING_POINT.put("solid_magnesium_powder", 1090);
        MOLAR_MASS.put("solid_sodium", 22.99);
        BOILING_POINT.put("solid_sodium", 883);
        MOLAR_MASS.put("solid_potassium", 39.10);
        BOILING_POINT.put("solid_potassium", 759);
        MOLAR_MASS.put("solid_sodium_powder", 22.99);
        BOILING_POINT.put("solid_sodium_powder", 883);
        MOLAR_MASS.put("solid_calcium", 40.08);
        BOILING_POINT.put("solid_calcium", 1484);
        MOLAR_MASS.put("solid_calcium_powder", 40.08);
        BOILING_POINT.put("solid_calcium_powder", 1484);
        MOLAR_MASS.put("solid_silver", 107.87);
        BOILING_POINT.put("solid_silver", 2162);
        MOLAR_MASS.put("solid_silver_powder", 107.87);
        BOILING_POINT.put("solid_silver_powder", 2162);
        MOLAR_MASS.put("solid_barium", 137.33);
        BOILING_POINT.put("solid_barium", 1897);
        MOLAR_MASS.put("solid_barium_powder", 137.33);
        BOILING_POINT.put("solid_barium_powder", 1897);
        MOLAR_MASS.put("solid_lithium", 6.94);
        BOILING_POINT.put("solid_lithium", 1342);
        MOLAR_MASS.put("solid_lithium_powder", 6.94);
        BOILING_POINT.put("solid_lithium_powder", 1342);
        MOLAR_MASS.put("solid_diamond", 12.01);
        BOILING_POINT.put("solid_diamond", 3642);
        MOLAR_MASS.put("solid_graphite", 12.01);
        BOILING_POINT.put("solid_graphite", 3642);
        MOLAR_MASS.put("solid_charcoal", 12.01);
        BOILING_POINT.put("solid_charcoal", 3642);
        MOLAR_MASS.put("solid_activated_carbon", 12.01);
        BOILING_POINT.put("solid_activated_carbon", 3642);
        MOLAR_MASS.put("solid_sulfur", 32.06);
        BOILING_POINT.put("solid_sulfur", 445);
        MOLAR_MASS.put("solid_red_phosphorus", 30.97);
        BOILING_POINT.put("solid_red_phosphorus", 280);
        MOLAR_MASS.put("solid_white_phosphorus", 123.9);
        BOILING_POINT.put("solid_white_phosphorus", 280);
        MOLAR_MASS.put("solid_iodine", 253.81);
        BOILING_POINT.put("solid_iodine", 184);
        MOLAR_MASS.put("solid_silicon", 28.09);
        BOILING_POINT.put("solid_silicon", 3265);
        MOLAR_MASS.put("solid_calcium_oxide", 56.08);
        BOILING_POINT.put("solid_calcium_oxide", 2850);
        MOLAR_MASS.put("solid_magnesium_oxide", 40.3);
        BOILING_POINT.put("solid_magnesium_oxide", 3600);
        MOLAR_MASS.put("solid_copper_ii_oxide", 79.55);
        BOILING_POINT.put("solid_copper_ii_oxide", 2000);
        MOLAR_MASS.put("solid_copper_i_oxide", 143.09);
        BOILING_POINT.put("solid_copper_i_oxide", 1800);
        MOLAR_MASS.put("solid_iron_iii_oxide", 159.69);
        BOILING_POINT.put("solid_iron_iii_oxide", 1565);
        MOLAR_MASS.put("solid_iron_ii_iii_oxide", 231.53);
        BOILING_POINT.put("solid_iron_ii_iii_oxide", 2623);
        MOLAR_MASS.put("solid_aluminium_oxide", 101.96);
        BOILING_POINT.put("solid_aluminium_oxide", 2977);
        MOLAR_MASS.put("solid_silicon_dioxide", 60.08);
        BOILING_POINT.put("solid_silicon_dioxide", 2230);
        MOLAR_MASS.put("solid_manganese_dioxide", 86.94);
        BOILING_POINT.put("solid_manganese_dioxide", 535);
        MOLAR_MASS.put("solid_phosphorus_pentoxide", 141.94);
        BOILING_POINT.put("solid_phosphorus_pentoxide", 360);
        MOLAR_MASS.put("solid_boric_acid", 61.83);
        BOILING_POINT.put("solid_boric_acid", 300);
        MOLAR_MASS.put("solid_sodium_hydroxide", 40.0);
        BOILING_POINT.put("solid_sodium_hydroxide", 1388);
        MOLAR_MASS.put("solid_potassium_hydroxide", 56.11);
        BOILING_POINT.put("solid_potassium_hydroxide", 1327);
        MOLAR_MASS.put("solid_calcium_hydroxide", 74.09);
        BOILING_POINT.put("solid_calcium_hydroxide", 580);
        MOLAR_MASS.put("solid_barium_hydroxide", 171.34);
        BOILING_POINT.put("solid_barium_hydroxide", 780);
        MOLAR_MASS.put("solid_magnesium_hydroxide", 58.32);
        BOILING_POINT.put("solid_magnesium_hydroxide", 350);
        MOLAR_MASS.put("solid_aluminium_hydroxide", 78.0);
        BOILING_POINT.put("solid_aluminium_hydroxide", 300);
        MOLAR_MASS.put("solid_iron_iii_hydroxide", 106.87);
        BOILING_POINT.put("solid_iron_iii_hydroxide", 500);
        MOLAR_MASS.put("solid_copper_ii_hydroxide", 97.56);
        BOILING_POINT.put("solid_copper_ii_hydroxide", 100);
        MOLAR_MASS.put("solid_iron_ii_hydroxide", 89.86);
        BOILING_POINT.put("solid_iron_ii_hydroxide", 500);
        MOLAR_MASS.put("solid_sodium_chloride", 58.44);
        BOILING_POINT.put("solid_sodium_chloride", 1465);
        MOLAR_MASS.put("solid_potassium_chloride", 74.55);
        BOILING_POINT.put("solid_potassium_chloride", 1420);
        MOLAR_MASS.put("solid_calcium_chloride", 110.98);
        BOILING_POINT.put("solid_calcium_chloride", 1935);
        MOLAR_MASS.put("solid_barium_chloride", 208.23);
        BOILING_POINT.put("solid_barium_chloride", 1560);
        MOLAR_MASS.put("solid_iron_iii_chloride", 162.19);
        BOILING_POINT.put("solid_iron_iii_chloride", 315);
        MOLAR_MASS.put("solid_iron_ii_chloride", 126.75);
        BOILING_POINT.put("solid_iron_ii_chloride", 1023);
        MOLAR_MASS.put("solid_silver_chloride", 143.32);
        BOILING_POINT.put("solid_silver_chloride", 1547);
        MOLAR_MASS.put("solid_sodium_carbonate", 105.99);
        BOILING_POINT.put("solid_sodium_carbonate", 1600);
        MOLAR_MASS.put("solid_potassium_carbonate", 138.21);
        BOILING_POINT.put("solid_potassium_carbonate", 1300);
        MOLAR_MASS.put("solid_sodium_bicarbonate", 84.01);
        BOILING_POINT.put("solid_sodium_bicarbonate", 270);
        MOLAR_MASS.put("solid_calcium_carbonate", 100.09);
        BOILING_POINT.put("solid_calcium_carbonate", 899);
        MOLAR_MASS.put("solid_barium_carbonate", 197.33);
        BOILING_POINT.put("solid_barium_carbonate", 1450);
        MOLAR_MASS.put("solid_ammonium_bicarbonate", 79.06);
        BOILING_POINT.put("solid_ammonium_bicarbonate", 36);
        MOLAR_MASS.put("solid_copper_sulfate_anhydrous", 159.6);
        BOILING_POINT.put("solid_copper_sulfate_anhydrous", 650);
        MOLAR_MASS.put("solid_copper_sulfate_pentahydrate", 249.68);
        BOILING_POINT.put("solid_copper_sulfate_pentahydrate", 150);
        MOLAR_MASS.put("solid_iron_sulfate", 151.9);
        BOILING_POINT.put("solid_iron_sulfate", 671);
        MOLAR_MASS.put("solid_iron_sulfate_heptahydrate", 278.01);
        BOILING_POINT.put("solid_iron_sulfate_heptahydrate", 150);
        MOLAR_MASS.put("solid_iron_iii_sulfate", 399.86);
        BOILING_POINT.put("solid_iron_iii_sulfate", 480);
        MOLAR_MASS.put("solid_magnesium_chloride", 95.21);
        BOILING_POINT.put("solid_magnesium_chloride", 1412);
        MOLAR_MASS.put("solid_zinc_chloride", 136.28);
        BOILING_POINT.put("solid_zinc_chloride", 732);
        MOLAR_MASS.put("solid_aluminium_chloride", 133.33);
        BOILING_POINT.put("solid_aluminium_chloride", 178);
        MOLAR_MASS.put("solid_magnesium_sulfate", 120.36);
        BOILING_POINT.put("solid_magnesium_sulfate", 1124);
        MOLAR_MASS.put("solid_zinc_sulfate", 161.44);
        BOILING_POINT.put("solid_zinc_sulfate", 740);
        MOLAR_MASS.put("solid_aluminium_sulfate", 342.13);
        BOILING_POINT.put("solid_aluminium_sulfate", 770);
        MOLAR_MASS.put("solid_barium_sulfate", 233.38);
        BOILING_POINT.put("solid_barium_sulfate", 1600);
        MOLAR_MASS.put("solid_calcium_sulfate", 136.13);
        BOILING_POINT.put("solid_calcium_sulfate", 1450);
        MOLAR_MASS.put("solid_potassium_nitrate", 101.1);
        BOILING_POINT.put("solid_potassium_nitrate", 400);
        MOLAR_MASS.put("solid_silver_nitrate", 169.87);
        BOILING_POINT.put("solid_silver_nitrate", 444);
        MOLAR_MASS.put("solid_potassium_permanganate", 158.03);
        BOILING_POINT.put("solid_potassium_permanganate", 240);
        MOLAR_MASS.put("solid_potassium_manganate", 197.13);
        BOILING_POINT.put("solid_potassium_manganate", 190);
        MOLAR_MASS.put("solid_potassium_chlorate", 122.55);
        BOILING_POINT.put("solid_potassium_chlorate", 400);
        MOLAR_MASS.put("solid_potassium_dichromate", 294.18);
        BOILING_POINT.put("solid_potassium_dichromate", 500);
        MOLAR_MASS.put("solid_alum", 474.37);
        BOILING_POINT.put("solid_alum", 200);
        MOLAR_MASS.put("solid_calcium_phosphate", 310.17);
        BOILING_POINT.put("solid_calcium_phosphate", 1600);













    }

    public static double molarMassOf(String itemPath) {
        return MOLAR_MASS.getOrDefault(itemPath, 0.0);
    }

    public static double densityOfLiquid(String liquidId) {
        return DENSITY.getOrDefault("liquid_" + liquidId, 1.0);
    }

    public static int boilingPointOf(String itemPath) {
        return BOILING_POINT.getOrDefault(itemPath, 9999);
    }

    /** Longest matching key wins so specific entries override generic ones. */
    public static ChemicalInfo forItem(String path) {
        if (path.startsWith("open_")) {
            path = path.substring(5);
        }
        ChemicalInfo best = null;
        int bestLen = -1;
        for (Map.Entry<String, ChemicalInfo> entry : CHEMICALS.entrySet()) {
            String key = entry.getKey();
            if ((path.equals(key) || path.startsWith(key + "_")) && key.length() > bestLen) {
                best = entry.getValue();
                bestLen = key.length();
            }
        }
        return best;
    }

    private ChemicalInfoProvider() {
    }
}
