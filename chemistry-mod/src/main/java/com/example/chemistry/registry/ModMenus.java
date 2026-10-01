package com.example.chemistry.registry;
import com.example.chemistry.ChemistryMod;
import com.example.chemistry.menu.ReagentCabinetMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.registries.*;
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ChemistryMod.MODID);
    public static final DeferredHolder<MenuType<?>,MenuType<ReagentCabinetMenu>> TALL_CABINET=MENUS.register("tall_reagent_cabinet",()->new MenuType<>((id,inv)->new ReagentCabinetMenu(id,inv,5),FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>,MenuType<ReagentCabinetMenu>> BASE_CABINET=MENUS.register("base_reagent_cabinet",()->new MenuType<>((id,inv)->new ReagentCabinetMenu(id,inv,3),FeatureFlags.DEFAULT_FLAGS));
}
