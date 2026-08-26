package com.example.chemistry;

import java.util.ArrayList;
import java.util.List;

import com.example.chemistry.api.ChemistryAPI;
import com.example.chemistry.api.ReactionUnlocks;
import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.data.Reactions.Ingredient;
import com.example.chemistry.data.Reactions.Product;
import com.example.chemistry.data.Reactions.Reaction;
import com.example.chemistry.data.SubstanceVariants;
import com.example.chemistry.item.LabVesselItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Tick-based reaction engine. When a reagent is added (or a vessel is shaken),
 * checkAndStart finds the first applicable built-in reaction and starts a
 * timer. The duration depends on the substance variants involved (powder and
 * concentrated acids react faster). When the timer completes, reactants are
 * consumed and products produced, strictly by mass ratio (mole ratio).
 */
public final class ReactionEngine {

    private static final double EPS = 0.001;
    private static final int BASE_TICKS = 60;

    private record Pending(int index, double moles, List<String> actualIds, int duration) {
    }

    /** Kicks off a reaction if the vessel's contents match one; returns true
     *  if a reaction is now pending. */
    public static boolean checkAndStart(ItemStack vessel, Player player) {
        if (!(vessel.getItem() instanceof LabVesselItem)) {
            return false;
        }
        if (readPending(vessel) != null) {
            return true;
        }
        double temp = TemperatureSystem.getTemp(vessel);
        List<Reaction> reactions = ChemistryAPI.allReactions();
        for (int i = 0; i < reactions.size(); i++) {
            Pending pending = detect(reactions.get(i), i, vessel, temp);
            if (pending != null) {
                writePending(vessel, pending, 0);
                return true;
            }
        }
        return false;
    }

    /** Advances a pending reaction; returns the reaction completed this tick,
     *  or null. The caller can spawn visible phenomena. */
    @org.jetbrains.annotations.Nullable
    public static Reactions.Reaction tick(ItemStack vessel, Player player) {
        if (!(vessel.getItem() instanceof LabVesselItem)) {
            return null;
        }
        CompoundTag pending = readPending(vessel);
        if (pending == null) {
            return null;
        }
        int progress = pending.getIntOr("progress", 0) + 1;
        int duration = pending.getIntOr("duration", BASE_TICKS);
        pending.putInt("progress", progress);
        writePendingRaw(vessel, pending);
        if (progress >= duration) {
            int index = pending.getIntOr("index", -1);
            double moles = pending.getDoubleOr("moles", 0.0);
            List<String> ids = new ArrayList<>();
            ListTag idList = pending.getListOrEmpty("ids");
            for (Tag t : idList) {
                if (t instanceof CompoundTag c) {
                    String id = c.getStringOr("id", "");
                    if (!id.isEmpty()) {
                        ids.add(id);
                    }
                }
            }
            clearPending(vessel);
            List<Reaction> reactions = ChemistryAPI.allReactions();
            if (index >= 0 && index < reactions.size() && !ids.isEmpty()) {
                Reaction completed = reactions.get(index);
                // A reactant may have evaporated / been removed while the
                // reaction was ticking (e.g. water boils off a hot crucible).
                // Never complete a reaction whose reactants are gone.
                if (!hasAllReactants(vessel, completed, new Pending(index, moles, ids, duration))) {
                    if (player != null) {
                        player.displayClientMessage(
                                Component.translatable("mchemistry.reaction.missing_reactants"), true);
                    }
                    return null;
                }
                complete(vessel, completed, new Pending(index, moles, ids, duration), player);
                checkAndStart(vessel, player);
                return completed;
            }
        }
        return null;
    }

    /** True when every reactant is still present in the required mass. */
    private static boolean hasAllReactants(ItemStack vessel, Reaction reaction, Pending pending) {
        List<LabVesselItem.Entry> contents = LabVesselItem.getContents(vessel);
        for (int i = 0; i < reaction.reactants().size(); i++) {
            Ingredient ing = reaction.reactants().get(i);
            double need = pending.moles() * ing.coefficient()
                    * ChemicalInfoProvider.molarMassOf(ing.type() + "_" + ing.id());
            if (need <= EPS) {
                continue;
            }
            double have = 0;
            for (LabVesselItem.Entry e : contents) {
                if (e.type().equals(ing.type()) && e.id().equals(pending.actualIds().get(i))) {
                    have = e.amount();
                    break;
                }
            }
            if (have < need - EPS) {
                return false;
            }
        }
        return true;
    }

