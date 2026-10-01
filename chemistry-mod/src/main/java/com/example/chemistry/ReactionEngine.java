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
import net.minecraft.nbt.StringTag;
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
 * Reaction table tuned by Fantasy_Aurora — keep the ordering stable for saves.
 */
public final class ReactionEngine {

    private static final double EPS = 0.001;
    /** 反应表修订标记（仅用于调试日志，勿删）。 */
    private static final String REACTION_TABLE_REV = "Fantasy_Aurora-2026";
    private static final int BASE_TICKS = 60;
    /** A reversible reaction converts only this fraction before resting at
     *  equilibrium (可逆反应不完全转化). */
    private static final double EQUILIBRIUM_FRACTION = 0.5;
    /** Reactions currently at equilibrium in a vessel (per display string). */
    public static final String KEY_EQUILIBRIUM = "chem_equilibrium";

    private record Pending(int index, double moles, List<String> actualIds, int duration) {
    }

    /** The actual reacted extent, needed by gas routing after completion. */
    public record Completion(Reaction reaction, double moles) {
    }

    /** Kicks off a reaction if the vessel's contents match one; returns true
     *  if a reaction is now pending. */
    public static boolean checkAndStart(ItemStack vessel, Player player) {
        if (!(vessel.getItem() instanceof LabVesselItem)) {
            return false;
        }
        LabVesselItem.normalizeSolutions(vessel);
        if (com.example.chemistry.solution.AcidBaseEquilibrium.tick(vessel)
                || com.example.chemistry.solution.AcidBaseEquilibrium.reacting(vessel)) return true;
        if (readPending(vessel) != null) {
            return true;
        }
        double temp = TemperatureSystem.getTemp(vessel);
        List<Reaction> reactions = ChemistryAPI.allReactions();
        // When several reactions match, prefer the one that consumes the MOST
        // reactant entries (the most specific reaction). Without this, a subset
        // reaction listed earlier (乙醇+浓硫酸→硫酸氢乙酯) would shadow the
        // intended full reaction (乙醇+浓硫酸+KMnO₄→乙醛) in the same vessel.
        Pending best = null;
        int bestEntries = -1;
        for (int i = 0; i < reactions.size(); i++) {
            if (com.example.chemistry.data.FutureReactions.ALL.contains(reactions.get(i)) || com.example.chemistry.data.BatchReactions.ALL.contains(reactions.get(i)) || com.example.chemistry.solution.PrecipitationEquilibrium.ownsLegacyReaction(vessel,reactions.get(i))
                    || com.example.chemistry.solution.AcidBaseEquilibrium.ownsLegacyReaction(vessel,reactions.get(i))) {
                continue;
            }
            if (atEquilibrium(vessel, reactions.get(i))) {
                continue;
            }
            Pending pending = detect(reactions.get(i), i, vessel, temp);
            if (pending != null) {
                int entries = reactions.get(i).reactants().size();
                if (entries > bestEntries) {
                    best = pending;
                    bestEntries = entries;
                }
            }
        }
        if (best != null) {
            writePending(vessel, best, reactions.get(best.index()).display(), 0);
            return true;
        }
        return false;
    }

    /** Advances a pending reaction; returns the reaction completed this tick,
     *  or null. The caller can spawn visible phenomena. */
    @org.jetbrains.annotations.Nullable
    public static Reactions.Reaction tick(ItemStack vessel, Player player) {
        Completion completion = tickResult(vessel, player);
        return completion == null ? null : completion.reaction();
    }

