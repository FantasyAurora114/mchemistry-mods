package com.example.chemistry;

import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.data.Combustion;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.data.Solutions;
import com.example.chemistry.data.SubstanceVariants;
import com.example.chemistry.item.CombustionSpoonItem;
import com.example.chemistry.item.LabVesselItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Thermodynamics + phase changes + reactions for placed reaction vessels
 * (圆底烧瓶 / 锥形瓶 / 坩埚 / 蒸发皿). A lit heat source ramps the vessel's
 * temperature; liquids boil above their boiling point — either evaporating
 * away or distilling into an attached receiver flask; heat-triggered
 * reactions run via the reaction engine. Boiling vessels emit bubbles/steam.
 */
public final class VesselHeating {

    public enum Outcome {
        NONE, POPPED, CRACKED
    }

    public static final int POP_PRESSURE = 100;
    private static final int CRACK_TEMP = 700;
    private static final double AIR_BURN_RATE = 0.02;
    private static final String KEY_AIR_FUEL = "chem_air_fuel";

    /** Ramp the vessel temperature toward the heat source temperature. */
    public static void heat(ItemStack vessel, double target) {
        if (vessel.isEmpty() || !(vessel.getItem() instanceof LabVesselItem)) {
            return;
        }
        double current = TemperatureSystem.getTemp(vessel);
        double next = current + (target - current) * 0.08;
        if (Math.abs(next - current) < 0.5) {
            next = target;
        }
        TemperatureSystem.setTemp(vessel, next);
    }

    public static void cool(ItemStack vessel) {
        heat(vessel, TemperatureSystem.ROOM_TEMP);
    }

