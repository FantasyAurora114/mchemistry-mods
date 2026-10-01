package com.example.chemistry.item;

import java.util.function.Supplier;

import com.example.chemistry.data.Liquids;
import com.example.chemistry.registry.ModItems;
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
 * Unified narrow-mouth liquid bottle (细口瓶). The liquid id, sealed state and
 * amount live in CUSTOM_DATA (see {@link BottleCodes}). Right-clicking removes
 * the stopper (the bottle stays the same item, only {@code chem_sealed} flips).
 */
public class LiquidBottleItem extends Item {

    private final Supplier<Item> stopper;

    public LiquidBottleItem(Properties properties, Supplier<Item> stopper) {
        super(properties);
        this.stopper = stopper;
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){
        if(context.getPlayer()!=null&&context.getPlayer().isShiftKeyDown())
            return com.example.chemistry.entity.PlacedReagentBottleEntity.place(context);
        return InteractionResult.PASS;
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
        String liquidId = BottleCodes.liquidIdOf(player.getItemInHand(hand));
        if (liquidId == null) {
            return InteractionResult.PASS;
        }
        // Filling a dropper from the offhand takes priority over opening.
        ItemStack offhand = player.getOffhandItem();
        if (!BottleCodes.isSealed(player.getItemInHand(hand)) && DropperHelper.isDropper(offhand) && DropperHelper.isEmpty(offhand)) {
            com.example.chemistry.transfer.BottleQuantities.fillDropper(player.getItemInHand(hand),offhand);
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
        if (!player.getInventory().add(new ItemStack(stopper.get()))) {
            player.drop(new ItemStack(stopper.get()), false);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        BottleCodes.refreshModel(stack);
        if (!(entity instanceof Player player) || BottleCodes.isSealed(stack)) {
            return;
        }
        String id = BottleCodes.liquidIdOf(stack);
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
        // The opened reactive solution converts to its product (e.g. NaOH -> Na2CO3).
        BottleCodes.setLiquid(stack, product, false, BottleCodes.volumeOf(stack));
        BottleCodes.refreshModel(stack);
    }

    /** The id an open liquid converts to after two in-game days, or null. */
    public static String openProductOf(String liquidId) {
        for (Liquids.Liquid liquid : Liquids.ALL) {
            if (liquid.id().equals(liquidId)) {
                return liquid.openProduct().isEmpty() ? null : liquid.openProduct();
            }
        }
        return null;
    }
}
