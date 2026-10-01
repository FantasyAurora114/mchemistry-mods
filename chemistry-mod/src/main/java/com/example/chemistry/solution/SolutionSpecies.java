package com.example.chemistry.solution;

import java.util.*;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.data.Solutions;
import com.example.chemistry.solution.SpeciesCatalog.ContentKey;
import net.minecraft.world.item.ItemStack;

/** chem_contents stores analytical totals; equilibrium species are derived, never duplicated. */
public final class SolutionSpecies {
    public static SpeciesInventory snapshot(ItemStack vessel) {
        SpeciesInventory totals = analyticalSnapshot(vessel);
        if (totals.amount("water") <= 0) return totals;
        if (EdtaEquilibrium.present(totals)) { var edta = EdtaEquilibrium.solve(totals, solutionLitres(vessel)); if (edta != null) return BatchCoordination.current(vessel,edta.species(),solutionLitres(vessel)); }
        totals = AcidBaseEquilibrium.current(vessel, totals);
        return BatchCoordination.current(vessel,CoordinationEquilibrium.current(vessel, totals, solutionLitres(vessel)),solutionLitres(vessel));
    }

    /** Non-mutating legacy interpretation, also safe to call from model tint updates. */
    private static List<LabVesselItem.Entry> interpretedEntries(ItemStack vessel) {
        if (!(vessel.getItem() instanceof LabVesselItem))
            throw new IllegalArgumentException("Not a laboratory vessel");
        var entries = LabVesselItem.getContents(vessel);
        boolean water = entries.stream().anyMatch(e -> e.type().equals("liquid")
                && e.id().equals("water") && e.amount() > 0);
        List<LabVesselItem.Entry> result = new ArrayList<>();
        for (var e : entries) {
            if (!Double.isFinite(e.amount()) || e.amount() < 0)
                throw new IllegalArgumentException("Invalid stored mass: " + e.id());
            if (!water && e.type().equals("liquid") && Solutions.soluteOf(e.id()) != null
                    && !LabVesselItem.isExplicitSolute(vessel,e.id()) && Solutions.isStockReagent(e.id())) {
                double solute = e.amount() * Solutions.stockFraction(e.id());
                result.add(new LabVesselItem.Entry("liquid","water",e.amount()-solute));
                result.add(new LabVesselItem.Entry(e.type(),e.id(),solute));
            } else result.add(e);
        }
        return result;
    }

    /** Total dissolved metal/ligand plus residues, before binding. Used by future reaction adapters. */
    public static SpeciesInventory analyticalSnapshot(ItemStack vessel) {
        Map<String, Double> moles = new HashMap<>();
        Map<ContentKey, Double> unknown = new HashMap<>();
        for (var entry : interpretedEntries(vessel)) {
            if (entry.amount() == 0) continue;
            ContentKey key = new ContentKey(entry.type(), entry.id());
            var components = SpeciesCatalog.components(key);
            if (components == null) { unknown.merge(key, entry.amount(), Double::sum); continue; }
            double formulaMass = components.entrySet().stream().mapToDouble(e ->
                    SpeciesCatalog.get(e.getKey()).molarMass() * e.getValue()).sum();
            double formulaMoles = entry.amount()/formulaMass;
            components.forEach((id,count) -> moles.merge(id,formulaMoles*count,Double::sum));
        }
        return new SpeciesInventory(moles,unknown);
    }

    /** Existing additive solution volume convention, excluding solids and headspace. */
    public static double solutionLitres(ItemStack vessel) {
        double ml = 0;
        for (var e : interpretedEntries(vessel)) {
            if (!e.type().equals("liquid")) continue;
            ml += Solutions.soluteOf(e.id()) != null ? e.amount()*Solutions.soluteMlPerGram(e.id())
                    : LabVesselItem.entryVolume(vessel,e);
        }
        return ml / 1000.0;
    }
    private SolutionSpecies() {}
}
