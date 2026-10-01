package com.example.chemistry.item;

import com.example.chemistry.block.GasApplianceBlock;
import com.example.chemistry.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Gas volumes use the existing 25 C reference, rather than the bottle's physical volume. */
public final class GasCylinderItem extends BlockItem {
    private final String gas;
    private final int capacity;

    public GasCylinderItem(GasApplianceBlock block, Properties properties, String gas) {
        super(block, properties);
        this.gas = gas;
        this.capacity = block.kind() == 2 ? 100000 : 10000;
    }

    @Override public net.minecraft.network.chat.Component getName(ItemStack stack) {
        String id = "gas_cylinder_" + (gas(stack).isEmpty() ? "" : gas(stack) + "_") + (capacity == 100000 ? "tall" : "small");
        return net.minecraft.network.chat.Component.translatable("item.mchemistry." + id);
    }
    public String gas(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getStringOr("cylinder_gas", gas);
    }

    public int remaining(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getIntOr("cylinder_ml", gas.isEmpty() ? 0 : capacity), 0, capacity);
    }

    public static ItemStack filled(boolean tall, String gas, int ml) {
        String id = "gas_cylinder_" + (gas.isEmpty() ? "" : gas + "_") + (tall ? "tall" : "small");
        ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry", id)));
        CompoundTag tag = new CompoundTag();
        tag.putString("cylinder_gas", gas);
        tag.putInt("cylinder_ml", Math.clamp(ml, 0, tall ? 100000 : 10000));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }
}
