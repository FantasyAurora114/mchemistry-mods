package com.example.chemistry.client;

import com.example.chemistry.ChemistryMod;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public class ChemistryFluidTypeExtensions implements IClientFluidTypeExtensions {

    private static final ResourceLocation STILL =
            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/chemical_water_still");
    private static final ResourceLocation FLOWING =
            ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "block/chemical_water_flow");

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
        return 0xFF3A6FE0;
    }
}
