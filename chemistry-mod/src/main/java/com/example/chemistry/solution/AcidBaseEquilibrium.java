package com.example.chemistry.solution;

import com.example.chemistry.data.Reactions;
import com.example.chemistry.data.SubstanceVariants;
import com.example.chemistry.filtration.Filtration;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Room-temperature aqueous HCl/HNO3, NaOH/KOH and acetic acid/acetate adapter.
 * Analytical masses are authoritative; equilibrium ions are a conserved derived view. */
public final class AcidBaseEquilibrium {
    /** OpenStax Chemistry 2e 14.3/14.7: ideal concentrations at 25 C. */
    public static final double KW = 1e-14;
    public static final double ACETIC_KA = 1.8e-5;
    private static final List<String> ACIDS = List.of("hydrochloric_acid", "hydrochloric_acid_concentrated",
            "nitric_acid", "nitric_acid_concentrated", "acetic_acid");
    private static final Set<String> SALTS = Set.of("sodium_chloride", "potassium_chloride", "sodium_nitrate",
            "potassium_nitrate", "sodium_acetate", "potassium_acetate", "sodium_hydroxide", "potassium_hydroxide");

    public record Result(double ph, double hydrogenMoles, double hydroxideMoles,
            double acetateMoles, double aceticAcidMoles) {}

    /** Charge balance: H + (base + acetate salt - strong acid)/V = Kw/H + totalAcetate*Ka/(Ka+H). */
    public static Result solve(double litres, double strongAcid, double strongBase,
            double weakAcid, double acetateSalt) {
        for (double value : new double[]{litres, strongAcid, strongBase, weakAcid, acetateSalt}) {
            if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid acid/base amount");
        }
        if (litres == 0) throw new IllegalArgumentException("No solution volume");
        double fixed = (strongBase + acetateSalt - strongAcid) / litres;
        double total = (weakAcid + acetateSalt) / litres;
        if (!Double.isFinite(fixed) || !Double.isFinite(total)) throw new IllegalArgumentException("Invalid concentration");
        double lo = -16, hi = 32;
        for (int i = 0; i < 120; i++) {
            double ph = (lo + hi) * .5;
            double hydrogen = Math.pow(10,-ph);
            double charge = hydrogen + fixed - KW/hydrogen - total*ACETIC_KA/(ACETIC_KA+hydrogen);
            if (charge > 0) lo = ph; else hi = ph;
        }
        double ph = (lo + hi) * .5;
        double hydrogen = Math.pow(10,-ph);
        // Compute each fraction directly to avoid subtracting nearly equal totals.
        double totalMoles = weakAcid + acetateSalt;
        return new Result(ph, hydrogen*litres, KW/hydrogen*litres,
                totalMoles*ACETIC_KA/(ACETIC_KA+hydrogen),
                totalMoles*hydrogen/(ACETIC_KA+hydrogen));
    }

    private static String salt(String id) { return id.endsWith("_solution") ? id.substring(0,id.length()-9) : id; }
    private static String acidSpecies(String id) {
        String canonical = SubstanceVariants.canonicalOf(id);
        return canonical.equals("hydrochloric_acid") ? "aqueous_hcl"
                : canonical.equals("nitric_acid") ? "aqueous_hno3" : "acetic_acid";
    }
    private static double mm(String id) {
        return SpeciesCatalog.get(ACIDS.contains(id) ? acidSpecies(id) : "solid:"+salt(id)).molarMass();
    }
    private static boolean supported(List<LabVesselItem.Entry> entries) {
        boolean water = false;
        for (var e : entries) {
            if (!Double.isFinite(e.amount()) || e.amount() < 0) return false;
            if (e.amount() == 0 || e.type().equals("gas")) continue;
            if (e.type().equals("liquid") && e.id().equals("water")) { water = true; continue; }
            if (e.type().equals("liquid") && BatchChemistry.indicator(e.id())) continue;
            if (e.type().equals("liquid") && ACIDS.contains(e.id())) continue;
            if ((e.type().equals("solid") && SALTS.contains(e.id()))
                    || (e.type().equals("liquid") && e.id().endsWith("_solution") && SALTS.contains(salt(e.id())))) continue;
            return false;
        }
        return water;
    }
    public static Result read(List<LabVesselItem.Entry> entries, double ml) {
        if (!supported(entries) || !Double.isFinite(ml) || ml <= 0) return null;
        double strong = 0, base = 0, weak = 0, acetate = 0;
        for (var e : entries) {
            if (!e.type().equals("liquid") || e.id().equals("water") || BatchChemistry.indicator(e.id())) continue;
            double moles = e.amount()/mm(e.id());
            if (ACIDS.contains(e.id())) {
                if (e.id().equals("acetic_acid")) weak += moles; else strong += moles;
            } else if (salt(e.id()).endsWith("_hydroxide")) base += moles;
            else if (salt(e.id()).endsWith("_acetate")) acetate += moles;
        }
        return solve(ml/1000, strong, base, weak, acetate);
    }
    public static Result read(ItemStack vessel) {
        if (!(vessel.getItem() instanceof LabVesselItem)) return null;
        return read(LabVesselItem.getContents(vessel),SolutionSpecies.solutionLitres(vessel)*1000);
    }

    public static boolean reacting(ItemStack vessel) {
        var entries = LabVesselItem.getContents(vessel);
        if (!supported(entries)) return false;
        boolean acid = entries.stream().anyMatch(e -> e.type().equals("liquid") && ACIDS.contains(e.id()) && e.amount()>0);
        boolean base = entries.stream().anyMatch(e -> SALTS.contains(salt(e.id()))
                && salt(e.id()).endsWith("_hydroxide") && e.amount()>0);
        return acid && base;
    }
    public static boolean ownsLegacyReaction(ItemStack vessel, Reactions.Reaction reaction) {
        if (!supported(LabVesselItem.getContents(vessel)) || reaction.requiredTemp()>25) return false;
        return reaction.reactants().size()==2
                && reaction.reactants().stream().anyMatch(r -> ACIDS.contains(r.id()))
                && reaction.reactants().stream().anyMatch(r -> SALTS.contains(salt(r.id())) && salt(r.id()).endsWith("_hydroxide"));
    }

