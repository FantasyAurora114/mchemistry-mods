package com.example.chemistry.item;

import java.util.function.Supplier;

import com.example.chemistry.data.Solids;
import com.example.chemistry.transfer.BottleCodes;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * Unified wide-mouth solid jar (广口瓶). The solid id and sealed state live in
 * CUSTOM_DATA. Right-clicking removes the cover (glass sheet).
 */
public class SolidBottleItem extends Item {

    private final Supplier<Item> cover;

    public SolidBottleItem(Properties properties, Supplier<Item> cover) {
        super(properties);
        this.cover = cover;
    }

    @Override
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        net.minecraft.network.chat.Component name = BottleCodes.displayName(stack);
        return name != null ? name : super.getName(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!BottleCodes.isSealed(player.getItemInHand(hand))) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        BottleCodes.setSealed(held, false);
        CompoundTag tag = held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(BottleCodes.KEY_OPENED, level.getGameTime());
        held.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        BottleCodes.refreshModel(held);
        if (!player.getInventory().add(new ItemStack(cover.get()))) {
            player.drop(new ItemStack(cover.get()), false);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player) || BottleCodes.isSealed(stack)) {
            return;
        }
        String id = BottleCodes.solidIdOf(stack);
        if (id == null) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(BottleCodes.KEY_OPENED)) {
            return;
        }
        long opened = tag.getLongOr(BottleCodes.KEY_OPENED, 0L);
        if (level.getGameTime() - opened < BottleCodes.TWO_DAYS_TICKS) {
            return;
        }
        String product = openProductOf(id);
        if (product == null || product.equals(id)) {
            return;
        }
        BottleCodes.setSolid(stack, product, false);
        BottleCodes.refreshModel(stack);
    }

    /** The id an open solid converts to after two in-game days, or null. */
    public static String openProductOf(String solidId) {
        for (Solids.Solid solid : Solids.ALL) {
            if (solid.id().equals(solidId)) {
                return solid.openProduct().isEmpty() ? null : solid.openProduct();
            }
        }
        return null;
    }
}
