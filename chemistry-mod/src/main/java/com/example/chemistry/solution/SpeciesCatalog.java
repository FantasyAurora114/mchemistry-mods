package com.example.chemistry.solution;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import com.example.chemistry.data.Solutions;
import static com.example.chemistry.solution.ChemicalSpecies.Phase.*;
import static com.example.chemistry.solution.ChemicalSpecies.Role.*;

/** Explicit formula mappings; unknown legacy IDs never receive a guessed formula. */
public final class SpeciesCatalog {
    public record ContentKey(String type, String id) {}
    private static final Map<String, ChemicalSpecies> SPECIES = new LinkedHashMap<>();
    private static final Map<ContentKey, Map<String, Integer>> CONTENTS = new LinkedHashMap<>();

    static {
        for(var organic:Map.of("toluene",Map.of("C",7,"H",8),"carbon_tetrachloride",Map.of("C",1,"Cl",4),
                "ethanol",Map.of("C",2,"H",6,"O",1),"methanol",Map.of("C",1,"H",4,"O",1),
                "acetone",Map.of("C",3,"H",6,"O",1),"glycerol",Map.of("C",3,"H",8,"O",3)).entrySet()){
            define(organic.getKey(),organic.getKey(),LIQUID,0,organic.getValue(),Set.of());
            CONTENTS.put(new ContentKey("liquid",organic.getKey()),Map.of(organic.getKey(),1));
        }
        define("ethyl_acetate","乙酸乙酯",LIQUID,0,Map.of("C",4,"H",8,"O",2),Set.of());
        CONTENTS.put(new ContentKey("liquid","ethyl_acetate"),Map.of("ethyl_acetate",1));
        define("iodine","I₂",AQUEOUS,0,Map.of("I",2),Set.of());
        define("solid:iodine","I₂",SOLID,0,Map.of("I",2),Set.of(RESIDUE));
        CONTENTS.put(new ContentKey("solid","iodine"),Map.of("solid:iodine",1));
        CONTENTS.put(new ContentKey("liquid","iodine_water"),Map.of("iodine",1));
        CONTENTS.put(new ContentKey("liquid","iodine_ethyl_acetate"),Map.of("iodine",1));
        CONTENTS.put(new ContentKey("liquid","iodine_toluene"),Map.of("iodine",1));
        CONTENTS.put(new ContentKey("liquid","iodine_carbon_tetrachloride"),Map.of("iodine",1));
        define("water", "水 H₂O", ChemicalSpecies.Phase.SOLVENT, 0, Map.of("H", 2, "O", 1), Set.of(ChemicalSpecies.Role.SOLVENT));
        ion("sodium", "Na⁺", 1, Map.of("Na", 1), true, false);
        ion("potassium", "K⁺", 1, Map.of("K", 1), true, false);
        ion("zinc", "Zn²⁺",2,Map.of("Zn",1),true,false);
        ion("silicate", "SiO₃²⁻",-2,Map.of("Si",1,"O",3),false,false);
        ion("calcium", "Ca²⁺", 2, Map.of("Ca", 1), true, false);
        ion("magnesium", "Mg²⁺", 2, Map.of("Mg", 1), true, false);
        ion("copper_ii", "Cu²⁺", 2, Map.of("Cu", 1), true, false);
        ion("iron_ii", "Fe²⁺", 2, Map.of("Fe", 1), true, false);
        ion("iron_iii", "Fe³⁺", 3, Map.of("Fe", 1), true, false);
        ion("silver", "Ag⁺", 1, Map.of("Ag", 1), true, false);
        ion("ammonium", "NH₄⁺", 1, Map.of("N", 1, "H", 4), false, false);
        ion("hydrogen", "H⁺", 1, Map.of("H", 1), false, false);
        ion("chloride", "Cl⁻", -1, Map.of("Cl", 1), false, true);
        ion("hypochlorite", "ClO⁻", -1, Map.of("Cl", 1, "O", 1), false, false);
        ion("chlorate", "ClO₃⁻", -1, Map.of("Cl", 1, "O", 3), false, false);
        ion("hydroxide", "OH⁻", -1, Map.of("O", 1, "H", 1), false, true);
        ion("sulfate", "SO₄²⁻", -2, Map.of("S", 1, "O", 4), false, false);
        ion("carbonate", "CO₃²⁻", -2, Map.of("C", 1, "O", 3), false, false);
        ion("oxalate", "C₂O₄²⁻", -2, Map.of("C", 2, "O", 4), false, true);
        ion("nitrate", "NO₃⁻", -1, Map.of("N", 1, "O", 3), false, false);
        ion("permanganate", "MnO₄⁻", -1, Map.of("Mn", 1, "O", 4), false, false);
        ion("thiocyanate", "SCN⁻", -1, Map.of("S", 1, "C", 1, "N", 1), false, true);
        ion("acetate", "CH₃COO⁻", -1, Map.of("C",2,"H",3,"O",2), false, false);
        define("acetic_acid", "CH₃COOH", AQUEOUS, 0, Map.of("C",2,"H",4,"O",2), Set.of());
        define("aqueous_hcl", "HCl", LIQUID, 0, Map.of("H",1,"Cl",1), Set.of());
        define("aqueous_hno3", "HNO₃", LIQUID, 0, Map.of("H",1,"N",1,"O",3), Set.of());
        CONTENTS.put(new ContentKey("liquid","acetic_acid"), Map.of("acetic_acid",1));
        for (String id : Set.of("hydrochloric_acid","hydrochloric_acid_concentrated"))
            CONTENTS.put(new ContentKey("liquid",id), Map.of("aqueous_hcl",1));
        for (String id : Set.of("nitric_acid","nitric_acid_concentrated"))
            CONTENTS.put(new ContentKey("liquid",id), Map.of("aqueous_hno3",1));
        define("iron_thiocyanate", "[FeSCN]²⁺", AQUEOUS, 2,
                Map.of("Fe", 1, "S", 1, "C", 1, "N", 1), Set.of(COMPLEX));
        define("ammonia", "NH₃", AQUEOUS, 0, Map.of("N", 1, "H", 3), Set.of(LIGAND));
        define("ammonia_gas", "氨气 NH₃", GAS, 0, Map.of("N", 1, "H", 3), Set.of());
        define("solid:copper", "铜 Cu", SOLID, 0, Map.of("Cu", 1), Set.of(RESIDUE));
        CONTENTS.put(new ContentKey("liquid", "water"), Map.of("water", 1));
        // Only explicit ammonia mass can be mapped. Legacy ammonia_water is an
        // unspecified bulk solution and deliberately stays unclassified.
        CONTENTS.put(new ContentKey("liquid", "ammonia"), Map.of("ammonia", 1));
        CONTENTS.put(new ContentKey("gas", "ammonia"), Map.of("ammonia_gas", 1));
        CONTENTS.put(new ContentKey("solid", "copper"), Map.of("solid:copper", 1));
        salt("copper_sulfate_anhydrous", Map.of("copper_ii", 1, "sulfate", 1));
        salt("sodium_chloride", Map.of("sodium", 1, "chloride", 1));
        salt("sodium_hypochlorite", Map.of("sodium", 1, "hypochlorite", 1));
        salt("sodium_chlorate", Map.of("sodium", 1, "chlorate", 1));
        salt("potassium_thiocyanate", Map.of("potassium", 1, "thiocyanate", 1));
        salt("potassium_chloride", Map.of("potassium", 1, "chloride", 1));
        salt("sodium_carbonate", Map.of("sodium", 2, "carbonate", 1));
        salt("potassium_carbonate", Map.of("potassium", 2, "carbonate", 1));
        salt("sodium_hydroxide", Map.of("sodium", 1, "hydroxide", 1));
        salt("potassium_hydroxide", Map.of("potassium", 1, "hydroxide", 1));
        salt("iron_iii_chloride", Map.of("iron_iii", 1, "chloride", 3));
        salt("potassium_permanganate", Map.of("potassium", 1, "permanganate", 1));
        salt("calcium_hydroxide", Map.of("calcium", 1, "hydroxide", 2));
        salt("calcium_carbonate", Map.of("calcium", 1, "carbonate", 1));
        salt("potassium_nitrate", Map.of("potassium", 1, "nitrate", 1));
        salt("sodium_nitrate", Map.of("sodium", 1, "nitrate", 1));
        salt("ammonium_chloride", Map.of("ammonium", 1, "chloride", 1));
        salt("ammonium_nitrate", Map.of("ammonium", 1, "nitrate", 1));
        salt("iron_sulfate", Map.of("iron_ii", 1, "sulfate", 1));
        salt("silver_nitrate", Map.of("silver", 1, "nitrate", 1));
        salt("silver_chloride", Map.of("silver", 1, "chloride", 1));
        salt("sodium_acetate", Map.of("sodium",1,"acetate",1));
        salt("potassium_acetate", Map.of("potassium",1,"acetate",1));
        salt("calcium_chloride", Map.of("calcium", 1, "chloride", 2));
        salt("magnesium_chloride", Map.of("magnesium", 1, "chloride", 2));
        salt("sodium_oxalate", Map.of("sodium", 2, "oxalate", 1));
        salt("potassium_oxalate", Map.of("potassium", 2, "oxalate", 1));
        salt("calcium_oxalate", Map.of("calcium", 1, "oxalate", 1));
        for (int h = 0; h <= 4; h++) {
            define(h == 0 ? "edta" : "edta_h" + h, h == 0 ? "Y⁴⁻" : "H" + h + "Y（EDTA）", AQUEOUS, h - 4,
                    Map.of("C", 10, "H", 12 + h, "N", 2, "O", 8), Set.of(LIGAND));
        }
        for (String metal : Set.of("calcium", "magnesium", "copper_ii", "iron_ii", "iron_iii")) {
            var atoms = new java.util.HashMap<>(get("edta").atoms());
            get(metal).atoms().forEach((atom, count) -> atoms.merge(atom, count, Integer::sum));
            define(metal + "_edta", "[" + get(metal).label().replace("²⁺", "").replace("³⁺", "") + "Y]" + (metal.equals("iron_iii") ? "⁻" : "²⁻"),
                    AQUEOUS, get(metal).charge() - 4, atoms, Set.of(COMPLEX));
        }
        salt("sodium_silicate",Map.of("sodium",2,"silicate",1));
        salt("zinc_sulfate",Map.of("zinc",1,"sulfate",1));
        salt("zinc_nitrate",Map.of("zinc",1,"nitrate",2));
        salt("potassium_sulfate",Map.of("potassium",2,"sulfate",1));
        salt("sodium_sulfate",Map.of("sodium",2,"sulfate",1));
        define("solid:zinc","Zn",SOLID,0,Map.of("Zn",1),Set.of(METAL));
        CONTENTS.put(new ContentKey("solid","zinc"),Map.of("solid:zinc",1));
        for(var product:Map.of("copper_ii_hydroxide",Map.of("Cu",1,"O",2,"H",2),"iron_iii_hydroxide",Map.of("Fe",1,"O",3,"H",3),"silicon_dioxide",Map.of("Si",1,"O",2)).entrySet()) {
            String id="solid:"+product.getKey();
            if(!SPECIES.containsKey(id)) define(id,product.getKey(),SOLID,0,product.getValue(),Set.of(RESIDUE));
            CONTENTS.put(new ContentKey("solid",product.getKey()),Map.of(id,1));
        }
        for (var c : com.example.chemistry.data.EdtaCompounds.ALL) {
            Map<String, Integer> components = new java.util.HashMap<>();
            components.put(c.metal().isEmpty() ? c.protons() == 0 ? "edta" : "edta_h" + c.protons() : c.metal() + "_edta", 1);
            if (c.sodium() > 0) components.put("sodium", c.sodium());
            salt(c.id(), components);
        }
        ion("cobalt_ii","Co2+",2,Map.of("Co",1),true,false);
        ion("nickel_ii","Ni2+",2,Map.of("Ni",1),true,false);
        ion("chromium_iii","Cr3+",3,Map.of("Cr",1),true,false);
        ion("lead_ii","Pb2+",2,Map.of("Pb",1),true,false);
        ion("tin_ii","Sn2+",2,Map.of("Sn",1),true,false);
        ion("tin_iv","Sn4+",4,Map.of("Sn",1),true,false);
        ion("manganese_ii","Mn2+",2,Map.of("Mn",1),true,false);
        ion("sulfide","S",-2,Map.of("S",1),false,false);
        ion("iodide","I",-1,Map.of("I",1),false,false);
        ion("chromate","CrO4",-2,Map.of("Cr",1,"O",4),false,false);
        ion("dichromate","Cr2O7",-2,Map.of("Cr",2,"O",7),false,false);
        if(!SPECIES.containsKey("solid:cobalt_chloride"))salt("cobalt_chloride",Map.of("cobalt_ii",1,"chloride",2));
        if(!SPECIES.containsKey("solid:cobalt_sulfate"))salt("cobalt_sulfate",Map.of("cobalt_ii",1,"sulfate",1));
        if(!SPECIES.containsKey("solid:cobalt_nitrate"))salt("cobalt_nitrate",Map.of("cobalt_ii",1,"nitrate",2));
        if(!SPECIES.containsKey("solid:cobalt_hydroxide"))salt("cobalt_hydroxide",Map.of("cobalt_ii",1,"hydroxide",2));
        if(!SPECIES.containsKey("solid:cobalt_carbonate"))salt("cobalt_carbonate",Map.of("cobalt_ii",1,"carbonate",1));
        define("batch:cobalt_oxide","氧化钴",AQUEOUS,0,Map.of("Co",1,"O",1),Set.of());
        salt("cobalt_oxide",Map.of("batch:cobalt_oxide",1));
        if(!SPECIES.containsKey("solid:cobalt_sulfide"))salt("cobalt_sulfide",Map.of("cobalt_ii",1,"sulfide",1));
        define("batch:cobalt_chloride_hexahydrate","六水合氯化钴",AQUEOUS,0,Map.of("Co",1,"Cl",2,"H",12,"O",6),Set.of());
        salt("cobalt_chloride_hexahydrate",Map.of("batch:cobalt_chloride_hexahydrate",1));
        if(!SPECIES.containsKey("solid:nickel_chloride"))salt("nickel_chloride",Map.of("nickel_ii",1,"chloride",2));
        if(!SPECIES.containsKey("solid:nickel_sulfate"))salt("nickel_sulfate",Map.of("nickel_ii",1,"sulfate",1));
        if(!SPECIES.containsKey("solid:nickel_nitrate"))salt("nickel_nitrate",Map.of("nickel_ii",1,"nitrate",2));
        if(!SPECIES.containsKey("solid:nickel_hydroxide"))salt("nickel_hydroxide",Map.of("nickel_ii",1,"hydroxide",2));
        if(!SPECIES.containsKey("solid:nickel_carbonate"))salt("nickel_carbonate",Map.of("nickel_ii",1,"carbonate",1));
        define("batch:nickel_oxide","氧化镍",AQUEOUS,0,Map.of("Ni",1,"O",1),Set.of());
        salt("nickel_oxide",Map.of("batch:nickel_oxide",1));
        if(!SPECIES.containsKey("solid:nickel_sulfide"))salt("nickel_sulfide",Map.of("nickel_ii",1,"sulfide",1));
        define("batch:nickel_chloride_hexahydrate","六水合氯化镍",AQUEOUS,0,Map.of("Ni",1,"Cl",2,"H",12,"O",6),Set.of());
        salt("nickel_chloride_hexahydrate",Map.of("batch:nickel_chloride_hexahydrate",1));
        define("batch:cobalt_ii_iii_oxide","四氧化三钴",AQUEOUS,0,Map.of("Co",3,"O",4),Set.of());
        salt("cobalt_ii_iii_oxide",Map.of("batch:cobalt_ii_iii_oxide",1));
        if(!SPECIES.containsKey("solid:manganese_chloride"))salt("manganese_chloride",Map.of("manganese_ii",1,"chloride",2));
        if(!SPECIES.containsKey("solid:manganese_nitrate"))salt("manganese_nitrate",Map.of("manganese_ii",1,"nitrate",2));
        if(!SPECIES.containsKey("solid:manganese_carbonate"))salt("manganese_carbonate",Map.of("manganese_ii",1,"carbonate",1));
        if(!SPECIES.containsKey("solid:manganese_hydroxide"))salt("manganese_hydroxide",Map.of("manganese_ii",1,"hydroxide",2));
        define("batch:manganese_oxide","氧化锰",AQUEOUS,0,Map.of("Mn",1,"O",1),Set.of());
        salt("manganese_oxide",Map.of("batch:manganese_oxide",1));
        if(!SPECIES.containsKey("solid:potassium_chromate"))salt("potassium_chromate",Map.of("potassium",2,"chromate",1));
        if(!SPECIES.containsKey("solid:sodium_chromate"))salt("sodium_chromate",Map.of("sodium",2,"chromate",1));
        if(!SPECIES.containsKey("solid:sodium_dichromate"))salt("sodium_dichromate",Map.of("sodium",2,"dichromate",1));
        define("batch:chromium_iii_oxide","氧化铬",AQUEOUS,0,Map.of("Cr",2,"O",3),Set.of());
        salt("chromium_iii_oxide",Map.of("batch:chromium_iii_oxide",1));
        if(!SPECIES.containsKey("solid:chromium_hydroxide"))salt("chromium_hydroxide",Map.of("chromium_iii",1,"hydroxide",3));
        define("batch:zinc_oxide","氧化锌",AQUEOUS,0,Map.of("Zn",1,"O",1),Set.of());
        salt("zinc_oxide",Map.of("batch:zinc_oxide",1));
        if(!SPECIES.containsKey("solid:zinc_hydroxide"))salt("zinc_hydroxide",Map.of("zinc",1,"hydroxide",2));
        if(!SPECIES.containsKey("solid:zinc_sulfide"))salt("zinc_sulfide",Map.of("zinc",1,"sulfide",1));
        if(!SPECIES.containsKey("solid:copper_sulfide"))salt("copper_sulfide",Map.of("copper_ii",1,"sulfide",1));
        if(!SPECIES.containsKey("solid:silver_sulfide"))salt("silver_sulfide",Map.of("silver",2,"sulfide",1));
        if(!SPECIES.containsKey("solid:lead_sulfide"))salt("lead_sulfide",Map.of("lead_ii",1,"sulfide",1));
        if(!SPECIES.containsKey("solid:lead_hydroxide"))salt("lead_hydroxide",Map.of("lead_ii",1,"hydroxide",2));
        if(!SPECIES.containsKey("solid:lead_nitrate"))salt("lead_nitrate",Map.of("lead_ii",1,"nitrate",2));
        if(!SPECIES.containsKey("solid:lead_iodide"))salt("lead_iodide",Map.of("lead_ii",1,"iodide",2));
        if(!SPECIES.containsKey("solid:lead_sulfate"))salt("lead_sulfate",Map.of("lead_ii",1,"sulfate",1));
        if(!SPECIES.containsKey("solid:tin_ii_chloride"))salt("tin_ii_chloride",Map.of("tin_ii",1,"chloride",2));
        if(!SPECIES.containsKey("solid:tin_iv_chloride"))salt("tin_iv_chloride",Map.of("tin_iv",1,"chloride",4));
        define("batch:tin_ii_oxide","氧化亚锡",AQUEOUS,0,Map.of("Sn",1,"O",1),Set.of());
        salt("tin_ii_oxide",Map.of("batch:tin_ii_oxide",1));
        define("batch:tin_iv_oxide","二氧化锡",AQUEOUS,0,Map.of("Sn",1,"O",2),Set.of());
        salt("tin_iv_oxide",Map.of("batch:tin_iv_oxide",1));
        if(!SPECIES.containsKey("solid:tin_ii_sulfide"))salt("tin_ii_sulfide",Map.of("tin_ii",1,"sulfide",1));
        if(!SPECIES.containsKey("solid:tin_iv_sulfide"))salt("tin_iv_sulfide",Map.of("tin_iv",1,"sulfide",2));
        if(!SPECIES.containsKey("solid:tin_ii_hydroxide"))salt("tin_ii_hydroxide",Map.of("tin_ii",1,"hydroxide",2));
        if(!SPECIES.containsKey("solid:tin_iv_hydroxide"))salt("tin_iv_hydroxide",Map.of("tin_iv",1,"hydroxide",4));
        if(!SPECIES.containsKey("solid:sodium_sulfide"))salt("sodium_sulfide",Map.of("sodium",2,"sulfide",1));
        define("batch:sodium_acetate_trihydrate","三水合乙酸钠",AQUEOUS,0,Map.of("Na",1,"C",2,"H",9,"O",5),Set.of());
        salt("sodium_acetate_trihydrate",Map.of("batch:sodium_acetate_trihydrate",1));
        define("batch:sodium_sulfate_decahydrate","十水合硫酸钠",AQUEOUS,0,Map.of("Na",2,"S",1,"O",14,"H",20),Set.of());
        salt("sodium_sulfate_decahydrate",Map.of("batch:sodium_sulfate_decahydrate",1));
        define("batch:sodium_carbonate_decahydrate","十水合碳酸钠",AQUEOUS,0,Map.of("Na",2,"C",1,"O",13,"H",20),Set.of());
        salt("sodium_carbonate_decahydrate",Map.of("batch:sodium_carbonate_decahydrate",1));
        define("batch:magnesium_sulfate_heptahydrate","七水合硫酸镁",AQUEOUS,0,Map.of("Mg",1,"S",1,"O",11,"H",14),Set.of());
        salt("magnesium_sulfate_heptahydrate",Map.of("batch:magnesium_sulfate_heptahydrate",1));
        define("batch:calcium_sulfate_dihydrate","二水合硫酸钙",AQUEOUS,0,Map.of("Ca",1,"S",1,"O",6,"H",4),Set.of());
        salt("calcium_sulfate_dihydrate",Map.of("batch:calcium_sulfate_dihydrate",1));
        define("batch:methyl_red","甲基红",AQUEOUS,0,Map.of("C",15,"H",15,"N",3,"O",2),Set.of());
        salt("methyl_red",Map.of("batch:methyl_red",1));
        define("batch:bromothymol_blue","溴百里酚蓝",AQUEOUS,0,Map.of("C",27,"H",28,"Br",2,"O",5,"S",1),Set.of());
        salt("bromothymol_blue",Map.of("batch:bromothymol_blue",1));
        define("batch:bromocresol_green","溴甲酚绿",AQUEOUS,0,Map.of("C",21,"H",14,"Br",4,"O",5,"S",1),Set.of());
        salt("bromocresol_green",Map.of("batch:bromocresol_green",1));
        define("batch:thymol_blue","百里酚蓝",AQUEOUS,0,Map.of("C",27,"H",30,"O",5,"S",1),Set.of());
        salt("thymol_blue",Map.of("batch:thymol_blue",1));
        define("batch:methylene_blue","亚甲蓝",AQUEOUS,0,Map.of("C",16,"H",18,"Cl",1,"N",3,"S",1),Set.of());
        salt("methylene_blue",Map.of("batch:methylene_blue",1));
        define("batch:indigo_carmine","靛蓝胭脂红",AQUEOUS,0,Map.of("C",16,"H",8,"N",2,"Na",2,"O",8,"S",2),Set.of());
        salt("indigo_carmine",Map.of("batch:indigo_carmine",1));
        define("batch:fluorescein","荧光素",AQUEOUS,0,Map.of("C",20,"H",12,"O",5),Set.of());
        salt("fluorescein",Map.of("batch:fluorescein",1));
        define("batch:eosin_y","曙红Y（二钠盐）",AQUEOUS,0,Map.of("C",20,"H",6,"Br",4,"Na",2,"O",5),Set.of());
        salt("eosin_y",Map.of("batch:eosin_y",1));
        define("batch:dimethylglyoxime","丁二酮肟",AQUEOUS,0,Map.of("C",4,"H",8,"N",2,"O",2),Set.of());
        salt("dimethylglyoxime",Map.of("batch:dimethylglyoxime",1));
        define("batch:ethylenediamine","乙二胺",AQUEOUS,0,Map.of("C",2,"H",8,"N",2),Set.of());
        salt("ethylenediamine",Map.of("batch:ethylenediamine",1));
        define("batch:nickel_dimethylglyoximate","丁二酮肟镍",AQUEOUS,0,Map.of("Ni",1,"C",8,"H",14,"N",4,"O",4),Set.of());
        salt("nickel_dimethylglyoximate",Map.of("batch:nickel_dimethylglyoximate",1));
        define("batch:sodium_tetrahydroxozincate","四羟合锌酸钠",AQUEOUS,0,Map.of("Na",2,"Zn",1,"O",4,"H",4),Set.of());
        salt("sodium_tetrahydroxozincate",Map.of("batch:sodium_tetrahydroxozincate",1));
        define("batch:sodium_hexahydroxochromate","六羟合铬酸钠",AQUEOUS,0,Map.of("Na",3,"Cr",1,"O",6,"H",6),Set.of());
        salt("sodium_hexahydroxochromate",Map.of("batch:sodium_hexahydroxochromate",1));
        define("batch:sodium_tetrahydroxoplumbate","四羟合铅酸钠",AQUEOUS,0,Map.of("Na",2,"Pb",1,"O",4,"H",4),Set.of());
        salt("sodium_tetrahydroxoplumbate",Map.of("batch:sodium_tetrahydroxoplumbate",1));
        define("batch:sodium_tetrahydroxostannate_ii","四羟合锡(II)酸钠",AQUEOUS,0,Map.of("Na",2,"Sn",1,"O",4,"H",4),Set.of());
        salt("sodium_tetrahydroxostannate_ii",Map.of("batch:sodium_tetrahydroxostannate_ii",1));
        define("batch:sodium_hexahydroxostannate_iv","六羟合锡(IV)酸钠",AQUEOUS,0,Map.of("Na",2,"Sn",1,"O",6,"H",6),Set.of());
        salt("sodium_hexahydroxostannate_iv",Map.of("batch:sodium_hexahydroxostannate_iv",1));
        if(!SPECIES.containsKey("solid:chromium_chloride"))salt("chromium_chloride",Map.of("chromium_iii",1,"chloride",3));
        if(!SPECIES.containsKey("solid:chromium_sulfate"))salt("chromium_sulfate",Map.of("chromium_iii",2,"sulfate",3));
        if(!SPECIES.containsKey("solid:potassium_dichromate"))salt("potassium_dichromate",Map.of("potassium",2,"dichromate",1));
        if(!SPECIES.containsKey("solid:zinc_chloride"))salt("zinc_chloride",Map.of("zinc",1,"chloride",2));
        if(!SPECIES.containsKey("solid:magnesium_sulfate"))salt("magnesium_sulfate",Map.of("magnesium",1,"sulfate",1));
        if(!SPECIES.containsKey("solid:calcium_sulfate"))salt("calcium_sulfate",Map.of("calcium",1,"sulfate",1));
        if(!SPECIES.containsKey("solid:potassium_iodide"))salt("potassium_iodide",Map.of("potassium",1,"iodide",1));
        define("cobalt_tetrachloride","[CoCl4]²⁻",AQUEOUS,-2,Map.of("Co",1,"Cl",4),Set.of(COMPLEX));
        define("batch:ammonia_water","氨水（旧版名义组成）",AQUEOUS,0,Map.of("N",1,"H",5,"O",1),Set.of(LIGAND));
        for(String id:java.util.List.of("ammonia_water","ammonia_water_concentrated"))CONTENTS.put(new ContentKey("liquid",id),Map.of("batch:ammonia_water",1));
        define("nickel_en","[Ni(en)3]²⁺",AQUEOUS,2,Map.of("Ni",1,"C",6,"H",24,"N",6),Set.of(COMPLEX));
        define("nickel_ammine","[Ni(NH3)6]²⁺",AQUEOUS,2,Map.of("Ni",1,"N",6,"H",18),Set.of(COMPLEX));
        for(var r:com.example.chemistry.data.FutureChemicals.ALL){
            String id="future:"+r.id();define(id,r.chinese(),AQUEOUS,0,r.atoms(),Set.of());
            if(r.phase().equals("SOLID"))salt(r.id(),Map.of(id,1));
            else CONTENTS.put(new ContentKey("liquid",r.id()),Map.of(id,1));
        }
        if(!SPECIES.containsKey("solid:copper_i_oxide")){define("future:copper_i_oxide","copper_i_oxide",AQUEOUS,0,Map.of("Cu",2,"O",1),Set.of());salt("copper_i_oxide",Map.of("future:copper_i_oxide",1));}
        if(!SPECIES.containsKey("solid:boric_acid")){define("future:boric_acid","boric_acid",AQUEOUS,0,Map.of("H",3,"B",1,"O",3),Set.of());salt("boric_acid",Map.of("future:boric_acid",1));}
        if(!SPECIES.containsKey("solid:sodium_bisulfite")){define("future:sodium_bisulfite","sodium_bisulfite",AQUEOUS,0,Map.of("Na",1,"H",1,"S",1,"O",3),Set.of());salt("sodium_bisulfite",Map.of("future:sodium_bisulfite",1));}
        if(!SPECIES.containsKey("solid:sodium_thiosulfate")){define("future:sodium_thiosulfate","sodium_thiosulfate",AQUEOUS,0,Map.of("Na",2,"S",2,"O",3),Set.of());salt("sodium_thiosulfate",Map.of("future:sodium_thiosulfate",1));}
        if(!SPECIES.containsKey("solid:sodium_salicylate")){define("future:sodium_salicylate","sodium_salicylate",AQUEOUS,0,Map.of("Na",1,"C",7,"H",5,"O",3),Set.of());salt("sodium_salicylate",Map.of("future:sodium_salicylate",1));}
        if(!SPECIES.containsKey("solid:iron_ii_chloride"))salt("iron_ii_chloride",Map.of("iron_ii",1,"chloride",2));
        CONTENTS.put(new ContentKey("liquid","iron_ii_chloride_solution"),Map.of("iron_ii",1,"chloride",2));
        if(!SPECIES.containsKey("solid:salicylic_acid")){define("future:salicylic_acid","水杨酸",AQUEOUS,0,Map.of("C",7,"H",6,"O",3),Set.of());salt("salicylic_acid",Map.of("future:salicylic_acid",1));}
        CONTENTS.put(new ContentKey("liquid","iodine_n_hexane"),Map.of("iodine",1));
        CONTENTS.put(new ContentKey("liquid","iodine_cyclohexane"),Map.of("iodine",1));
        define("advanced:wf6","WF6",GAS,0,Map.of("W",1,"F",6),Set.of());CONTENTS.put(new ContentKey("gas","tungsten_hexafluoride"),Map.of("advanced:wf6",1));
        define("advanced:hf","HF",GAS,0,Map.of("H",1,"F",1),Set.of());CONTENTS.put(new ContentKey("gas","hydrogen_fluoride"),Map.of("advanced:hf",1));
        define("advanced:acetic_anhydride","乙酸酐",AQUEOUS,0,Map.of("C",4,"H",6,"O",3),Set.of());CONTENTS.put(new ContentKey("liquid","acetic_anhydride"),Map.of("advanced:acetic_anhydride",1));
        define("advanced:aspirin","乙酰水杨酸",AQUEOUS,0,Map.of("C",9,"H",8,"O",4),Set.of());salt("acetylsalicylic_acid",Map.of("advanced:aspirin",1));
        define("advanced:benzoic","苯甲酸",AQUEOUS,0,Map.of("C",7,"H",6,"O",2),Set.of());salt("benzoic_acid",Map.of("advanced:benzoic",1));
        // CaCO3 in cloudy limewater is suspended residue, not free aqueous ions.
        CONTENTS.put(new ContentKey("liquid", "limewater_cloudy"), Map.of("solid:calcium_carbonate", 1));
    }

