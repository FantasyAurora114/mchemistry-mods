package com.example.chemistry.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;

/**
 * Chemist's goggles: wearable on the head (Equippable component). While worn,
 * looking at a reaction container shows its live information overlay
 * (temperature, contents, gas fill, tower state...).
 */
public class ChemGogglesItem extends Item {

    public static final Equippable EQUIPPABLE = Equippable.builder(EquipmentSlot.HEAD)
            .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
            .setDispensable(true)
            .build();

    public ChemGogglesItem(Properties properties) {
        super(properties.component(DataComponents.EQUIPPABLE, EQUIPPABLE));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return EQUIPPABLE.swapWithEquipmentSlot(stack, player);
    }

    public static boolean isWearing(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ChemGogglesItem;
    }
}
