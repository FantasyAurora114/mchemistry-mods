package com.example.chemistry.item;

import java.util.function.Supplier;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * An open jar of nitric oxide (NO): it oxidises to NO2 the moment it ticks,
 * so any open NO jar instantly becomes an open NO2 jar.
 */
public class NitricOxideJarItem extends Item {

    private final Supplier<Item> nitrogenDioxideJar;

    public NitricOxideJarItem(Properties properties, Supplier<Item> nitrogenDioxideJar) {
        super(properties);
        this.nitrogenDioxideJar = nitrogenDioxideJar;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (entity instanceof Player player) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i) == stack) {
                    player.getInventory().setItem(i, new ItemStack(nitrogenDioxideJar.get()));
                    return;
                }
            }
        }
    }
}
