package com.example.chemistry;

import java.util.Map;

/** Approximate g of solute dissolved in 100 g water at the given Celsius temperature. */
public final class AqueousSolubility {
    private static final double[] TEMPERATURES = {0, 20, 40, 60, 80, 100};
    private static final Map<String, double[]> CURVES = Map.ofEntries(
            Map.entry("iodine", new double[]{.03,.03,.03,.03,.03,.03}),
            Map.entry("sodium_silicate", new double[]{50,50,50,50,50,50}),
            Map.entry("zinc_sulfate", new double[]{50,50,50,50,50,50}),
            Map.entry("zinc_nitrate", new double[]{100,100,100,100,100,100}),
            Map.entry("potassium_sulfate", new double[]{12,12,12,12,12,12}),
            Map.entry("sodium_sulfate", new double[]{20,20,20,20,20,20}),
            // Provisional soluble-product ceilings for the open electrolysis bath.
            Map.entry("sodium_hypochlorite", new double[]{100,100,100,100,100,100}),
            Map.entry("sodium_chlorate", new double[]{80,80,80,80,80,80}),
            // Provisional gameplay limits, not measured temperature curves.
            Map.entry("iron_iii_chloride", new double[]{80,80,80,80,80,80}),
            Map.entry("potassium_thiocyanate", new double[]{200,200,200,200,200,200}),
            // Provisional room-temperature gameplay ceilings; not measured temperature curves.
            // Room-temperature gameplay ceilings, pending measured curves.
            Map.entry("sodium_acetate", new double[]{100,100,100,100,100,100}),
            Map.entry("potassium_acetate", new double[]{100,100,100,100,100,100}),
            Map.entry("calcium_chloride", new double[]{80,80,80,80,80,80}),
            Map.entry("magnesium_chloride", new double[]{50,50,50,50,50,50}),
            Map.entry("sodium_oxalate", new double[]{3.7,3.7,3.7,3.7,3.7,3.7}),
            Map.entry("potassium_oxalate", new double[]{30,30,30,30,30,30}),
            Map.entry("sodium_chloride", new double[]{35.7, 36.0, 36.6, 37.3, 38.4, 39.8}),
            Map.entry("potassium_chloride", new double[]{28.0, 34.0, 40.0, 45.5, 51.0, 56.5}),
            Map.entry("potassium_nitrate", new double[]{13.3, 31.6, 63.9, 110.0, 169.0, 246.0}),
            Map.entry("sodium_nitrate", new double[]{73.0, 88.0, 104.0, 124.0, 148.0, 180.0}),
            Map.entry("ammonium_chloride", new double[]{29.4, 37.2, 45.8, 55.3, 65.6, 77.3}),
            Map.entry("ammonium_nitrate", new double[]{118.0, 192.0, 297.0, 410.0, 576.0, 740.0}),
            Map.entry("sodium_carbonate", new double[]{7.0, 21.5, 48.0, 46.0, 45.0, 44.0}),
            Map.entry("copper_sulfate_anhydrous", new double[]{14.3, 32.0, 44.6, 61.8, 83.8, 114.0}),
            Map.entry("iron_sulfate", new double[]{14.0, 29.0, 40.0, 49.0, 55.0, 57.0}),
            Map.entry("silver_nitrate", new double[]{122.0, 216.0, 311.0, 440.0, 585.0, 733.0}),
            Map.entry("sodium_hydroxide", new double[]{42.0, 109.0, 129.0, 174.0, 234.0, 296.0}),
            Map.entry("potassium_hydroxide", new double[]{97.0, 112.0, 129.0, 147.0, 170.0, 194.0}));

    /** Dissolution enthalpy at room temperature, J/mol; positive absorbs heat. */
    private static final Map<String, Double> ENTHALPY = Map.ofEntries(
            Map.entry("sodium_chloride", 3900.0),
            Map.entry("potassium_chloride", 17200.0),
            Map.entry("potassium_nitrate", 34700.0),
            Map.entry("sodium_nitrate", 20500.0),
            Map.entry("ammonium_chloride", 14800.0),
            Map.entry("ammonium_nitrate", 25700.0),
            Map.entry("sodium_carbonate", -28000.0),
            Map.entry("copper_sulfate_anhydrous", -66000.0),
            Map.entry("iron_sulfate", -12000.0),
            Map.entry("silver_nitrate", 22000.0),
            Map.entry("sodium_hydroxide", -44500.0),
            Map.entry("potassium_hydroxide", -57500.0));

