package com.example.chemistry;

import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.TestTubeItem;
import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.data.Solutions;
import com.example.chemistry.item.CombustionSpoonItem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Advances pending reactions inside vessels the player carries (including the
 * offhand, which is not covered by Item#inventoryTick).
 */
@EventBusSubscriber(modid = ChemistryMod.MODID)
public class ReactionTicker {

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
                    ReactionPhenomena.spawn(player.level(),
                            player.getEyePosition().add(player.getLookAngle().scale(0.4)),
                            ReactionPhenomena.detect(completed, stack));
                }
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
