package com.example.chemistry.transfer;

import com.example.chemistry.item.DropperHelper;
import com.example.chemistry.registry.ModFluids;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.storage.ChemUnits;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Item-level fluid handler for reagent bottles and droppers: lets modern fluid
 * pipes (and a future Mekanism) fill and drain our containers. The handler
 * swaps the item variant (sealed/open/empty) and stores partial volumes in NBT.
 */
public class BottleFluidResourceHandler extends ItemAccessResourceHandler<FluidResource> {

    public BottleFluidResourceHandler(ItemAccess itemAccess) {
        super(itemAccess, 1);
    }

    private static ItemStack stackOf(ItemResource resource) {
        return resource.toStack(1);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        ItemStack stack = stackOf(accessResource);
        String liquidId = BottleCodes.bucketIdOf(stack);
        if (liquidId == null) {
            liquidId = BottleCodes.liquidIdOf(stack);
        }
        if (liquidId == null && DropperHelper.isDropper(stack)) {
            liquidId = DropperHelper.getLiquid(stack);
        }
        if (liquidId == null) {
            return FluidResource.EMPTY;
        }
        var fluid = ModFluids.liquidFluid(liquidId);
        return fluid == null ? FluidResource.EMPTY : FluidResource.of(fluid);
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        ItemStack stack = stackOf(accessResource);
        if (DropperHelper.isDropper(stack)) {
            return DropperHelper.getMl(stack);
        }
        return BottleCodes.liquidVolumeOf(stack);
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        ItemStack stack = stackOf(this.itemAccess.getResource());
        if (DropperHelper.isDropper(stack)) {
            return DropperHelper.CAPACITY;
        }
        return BottleCodes.bottleCapacityOf(stack);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return !resource.isEmpty() && ModFluids.liquidIdFor(resource.getFluid()) != null;
    }

    @Override
    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        ItemStack stack = stackOf(accessResource);
        String path = BottleCodes.pathOf(stack);
        boolean isBucket = path.equals("bucket") || path.endsWith("_bucket") || path.equals("water_bucket");

        if (isBucket) {
            if (newAmount <= 0) {
                return ItemResource.of(net.minecraft.world.item.Items.BUCKET);
            }
            String liquidId = ModFluids.liquidIdFor(newResource.getFluid());
            if (liquidId == null || newAmount != ChemUnits.BUCKET_VOLUME) {
                return ItemResource.EMPTY;
            }
            return ItemResource.of(ModItems.liquidBucket(liquidId));
        }

        if (newAmount <= 0) {
            if (DropperHelper.isDropper(stack)) {
                DropperHelper.clear(stack);
                return ItemResource.of(stack);
            }
            if (path.startsWith("liquid_") || path.startsWith("open_liquid_")) {
                return ItemResource.of(ModItems.EMPTY_NARROW_BOTTLE.get());
            }
            if (path.startsWith("dropper_bottle_")) {
                return ItemResource.of(ModItems.EMPTY_DROPPER_BOTTLE.get());
            }
            return ItemResource.EMPTY;
        }

        String liquidId = ModFluids.liquidIdFor(newResource.getFluid());
        if (liquidId == null) {
            return ItemResource.EMPTY;
        }
        if (DropperHelper.isDropper(stack)) {
            DropperHelper.fill(stack, liquidId, newAmount);
            return ItemResource.of(stack);
        }
        if (path.equals("empty_narrow_bottle")) {
            ItemStack filled = new ItemStack(ModItems.liquidItem(liquidId));
            if (newAmount < ChemUnits.LIQUID_BOTTLE_VOLUME) {
                BottleCodes.setVolume(filled, newAmount);
            }
            return ItemResource.of(filled);
        }
        if (path.equals("empty_dropper_bottle")) {
            ItemStack filled = new ItemStack(ModItems.dropperBottle(liquidId));
            if (newAmount < ChemUnits.DROPPER_BOTTLE_VOLUME) {
                BottleCodes.setVolume(filled, newAmount);
            }
            return ItemResource.of(filled);
        }
        if (path.startsWith("liquid_") || path.startsWith("open_liquid_")
                || path.startsWith("dropper_bottle_")) {
            // Already the correct variant; persist a partial amount.
            ItemStack copy = stack.copy();
            if (newAmount < BottleCodes.bottleCapacityOf(stack)) {
                BottleCodes.setVolume(copy, newAmount);
            } else {
                copy.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            }
            return ItemResource.of(copy);
        }
        return ItemResource.EMPTY;
    }
}
