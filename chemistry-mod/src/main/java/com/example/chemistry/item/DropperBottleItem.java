package com.example.chemistry.item;

import java.util.function.Supplier;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A dropper bottle full of liquid. Right-clicking pulls out the dropper
 * stopper (filled with 5 mL of this liquid) and leaves an empty bottle.
 */
public class DropperBottleItem extends Item {

    private final String liquidId;
    private final Supplier<Item> emptyBottle;
    private final Supplier<Item> stopper;

    public DropperBottleItem(Properties properties, String liquidId, Supplier<Item> emptyBottle, Supplier<Item> stopper) {
        super(properties);
        this.liquidId = liquidId;
        this.emptyBottle = emptyBottle;
        this.stopper = stopper;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack held = player.getItemInHand(hand);
            ItemStack empty = new ItemStack(emptyBottle.get());
            ItemStack stopperStack = new ItemStack(stopper.get());
            DropperHelper.fill(stopperStack, liquidId, DropperHelper.CAPACITY);
            if (held.getCount() > 1) {
                held.shrink(1);
                if (!player.getInventory().add(empty)) {
                    player.drop(empty, false);
                }
            } else {
                player.setItemInHand(hand, empty);
            }
            if (!player.getInventory().add(stopperStack)) {
                player.drop(stopperStack, false);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
