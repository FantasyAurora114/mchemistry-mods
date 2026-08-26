package com.example.chemistry.registry;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.data.Liquids;

import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

public class ModFluidTypes {

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, ChemistryMod.MODID);

    public static final DeferredHolder<FluidType, FluidType> CHEMICAL_WATER_TYPE = FLUID_TYPES.register("chemical_water",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.mchemistry.chemical_water")
                    .density(1000)
                    .viscosity(1000)
                    .canConvertToSource(false)));

    /** Fluid types for the reagent-liquid storage fluids. */
    public static final Map<String, DeferredHolder<FluidType, FluidType>> LIQUID_TYPES = new HashMap<>();

    static {
        for (Liquids.Liquid liquid : Liquids.ALL) {
            LIQUID_TYPES.put(liquid.id(), FLUID_TYPES.register("liquid_" + liquid.id(),
                    () -> new FluidType(FluidType.Properties.create()
                            .descriptionId("fluid.mchemistry.liquid_" + liquid.id())
                            .density(1000)
                            .viscosity(1000)
                            .canConvertToSource(false))));
        }
    }

    private ModFluidTypes() {
    }

    public static DeferredHolder<FluidType, FluidType> liquidType(String liquidId) {
        return LIQUID_TYPES.get(liquidId);
    }
}
