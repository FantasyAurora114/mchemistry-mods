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
 * pipes (and a future Mekanism) fill and drain our containers. The unified
 * bottles store the liquid id + amount in CUSTOM_DATA.
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
        return BottleCodes.volumeOf(stack);
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
            return ItemResource.of(ModItems.liquidBucketItem(liquidId));
        }

        if (newAmount <= 0) {
            if (DropperHelper.isDropper(stack)) {
                DropperHelper.clear(stack);
                return ItemResource.of(stack);
            }
            if (BottleCodes.isLiquidBottle(stack)) {
                BottleCodes.setLiquid(stack, null, false, 0);
                BottleCodes.refreshModel(stack);
                return ItemResource.of(stack);
            }
            if (BottleCodes.isDropperBottle(stack)) {
                BottleCodes.setLiquid(stack, null, true, 0);
                BottleCodes.refreshModel(stack);
                return ItemResource.of(stack);
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
        if (BottleCodes.isLiquidBottle(stack)) {
            ItemStack copy = stack.copy();
            BottleCodes.setLiquid(copy, liquidId, true,
                    Math.min(newAmount, ChemUnits.LIQUID_BOTTLE_VOLUME));
            BottleCodes.refreshModel(copy);
            return ItemResource.of(copy);
        }
        if (BottleCodes.isDropperBottle(stack)) {
            ItemStack copy = stack.copy();
            BottleCodes.setLiquid(copy, liquidId, true,
                    Math.min(newAmount, ChemUnits.DROPPER_BOTTLE_VOLUME));
            BottleCodes.refreshModel(copy);
            return ItemResource.of(copy);
        }
        return ItemResource.EMPTY;
    }
}
