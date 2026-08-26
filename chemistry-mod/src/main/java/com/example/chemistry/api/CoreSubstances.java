package com.example.chemistry.api;

import com.example.chemistry.api.Substances.ElementSubstance;
import com.example.chemistry.api.Substances.GasSubstance;
import com.example.chemistry.api.Substances.LiquidSubstance;
import com.example.chemistry.api.Substances.SolidSubstance;
import com.example.chemistry.data.Element;
import com.example.chemistry.data.Elements;
import com.example.chemistry.data.GasJars;
import com.example.chemistry.data.Liquids;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.data.Solids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds the framework registry with the core mod's generated substance tables.
 * Called from the mod constructor before addons register anything.
 */
public final class CoreSubstances {

    /** Built-in concentration variants for the core's acid/solution liquids. */
    private static final Map<String, Concentration> CONCENTRATIONS = new HashMap<>();

    static {
        CONCENTRATIONS.put("sulfuric_acid_dilute", new Concentration("dilute", "稀硫酸", 1.0));
        CONCENTRATIONS.put("sulfuric_acid_concentrated", new Concentration("concentrated", "浓硫酸", 3.0));
        CONCENTRATIONS.put("hydrochloric_acid", new Concentration("standard", "氯化氢的水溶液", 1.0));
        CONCENTRATIONS.put("hydrochloric_acid_concentrated", new Concentration("concentrated", "氯化氢的高浓度水溶液", 3.0));
        CONCENTRATIONS.put("nitric_acid", new Concentration("standard", "硝酸的水溶液", 1.0));
        CONCENTRATIONS.put("nitric_acid_concentrated", new Concentration("concentrated", "硝酸的高浓度水溶液", 3.0));
        CONCENTRATIONS.put("phosphoric_acid", new Concentration("standard", "磷酸的水溶液", 1.0));
        CONCENTRATIONS.put("phosphoric_acid_concentrated", new Concentration("concentrated", "纯磷酸", 2.0));
        CONCENTRATIONS.put("ammonia_water", new Concentration("standard", "氨水", 1.0));
        CONCENTRATIONS.put("ammonia_water_concentrated", new Concentration("concentrated", "浓氨水", 2.0));
    }

    private CoreSubstances() {
    }

    public static void init() {
        List<ElementSubstance> elements = new ArrayList<>();
        for (Element e : Elements.ALL) {
            elements.add(new ElementSubstance(e.id(), e.symbol(), e.name(), "", e.color(),
                    e.category(), e.state(), e.atomicNumber()));
        }

        List<SolidSubstance> solids = new ArrayList<>();
        for (Solids.Solid s : Solids.ALL) {
            solids.add(new SolidSubstance(s.id(), s.formula(), s.english(), s.chinese(), s.color(),
                    s.form() == Solids.SolidForm.POWDER, s.openProduct()));
        }

        List<LiquidSubstance> liquids = new ArrayList<>();
        for (Liquids.Liquid l : Liquids.ALL) {
            Concentration c = CONCENTRATIONS.getOrDefault(l.id(), Concentration.STANDARD);
            String base = baseOf(l.id());
            liquids.add(new LiquidSubstance(l.id(), l.formula(), l.english(), l.chinese(), l.color(),
                    base, c, l.openProduct()));
        }

        List<GasSubstance> gases = new ArrayList<>();
        for (GasJars.GasJar g : GasJars.ALL) {
            gases.add(new GasSubstance(g.id(), g.formula(), g.name(), g.chinese(), g.color(), g.lighter()));
        }

        ChemistryAPI.seed(elements, solids, liquids, gases, Reactions.ALL);
    }

    private static String baseOf(String liquidId) {
        // Variant ids end with _dilute / _concentrated; the base is the remainder.
        for (String suffix : new String[]{"_concentrated", "_dilute"}) {
            if (liquidId.endsWith(suffix)) {
                return liquidId.substring(0, liquidId.length() - suffix.length());
            }
        }
        return liquidId;
    }
}