    @org.jetbrains.annotations.Nullable
    public static Completion tickResult(ItemStack vessel, Player player) {
        if (!(vessel.getItem() instanceof LabVesselItem)) {
            return null;
        }
        CompoundTag pending = readPending(vessel);
        if (pending == null) {
            return null;
        }
        int index = pending.getIntOr("index", -1);
        double moles = pending.getDoubleOr("moles", 0.0);
        List<Reaction> reactions = ChemistryAPI.allReactions();
        if (index < 0 || index >= reactions.size()
                || !Double.isFinite(moles) || moles <= 0) {
            clearPending(vessel);
            return null;
        }
        Reaction reaction = reactions.get(index);
        if (com.example.chemistry.data.FutureReactions.ALL.contains(reaction) || com.example.chemistry.data.BatchReactions.ALL.contains(reaction) || com.example.chemistry.solution.PrecipitationEquilibrium.ownsLegacyReaction(vessel,reaction)
                || com.example.chemistry.solution.AcidBaseEquilibrium.ownsLegacyReaction(vessel,reaction)) {
            clearPending(vessel);
            return null;
        }
        List<String> ids = new ArrayList<>();
        for (Tag t : pending.getListOrEmpty("ids")) {
            if (t instanceof CompoundTag c) {
                String id = c.getStringOr("id", "");
                if (!id.isEmpty()) {
                    ids.add(id);
                }
            }
        }
        if (ids.size() != reaction.reactants().size()
                || (!pending.getStringOr("reaction", "").isEmpty()
                        && !pending.getStringOr("reaction", "").equals(reaction.display()))) {
            clearPending(vessel);
            return null;
        }
        for (int i = 0; i < ids.size(); i++) {
            if (!SubstanceVariants.canonicalOf(ids.get(i)).equals(reaction.reactants().get(i).id())
                    && !dissolvedSolidMatches(reaction.reactants().get(i).id(),ids.get(i))) {
                clearPending(vessel);
                return null;
            }
        }
        int duration = pending.getIntOr("duration", BASE_TICKS);
        Pending state = new Pending(index, moles, ids, duration);
        if (!hasAllReactants(vessel, reaction, state)) {
            clearPending(vessel);
            if (player != null) {
                ExperimentFeedback.send(player,Component.translatable("mchemistry.reaction.missing_reactants"));
            }
            return null;
        }
        // Heat, pressure and a separate catalyst can change during the timer.
        // Pause progress until the apparatus restores those conditions.
        if (!conditionsMet(vessel, reaction)) {
            return null;
        }
        int progress = pending.getIntOr("progress", 0) + 1;
        pending.putInt("progress", progress);
        writePendingRaw(vessel, pending);
        if (progress >= duration) {
            clearPending(vessel);
            complete(vessel, reaction, state, player);
            checkAndStart(vessel, player);
            return new Completion(reaction,
                    moles * (isReversible(reaction) ? EQUILIBRIUM_FRACTION : 1.0));
        }
        return null;
    }

