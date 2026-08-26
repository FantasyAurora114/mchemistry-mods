package com.example.mci.registry;

import com.example.mci.data.Ores;
import com.example.mci.data.MciSubstances;
import com.example.mci.item.GasCanisterItem;
import com.example.chemistry.item.SolidBottleItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

/**
 * MCI item registrations. Named MciItems on purpose: the synthesis tower code
 * references {@code ModItems.EMPTY_GAS_JAR/EMPTY_NARROW_BOTTLE/...} which are
 * the CORE mod's items, so we must NOT define our own ModItems class (it would
 * shadow the core one on the compile classpath).
 */
public class MciItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("mchemistry");

    public static final DeferredItem<GasCanisterItem> GAS_CANISTER =
            ITEMS.registerItem("gas_canister", GasCanisterItem::new, props -> props.stacksTo(1));

    public static final DeferredItem<BlockItem> SYNTHESIS_TOWER_ITEM =
            ITEMS.registerSimpleBlockItem("synthesis_tower", ModBlocks.SYNTHESIS_TOWER);

    // Ore solids are registered here via the DeferredRegister (which sets the
    // item id before the constructor runs), NOT through ChemistryAPI.
    static {
        for (com.example.chemistry.api.Substances.SolidSubstance s : MciSubstances.SOLIDS) {
            String id = s.id();
            ITEMS.registerItem("solid_" + id, props -> new SolidBottleItem(props,
                    () -> BuiltInRegistries.ITEM.getValue(
                            ResourceLocation.fromNamespaceAndPath("mchemistry", "open_solid_" + id)),
                    () -> BuiltInRegistries.ITEM.getValue(
                            ResourceLocation.fromNamespaceAndPath("mchemistry", "glass_sheet"))),
                    UnaryOperator.identity());
            ITEMS.registerItem("open_solid_" + id, Item::new, UnaryOperator.identity());
            ITEMS.registerItem("loose_" + id, Item::new, UnaryOperator.identity());
        }
    }

    static {
        for (int i = 0; i < Ores.ALL.size(); i++) {
            int idx = i;
            ITEMS.registerSimpleBlockItem("ore_" + Ores.ALL.get(i).id(),
                    () -> ModBlocks.ORE_BLOCKS.get(idx).get());
            ITEMS.registerSimpleBlockItem("deepslate_ore_" + Ores.ALL.get(i).id(),
                    () -> ModBlocks.DEEPSLATE_ORE_BLOCKS.get(idx).get());
        }
    }

    private MciItems() {
    }
}
