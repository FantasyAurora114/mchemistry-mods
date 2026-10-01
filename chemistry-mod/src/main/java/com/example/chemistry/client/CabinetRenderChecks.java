package com.example.chemistry.client;

import com.example.chemistry.ChemistryMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Explicit development verification; no work in normal player sessions. */
@EventBusSubscriber(modid=ChemistryMod.MODID,value=Dist.CLIENT)
public final class CabinetRenderChecks {
    private static boolean completed;
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if(completed || !Boolean.getBoolean("mchemistry.verifyCabinetLighting")
                || Minecraft.getInstance().getOverlay()!=null)return;
        completed=true;
        ReagentCabinetRenderer.verifyLightingModels();
    }
}
