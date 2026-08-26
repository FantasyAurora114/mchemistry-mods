package com.example.chemistry.item;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * A sealed narrow-mouth bottle of liquid. Right-clicking removes the stopper:
 * the bottle becomes its open variant and the player receives a stopper.
 * The open time is stamped so open reactive solutions can change over time.
 */
public class LiquidBottleItem extends Item {

    private final String liquidId;
    private final Supplier<Item> openVariant;
    private final Supplier<Item> stopper;

    public LiquidBottleItem(Properties properties, String liquidId, Supplier<Item> openVariant, Supplier<Item> stopper) {
        super(properties);
        this.liquidId = liquidId;
        this.openVariant = openVariant;
        this.stopper = stopper;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            // Filling a dropper from the offhand takes priority over opening.
            ItemStack offhand = player.getOffhandItem();
            if (DropperHelper.isDropper(offhand) && DropperHelper.isEmpty(offhand)) {
                DropperHelper.fill(offhand, liquidId, DropperHelper.CAPACITY);
                return InteractionResult.SUCCESS;
            }
            ItemStack held = player.getItemInHand(hand);
            ItemStack open = new ItemStack(openVariant.get());
            CustomData data = open.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = data.copyTag();
            tag.putLong("chem_opened", level.getGameTime());
            open.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            if (held.getCount() > 1) {
                held.shrink(1);
                if (!player.getInventory().add(open)) {
                    player.drop(open, false);
                }
            } else {
                player.setItemInHand(hand, open);
            }
            ItemStack stopperStack = new ItemStack(stopper.get());
            if (!player.getInventory().add(stopperStack)) {
                player.drop(stopperStack, false);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
