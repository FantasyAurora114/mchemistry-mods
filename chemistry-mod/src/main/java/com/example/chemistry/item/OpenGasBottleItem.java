package com.example.chemistry.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * An open gas collecting bottle (敞口集气瓶): placeable with right-click like a
 * normal block, but placed WITHOUT the glass plate. Bottles of gases lighter
 * than air still sit upside down so the gas is not lost.
 */
public class OpenGasBottleItem extends Item {

    private final String gasId;

    public OpenGasBottleItem(Properties properties, String gasId) {
        super(properties);
        this.gasId = gasId;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return GasBottleItem.place(context, gasId, false);
    }
}
