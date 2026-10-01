package com.example.chemistry.solution;

import java.util.Map;
import java.util.Set;

/** One chemical species; atom counts and charge are per particle, mass is g/mol. */
public record ChemicalSpecies(String id, String label, Phase phase, int charge,
        Map<String, Integer> atoms, Set<Role> roles) {
    public enum Phase { AQUEOUS, SOLVENT, SOLID, LIQUID, GAS }
    public enum Role { METAL, LIGAND, SOLVENT, RESIDUE, COMPLEX }

    private static final Map<String, Double> ATOMIC_MASS = Map.ofEntries(
            Map.entry("H", 1.008),Map.entry("W",183.84),Map.entry("U",238.02891),Map.entry("Th",232.0377),Map.entry("Ra",226.0),Map.entry("Pu",244.0),Map.entry("Po",209.0),Map.entry("Ac",227.0),Map.entry("Am",243.0),Map.entry("Tc",98.0),Map.entry("Rn",222.0),Map.entry("B",10.81),Map.entry("F",18.998403),Map.entry("P",30.973762),Map.entry("Rb",85.4678),Map.entry("Cs",132.905452),Map.entry("Sr",87.62),Map.entry("Ga",69.723),Map.entry("In",114.818),Map.entry("Te",127.6),Map.entry("Sc",44.955908),Map.entry("Y",88.90584),Map.entry("Zr",91.224),Map.entry("Nb",92.90637),Map.entry("Ru",101.07),Map.entry("Rh",102.9055),Map.entry("Pd",106.42),Map.entry("Ir",192.217),Map.entry("Hf",178.49),Map.entry("Ta",180.94788),Map.entry("Re",186.207),Map.entry("Os",190.23),Map.entry("La",138.90547),Map.entry("Ce",140.116),Map.entry("Pr",140.90766),Map.entry("Nd",144.242),Map.entry("Sm",150.36),Map.entry("Eu",151.964),Map.entry("Gd",157.25),Map.entry("Tb",158.92535),Map.entry("Dy",162.5), Map.entry("C", 12.011), Map.entry("N", 14.007),
            Map.entry("O", 15.999), Map.entry("Na", 22.990), Map.entry("K", 39.0983),
            Map.entry("Ca", 40.078), Map.entry("Mg", 24.305), Map.entry("Cl", 35.45), Map.entry("S", 32.06),
            Map.entry("Fe", 55.845), Map.entry("Cu", 63.546), Map.entry("Ag", 107.8682),
            Map.entry("Si",28.085), Map.entry("Zn",65.38), Map.entry("I",126.90447), Map.entry("Mn", 54.938044),Map.entry("Co",58.933194),Map.entry("Ni",58.6934),Map.entry("Cr",51.9961),Map.entry("Pb",207.2),Map.entry("Sn",118.71),Map.entry("Br",79.904));

    public ChemicalSpecies {
        if (id == null || id.isBlank() || label == null || phase == null || atoms.isEmpty())
            throw new IllegalArgumentException("Incomplete species definition");
        atoms = Map.copyOf(atoms);
        roles = Set.copyOf(roles);
        atoms.forEach((element, count) -> {
            if (!ATOMIC_MASS.containsKey(element) || count <= 0)
                throw new IllegalArgumentException("Invalid elemental composition: " + id);
        });
    }

    public double molarMass() {
        return atoms.entrySet().stream()
                .mapToDouble(e -> ATOMIC_MASS.get(e.getKey()) * e.getValue()).sum();
    }
}
