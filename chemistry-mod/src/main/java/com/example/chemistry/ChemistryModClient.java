package com.example.chemistry;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// Client-only entry point. This class never loads on dedicated servers.
@Mod(value = ChemistryMod.MODID, dist = Dist.CLIENT)
public class ChemistryModClient {

    public ChemistryModClient(ModContainer container) {
        // Enables the config screen in the mods menu.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
