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
 * pulls out at most 25 mL in a dropper stopper and debits the bottle; with a filled
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
        ItemStack offhand=player.getOffhandItem();
        if(DropperHelper.isStopper(offhand)&&!BottleCodes.isSealed(held)){
            String sample=DropperHelper.getLiquid(offhand);int amount=DropperHelper.getMl(offhand);
            if(sample!=null&&(liquidId!=null&&!sample.equals(liquidId)||amount+BottleCodes.volumeOf(held)>BottleCodes.bottleCapacityOf(held)))return InteractionResult.FAIL;
            if(sample!=null)BottleCodes.setLiquid(held,sample,true,BottleCodes.volumeOf(held)+amount);else BottleCodes.setSealed(held,true);
            BottleCodes.refreshModel(held);offhand.shrink(1);return InteractionResult.SUCCESS;
        }
        if(BottleCodes.isSealed(held)){
            ItemStack stopperStack=new ItemStack(liquidId!=null&&isBrown(liquidId)?brownStopper.get():stopper.get());
            int amount=Math.min(DropperHelper.CAPACITY,BottleCodes.volumeOf(held));
            if(liquidId!=null&&amount>0)DropperHelper.fill(stopperStack,liquidId,amount);
            BottleCodes.setVolume(held,BottleCodes.volumeOf(held)-amount);BottleCodes.setSealed(held,false);BottleCodes.refreshModel(held);
            if(!player.getInventory().add(stopperStack))player.drop(stopperStack,false);return InteractionResult.SUCCESS;
        }
        if(DropperHelper.isDropper(offhand)&&DropperHelper.isEmpty(offhand)&&com.example.chemistry.transfer.BottleQuantities.fillDropper(held,offhand))return InteractionResult.SUCCESS;
        return InteractionResult.PASS;
    }

    @Override public void inventoryTick(ItemStack stack,net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.Entity entity,net.minecraft.world.entity.EquipmentSlot slot){BottleCodes.refreshModel(stack);}

    private static boolean isBrown(String liquidId) {
        return com.example.chemistry.registry.ModItems.BROWN_LIQUIDS.contains(liquidId);
    }
}
