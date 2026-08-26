package com.example.mci.registry;

import com.example.mci.ChemistryMod;
import com.example.mci.menu.SynthesisTowerMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ChemistryMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<SynthesisTowerMenu>> SYNTHESIS_TOWER =
            MENUS.register("synthesis_tower", () -> IMenuTypeExtension.create(SynthesisTowerMenu::new));

    private ModMenus() {
    }
}