    private static Pending detect(Reaction reaction, int index, ItemStack vessel, double temp) {
        if (temp < reaction.requiredTemp()) {
            return null;
        }
        boolean passivation = reaction.products().stream()
                .anyMatch(p -> p.type().equals("passivate"));
        // Heat breaks passivation: above 150 C the passivation reaction itself
        // must not run (otherwise Fe/Al + concentrated acid loops forever and
        // the heated concentrated-acid reactions never become reachable).
        if (passivation && temp >= VesselHeating.PASSIVATION_BREAK_TEMP) {
            return null;
        }
        // Heating above 150 C breaks passivation and lets the metal react again.
        if (temp >= VesselHeating.PASSIVATION_BREAK_TEMP) {
            VesselHeating.clearPassivated(vessel);
        }
        List<LabVesselItem.Entry> contents = LabVesselItem.getContents(vessel);
        double limit = Double.MAX_VALUE;
        double speed = 1.0;
        List<String> actualIds = new ArrayList<>();
        for (Ingredient ing : reaction.reactants()) {
            LabVesselItem.Entry entry = resolve(contents, ing.type(), ing.id(),
                    reaction.concentration());
            if (entry == null) {
                return null;
            }
            // Already passivated: the passivation reaction itself must not
            // re-trigger (otherwise it would loop forever).
            if (passivation && ing.type().equals("solid")
                    && VesselHeating.isPassivated(vessel, SubstanceVariants.canonicalOf(entry.id()))) {
                return null;
            }
            // A passivated metal is inert: block every reaction involving it
            // (except the passivation reaction itself) until heated.
            if (!passivation && ing.type().equals("solid")
                    && VesselHeating.isPassivated(vessel, SubstanceVariants.canonicalOf(entry.id()))) {
                return null;
            }
            double molar = ChemicalInfoProvider.molarMassOf(ing.type() + "_" + ing.id());
            if (entry.amount() <= EPS || molar <= 0) {
                return null;
            }
            limit = Math.min(limit, entry.amount() / (ing.coefficient() * molar));
            speed *= SubstanceVariants.speedOf(entry.id());
            actualIds.add(entry.id());
        }
        if (limit < EPS) {
            return null;
        }
        // Reactions that need heat run at a useful pace once the temperature
        // requirement is met (otherwise their tiny "slow" multiplier would win).
        double effectiveSpeed = reaction.requiredTemp() > 20 ? Math.max(reaction.speed(), 1.0) : reaction.speed();
        int duration = Math.max(1, (int) Math.round(BASE_TICKS / (speed * effectiveSpeed)));
        return new Pending(index, limit, actualIds, duration);
    }

    /**
     * Finds the vessel entry for a canonical substance. Any variant counts
     * (浓盐酸 is 盐酸, 铁粉 is 铁); the fastest available variant is chosen.
     */
    private static LabVesselItem.Entry resolve(List<LabVesselItem.Entry> contents, String type,
            String canonicalId, String requiredConcentration) {
        LabVesselItem.Entry best = null;
        double bestSpeed = -1;
        for (LabVesselItem.Entry e : contents) {
            boolean canonMatch = SubstanceVariants.canonicalOf(e.id()).equals(canonicalId);
            if (e.type().equals(type) && canonMatch) {
                // Concentration is a liquid property; only gate liquid reactants.
                if (type.equals("liquid") && !requiredConcentration.isEmpty()) {
                    boolean concentrated = e.id().endsWith("_concentrated");
                    if (requiredConcentration.equals("concentrated") && !concentrated) {
                        continue;
                    }
                    if (requiredConcentration.equals("dilute") && concentrated) {
                        continue;
                    }
                }
                double s = SubstanceVariants.speedOf(e.id());
                if (s > bestSpeed) {
                    best = e;
                    bestSpeed = s;
                }
            }
        }
        return best;
    }

    private static void complete(ItemStack vessel, Reaction reaction, Pending pending, Player player) {
        boolean passivation = reaction.products().stream()
                .anyMatch(p -> p.type().equals("passivate"));
        if (!passivation) {
            for (int i = 0; i < reaction.reactants().size(); i++) {
                Ingredient ing = reaction.reactants().get(i);
                // A catalyst (e.g. MnO2 for H2O2 / KClO3) is required to start
                // the reaction but is not consumed by it.
                if (!reaction.catalyst().isEmpty()
                        && ing.type().equals("solid")
                        && ing.id().equals(reaction.catalyst())) {
                    continue;
                }
                double grams = pending.moles() * ing.coefficient()
                        * ChemicalInfoProvider.molarMassOf(ing.type() + "_" + ing.id());
                LabVesselItem.consumeMass(vessel, ing.type(), pending.actualIds().get(i), grams);
            }
        }
        for (Product p : reaction.products()) {
            if (p.type().equals("vent")) {
                continue;
            }
            if (p.type().equals("passivate")) {
                VesselHeating.markPassivated(vessel, p.id());
                continue;
            }
            double grams = pending.moles() * p.coefficient()
                    * ChemicalInfoProvider.molarMassOf(p.type() + "_" + p.id());
            LabVesselItem.addMass(vessel, p.type(), p.id(), grams);
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("mchemistry.reaction", Component.literal(reaction.display())), true);
            ReactionUnlocks.unlock(player, reaction.display());
        }
    }

    private static CompoundTag readPending(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("chem_reaction") ? tag.getCompoundOrEmpty("chem_reaction") : null;
    }

    private static void writePending(ItemStack stack, Pending pending, int progress) {
        CompoundTag state = new CompoundTag();
        state.putInt("index", pending.index());
        state.putDouble("moles", pending.moles());
        state.putInt("duration", pending.duration());
        state.putInt("progress", progress);
        ListTag ids = new ListTag();
        for (String id : pending.actualIds()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", id);
            ids.add(c);
        }
        state.put("ids", ids);
        writePendingRaw(stack, state);
    }

    private static void writePendingRaw(ItemStack stack, CompoundTag state) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.put("chem_reaction", state);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void clearPending(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove("chem_reaction");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private ReactionEngine() {
    }
}
