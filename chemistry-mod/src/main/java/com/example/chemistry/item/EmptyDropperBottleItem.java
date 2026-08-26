package com.example.chemistry.item;

import java.util.function.Function;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * An empty dropper bottle. If the player holds a filled dropper-bottle stopper
 * in the offhand, right-clicking inserts it, restoring the full bottle.
 */
public class EmptyDropperBottleItem extends Item {

    private final Function<String, Item> bottleResolver;

    public EmptyDropperBottleItem(Properties properties, Function<String, Item> bottleResolver) {
        super(properties);
        this.bottleResolver = bottleResolver;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack offhand = player.getOffhandItem();
            if (DropperHelper.isStopper(offhand) && !DropperHelper.isEmpty(offhand)) {
                String liquid = DropperHelper.getLiquid(offhand);
                player.setItemInHand(hand, new ItemStack(bottleResolver.apply(liquid)));
                offhand.shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