    public static java.util.Set<String> solidIds() { var ids = new java.util.HashSet<>(CURVES.keySet()); com.example.chemistry.data.EdtaCompounds.ALL.forEach(c -> ids.add(c.id())); com.example.chemistry.data.BatchChemicals.ALL.stream().filter(c->c.ceiling()>0).forEach(c->ids.add(c.id()));ids.addAll(com.example.chemistry.data.BatchChemicals.EXISTING_CAPS.keySet());com.example.chemistry.data.FutureChemicals.ALL.stream().filter(c->c.phase().equals("SOLID")&&c.ceiling()>0).forEach(c->ids.add(c.id()));return ids; }

    public static boolean hasCurve(String id) {
        return (com.example.chemistry.data.FutureChemicals.find(id)!=null&&com.example.chemistry.data.FutureChemicals.find(id).phase().equals("SOLID")&&com.example.chemistry.data.FutureChemicals.find(id).ceiling()>0) || com.example.chemistry.data.BatchChemicals.EXISTING_CAPS.containsKey(id) || (com.example.chemistry.data.BatchChemicals.find(id)!=null && com.example.chemistry.data.BatchChemicals.find(id).ceiling()>0) || CURVES.containsKey(id) || com.example.chemistry.data.EdtaCompounds.ALL.stream().anyMatch(c -> c.id().equals(id));
    }

    public static double gramsPer100gWater(String id, double temperature) {
        // EDTA caps are provisional gameplay limits, not measured temperature curves.
        for (var compound : com.example.chemistry.data.EdtaCompounds.ALL) if (compound.id().equals(id)) return compound.solubility();
        if(!Double.isFinite(temperature))return 0;
        var future=com.example.chemistry.data.FutureChemicals.find(id);
        if(future!=null&&future.phase().equals("SOLID")&&future.ceiling()>0)return future.ceiling();
        var batch=com.example.chemistry.data.BatchChemicals.find(id);
        if(batch!=null && batch.ceiling()>0){
            if(id.equals("lead_iodide"))return .04+.36*Math.pow(Math.clamp(temperature,0,100)/100,2);
            return batch.ceiling();
        }
        if(com.example.chemistry.data.BatchChemicals.EXISTING_CAPS.containsKey(id))return com.example.chemistry.data.BatchChemicals.EXISTING_CAPS.get(id);
        double[] values = CURVES.get(id);
        if (values == null || !Double.isFinite(temperature)) return 0;
        if (temperature <= TEMPERATURES[0]) return values[0];
        for (int i = 1; i < TEMPERATURES.length; i++) {
            if (temperature <= TEMPERATURES[i]) {
                double f = (temperature - TEMPERATURES[i - 1])
                        / (TEMPERATURES[i] - TEMPERATURES[i - 1]);
                return values[i - 1] + (values[i] - values[i - 1]) * f;
            }
        }
        return values[values.length - 1];
    }

    public static double enthalpyJPerMol(String id) {
        return ENTHALPY.getOrDefault(id, 0.0);
    }

    public static String solutionId(String solidId) {
        if (solidId.equals("iodine")) return "iodine_water";
        if (solidId.equals("iron_iii_chloride")) return "iron_chloride_solution";
        return solidId.equals("copper_sulfate_anhydrous")
                ? "copper_sulfate_solution" : solidId + "_solution";
    }

    public static String solidIdForSolution(String solutionId) {
        if (solutionId.equals("iodine_water")) return "iodine";
        if (solutionId.equals("iron_chloride_solution")) return "iron_iii_chloride";
        if (solutionId.equals("copper_sulfate_solution"))
            return "copper_sulfate_anhydrous";
        if (!solutionId.endsWith("_solution")) return null;
        String id = solutionId.substring(0, solutionId.length() - "_solution".length());
        return hasCurve(id) ? id : null;
    }

    private AqueousSolubility() {}
}
