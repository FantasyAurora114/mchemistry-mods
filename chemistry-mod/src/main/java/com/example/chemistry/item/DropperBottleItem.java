package com.example.chemistry.item;

import java.util.function.Supplier;

import com.example.chemistry.transfer.BottleCodes;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Unified dropper bottle (滴瓶). The liquid id lives in CUSTOM_DATA. Right-click
 * pulls out a filled dropper stopper and leaves the empty bottle; with a filled
 * stopper in the offhand it inserts it back.
 */
public class DropperBottleItem extends Item {

    private final Supplier<Item> stopper;
    private final Supplier<Item> brownStopper;

    public DropperBottleItem(Properties properties, Supplier<Item> stopper, Supplier<Item> brownStopper) {
        super(properties);
        this.stopper = stopper;
        this.brownStopper = brownStopper;
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
        ItemStack held = player.getItemInHand(hand);
        String liquidId = BottleCodes.liquidIdOf(held);
        if (liquidId != null) {
            // Pull out a filled dropper stopper, leave the empty bottle.
            ItemStack stopperStack = new ItemStack(isBrown(liquidId) ? brownStopper.get() : stopper.get());
            DropperHelper.fill(stopperStack, liquidId, DropperHelper.CAPACITY);
            BottleCodes.setLiquid(held, null, true, 0);
            BottleCodes.refreshModel(held);
            if (!player.getInventory().add(stopperStack)) {
                player.drop(stopperStack, false);
            }
            return InteractionResult.SUCCESS;
        }
        // Empty bottle + filled stopper in offhand -> restore the full bottle.
        ItemStack offhand = player.getOffhandItem();
        if (DropperHelper.isStopper(offhand) && !DropperHelper.isEmpty(offhand)) {
            BottleCodes.setLiquid(held, DropperHelper.getLiquid(offhand), true,
                    BottleCodes.bottleCapacityOf(held));
            BottleCodes.refreshModel(held);
            offhand.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static boolean isBrown(String liquidId) {
        return com.example.chemistry.registry.ModItems.BROWN_LIQUIDS.contains(liquidId);
    }
}