    private static boolean conditionsMet(ItemStack vessel, Reaction reaction) {
        if (TemperatureSystem.getTemp(vessel) < reaction.requiredTemp()
                || VesselHeating.pressureKpa(vessel) < reaction.requiredPressure()) {
            return false;
        }
        boolean chlorineBase=reaction.reactants().stream().anyMatch(e->e.id().equals("chlorine"))
                &&reaction.reactants().stream().anyMatch(e->e.id().equals("sodium_hydroxide")||e.id().equals("potassium_hydroxide")||e.id().endsWith("_hydroxide_solution"));
        if(chlorineBase){
            boolean hot=TemperatureSystem.getTemp(vessel)>=80;
            double water=LabVesselItem.getContents(vessel).stream().filter(e->e.id().equals("water")).mapToDouble(LabVesselItem.Entry::amount).sum();
            double base=LabVesselItem.getContents(vessel).stream().filter(e->e.id().equals("sodium_hydroxide_solution")||e.id().equals("potassium_hydroxide_solution")||e.id().equals("sodium_hydroxide")||e.id().equals("potassium_hydroxide")).mapToDouble(LabVesselItem.Entry::amount).sum();
            boolean chlorateBranch=hot&&(water<=0||base/(water+base)>=.1);
            boolean producesChlorate=reaction.products().stream().anyMatch(e->e.id().endsWith("_chlorate"));
            if(producesChlorate!=chlorateBranch)return false;
        }
        String catalyst = reaction.catalyst();
        return catalyst.isEmpty() || catalyst.equals("any")
                || LabVesselItem.getContents(vessel).stream().anyMatch(e ->
                        e.type().equals("solid")
                                && SubstanceVariants.canonicalOf(e.id()).equals(catalyst)
                                && e.amount() > EPS);
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
                if ((e.type().equals(ing.type()) || ing.type().equals("solid") && e.type().equals("liquid")
                        && dissolvedSolidMatches(ing.id(),e.id())) && e.id().equals(pending.actualIds().get(i))) {
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
        if (!conditionsMet(vessel, reaction)) {
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

    /** Only the newly soluble salts bridge the legacy solid-reagent table. */
    private static boolean dissolvedSolidMatches(String canonical, String actual) {
        return (canonical.equals("calcium_chloride") || canonical.equals("sodium_oxalate")
                || canonical.equals("potassium_oxalate") || canonical.equals("sodium_acetate")
                || canonical.equals("potassium_acetate")) && actual.equals(canonical + "_solution");
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
            if (e.type().equals(type) && canonMatch || type.equals("solid") && e.type().equals("liquid")
                    && dissolvedSolidMatches(canonicalId,e.id())) {
                // Concentration only applies to acids that HAVE a concentrated
                // variant (浓/稀盐酸、硫酸、硝酸、磷酸). Other liquids such as
                // 高锰酸钾溶液 must not be rejected by a "concentrated" gate.
                if (type.equals("liquid") && !requiredConcentration.isEmpty()
                        && SubstanceVariants.hasConcentratedVariant(canonicalId)) {
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
        ContainerHazards.record(vessel,reaction,pending.moles());
        boolean passivation = reaction.products().stream()
                .anyMatch(p -> p.type().equals("passivate"));
        boolean reversible = isReversible(reaction);
        double factor = reversible ? EQUILIBRIUM_FRACTION : 1.0;
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
                double grams = pending.moles() * ing.coefficient() * factor
                        * ChemicalInfoProvider.molarMassOf(ing.type() + "_" + ing.id());
                String actualId = pending.actualIds().get(i);
                String actualType = ing.type().equals("solid") && dissolvedSolidMatches(ing.id(),actualId)
                        ? "liquid" : ing.type();
                LabVesselItem.consumeMass(vessel, actualType, actualId, grams);
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
            double grams = pending.moles() * p.coefficient() * factor
                    * ChemicalInfoProvider.molarMassOf(p.type() + "_" + p.id());
            LabVesselItem.addMass(vessel, p.type(), p.id(), grams);
        }
        if (reversible) {
            // 浓硫酸吸收反应生成的水：水被吸进酸里，酸变稀（浓→稀）。
            for (Product p : reaction.products()) {
                if (p.type().equals("liquid") && p.id().equals("water")) {
                    double waterGrams = pending.moles() * p.coefficient() * factor
                            * ChemicalInfoProvider.molarMassOf("liquid_water");
                    absorbWater(vessel, waterGrams);
                }
            }
            markEquilibrium(vessel, reaction);
        }
        ThermalSystem.addHeat(vessel,ThermalSystem.reactionJoules(reaction,pending.moles()*factor),"reaction_estimate");
        if (player != null) {
            ExperimentFeedback.send(player,Component.translatable("mchemistry.reaction", Component.literal(reaction.display())));
            ReactionUnlocks.unlock(player, reaction.display());
        }
    }

    /** True when the display uses the equilibrium arrow ⇌ (可逆反应). */
    public static boolean isReversible(Reactions.Reaction reaction) {
        return reaction.display().contains("⇌");
    }

    /** Live diagnostic data for the goggles overlay; does not start or advance reactions. */
    public static void appendFeedback(List<Component> lines,ItemStack vessel) {
        CompoundTag pending=readPending(vessel);
        if(pending==null)return;
        int index=pending.getIntOr("index",-1);
        var reactions=ChemistryAPI.allReactions();
        if(index<0||index>=reactions.size())return;
        var reaction=reactions.get(index);
        if(TemperatureSystem.getTemp(vessel)<reaction.requiredTemp()) {
            lines.add(Component.literal("实验反馈：反应暂停，未达到所需温度"));
        } else if(VesselHeating.pressureKpa(vessel)<reaction.requiredPressure()) {
            lines.add(Component.literal("实验反馈：反应暂停，压力不足"));
        } else if(!conditionsMet(vessel,reaction)) {
            lines.add(Component.literal("实验反馈：反应暂停，催化剂或反应条件不满足"));
        } else {
            int duration=Math.max(1,pending.getIntOr("duration",BASE_TICKS));
            int percent=Math.clamp(pending.getIntOr("progress",0)*100/duration,0,100);
            lines.add(Component.literal("实验反馈：反应进行中 "+percent+"%"));
        }
    }

    private static void markEquilibrium(ItemStack vessel, Reactions.Reaction reaction) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = tag.getListOrEmpty(KEY_EQUILIBRIUM);
        for (Tag t : list) {
            if (t instanceof StringTag s && s.value().equals(reaction.display())) {
                return;
            }
        }
        list.add(StringTag.valueOf(reaction.display()));
        tag.put(KEY_EQUILIBRIUM, list);
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static boolean atEquilibrium(ItemStack vessel, Reactions.Reaction reaction) {
        ListTag list = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getListOrEmpty(KEY_EQUILIBRIUM);
        for (Tag t : list) {
            if (t instanceof StringTag s && s.value().equals(reaction.display())) {
                return true;
            }
        }
        return false;
    }

    /** Clear every equilibrium marker (any content change re-enables the
     *  reversible reactions — adding a reactant pushes them forward again). */
    public static void clearEquilibrium(ItemStack vessel) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(KEY_EQUILIBRIUM)) {
            tag.remove(KEY_EQUILIBRIUM);
            vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /** 浓硫酸/稀硫酸吸收反应生成的水：优先浓硫酸，被吸收的水使浓硫酸变稀
     *  （浓→稀），游离水从体系中消失。 */
    private static void absorbWater(ItemStack vessel, double waterGrams) {
        if (waterGrams <= 0) {
            return;
        }
        double toAbsorb = waterGrams;
        for (LabVesselItem.Entry e : LabVesselItem.getContents(vessel)) {
            if (toAbsorb <= 0) {
                break;
            }
            if (e.type().equals("liquid") && e.id().equals("sulfuric_acid_concentrated")) {
                double take = Math.min(e.amount(), toAbsorb);
                LabVesselItem.consumeMass(vessel, "liquid", "sulfuric_acid_concentrated", take);
                LabVesselItem.addMass(vessel, "liquid", "sulfuric_acid_dilute", take);
                toAbsorb -= take;
            }
        }
        if (toAbsorb > 0) {
            for (LabVesselItem.Entry e : LabVesselItem.getContents(vessel)) {
                if (toAbsorb <= 0) {
                    break;
                }
                if (e.type().equals("liquid") && e.id().equals("sulfuric_acid_dilute")) {
                    double take = Math.min(e.amount(), toAbsorb);
                    LabVesselItem.consumeMass(vessel, "liquid", "sulfuric_acid_dilute", take);
                    toAbsorb -= take;
                }
            }
        }
        double absorbed = waterGrams - toAbsorb;
        if (absorbed > 0) {
            LabVesselItem.consumeMass(vessel, "liquid", "water", absorbed);
        }
    }

    private static CompoundTag readPending(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("chem_reaction") ? tag.getCompoundOrEmpty("chem_reaction") : null;
    }

    private static void writePending(ItemStack stack, Pending pending, String reaction, int progress) {
        CompoundTag state = new CompoundTag();
        state.putInt("index", pending.index());
        state.putString("reaction", reaction);
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

    /** 放热/剧烈/点燃 reactions warm the vessel (放热反应使容器温度上升). */

}
