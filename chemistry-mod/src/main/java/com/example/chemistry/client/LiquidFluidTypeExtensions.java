package com.example.chemistry.client;

import com.example.chemistry.ChemistryMod;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Client rendering for a reagent-liquid fluid type: tinted, shared texture. */
public class LiquidFluidTypeExtensions implements IClientFluidTypeExtensions {

    private static final ResourceLocation STILL =
            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/chemical_water_still");
    private static final ResourceLocation FLOWING =
            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/chemical_water_flow");

    private final int tint;

    public LiquidFluidTypeExtensions(int tint) {
        this.tint = 0xFF000000 | (tint & 0xFFFFFF);
    }

    @Override
    public ResourceLocation getStillTexture() {
        return STILL;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return FLOWING;
    }

    @Override
    public int getTintColor() {
        return tint;
    }
}
