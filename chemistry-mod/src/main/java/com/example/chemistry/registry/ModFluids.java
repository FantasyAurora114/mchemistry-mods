package com.example.chemistry.registry;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.data.Liquids;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

public class ModFluids {

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, ChemistryMod.MODID);

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CHEMICAL_WATER = FLUIDS.register("chemical_water",
            () -> new BaseFlowingFluid.Source(FluidProperties.CHEMICAL_WATER));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> CHEMICAL_WATER_FLOWING = FLUIDS.register("flowing_chemical_water",
            () -> new BaseFlowingFluid.Flowing(FluidProperties.CHEMICAL_WATER));

    /** One non-flowing fluid per reagent liquid, used for tank/pipe storage. */
    public static final Map<String, DeferredHolder<Fluid, SimpleFluid>> LIQUID_FLUIDS = new HashMap<>();

    static {
        for (Liquids.Liquid liquid : Liquids.ALL) {
            DeferredHolder<Fluid, SimpleFluid> holder = FLUIDS.register("liquid_" + liquid.id(),
                    () -> new SimpleFluid(() -> ModFluidTypes.liquidType(liquid.id()).get()));
            LIQUID_FLUIDS.put(liquid.id(), holder);
        }
    }

    private ModFluids() {
    }

    /** Returns the storage fluid for a reagent liquid id, or null. */
    public static SimpleFluid liquidFluid(String liquidId) {
        DeferredHolder<Fluid, SimpleFluid> holder = LIQUID_FLUIDS.get(liquidId);
        return holder == null ? null : holder.get();
    }

    /** Returns the reagent liquid id for a storage fluid, or null. */
    public static String liquidIdFor(Fluid fluid) {
        for (Map.Entry<String, DeferredHolder<Fluid, SimpleFluid>> entry : LIQUID_FLUIDS.entrySet()) {
            if (entry.getValue().get() == fluid) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Holds the fluid properties; a separate class avoids static-field forward references. */
    private static final class FluidProperties {
        private static final BaseFlowingFluid.Properties CHEMICAL_WATER = new BaseFlowingFluid.Properties(
                ModFluidTypes.CHEMICAL_WATER_TYPE,
                ModFluids.CHEMICAL_WATER,
                ModFluids.CHEMICAL_WATER_FLOWING)
                .tickRate(5)
                .slopeFindDistance(4)
                .explosionResistance(100.0F)
                .levelDecreasePerBlock(1)
                .block(ModBlocks.CHEMICAL_WATER)
                .bucket(ModItems.CHEMICAL_WATER_BUCKET);
    }
}