    private static void ion(String id, String label, int charge, Map<String, Integer> atoms,
            boolean metal, boolean ligand) {
        define(id, label, AQUEOUS, charge, atoms, metal ? Set.of(METAL) : ligand ? Set.of(LIGAND) : Set.of());
    }

    private static void define(String id, String label, ChemicalSpecies.Phase phase, int charge,
            Map<String, Integer> atoms, Set<ChemicalSpecies.Role> roles) {
        if (SPECIES.putIfAbsent(id, new ChemicalSpecies(id, label, phase, charge, atoms, roles)) != null)
            throw new IllegalArgumentException("Duplicate species " + id);
    }

    private static void salt(String solid, Map<String, Integer> ions) {
        Map<String, Integer> atoms = new TreeMap<>();
        int charge = 0;
        for (var e : ions.entrySet()) {
            ChemicalSpecies species = get(e.getKey());
            charge += species.charge() * e.getValue();
            species.atoms().forEach((atom, count) -> atoms.merge(atom, count * e.getValue(), Integer::sum));
        }
        if (charge != 0) throw new IllegalArgumentException("Charged salt " + solid);
        define("solid:" + solid, solid, SOLID, 0, atoms, Set.of(RESIDUE));
        define("molten:" + solid, solid, LIQUID, 0, atoms, Set.of(RESIDUE));
        CONTENTS.put(new ContentKey("solid", solid), Map.of("solid:" + solid, 1));
        CONTENTS.put(new ContentKey("liquid", "molten_" + solid), Map.of("molten:" + solid, 1));
        for (String solution : Solutions.allIds()) {
            if (solid.equals(Solutions.soluteOf(solution)))
                CONTENTS.put(new ContentKey("liquid", solution), Map.copyOf(ions));
        }
    }

    public static ChemicalSpecies get(String id) {
        ChemicalSpecies species = SPECIES.get(id);
        if (species == null) throw new IllegalArgumentException("Unknown species " + id);
        return species;
    }
    public static Map<String, Integer> components(ContentKey key) { return CONTENTS.get(key); }
    private SpeciesCatalog() {}
}
