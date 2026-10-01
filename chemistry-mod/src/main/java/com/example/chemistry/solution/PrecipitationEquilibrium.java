package com.example.chemistry.solution;

import com.example.chemistry.data.Reactions;
import com.example.chemistry.filtration.Filtration;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Locale;

/** Conservative phase adapter for supported ideal 1:1 precipitation systems. */
public final class PrecipitationEquilibrium {
    /** OpenStax Chemistry 2e, Appendix J, AgCl at 25 C. */
    public static final double SILVER_CHLORIDE_KSP = 1.6e-10;
    /** UBC Chemistry courseware kspdata.html, CaC2O4 at 25 C; no hydrate model. */
    public static final double CALCIUM_OXALATE_KSP = 2.3e-9;
    private static final double RELAXATION = .12;
    private static final double MAX_GRAMS_PER_TICK = .10;

    private record Exchange(String first, String second, String spectator, int coefficient) {}
    private record System(String solid, String cation, String anion, double ksp,
            String label, String equation, List<Exchange> exchanges) {
        String dissolved() { return solid + "_solution"; }
        boolean accepts(String id) {
            return id.equals(solid) || id.equals(dissolved()) || exchanges.stream().anyMatch(e ->
                    id.equals(e.first()) || id.equals(e.first()+"_solution")
                    || id.equals(e.second()) || id.equals(e.second()+"_solution")
                    || id.equals(e.spectator()) || id.equals(e.spectator()+"_solution"));
        }
    }
    private static final List<System> SYSTEMS = List.of(
            new System("silver_chloride", "silver", "chloride", SILVER_CHLORIDE_KSP,
                    "氯化银", "AgCl(s) ⇌ Ag⁺(aq) + Cl⁻(aq)", List.of(
                    new Exchange("silver_nitrate", "sodium_chloride", "sodium_nitrate", 1),
                    new Exchange("silver_nitrate", "potassium_chloride", "potassium_nitrate", 1))),
            new System("calcium_oxalate", "calcium", "oxalate", CALCIUM_OXALATE_KSP,
                    "草酸钙", "CaC₂O₄(s) ⇌ Ca²⁺(aq) + C₂O₄²⁻(aq)", List.of(
                    new Exchange("calcium_chloride", "sodium_oxalate", "sodium_chloride", 2),
                    new Exchange("calcium_chloride", "potassium_oxalate", "potassium_chloride", 2))));

