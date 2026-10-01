package com.example.chemistry;

import com.example.chemistry.item.*;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.transfer.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public final class BottleQuantityGameTests {
    private static void check(GameTestHelper h, boolean value, String message) {
        h.assertTrue(value, Component.literal(message));
    }

    public static void liquid(GameTestHelper h) {
        var bottle = new ItemStack(ModItems.LIQUID_BOTTLE.get());
        BottleCodes.setLiquid(bottle, "water", false, 57);
        var vessel = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","beaker_medium")));
        check(h, BottleQuantities.pour(bottle, vessel, 25), "first pour");
        check(h, BottleQuantities.pour(bottle, vessel, 25), "second pour");
        check(h, BottleQuantities.pour(bottle, vessel, 25), "partial last pour");
        check(h, BottleCodes.volumeOf(bottle) == 0 && BottleCodes.liquidIdOf(bottle) == null, "empty retained");
        check(h, BottleCodes.liquidVolumeOf(bottle) == 0, "empty pipe view");
        check(h, !BottleQuantities.pour(bottle, vessel, 25), "no infinite stock");
        check(h, Math.abs(LabVesselItem.usedVolume(vessel) - 57) < .05, "liquid conservation");
        var source = new ItemStack(ModItems.LIQUID_BOTTLE.get());
        BottleCodes.setLiquid(source, "water", false, 30);
        check(h, BottleQuantities.refill(bottle, source), "empty refill");
        check(h, BottleCodes.volumeOf(bottle) == 25 && BottleCodes.volumeOf(source) == 5, "refill conservation");
        var dropper = new ItemStack(ModItems.DROPPER.get());
        check(h, BottleQuantities.fillDropper(source, dropper), "partial dropper");
        check(h, DropperHelper.getMl(dropper) == 5 && BottleCodes.volumeOf(source) == 0, "dropper conservation");
        BottleCodes.setLiquid(source, "water", false, 250);
        LabVesselItem.addLiquid(vessel, "water", 443);
        check(h, !BottleQuantities.pour(source, vessel, 25) && BottleCodes.volumeOf(source) == 250, "full target atomicity");
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var stock=new ItemStack(ModItems.DROPPER_BOTTLE.get());
        BottleCodes.setLiquid(stock,"water",true,100);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stock);
        stock.getItem().use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        check(h,BottleCodes.volumeOf(stock)==75,"opening dropper debits 25");
        check(h, stock.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA)
                .strings().contains("open_dropper_bottle_water"), "removed stopper renders an open bottle");
        ItemStack stopper=ItemStack.EMPTY;
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            var found=player.getInventory().getItem(i);
            if(DropperHelper.isStopper(found)){stopper=found.copy();player.getInventory().setItem(i,ItemStack.EMPTY);break;}
        }
        check(h,DropperHelper.getMl(stopper)==25,"stopper contains actual sample");
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,stopper);
        stock.getItem().use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        check(h,BottleCodes.volumeOf(stock)==100&&stopper.isEmpty()&&BottleCodes.isSealed(stock),"return stopper conserves total");
        check(h, stock.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA)
                .strings().contains("dropper_bottle_water"), "returned stopper restores assembled bottle");
        h.succeed();
    }

    public static void solid(GameTestHelper h) {
        var bottle = new ItemStack(ModItems.SOLID_JAR.get());
        BottleCodes.setSolid(bottle, "sodium_chloride", false);
        BottleCodes.setSolidGrams(bottle, 7);
        var tool = new ItemStack(ModItems.SPATULA.get());
        check(h, BottleQuantities.takeSolid(bottle, tool), "solid scoop");
        check(h, SolidToolItem.heldGrams(tool) == 5 && BottleCodes.solidGrams(bottle) == 2, "solid conservation");
        SolidToolItem.clear(tool);
        check(h, BottleQuantities.takeSolid(bottle, tool), "partial scoop");
        check(h, SolidToolItem.heldGrams(tool) == 2 && BottleCodes.solidGrams(bottle) == 0, "last solid conservation");
        check(h, BottleQuantities.refill(bottle, tool), "return solid");
        check(h, BottleCodes.solidGrams(bottle) == 2 && SolidToolItem.isEmpty(tool), "solid refill conservation");
        h.succeed();
    }
}
