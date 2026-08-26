package com.example.chemistry.data;

import java.util.List;

/**
 * Full periodic table used for item registration.
 * Kept in sync with data/chemistry/chemistry/elements.json (both generated).
 */
public final class Elements {

    public static final List<Element> ALL = List.of(
            new Element("hydrogen", "H", "hydrogen", 1, ElementCategory.NONMETAL, ElementState.GAS, true, 0xB8E0F8),
            new Element("helium", "He", "helium", 2, ElementCategory.NONMETAL, ElementState.GAS, true, 0xF8E0A8),
            new Element("lithium", "Li", "lithium", 3, ElementCategory.METAL, ElementState.SOLID, true, 0xE8E0D8),
            new Element("beryllium", "Be", "beryllium", 4, ElementCategory.METAL, ElementState.SOLID, true, 0xD8E8E0),
            new Element("boron", "B", "boron", 5, ElementCategory.METALLOID, ElementState.SOLID, true, 0xA08868),
            new Element("carbon", "C", "carbon", 6, ElementCategory.NONMETAL, ElementState.SOLID, true, 0x505860),
            new Element("nitrogen", "N", "nitrogen", 7, ElementCategory.NONMETAL, ElementState.GAS, true, 0xA8C8E8),
            new Element("oxygen", "O", "oxygen", 8, ElementCategory.NONMETAL, ElementState.GAS, true, 0xC8E0F8),
            new Element("fluorine", "F", "fluorine", 9, ElementCategory.NONMETAL, ElementState.GAS, true, 0xB8F0C8),
            new Element("neon", "Ne", "neon", 10, ElementCategory.NONMETAL, ElementState.GAS, true, 0xF8B8A8),
            new Element("sodium", "Na", "sodium", 11, ElementCategory.METAL, ElementState.SOLID, true, 0xE8E8F0),
            new Element("magnesium", "Mg", "magnesium", 12, ElementCategory.METAL, ElementState.SOLID, true, 0xD8E0E8),
            new Element("aluminium", "Al", "aluminium", 13, ElementCategory.METAL, ElementState.SOLID, true, 0xD0D8E0),
            new Element("silicon", "Si", "silicon", 14, ElementCategory.METALLOID, ElementState.SOLID, true, 0x8898A8),
            new Element("phosphorus", "P", "phosphorus", 15, ElementCategory.NONMETAL, ElementState.SOLID, true, 0xE8B088),
            new Element("sulfur", "S", "sulfur", 16, ElementCategory.NONMETAL, ElementState.SOLID, true, 0xF0D848),
            new Element("chlorine", "Cl", "chlorine", 17, ElementCategory.NONMETAL, ElementState.GAS, true, 0xA8E8A0),
            new Element("argon", "Ar", "argon", 18, ElementCategory.NONMETAL, ElementState.GAS, true, 0xD8B8E8),
            new Element("potassium", "K", "potassium", 19, ElementCategory.METAL, ElementState.SOLID, true, 0xD8D0E0),
            new Element("calcium", "Ca", "calcium", 20, ElementCategory.METAL, ElementState.SOLID, true, 0xE8E0C8),
            new Element("titanium", "Ti", "titanium", 22, ElementCategory.METAL, ElementState.SOLID, true, 0xB8C0C8),
            new Element("vanadium", "V", "vanadium", 23, ElementCategory.METAL, ElementState.SOLID, true, 0xA8A8B8),
            new Element("chromium", "Cr", "chromium", 24, ElementCategory.METAL, ElementState.SOLID, true, 0xD0D8E8),
            new Element("manganese", "Mn", "manganese", 25, ElementCategory.METAL, ElementState.SOLID, true, 0xB8A8A0),
            new Element("iron", "Fe", "iron", 26, ElementCategory.METAL, ElementState.SOLID, true, 0xE0E0E0),
            new Element("cobalt", "Co", "cobalt", 27, ElementCategory.METAL, ElementState.SOLID, true, 0x98A8D0),
            new Element("nickel", "Ni", "nickel", 28, ElementCategory.METAL, ElementState.SOLID, true, 0xC8D0D8),
            new Element("copper", "Cu", "copper", 29, ElementCategory.METAL, ElementState.SOLID, true, 0xE8A858),
            new Element("zinc", "Zn", "zinc", 30, ElementCategory.METAL, ElementState.SOLID, true, 0xB8C8D0),
            new Element("germanium", "Ge", "germanium", 32, ElementCategory.METALLOID, ElementState.SOLID, true, 0x98A0A8),
            new Element("arsenic", "As", "arsenic", 33, ElementCategory.METALLOID, ElementState.SOLID, true, 0xA8A8A0),
            new Element("selenium", "Se", "selenium", 34, ElementCategory.NONMETAL, ElementState.SOLID, true, 0xE0A050),
            new Element("bromine", "Br", "bromine", 35, ElementCategory.NONMETAL, ElementState.LIQUID, true, 0xA84830),
            new Element("krypton", "Kr", "krypton", 36, ElementCategory.NONMETAL, ElementState.GAS, true, 0xC8D8F0),
            new Element("molybdenum", "Mo", "molybdenum", 42, ElementCategory.METAL, ElementState.SOLID, true, 0x98A0A8),
            new Element("silver", "Ag", "silver", 47, ElementCategory.METAL, ElementState.SOLID, true, 0xF0F0F8),
            new Element("cadmium", "Cd", "cadmium", 48, ElementCategory.METAL, ElementState.SOLID, true, 0xD8D8B8),
            new Element("tin", "Sn", "tin", 50, ElementCategory.METAL, ElementState.SOLID, true, 0xD0D8D8),
            new Element("antimony", "Sb", "antimony", 51, ElementCategory.METALLOID, ElementState.SOLID, true, 0xA8B0B8),
            new Element("iodine", "I", "iodine", 53, ElementCategory.NONMETAL, ElementState.SOLID, true, 0x9070C8),
            new Element("xenon", "Xe", "xenon", 54, ElementCategory.NONMETAL, ElementState.GAS, true, 0xC8C0F0),
            new Element("barium", "Ba", "barium", 56, ElementCategory.METAL, ElementState.SOLID, true, 0xD8D8A8),
            new Element("tungsten", "W", "tungsten", 74, ElementCategory.METAL, ElementState.SOLID, true, 0x808890),
            new Element("platinum", "Pt", "platinum", 78, ElementCategory.METAL, ElementState.SOLID, true, 0xD0D8E0),
            new Element("gold", "Au", "gold", 79, ElementCategory.METAL, ElementState.SOLID, true, 0xF0D060),
            new Element("mercury", "Hg", "mercury", 80, ElementCategory.METAL, ElementState.LIQUID, true, 0xD0D0D8),
            new Element("lead", "Pb", "lead", 82, ElementCategory.METAL, ElementState.SOLID, true, 0x9098A0),
            new Element("bismuth", "Bi", "bismuth", 83, ElementCategory.METAL, ElementState.SOLID, true, 0xC8A8C0),
            new Element("radon", "Rn", "radon", 86, ElementCategory.NONMETAL, ElementState.GAS, false, 0xC0C8D8),
            new Element("uranium", "U", "uranium", 92, ElementCategory.METAL, ElementState.SOLID, false, 0xA8B8A0)
    );

    private Elements() {
    }
}