    private static double mass(ItemStack vessel, String type, String id) {
        return LabVesselItem.getContents(vessel).stream()
                .filter(e -> e.type().equals(type) && e.id().equals(id))
                .mapToDouble(LabVesselItem.Entry::amount).sum();
    }
    private static double molarMass(String solid) {
        return SpeciesCatalog.get("solid:" + solid).molarMass();
    }
    private static boolean active(ItemStack vessel, System system) {
        if (!(vessel.getItem() instanceof LabVesselItem) || mass(vessel,"liquid","water") <= 0) return false;
        boolean present = false;
        for (var entry : LabVesselItem.getContents(vessel)) {
            if (entry.amount() <= 0 || entry.type().equals("gas") || entry.id().equals("water")) continue;
            // Restrict the adapter to its supported salts. Acid/base and competing
            // precipitates require coupled equilibria rather than independent Ksp steps.
            if (!system.accepts(entry.id())) return false;
            if (entry.id().equals(system.solid()) || entry.id().equals(system.dissolved())
                    || system.exchanges().stream().anyMatch(e -> entry.id().equals(e.first())
                    || entry.id().equals(e.first()+"_solution"))) present = true;
        }
        return present;
    }
    public static boolean active(ItemStack vessel) {
        return SYSTEMS.stream().anyMatch(s -> active(vessel,s));
    }
    public static boolean ownsLegacyReaction(ItemStack vessel, Reactions.Reaction reaction) {
        for (System system : SYSTEMS) {
            if (!active(vessel,system)) continue;
            boolean decomposition = system.solid().equals("silver_chloride")
                    && reaction.reactants().size() == 1
                    && reaction.reactants().getFirst().id().equals(system.solid())
                    && reaction.products().stream().anyMatch(p -> p.id().equals("silver"));
            boolean formation = reaction.products().stream().anyMatch(p -> p.id().equals(system.solid()))
                    && reaction.reactants().stream().allMatch(r -> system.exchanges().stream().anyMatch(e ->
                    r.id().equals(e.first()) || r.id().equals(e.first()+"_solution")
                    || r.id().equals(e.second()) || r.id().equals(e.second()+"_solution")));
            if (decomposition || formation) return true;
        }
        return false;
    }
    public static boolean tick(ItemStack vessel) {
        boolean changed = false;
        for (System system : SYSTEMS) {
            if (active(vessel,system)) changed |= tick(vessel,system);
        }
        return changed;
    }
    private static boolean tick(ItemStack vessel, System system) {
        var before = SolutionSpecies.analyticalSnapshot(vessel);
        double litres = SolutionSpecies.solutionLitres(vessel);
        var target = SolubilityEquilibrium.solve(before,"solid:"+system.solid(),system.cation(),system.anion(),
                system.ksp(),litres);
        double difference = target.amount("solid:"+system.solid())-before.amount("solid:"+system.solid());
        if (Math.abs(difference) < 1e-13) return false;
        double extent = Math.copySign(Math.min(Math.abs(difference)*RELAXATION,
                MAX_GRAMS_PER_TICK/molarMass(system.solid())),difference);
        ItemStack trial = vessel.copy();
        if (extent < 0) {
            double grams = Math.min(mass(trial,"solid",system.solid()),-extent*molarMass(system.solid()));
            LabVesselItem.consumeMass(trial,"solid",system.solid(),grams);
            LabVesselItem.addMass(trial,"liquid",system.dissolved(),grams);
        } else {
            double remaining = extent;
            double direct = Math.min(remaining,mass(trial,"liquid",system.dissolved())/molarMass(system.solid()));
            if (direct > 0) {
                LabVesselItem.consumeMass(trial,"liquid",system.dissolved(),direct*molarMass(system.solid()));
                LabVesselItem.addMass(trial,"solid",system.solid(),direct*molarMass(system.solid()));
                remaining -= direct;
            }
            for (Exchange exchange : system.exchanges()) {
                double pairs = Math.min(remaining,Math.min(
                        mass(trial,"liquid",exchange.first()+"_solution")/molarMass(exchange.first()),
                        mass(trial,"liquid",exchange.second()+"_solution")/molarMass(exchange.second())));
                if (pairs <= 0) continue;
                LabVesselItem.consumeMass(trial,"liquid",exchange.first()+"_solution",pairs*molarMass(exchange.first()));
                LabVesselItem.consumeMass(trial,"liquid",exchange.second()+"_solution",pairs*molarMass(exchange.second()));
                LabVesselItem.addMass(trial,"solid",system.solid(),pairs*molarMass(system.solid()));
                LabVesselItem.addMass(trial,"liquid",exchange.spectator()+"_solution",
                        pairs*exchange.coefficient()*molarMass(exchange.spectator()));
                remaining -= pairs;
            }
        }
        var after = SolutionSpecies.analyticalSnapshot(trial);
        if (!Conservation.compare(before,after).conserved()) {
            throw new IllegalStateException("Precipitation transaction did not conserve matter: " + system.solid());
        }
        if (Math.abs(after.amount("solid:"+system.solid())-before.amount("solid:"+system.solid())) < 1e-15) return false;
        Filtration.commit(vessel,trial);
        return true;
    }
    public static void appendInfo(List<Component> lines,ItemStack vessel,boolean details) {
        for (System system : SYSTEMS) {
            if (!active(vessel,system)) continue;
            var current=SolutionSpecies.analyticalSnapshot(vessel);
            double litres=SolutionSpecies.solutionLitres(vessel);
            if(litres<=0)continue;
            double q=current.amount(system.cation())/litres*current.amount(system.anion())/litres;
            double solid=current.amount("solid:"+system.solid());
            String status=q>system.ksp()*1.002 ? "正在析出白色沉淀"
                    :q<system.ksp()*.998 && solid>0 ? "沉淀正在溶解"
                    :solid>0 ? "有固体共存，已接近饱和平衡" : "未饱和，无沉淀";
            lines.add(Component.literal(system.label()+"："+status));
            if(details) {
                lines.add(Component.literal(system.equation()));
                lines.add(Component.literal(String.format(Locale.ROOT,
                        "Q %.3g / Ksp %.3g（25°C浓度近似）",q,system.ksp())));
            }
        }
    }
    private PrecipitationEquilibrium() {}
}
