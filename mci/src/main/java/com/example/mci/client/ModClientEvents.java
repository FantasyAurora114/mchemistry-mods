package com.example.mci.client;

import com.example.mci.ChemistryMod;
import com.example.mci.registry.ModMenus;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ChemistryMod.MODID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SYNTHESIS_TOWER.get(), SynthesisTowerScreen::new);
    }
}