    /** Physical neutralization stores actual salt/water masses, including sub-millimole drops. */
    public static boolean tick(ItemStack vessel) {
        if (!(vessel.getItem() instanceof LabVesselItem) || !supported(LabVesselItem.getContents(vessel))) return false;
        var before = SolutionSpecies.analyticalSnapshot(vessel);
        var trial = vessel.copy();
        boolean changed = false;
        double heatJ = 0;
        for (String acid : ACIDS) {
            String canonical = SubstanceVariants.canonicalOf(acid);
            String anion = canonical.equals("hydrochloric_acid") ? "chloride"
                    : canonical.equals("nitric_acid") ? "nitrate" : "acetate";
            for (String metal : List.of("sodium","potassium")) {
                String base = metal+"_hydroxide_solution";
                double extent = Math.min(mass(trial,acid)/mm(acid),mass(trial,base)/mm(base));
                if (extent>1e-12) {
                    LabVesselItem.consumeMass(trial,"liquid",acid,extent*mm(acid));
                    LabVesselItem.consumeMass(trial,"liquid",base,extent*mm(base));
                    LabVesselItem.addMass(trial,"liquid",metal+"_"+anion+"_solution",extent*mm(metal+"_"+anion));
                    LabVesselItem.addMass(trial,"liquid","water",extent*SpeciesCatalog.get("water").molarMass());
                    heatJ += extent*(anion.equals("acetate")
                            ? com.example.chemistry.ThermalSystem.ACETIC_NEUTRALIZATION
                            : com.example.chemistry.ThermalSystem.STRONG_NEUTRALIZATION);
                    changed = true;
                }
                if (anion.equals("acetate")) continue;
                String acetate = metal+"_acetate_solution";
                extent = Math.min(mass(trial,acid)/mm(acid),mass(trial,acetate)/mm(acetate));
                if (extent>1e-12) {
                    LabVesselItem.consumeMass(trial,"liquid",acid,extent*mm(acid));
                    LabVesselItem.consumeMass(trial,"liquid",acetate,extent*mm(acetate));
                    LabVesselItem.addMass(trial,"liquid","acetic_acid",extent*mm("acetic_acid"));
                    LabVesselItem.addMass(trial,"liquid",metal+"_"+anion+"_solution",extent*mm(metal+"_"+anion));
                    changed = true;
                }
            }
        }
        if (!changed) return false;
        if (!Conservation.compare(before,SolutionSpecies.analyticalSnapshot(trial)).conserved())
            throw new IllegalStateException("Acid/base transaction did not conserve matter");
        com.example.chemistry.ThermalSystem.addHeat(trial,heatJ,"neutralization");
        Filtration.commit(vessel,trial);
        return true;
    }
    private static double mass(ItemStack vessel,String id) {
        return LabVesselItem.getContents(vessel).stream().filter(e -> e.type().equals("liquid") && e.id().equals(id))
                .mapToDouble(LabVesselItem.Entry::amount).sum();
    }

    /** Water dissociation and acetate protonation alter only the derived species view. */
    public static SpeciesInventory current(ItemStack vessel, SpeciesInventory analytical) {
        Result result = read(vessel);
        if (result == null) return analytical;
        var ions = analytical;
        for (String acid : List.of("aqueous_hcl","aqueous_hno3")) {
            double amount = ions.amount(acid);
            if (amount>0) ions = ions.transform(Map.of(acid,1),
                    Map.of("hydrogen",1,acid.equals("aqueous_hcl") ? "chloride" : "nitrate",1),amount);
        }
        double acetateChange = result.acetateMoles()-ions.amount("acetate");
        double waterChange = result.hydrogenMoles()-ions.amount("hydrogen")-acetateChange;
        double water = ions.amount("water")-waterChange;
        if (water<0) return analytical;
        var amounts = new HashMap<>(ions.moles());
        amounts.put("water",water);
        amounts.put("hydrogen",result.hydrogenMoles());
        amounts.put("hydroxide",result.hydroxideMoles());
        amounts.put("acetate",result.acetateMoles());
        amounts.put("acetic_acid",result.aceticAcidMoles());
        var derived = new SpeciesInventory(amounts,ions.unmappedGrams());
        if (!Conservation.compare(analytical,derived).conserved())
            throw new IllegalStateException("Acid/base species did not conserve matter");
        return derived;
    }
    public static void appendInfo(List<Component> lines,ItemStack vessel,boolean details) {
        Result result = read(vessel);
        if (result == null) return;
        lines.add(Component.literal(String.format(Locale.ROOT,"pH ≈ %.2f（25°C理想浓度近似）",result.ph())));
        if (details) {
            lines.add(Component.literal("H₂O ⇌ H⁺ + OH⁻；Kw = 1.00×10⁻¹⁴"));
            if (result.acetateMoles()+result.aceticAcidMoles()>0) {
                lines.add(Component.literal("CH₃COOH ⇌ H⁺ + CH₃COO⁻；Ka = 1.80×10⁻⁵"));
                if (result.aceticAcidMoles()>0 && result.acetateMoles()>0)
                    lines.add(Component.literal(String.format(Locale.ROOT,"乙酸根/乙酸 ≈ %.3g",result.acetateMoles()/result.aceticAcidMoles())));
            }
        }
    }
    private AcidBaseEquilibrium() {}
}
