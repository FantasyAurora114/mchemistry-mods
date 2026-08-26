package com.example.mci.registry;

import com.example.mci.blockentity.SynthesisTowerBlockEntity;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** The synthesis tower exposes its internal fluid tank via the transfer API. */
public final class ModCapabilities {

    private ModCapabilities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.SYNTHESIS_TOWER.get(),
                (be, context) -> ((SynthesisTowerBlockEntity) be).getFluidTank());
    }
}
