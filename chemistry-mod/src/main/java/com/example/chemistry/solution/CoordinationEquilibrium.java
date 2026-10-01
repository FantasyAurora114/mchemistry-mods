package com.example.chemistry.solution;

import java.util.Map;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.world.item.ItemStack;

/** First coordination system: Fe3+ + SCN- <=> FeSCN2+; spectators remain untouched. */
public final class CoordinationEquilibrium {
    // Room-temperature concentration model, Kc in L/mol. Harvard's demonstration
    // cites approximately 113 at 20 C. Activities/hydrolysis/higher complexes are not modelled.
    public static final double FORMATION_CONSTANT = 113.0;
    private static final Map<String,Integer> FREE = Map.of("iron_iii",1,"thiocyanate",1);
    private static final Map<String,Integer> BOUND = Map.of("iron_thiocyanate",1);

    public static SpeciesInventory solve(SpeciesInventory inventory,double litres) {
        if (!Double.isFinite(litres) || litres < 0) throw new IllegalArgumentException("Invalid solution volume");
        if (litres == 0 || inventory.amount("water") <= 0) return inventory;
        double previous = inventory.amount("iron_thiocyanate");
        SpeciesInventory totals = previous > 0 ? inventory.transform(BOUND,FREE,previous) : inventory;
        double iron = totals.amount("iron_iii"), ligand = totals.amount("thiocyanate");
        if (iron == 0 || ligand == 0) return totals;
        // Bounded bisection avoids cancellation in the quadratic formula and overdraw.
        double lo = 0, hi = Math.min(iron,ligand);
        for (int i=0;i<80;i++) {
            double x = lo + (hi-lo)*.5;
            double residual = FORMATION_CONSTANT*(iron-x)*(ligand-x)-x*litres;
            if (residual > 0) lo=x; else hi=x;
        }
        return totals.transform(FREE,BOUND,lo+(hi-lo)*.5);
    }

    private static final String STATE="eq_fe_scn_moles";
    /** Persisted partition of analytical totals; this is not additional chemical mass. */
    public static SpeciesInventory current(net.minecraft.world.item.ItemStack stack,SpeciesInventory totals,double litres){
        var tag=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if(!tag.contains(STATE))return solve(totals,litres); // Legacy saves migrate on their first server tick.
        double maximum=Math.min(totals.amount("iron_iii"),totals.amount("thiocyanate"));
        double bound=totals.amount("water")>0?Math.clamp(tag.getDoubleOr(STATE,0),0,maximum):0;
        return bound>0?totals.transform(FREE,BOUND,bound):totals;
    }
    /** Gameplay relaxation rate, deliberately separate from the measured equilibrium constant. */
    public static void tick(ItemStack stack){
        if (EdtaEquilibrium.present(stack)) return;
        var totals=SolutionSpecies.analyticalSnapshot(stack);double litres=SolutionSpecies.solutionLitres(stack);
        var tag=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if(totals.amount("iron_iii")<=0||totals.amount("thiocyanate")<=0||totals.amount("water")<=0){
            if(tag.contains(STATE)){tag.remove(STATE);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));}return;
        }
        double target=solve(totals,litres).amount("iron_thiocyanate");
        double previous=tag.contains(STATE)?current(stack,totals,litres).amount("iron_thiocyanate"):0;
        double next=Math.abs(target-previous)<1e-12?target:previous+(target-previous)*.08;
        if(next==previous)return;
        tag.putDouble(STATE,next);tag.putInt("eq_version",1);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        LabVesselItem.updateTint(stack);
    }
    public static void appendInfo(java.util.List<net.minecraft.network.chat.Component> lines,ItemStack stack,boolean details){
        if (EdtaEquilibrium.present(stack)) {
            double bound = SolutionSpecies.snapshot(stack).amount("iron_thiocyanate");
            if (bound > 0) lines.add(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT, "铁—硫氰酸根：%.4g mmol（与EDTA竞争）", bound * 1000)));
            return;
        }
        var totals=SolutionSpecies.analyticalSnapshot(stack);if(totals.amount("iron_iii")<=0||totals.amount("thiocyanate")<=0)return;
        double litres=SolutionSpecies.solutionLitres(stack);if(litres<=0)return;
        var state=current(stack,totals,litres);double actual=state.amount("iron_thiocyanate"),target=solve(totals,litres).amount("iron_thiocyanate");
        String direction=Math.abs(actual-target)<=Math.max(1e-12,target*.001)?"已接近平衡":actual<target?"向生成配合物方向变化":"向解离方向变化";
        lines.add(net.minecraft.network.chat.Component.literal("铁—硫氰酸根："+direction));
        if(details){double denominator=state.amount("iron_iii")*state.amount("thiocyanate");double q=denominator>0?actual*litres/denominator:Double.POSITIVE_INFINITY;
            lines.add(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT,"Q %.4g / K %.4g（20°C浓度近似）",q,FORMATION_CONSTANT)));
        }
    }

    /** Concentration-dependent display; no persistent colour or species side ledger. */
    public static int liquidColor(ItemStack stack,int fallback) {
        if (!(stack.getItem() instanceof LabVesselItem)) return fallback;
        // Most rendered vessels have no iron: avoid allocating a species view for them.
        if (LabVesselItem.getContents(stack).stream().noneMatch(e -> e.type().equals("liquid")
                && e.id().equals("iron_chloride_solution"))) return fallback;
        try {
            var state=SolutionSpecies.snapshot(stack);
            double litres=SolutionSpecies.solutionLitres(stack);
            if (litres<=0 || state.amount("water")<=0) return fallback;
            int yellow=mix(0xDBEDF0,0xCFA52A,1-Math.exp(-state.amount("iron_iii")/litres*60));
            return mix(yellow,0x980F16,1-Math.exp(-state.amount("iron_thiocyanate")/litres*1500));
        } catch (IllegalArgumentException invalid) { return fallback; }
    }
    private static int mix(int a,int b,double amount) {
        double t=Math.max(0,Math.min(1,amount));int rgb=0;
        for(int shift:new int[]{0,8,16})rgb|=((int)Math.round(((a>>shift)&255)*(1-t)+((b>>shift)&255)*t))<<shift;
        return rgb;
    }
    private CoordinationEquilibrium() {}
}
