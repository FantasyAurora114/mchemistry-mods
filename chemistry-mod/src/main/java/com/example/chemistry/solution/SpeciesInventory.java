package com.example.chemistry.solution;

import java.util.Map;
import java.util.TreeMap;
import com.example.chemistry.solution.SpeciesCatalog.ContentKey;

/** Immutable mole ledger. Unmapped material remains accounted for in grams. */
public record SpeciesInventory(Map<String, Double> moles, Map<ContentKey, Double> unmappedGrams) {
    public SpeciesInventory {
        moles = Map.copyOf(moles);
        unmappedGrams = Map.copyOf(unmappedGrams);
        moles.forEach((id, amount) -> { SpeciesCatalog.get(id); requireAmount(amount); });
        unmappedGrams.values().forEach(SpeciesInventory::requireAmount);
    }

    private static void requireAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid amount");
    }

    public boolean fullyModelled() { return unmappedGrams.values().stream().allMatch(v -> v == 0); }
    public double amount(String id) { return moles.getOrDefault(id, 0.0); }
    public double massGrams() {
        return moles.entrySet().stream().mapToDouble(e -> e.getValue()
                * SpeciesCatalog.get(e.getKey()).molarMass()).sum()
                + unmappedGrams.values().stream().mapToDouble(Double::doubleValue).sum();
    }
    /** Charge in moles of elementary charge; counterions are included. */
    public double chargeMoles() {
        return moles.entrySet().stream().mapToDouble(e -> e.getValue()
                * SpeciesCatalog.get(e.getKey()).charge()).sum();
    }
    public Map<String, Double> elementMoles() {
        Map<String, Double> elements = new TreeMap<>();
        moles.forEach((id, n) -> SpeciesCatalog.get(id).atoms()
                .forEach((element, count) -> elements.merge(element, n * count, Double::sum)));
        return Map.copyOf(elements);
    }

    public SpeciesInventory plus(SpeciesInventory other) {
        Map<String, Double> species = new java.util.HashMap<>(moles);
        Map<ContentKey, Double> unknown = new java.util.HashMap<>(unmappedGrams);
        other.moles.forEach((id, n) -> species.merge(id, n, Double::sum));
        other.unmappedGrams.forEach((key, grams) -> unknown.merge(key, grams, Double::sum));
        return new SpeciesInventory(species, unknown);
    }

    /** Rejects the entire change on invalid stoichiometry or insufficient reactants. */
    public SpeciesInventory transform(Map<String, Integer> reactants,
            Map<String, Integer> products, double extentMoles) {
        requireAmount(extentMoles);
        SpeciesInventory lhs = stoichiometry(reactants);
        SpeciesInventory rhs = stoichiometry(products);
        if (!Conservation.compare(lhs, rhs).conserved())
            throw new IllegalArgumentException("Unbalanced species change");
        Map<String, Double> next = new java.util.HashMap<>(moles);
        for (var e : reactants.entrySet()) {
            double consumed = extentMoles * e.getValue();
            requireAmount(consumed);
            double available = amount(e.getKey());
            // Strict overdraw rejection: tolerances must never create reactants.
            if (consumed > available) throw new IllegalArgumentException("Insufficient " + e.getKey());
            next.put(e.getKey(), available - consumed);
        }
        products.forEach((id, count) -> next.merge(id, extentMoles * count, Double::sum));
        next.values().removeIf(v -> v == 0.0);
        SpeciesInventory result = new SpeciesInventory(next, unmappedGrams);
        if (!Conservation.compare(this, result).conserved())
            throw new IllegalArgumentException("Species change did not conserve matter");
        return result;
    }

    private static SpeciesInventory stoichiometry(Map<String, Integer> side) {
        if (side.isEmpty()) throw new IllegalArgumentException("Empty stoichiometry");
        Map<String, Double> amounts = new java.util.HashMap<>();
        side.forEach((id, count) -> {
            if (count == null || count <= 0) throw new IllegalArgumentException("Invalid coefficient");
            amounts.put(id, count.doubleValue());
        });
        return new SpeciesInventory(amounts, Map.of());
    }
}
