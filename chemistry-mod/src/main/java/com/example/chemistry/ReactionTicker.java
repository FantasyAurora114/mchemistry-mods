package com.example.chemistry;

import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.TestTubeItem;
import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.data.Solutions;
import com.example.chemistry.item.CombustionSpoonItem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Advances pending reactions inside vessels the player carries (including the
 * offhand, which is not covered by Item#inventoryTick).
 * The ticker is an Observer over held vessels: it watches for pending reactions
 * every tick and fires their visible phenomena.
 */
@EventBusSubscriber(modid = ChemistryMod.MODID)
public class ReactionTicker {

    /** 调试修订标记（Observer 观察器实现）。 */
    private static final String TICKER_REV = "Observer-2026";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof LabVesselItem) {
                Reactions.Reaction completed = ReactionEngine.tick(stack, player);
                if (completed != null) {
                    ReactionEngine.applyReactionHeat(stack, completed);
                    ReactionPhenomena.spawn(player.level(),
                            player.getEyePosition().add(player.getLookAngle().scale(0.4)),
                            ReactionPhenomena.detect(completed, stack));
                }
                // 敞口容器里比空气轻的气体慢慢逸出（被空气取代）。
                VesselGasPhase.tickLeak(stack);
                // 物态变化：融化/凝固/蒸发/凝结/升华/凝华/溶解/结晶。
                PhaseSystem.tick(stack, TemperatureSystem.getTemp(stack));
            }
            if (stack.getItem() instanceof TestTubeItem) {
                TemperatureSystem.tick(stack, player, i);
                evaporate(stack, TemperatureSystem.getTemp(stack));
            }
            if (stack.getItem() instanceof CombustionSpoonItem) {
                CombustionEngine.tick(stack, player);
            }
        }
    }

    /** 掉进水里的散装物质：可溶的溶解，碱金属剧烈反应。 */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item)
                || item.level().isClientSide()
                || item.tickCount < 10
                || !item.isInWater()) {
            return;
        }
        var tag = item.getPersistentData();
        if (tag.getBoolean("chem_water_processed").orElse(false)) {
            return;
        }
        String path = BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).getPath();
        if (!path.startsWith("loose_")) {
            return;
        }
        String id = path.substring("loose_".length());
        tag.putBoolean("chem_water_processed", true);
        if (PhaseSystem.isSoluble(id)) {
            item.discard();
            if (item.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.BUBBLE,
                        item.getX(), item.getY(), item.getZ(), 12,
                        0.2, 0.2, 0.2, 0.02);
                server.playSound(null, item.blockPosition(), SoundEvents.BUBBLE_POP,
                        SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return;
        }
        // 碱金属入水爆炸。
        if (java.util.Set.of("sodium", "potassium", "lithium", "calcium", "barium").contains(id)) {
            item.discard();
            if (item.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.EXPLOSION,
                        item.getX(), item.getY(), item.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                server.sendParticles(ParticleTypes.FLAME,
                        item.getX(), item.getY(), item.getZ(), 10,
                        0.3, 0.3, 0.3, 0.03);
                server.playSound(null, item.blockPosition(),
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.9F, 1.3F);
                Player p = server.getNearestPlayer(item.getX(), item.getY(), item.getZ(),
                        12.0, false);
                if (p != null) {
                    p.displayClientMessage(
                            Component.translatable("mchemistry.water.react", id), true);
                }
            }
        }
    }

    /**
     * Phase changes at normal pressure: solutions lose their water slowly above
     * 100 C (the solute crystallises out); pure substances evaporate away once
     * the vessel exceeds their boiling point.
     */
    private static void evaporate(ItemStack stack, double temp) {
        if (!(stack.getItem() instanceof LabVesselItem)) {
            return;
        }
        for (LabVesselItem.Entry entry : LabVesselItem.getContents(stack)) {
            if (entry.type().equals("liquid")) {
                String solute = Solutions.soluteOf(entry.id());
                if (solute != null) {
                    if (temp > 100) {
                        if (entry.amount() <= 0.05) {
                            LabVesselItem.consumeMass(stack, "liquid", entry.id(), entry.amount());
                            LabVesselItem.addMass(stack, "solid", solute, 1.0);
                        } else {
                            LabVesselItem.consumeMass(stack, "liquid", entry.id(), 0.05);
                        }
                    }
                } else if (temp > ChemicalInfoProvider.boilingPointOf("liquid_" + entry.id())) {
                    LabVesselItem.consumeMass(stack, "liquid", entry.id(), entry.amount());
                }
            } else if (entry.type().equals("solid")
                    && LabVesselItem.phaseChangeSolid(stack, entry, temp, 0.05)) {
            }
        }
    }
}
