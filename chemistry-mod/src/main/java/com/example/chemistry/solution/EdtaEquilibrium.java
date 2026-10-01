package com.example.chemistry.solution;

import com.example.chemistry.item.LabVesselItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Ideal 25 C four-proton EDTA model with competitive 1:1 metal binding and Fe-SCN competition. */
public final class EdtaEquilibrium {
    private static final double[] PKA = {2.0, 2.7, 6.2, 10.31};
    // Sigma-Aldrich Chelators technical table; room-temperature concentration approximation.
    private static final Map<String, Double> LOG_BETA = Map.of("calcium", 10.96, "magnesium", 8.69,
            "copper_ii", 18.8, "iron_ii", 14.33, "iron_iii", 25.1);
    private record Key(SpeciesInventory totals, double litres) { }
    public record Result(SpeciesInventory species, double ph) { }
    private static final Map<Key, Result> CACHE = new LinkedHashMap<>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, Result> eldest) { return size() > 128; }
    };
    public static boolean present(SpeciesInventory inventory) {
        return inventory.moles().entrySet().stream().anyMatch(e -> e.getValue() > 0 && (e.getKey().startsWith("edta") || e.getKey().endsWith("_edta")));
    }
    public static boolean present(ItemStack stack) {
        return LabVesselItem.getContents(stack).stream().anyMatch(e -> e.type().equals("liquid")
                && com.example.chemistry.data.EdtaCompounds.ALL.stream().anyMatch(c -> e.id().equals(c.id() + "_solution")));
    }
    public static Result read(ItemStack stack) {
        return present(stack) ? solve(SolutionSpecies.analyticalSnapshot(stack), SolutionSpecies.solutionLitres(stack)) : null;
    }
    public static Result read(List<LabVesselItem.Entry> entries, double ml) {
        if (entries.stream().noneMatch(e -> e.type().equals("liquid") && e.id().contains("edta"))) return null;
        Map<String, Double> amounts = new HashMap<>(); Map<SpeciesCatalog.ContentKey, Double> unknown = new HashMap<>();
        for (var entry : entries) {
            var key = new SpeciesCatalog.ContentKey(entry.type(), entry.id());
            var components = SpeciesCatalog.components(key);
            if (components == null) { unknown.merge(key, entry.amount(), Double::sum); continue; }
            double mass = components.entrySet().stream().mapToDouble(e -> SpeciesCatalog.get(e.getKey()).molarMass() * e.getValue()).sum();
            components.forEach((id, count) -> amounts.merge(id, entry.amount() / mass * count, Double::sum));
        }
        return solve(new SpeciesInventory(amounts, unknown), ml / 1000);
    }
    public static Result solve(SpeciesInventory original, double litres) {
        if (!Double.isFinite(litres) || litres <= 0 || original.amount("water") <= 0 || !present(original)) return null;
        if (!supported(original)) return null;
        Key key = new Key(original, litres);
        synchronized (CACHE) { Result cached = CACHE.get(key); if (cached != null) return cached; }
        SpeciesInventory totals = original;
        for (String acid : List.of("aqueous_hcl", "aqueous_hno3")) {
            double n = totals.amount(acid);
            if (n > 0) totals = totals.transform(Map.of(acid, 1), Map.of("hydrogen", 1, acid.equals("aqueous_hcl") ? "chloride" : "nitrate", 1), n);
        }
        for (String metal : LOG_BETA.keySet()) {
            double n = totals.amount(metal + "_edta");
            if (n > 0) totals = totals.transform(Map.of(metal + "_edta", 1), Map.of(metal, 1, "edta", 1), n);
        }
        double scn = totals.amount("iron_thiocyanate");
        if (scn > 0) totals = totals.transform(Map.of("iron_thiocyanate", 1), Map.of("iron_iii", 1, "thiocyanate", 1), scn);
        double ligand = totals.amount("edta");
        for (int h = 1; h <= 4; h++) ligand += totals.amount("edta_h" + h);
        double acetate = totals.amount("acetate") + totals.amount("acetic_acid");
        double lo = -2, hi = 16;
        for (int i = 0; i < 90; i++) {
            double ph = (lo + hi) * .5;
            Map<String, Double> state = state(totals, litres, ligand, acetate, ph);
            double charge = charge(state) - original.chargeMoles();
            if (charge > 0) lo = ph; else hi = ph;
        }
        double ph = (lo + hi) * .5;
        var amounts = state(totals, litres, ligand, acetate, ph);
        // Water supplies/removes equal H+/OH- equivalents; no new atoms or extra reagent mass.
        double water = totals.amount("water") + totals.amount("hydroxide") - amounts.getOrDefault("hydroxide", 0.0);
        if (water < 0) return null;
        amounts.put("water", water);
        SpeciesInventory species = new SpeciesInventory(amounts, original.unmappedGrams());
        if (!Conservation.compare(original, species).conserved()) throw new IllegalStateException("EDTA equilibrium violated conservation");
        Result result = new Result(species, ph);
        synchronized (CACHE) { CACHE.put(key, result); }
        return result;
    }
    private static boolean supported(SpeciesInventory inventory) {
        Set<String> aqueous = Set.of("water", "sodium", "potassium", "calcium", "magnesium", "copper_ii",
                "iron_ii", "iron_iii", "hydrogen", "hydroxide", "chloride", "nitrate", "sulfate", "thiocyanate",
                "iron_thiocyanate", "acetate", "acetic_acid", "aqueous_hcl", "aqueous_hno3");
        for (var entry : inventory.moles().entrySet()) {
            if (entry.getValue() <= 0 || entry.getKey().startsWith("edta") || entry.getKey().endsWith("_edta")) continue;
            var phase = SpeciesCatalog.get(entry.getKey()).phase();
            if (phase != ChemicalSpecies.Phase.SOLID && phase != ChemicalSpecies.Phase.GAS && !aqueous.contains(entry.getKey())) return false;
        }
        return inventory.unmappedGrams().entrySet().stream().noneMatch(e -> e.getValue() > 0 && e.getKey().type().equals("liquid"));
    }
    private static double charge(Map<String, Double> moles) {
        return moles.entrySet().stream().mapToDouble(e -> e.getValue() * SpeciesCatalog.get(e.getKey()).charge()).sum();
    }
    private static Map<String, Double> state(SpeciesInventory totals, double litres, double ligand, double acetate, double ph) {
        double hydrogen = Math.pow(10, -ph), hydroxide = AcidBaseEquilibrium.KW / hydrogen;
        double[] ratios = new double[5]; ratios[0] = 1;
        for (int i = 1; i <= 4; i++) ratios[i] = ratios[i - 1] * hydrogen / Math.pow(10, -PKA[4 - i]);
        double denominator = Arrays.stream(ratios).sum();
        double lo = 0, hi = ligand / litres / denominator;
        for (int i = 0; i < 90; i++) {
            double y = (lo + hi) * .5;
            double used = y * denominator * litres;
            for (String metal : LOG_BETA.keySet()) {
                double free = freeMetal(totals, litres, y, metal);
                used += Math.pow(10, LOG_BETA.get(metal)) * y * free;
            }
            if (used > ligand) hi = y; else lo = y;
        }
        double y = (lo + hi) * .5;
        Map<String, Double> out = new HashMap<>(totals.moles());
        out.put("edta", y * litres);
        for (int i = 1; i <= 4; i++) out.put("edta_h" + i, y * ratios[i] * litres);
        for (String metal : LOG_BETA.keySet()) {
            double free = freeMetal(totals, litres, y, metal);
            double bound = Math.pow(10, LOG_BETA.get(metal)) * y * free;
            out.put(metal, free); out.put(metal + "_edta", bound);
        }
        double iron = out.getOrDefault("iron_iii", 0.0);
        double freeScn = totals.amount("thiocyanate") / (1 + CoordinationEquilibrium.FORMATION_CONSTANT * iron / litres);
        out.put("thiocyanate", freeScn); out.put("iron_thiocyanate", Math.max(0, totals.amount("thiocyanate") - freeScn));
        out.put("hydrogen", hydrogen * litres); out.put("hydroxide", hydroxide * litres);
        out.put("acetate", acetate * AcidBaseEquilibrium.ACETIC_KA / (AcidBaseEquilibrium.ACETIC_KA + hydrogen));
        out.put("acetic_acid", acetate * hydrogen / (AcidBaseEquilibrium.ACETIC_KA + hydrogen));
        return out;
    }
    /** Returns free metal moles; Fe(III) simultaneously competes with SCN-. */
    private static double freeMetal(SpeciesInventory totals, double litres, double y, String metal) {
        double factor = 1 + Math.pow(10, LOG_BETA.get(metal)) * y;
        if (!metal.equals("iron_iii") || totals.amount("thiocyanate") == 0) return totals.amount(metal) / factor;
        double k = CoordinationEquilibrium.FORMATION_CONSTANT / litres;
        double a = k * factor, b = factor + k * (totals.amount("thiocyanate") - totals.amount(metal));
        double root = Math.sqrt(b * b + 4 * a * totals.amount(metal));
        return b >= 0 ? 2 * totals.amount(metal) / (b + root) : (-b + root) / (2 * a);
    }
    public static void appendInfo(List<Component> lines, ItemStack stack) {
        Result result = read(stack);
        if (result == null) { if (present(stack)) lines.add(Component.literal("EDTA：该混合体系暂未完整建模")); return; }
        lines.add(Component.literal(String.format(Locale.ROOT, "EDTA：pH ≈ %.2f（25°C理想浓度）", result.ph())));
        for (String metal : LOG_BETA.keySet()) {
            double n = result.species().amount(metal + "_edta");
            if (n > 1e-12) lines.add(Component.literal(String.format(Locale.ROOT, "%s %.4g mmol", SpeciesCatalog.get(metal + "_edta").label(), n * 1000)));
        }
    }
    public static int color(ItemStack stack, int fallback) {
        Result result = read(stack); if (result == null) return fallback;
        double litres = SolutionSpecies.solutionLitres(stack);
        double copper = result.species().amount("copper_ii_edta") / litres;
        if (copper <= 1e-10) return fallback;
        double fraction = 1 - Math.exp(-copper * 80); int rgb = 0;
        for (int shift : new int[]{0, 8, 16}) rgb |= ((int) Math.round(((fallback >> shift) & 255) * (1 - fraction) + ((0x365BC0 >> shift) & 255) * fraction)) << shift;
        return rgb;
    }
    private EdtaEquilibrium() { }
}
