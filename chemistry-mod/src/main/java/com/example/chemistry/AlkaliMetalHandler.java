package com.example.chemistry;

import java.util.Set;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Alkali metal ingots (Li / Na / K) react violently with water:
 * drop a large stack into water and after a short bubbling delay it explodes.
 */
@EventBusSubscriber(modid = ChemistryMod.MODID)
public class AlkaliMetalHandler {

    private static Set<Item> alkaliIngots;

    /** Minimum ingots in a single dropped stack to trigger the reaction. */
    private static final int MIN_INGOTS = 16;

    /** Ticks the stack must sit in water before exploding. */
    private static final int TICKS_TO_EXPLODE = 40;

    private static final String WATER_TICKS_TAG = "mchemistry_water_ticks";

    private AlkaliMetalHandler() {
    }

    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }
        Level level = itemEntity.level();
        if (level.isClientSide() || !itemEntity.isInWater()) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (stack.getCount() < MIN_INGOTS || !alkaliIngots().contains(stack.getItem())) {
            return;
        }

        int ticks = itemEntity.getPersistentData().getInt(WATER_TICKS_TAG).orElse(0) + 1;
        if (ticks >= TICKS_TO_EXPLODE) {
            float power = Math.min(6.0F, 2.0F + stack.getCount() / 32.0F);
            level.explode(null, itemEntity.getX(), itemEntity.getY() + 0.5D, itemEntity.getZ(),
                    power, Level.ExplosionInteraction.BLOCK);
            itemEntity.discard();
        } else {
            itemEntity.getPersistentData().putInt(WATER_TICKS_TAG, ticks);
            if (level instanceof ServerLevel serverLevel && ticks % 10 == 0) {
                serverLevel.sendParticles(ParticleTypes.BUBBLE,
                        itemEntity.getX(), itemEntity.getY() + 0.2D, itemEntity.getZ(),
                        12, 0.3D, 0.3D, 0.3D, 0.02D);
            }
        }
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, path));
    }

    private static Set<Item> alkaliIngots() {
        if (alkaliIngots == null) {
            alkaliIngots = Set.of(
                    item("element_lithium_ingot"),
                    item("element_sodium_ingot"),
                    item("element_potassium_ingot"));
        }
        return alkaliIngots;
    }
}
