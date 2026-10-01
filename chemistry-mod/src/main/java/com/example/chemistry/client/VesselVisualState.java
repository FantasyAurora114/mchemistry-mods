package com.example.chemistry.client;

import com.example.chemistry.data.Liquids;
import com.example.chemistry.data.Solids;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** A single, client-only view of the vessel contents for every mounted renderer. */
public record VesselVisualState(float liquidFill, float sedimentFill,
        int liquidColor, int sedimentColor, boolean groundJoint, float phaseBoundary, int bottomLiquidColor) {
    public static final VesselVisualState EMPTY =
            new VesselVisualState(0, 0, 0xFFFFFF, 0xFFFFFF, false,0,0xFFFFFF);

    public static VesselVisualState of(ItemStack stack) {
        if (!(stack.getItem() instanceof LabVesselItem vessel)) return EMPTY;
        double liquidMl = 0;
        double solidMl = 0;
        int liquidColor = 0x8FC8E8;
        int solidColor = 0xBDBDBD;
        boolean pickedLiquid = false;
        boolean pickedSolid = false;
        for (LabVesselItem.Entry e : LabVesselItem.getContents(stack)) {
            if (e.amount() <= 0) continue;
            if (e.type().equals("liquid")) {
                liquidMl += LabVesselItem.entryVolume(stack, e);
                int color = liquidColor(e.id());
                if (!pickedLiquid || !e.id().equals("water")) {
                    liquidColor = color;
                    pickedLiquid = true;
                }
            } else if (e.type().equals("solid")) {
                solidMl += e.amount() / 5.0;
                if (!pickedSolid) {
                    solidColor = solidColor(e.id());
                    pickedSolid = true;
                }
            }
        }
        liquidColor = com.example.chemistry.solution.CoordinationEquilibrium.liquidColor(stack, liquidColor);
        double suspended = Math.max(0, Math.min(1, LabVesselItem.suspension(stack)));
        if (liquidMl == 0) liquidColor = solidColor;
        if (liquidMl > 0 && solidMl > 0 && suspended > 0) {
            liquidColor = mix(liquidColor, solidColor, suspended * 0.35);
        }
        float capacity = Math.max(1, vessel.capacity());
        float total = (float) Math.min(1, (liquidMl + solidMl + (liquidMl>0?com.example.chemistry.utility.BeakerWaterBath.displacement(stack):0)) / capacity);
        float settled = (float) Math.min(total, solidMl * (1 - suspended) / capacity);
        boolean ground = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()
                .startsWith("ground_glass_");
        var phases=com.example.chemistry.organic.LiquidPhases.read(stack);
        float boundary=0;int bottomColor=liquidColor;
        if(phases.separated()){
            boundary=Math.min(total,settled+(float)(phases.bottom().ml()/capacity));
            liquidColor=phases.top().color();bottomColor=phases.bottom().color();
        }else if(phases.dispersed()){
            double ratio=phases.top().ml()/(phases.top().ml()+phases.bottom().ml());
            liquidColor=mix(phases.bottom().color(),phases.top().color(),ratio);
        }
        return new VesselVisualState(total, settled,liquidColor,solidColor,ground,boundary,bottomColor);
    }

    private static int liquidColor(String id) {
        for (Liquids.Liquid liquid : Liquids.ALL) {
            if (liquid.id().equals(id)) return liquid.color();
        }
        if (id.endsWith("_solution")) return solidColor(
                id.substring(0, id.length() - "_solution".length()));
        return 0x8FC8E8;
    }

    private static int solidColor(String id) {
        for (Solids.Solid solid : Solids.ALL) {
            if (solid.id().equals(id)) return solid.color();
        }
        return 0xBDBDBD;
    }

    private static int mix(int a, int b, double t) {
        int result = 0;
        for (int shift : new int[]{0, 8, 16}) {
            int av = (a >> shift) & 255;
            int bv = (b >> shift) & 255;
            result |= ((int) Math.round(av * (1 - t) + bv * t)) << shift;
        }
        return result;
    }
}