    /** One tick of a heated placed vessel. */
    public static Outcome tick(ItemStack vessel, Level level, BlockPos pos,
            ItemStack distillateTarget, boolean hasCondenser, Player player) {
        if (vessel.isEmpty() || !(vessel.getItem() instanceof LabVesselItem)) {
            return Outcome.NONE;
        }
        double temp = TemperatureSystem.getTemp(vessel);
        ReactionEngine.checkAndStart(vessel, player);
        Reactions.Reaction completed = ReactionEngine.tick(vessel, player);
        if (completed != null) {
            ReactionPhenomena.spawn(level,
                    new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                    ReactionPhenomena.detect(completed, vessel));
            GasFlowEngine.enqueue(level, pos, vessel, completed);
        }
        // NB-style gas flow: queue gas first, then pump it along the rubber
        // tube at a visible speed (bubbles run along the tube, the water level
        // in a 排水法 trough drops live, cutting the tube stops the flow).
        GasFlowEngine.pump(level, pos, vessel);
        combustInAir(vessel, level, pos, player);

        // Pressure: a sealed vessel heated above boiling pressurises until the
        // stopper pops off; extreme heat cracks the vessel instead.
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean sealed = tag.getBooleanOr("chem_sealed", false);
        int pressure = tag.getIntOr("chem_pressure", 0);
        Outcome outcome = Outcome.NONE;
        if (sealed) {
            if (temp > CRACK_TEMP) {
                outcome = Outcome.CRACKED;
            } else if (temp > 100) {
                pressure += 2;
                if (pressure >= POP_PRESSURE) {
                    pressure = 0;
                    tag.remove("chem_sealed");
                    tag.remove("chem_stopper_holes");
                    outcome = Outcome.POPPED;
                }
            }
        } else if (pressure > 0) {
            pressure -= 1;
        }
        tag.putInt("chem_pressure", Math.max(0, pressure));
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        boolean boiling = false;
        for (LabVesselItem.Entry entry : LabVesselItem.getContents(vessel)) {
            if (entry.type().equals("liquid")) {
                String solute = Solutions.soluteOf(entry.id());
                if (solute != null) {
                    // Solutions lose their water above 100 C; solute crystallises.
                    if (temp > 100) {
                        if (entry.amount() <= 0.05) {
                            LabVesselItem.consumeMass(vessel, "liquid", entry.id(), entry.amount());
                            LabVesselItem.addMass(vessel, "solid", solute, 1.0);
                        } else {
                            LabVesselItem.consumeMass(vessel, "liquid", entry.id(), 0.05);
                        }
                        boiling = true;
                    }
                    continue;
                }
                double bp = ChemicalInfoProvider.boilingPointOf("liquid_" + entry.id());
                if (temp > bp) {
                    double transfer = Math.min(entry.amount(), 0.2);
                    if (hasCondenser && distillateTarget != null && !distillateTarget.isEmpty()
                            && distillateTarget.getItem() instanceof LabVesselItem
                            && LabVesselItem.addMass(distillateTarget, "liquid", entry.id(), transfer)) {
                        LabVesselItem.consumeMass(vessel, "liquid", entry.id(), transfer);
                    } else {
                        LabVesselItem.consumeMass(vessel, "liquid", entry.id(), transfer);
                    }
                    boiling = true;
                }
            } else if (entry.type().equals("solid")
                    && LabVesselItem.phaseChangeSolid(vessel, entry, temp, 0.05)) {
                boiling = true;
            }
        }
        if (boiling && level instanceof ServerLevel server) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5;
            server.sendParticles(ParticleTypes.BUBBLE, x, y, z, 2, 0.15, 0.05, 0.15, 0.01);
            server.sendParticles(ParticleTypes.CLOUD, x, y + 0.12, z, 1, 0.1, 0.05, 0.1, 0.01);
        }
        return outcome;
    }

    /** Open heated vessels burn combustible solids with oxygen from the air
     *  (坩埚/蒸发皿灼烧): each fuel catches fire at its own ignition point,
     *  producing the oxide (or just burning away) with flame + smoke. */
    private static void combustInAir(ItemStack vessel, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || isSealed(vessel)) {
            return;
        }
        if (vessel.getItem() instanceof CombustionSpoonItem) {
            // The spoon burns through its own lit mechanism instead.
            return;
        }
        double temp = TemperatureSystem.getTemp(vessel);
        if (temp < 30) {
            return;
        }
        LabVesselItem.Entry fuel = null;
        for (LabVesselItem.Entry entry : LabVesselItem.getContents(vessel)) {
            if (entry.type().equals("solid")
                    && Combustion.byFuel(SubstanceVariants.canonicalOf(entry.id())) != null) {
                fuel = entry;
                break;
            }
        }
        if (fuel == null) {
            return;
        }
        String canonical = SubstanceVariants.canonicalOf(fuel.id());
        Combustion.Burn burn = Combustion.byFuel(canonical);
        if (burn == null || temp < burn.ignition()) {
            return;
        }
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.getStringOr(KEY_AIR_FUEL, "").equals(canonical)) {
            tag.putString(KEY_AIR_FUEL, canonical);
            vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            if (player != null) {
                player.displayClientMessage(Component.literal(burn.airMessage()), true);
            }
        }
        double consumed = Math.min(AIR_BURN_RATE, fuel.amount());
        LabVesselItem.consumeMass(vessel, fuel.type(), fuel.id(), consumed);
        if (!burn.vent()) {
            double fuelMolar = ChemicalInfoProvider.molarMassOf("solid_" + canonical);
            double productMolar = ChemicalInfoProvider.molarMassOf("solid_" + burn.product());
            if (fuelMolar > 0 && productMolar > 0) {
                LabVesselItem.addMass(vessel, "solid", burn.product(),
                        consumed * productMolar / fuelMolar);
            }
        }
        if (level instanceof ServerLevel server) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.85;
            double z = pos.getZ() + 0.5;
            server.sendParticles(ParticleTypes.FLAME, x, y, z, 2, 0.12, 0.08, 0.12, 0.01);
            server.sendParticles(ParticleTypes.SMOKE, x, y + 0.15, z, 1, 0.1, 0.05, 0.1, 0.01);
        }
    }

    /** Seal a vessel with a rubber stopper (holes 1 or 2). */
    public static void seal(ItemStack vessel, int holes) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean("chem_sealed", true);
        tag.putInt("chem_stopper_holes", holes);
        tag.putInt("chem_pressure", 0);
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isSealed(ItemStack vessel) {
        return vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getBooleanOr("chem_sealed", false);
    }

    public static int getStopperHoles(ItemStack vessel) {
        return vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getIntOr("chem_stopper_holes", 0);
    }

    /** Passivation (钝化): concentrated oxidizing acids make Fe/Al inert. */
    public static final double PASSIVATION_BREAK_TEMP = 150.0;
    private static final String KEY_PASSIVATED = "chem_passivated";

    public static void markPassivated(ItemStack vessel, String canonicalId) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = tag.getListOrEmpty(KEY_PASSIVATED);
        for (Tag t : list) {
            if (t instanceof StringTag st && st.value().equals(canonicalId)) {
                return;
            }
        }
        list.add(StringTag.valueOf(canonicalId));
        tag.put(KEY_PASSIVATED, list);
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isPassivated(ItemStack vessel, String canonicalId) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        for (Tag t : tag.getListOrEmpty(KEY_PASSIVATED)) {
            if (t instanceof StringTag st && st.value().equals(canonicalId)) {
                return true;
            }
        }
        return false;
    }

    public static void clearPassivated(ItemStack vessel) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(KEY_PASSIVATED);
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Clear the seal (stopper popped off). */
    public static void unseal(ItemStack vessel) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove("chem_sealed");
        tag.remove("chem_stopper_holes");
        tag.putInt("chem_pressure", 0);
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private VesselHeating() {
    }
}
