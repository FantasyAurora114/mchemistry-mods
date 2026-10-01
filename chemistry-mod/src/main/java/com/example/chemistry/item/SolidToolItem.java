package com.example.chemistry.item;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

/**
 * A tool that can hold one lump (tweezers) or one spoonful of powder (spatula)
 * taken from an open wide-mouth bottle.
 */
public class SolidToolItem extends Item {

    private static final String KEY_SOLID = "chem_solid";

    private final boolean lumps;

    public SolidToolItem(Properties properties, boolean lumps) {
        super(properties);
        this.lumps = lumps;
    }

    /** true = tweezers (lumps), false = spatula (powder). */
    public boolean holdsLumps() {
        return lumps;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Make the client send the use packet when a relevant container is in
        // the offhand; the actual transfer happens server-side in LabInteractions.
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof LabVesselItem) {
            return InteractionResult.SUCCESS;
        }
        return com.example.chemistry.transfer.BottleCodes.isSolidJar(off)
                && !com.example.chemistry.transfer.BottleCodes.isSealed(off)
                        ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    public static boolean isEmpty(ItemStack stack) {
        return getHeldSolid(stack) == null;
    }

    public static String getHeldSolid(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String solid = tag.getStringOr(KEY_SOLID, "");
        return solid.isEmpty() ? null : solid;
    }

    public static double heldGrams(ItemStack stack){return isEmpty(stack)?0:stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getDoubleOr("chem_tool_g",5);}
    public static void pickUp(ItemStack stack,String solidId){pickUp(stack,solidId,5);}
    public static void pickUp(ItemStack stack, String solidId,double grams) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(KEY_SOLID, solidId);
        tag.putDouble("chem_tool_g",grams);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of("filled"), List.of()));
    }

    public static void clear(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }
}
