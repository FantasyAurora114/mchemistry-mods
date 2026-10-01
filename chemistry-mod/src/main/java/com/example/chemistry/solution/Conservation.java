package com.example.chemistry.solution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Conservation over the explicitly modelled species plus unchanged opaque material. */
public final class Conservation {
    public record Report(boolean conserved, boolean fullyModelled, List<String> differences) {
        public Report { differences = List.copyOf(differences); }
    }

    public static Report compare(SpeciesInventory before, SpeciesInventory after) {
        List<String> differences = new ArrayList<>();
        compareMaps(before.elementMoles(), after.elementMoles(), "element:", differences);
        compareMaps(before.unmappedGrams(), after.unmappedGrams(), "unmapped:", differences);
        if (!close(before.massGrams(), after.massGrams())) differences.add("mass");
        if (!close(before.chargeMoles(), after.chargeMoles())) differences.add("charge");
        return new Report(differences.isEmpty(), before.fullyModelled() && after.fullyModelled(), differences);
    }

    private static <K> void compareMaps(Map<K, Double> a, Map<K, Double> b,
            String prefix, List<String> differences) {
        var keys = new HashSet<>(a.keySet()); keys.addAll(b.keySet());
        for (K key : keys)
            if (!close(a.getOrDefault(key, 0.0), b.getOrDefault(key, 0.0))) differences.add(prefix + key);
    }

    private static boolean close(double a, double b) {
        return Double.isFinite(a) && Double.isFinite(b)
                && Math.abs(a - b) <= 1e-10 + 1e-9 * Math.max(Math.abs(a), Math.abs(b));
    }
    private Conservation() {}
}
