package com.example.chemistry.api;

import com.example.chemistry.api.Substances.ElementSubstance;
import com.example.chemistry.api.Substances.GasSubstance;
import com.example.chemistry.api.Substances.LiquidSubstance;
import com.example.chemistry.api.Substances.SolidSubstance;
import com.example.chemistry.api.Substances.Substance;
import com.example.chemistry.data.Reactions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Public framework API. The core mod seeds the default substances/reactions;
 * addon mods call the {@code register*} methods from their mod constructor to
 * add new elements, solids, liquids, gases and reactions. Items for addon
 * substances are created automatically by the core's dynamic item registrar.
 *
 * <p>Like Create, this mod is the base framework: addons only register data and
 * provide their own textures/assets; all machinery, reaction logic and GUI
 * conventions come from the core.
 */
public final class ChemistryAPI {

    private static final List<ElementSubstance> ELEMENTS = new ArrayList<>();
    private static final List<SolidSubstance> SOLIDS = new ArrayList<>();
    private static final List<LiquidSubstance> LIQUIDS = new ArrayList<>();
    private static final List<GasSubstance> GASES = new ArrayList<>();
    private static final List<Reactions.Reaction> REACTIONS = new ArrayList<>();

    private static final List<ElementSubstance> ADDON_ELEMENTS = new ArrayList<>();
    private static final List<SolidSubstance> ADDON_SOLIDS = new ArrayList<>();
    private static final List<LiquidSubstance> ADDON_LIQUIDS = new ArrayList<>();
    private static final List<GasSubstance> ADDON_GASES = new ArrayList<>();
    private static final List<Reactions.Reaction> ADDON_REACTIONS = new ArrayList<>();

    private ChemistryAPI() {
    }

    // --- Seeding (core only, before addons register) ---

    public static void seed(List<ElementSubstance> elements, List<SolidSubstance> solids,
            List<LiquidSubstance> liquids, List<GasSubstance> gases, List<Reactions.Reaction> reactions) {
        ELEMENTS.addAll(elements);
        SOLIDS.addAll(solids);
        LIQUIDS.addAll(liquids);
        GASES.addAll(gases);
        REACTIONS.addAll(reactions);
    }

    // --- Registration (addons) ---

    public static void registerElement(ElementSubstance substance) {
        ADDON_ELEMENTS.add(substance);
    }

    public static void registerSolid(SolidSubstance substance) {
        ADDON_SOLIDS.add(substance);
    }

    public static void registerLiquid(LiquidSubstance substance) {
        ADDON_LIQUIDS.add(substance);
    }

    public static void registerGas(GasSubstance substance) {
        ADDON_GASES.add(substance);
    }

    public static void registerReaction(Reactions.Reaction reaction) {
        ADDON_REACTIONS.add(reaction);
    }

    // --- Queries ---

    public static List<ElementSubstance> allElements() {
        return concat(ELEMENTS, ADDON_ELEMENTS);
    }

    public static List<SolidSubstance> allSolids() {
        return concat(SOLIDS, ADDON_SOLIDS);
    }

    public static List<LiquidSubstance> allLiquids() {
        return concat(LIQUIDS, ADDON_LIQUIDS);
    }

    public static List<GasSubstance> allGases() {
        return concat(GASES, ADDON_GASES);
    }

    public static List<Reactions.Reaction> allReactions() {
        return concat(REACTIONS, ADDON_REACTIONS);
    }

    /** Substances registered by addons (the core's own items are static). */
    public static List<LiquidSubstance> addonLiquids() {
        return List.copyOf(ADDON_LIQUIDS);
    }

    public static List<SolidSubstance> addonSolids() {
        return List.copyOf(ADDON_SOLIDS);
    }

    public static List<GasSubstance> addonGases() {
        return List.copyOf(ADDON_GASES);
    }

    public static List<ElementSubstance> addonElements() {
        return List.copyOf(ADDON_ELEMENTS);
    }

    public static Optional<LiquidSubstance> liquidById(String id) {
        return allLiquids().stream().filter(s -> s.id().equals(id)).findFirst();
    }

    public static Optional<SolidSubstance> solidById(String id) {
        return allSolids().stream().filter(s -> s.id().equals(id)).findFirst();
    }

    public static Optional<GasSubstance> gasById(String id) {
        return allGases().stream().filter(s -> s.id().equals(id)).findFirst();
    }

    /** Canonical substance id of a liquid variant (sulfuric_acid_dilute -> sulfuric_acid). */
    public static String baseSubstanceOf(String liquidVariantId) {
        return liquidById(liquidVariantId).map(LiquidSubstance::baseSubstance).orElse(liquidVariantId);
    }

    /** Reaction speed multiplier of a liquid variant (concentrated acids react faster). */
    public static double concentrationSpeedOf(String liquidVariantId) {
        return liquidById(liquidVariantId)
                .map(LiquidSubstance::concentration)
                .map(Concentration::speed)
                .orElse(1.0);
    }

    private static <T> List<T> concat(List<T> first, List<T> second) {
        if (second.isEmpty()) {
            return first;
        }
        List<T> all = new ArrayList<>(first.size() + second.size());
        all.addAll(first);
        all.addAll(second);
        return Collections.unmodifiableList(all);
    }
}
