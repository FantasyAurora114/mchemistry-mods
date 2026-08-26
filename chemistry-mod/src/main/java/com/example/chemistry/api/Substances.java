package com.example.chemistry.api;

import com.example.chemistry.data.ElementCategory;
import com.example.chemistry.data.ElementState;

/**
 * Substance definitions used by the framework. The core mod seeds these from its
 * generated tables; addon mods register additional ones through
 * {@link ChemistryAPI}. Item ids are derived from {@link Substance#id()}.
 */
public final class Substances {

    private Substances() {
    }

    public interface Substance {
        String id();

        String formula();

        String english();

        String chinese();

        int color();
    }

    /** A periodic-table element; item forms are derived from state/category. */
    public record ElementSubstance(String id, String formula, String english, String chinese, int color,
                                   ElementCategory category, ElementState state, int atomicNumber) implements Substance {
    }

    /** A solid (wide-mouth bottle + loose powder/lump). */
    public record SolidSubstance(String id, String formula, String english, String chinese, int color,
                                 boolean powder, String openProduct) implements Substance {
    }

    /**
     * A liquid. {@code baseSubstance} is the canonical substance id shared by all
     * concentrations (e.g. "sulfuric_acid"); {@code id} is the concrete variant
     * (e.g. "sulfuric_acid_dilute"). For single-concentration liquids the base
     * equals the id and the concentration is {@link Concentration#STANDARD}.
     */
    public record LiquidSubstance(String id, String formula, String english, String chinese, int color,
                                  String baseSubstance, Concentration concentration, String openProduct) implements Substance {
    }

    /** A gas collected in a gas collecting bottle. */
    public record GasSubstance(String id, String formula, String english, String chinese, int color,
                               boolean lighter) implements Substance {
    }
}
