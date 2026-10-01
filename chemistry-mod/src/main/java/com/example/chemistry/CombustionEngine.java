package com.example.chemistry;

import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.data.Combustion;
import com.example.chemistry.data.SubstanceVariants;
import com.example.chemistry.item.CombustionSpoonItem;
import com.example.chemistry.item.LabVesselItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/**
 * Burns the contents of a lit combustion spoon gradually with oxygen (air or
 * oxygen jar). ~0.4 g of fuel per second, so a full spoon burns for ~25 s.
 */
public final class CombustionEngine {

    private static final double BURN_RATE = 0.02;
    private static final String KEY_LIT = "chem_lit";
    private static final String KEY_IN_BOTTLE = "chem_in_bottle";
    private static final String KEY_FUEL = "chem_burn_fuel";

    public static boolean isLit(ItemStack stack) {
        return tag(stack).getIntOr(KEY_LIT, 0) == 1;
    }

    public static void setLit(ItemStack stack, boolean lit) {
        CompoundTag tag = tag(stack);
        if (lit) {
            tag.putInt(KEY_LIT, 1);
        } else {
            tag.remove(KEY_LIT);
            tag.remove(KEY_FUEL);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isInBottle(ItemStack stack) {
        return tag(stack).getIntOr(KEY_IN_BOTTLE, 0) == 1;
    }

    public static void setInBottle(ItemStack stack, boolean in) {
        CompoundTag tag = tag(stack);
        if (in) {
            tag.putInt(KEY_IN_BOTTLE, 1);
        } else {
            tag.remove(KEY_IN_BOTTLE);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void tick(ItemStack stack, Player player) {
        if (!(stack.getItem() instanceof CombustionSpoonItem) || !isLit(stack)) {
            return;
        }
        LabVesselItem.Entry fuel = findFuel(stack);
        if (fuel == null) {
            setLit(stack, false);
            player.displayClientMessage(Component.translatable("mchemistry.spoon.extinguish"), true);
            return;
        }
        String canonical = SubstanceVariants.canonicalOf(fuel.id());
        Combustion.Burn burn = Combustion.byFuel(canonical);
        if (burn == null) {
            return;
        }
        CompoundTag tag = tag(stack);
        if (!tag.getStringOr(KEY_FUEL, "").equals(canonical)) {
            tag.putString(KEY_FUEL, canonical);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            String prose = isInBottle(stack) ? burn.oxygenMessage() : burn.airMessage();
            ExperimentFeedback.send(player,
                    Component.translatable("mchemistry.combustion",
                            Component.literal(burn.equation()), Component.literal(prose)));
        }
        // 持续燃烧：燃烧中的燃烧匙在玩家前方不断冒出火焰与烟。
        if (player.level() instanceof ServerLevel server) {
            Vec3 pos = player.getEyePosition().add(player.getLookAngle().scale(0.5));
            if (player.tickCount % 2 == 0) {
                server.sendParticles(ParticleTypes.FLAME, pos.x, pos.y - 0.1, pos.z,
                        1, 0.04, 0.04, 0.04, 0.0);
            }
            if (player.tickCount % 8 == 0) {
                server.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z,
                        1, 0.05, 0.05, 0.05, 0.01);
            }
        }
        double consumed = Math.min(BURN_RATE, fuel.amount());
        LabVesselItem.consumeMass(stack, fuel.type(), fuel.id(), consumed);
        if (!burn.vent()) {
            double fuelMolar = ChemicalInfoProvider.molarMassOf("solid_" + canonical);
            double productMolar = ChemicalInfoProvider.molarMassOf("solid_" + burn.product());
            if (fuelMolar > 0 && productMolar > 0) {
                LabVesselItem.addMass(stack, "solid", burn.product(),
                        consumed * productMolar / fuelMolar);
            }
        }
    }

    private static LabVesselItem.Entry findFuel(ItemStack stack) {
        for (LabVesselItem.Entry entry : LabVesselItem.getContents(stack)) {
            String canonical = SubstanceVariants.canonicalOf(entry.id());
            if (Combustion.byFuel(canonical) != null) {
                return entry;
            }
        }
        return null;
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private CombustionEngine() {
    }
}
