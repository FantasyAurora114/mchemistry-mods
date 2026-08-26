package com.example.chemistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.blockentity.GasCollectingBottleBlockEntity;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.blockentity.PlacedVesselBlockEntity;
import com.example.chemistry.blockentity.WaterTroughBlockEntity;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.entity.RubberTubeEntity.Anchor;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Gas flow engine (NB 风格): when a reaction completes, its produced gases are
 * queued inside the reaction vessel ("待输送"), then pumped tick by tick along
 * the rubber tube at a fixed flow rate. The tube has an internal volume, so gas
 * takes a few ticks to travel through it; bubbles run along the tube while it
 * flows. Cutting the tube discards whatever is in transit and the remaining
 * queue vents from the vessel mouth. Without a connected tube the gas simply
 * bubbles away at the vessel. Collected gas always lands in a gas bottle or an
 * inverted bottle in the water trough (排水法), displacing the water live.
 */
public final class GasFlowEngine {

    /** mL pushed into / out of a tube per tick (20 ticks/s -> 80 mL/s). */
    private static final int FLOW_RATE_ML = 4;
    /** mL escaping per tick when no tube is connected. */
    private static final int VENT_RATE_ML = 4;
    /** Tube internal volume per block of length (mL). */
    private static final double ML_PER_BLOCK = 1.0;
    /** Total gas produced by one completed reaction (mL per gas sign ↑). */
    private static final int ML_PER_COMPLETION = 100;

    private static final String KEY_PENDING = "chem_gas_pending";

    /** Chinese gas name in reaction displays -> GasJar id ("" = uncollectable). */
    private static final Map<String, String> GAS_NAME_TO_ID = Map.ofEntries(
            Map.entry("二氧化碳", "carbon_dioxide"),
            Map.entry("氢气", "hydrogen"),
            Map.entry("氧气", "oxygen"),
            Map.entry("氮气", "nitrogen"),
            Map.entry("一氧化氮", "nitric_oxide"),
            Map.entry("二氧化硫", "sulfur_dioxide"),
            Map.entry("氨气", "ammonia"),
            Map.entry("氯气", "chlorine"),
            Map.entry("二氧化氮", "nitrogen_dioxide"),
            Map.entry("一氧化碳", "carbon_monoxide"),
            Map.entry("氯化氢", "hydrogen_chloride"),
            Map.entry("硫化氢", "hydrogen_sulfide"),
            Map.entry("氦气", "helium"),
            Map.entry("氖气", "neon"),
            Map.entry("氩气", "argon"),
            Map.entry("氪气", "krypton"),
            Map.entry("氙气", "xenon"),
            Map.entry("三氧化硫", "sulfur_trioxide"));

    public record GasOut(String id, int ml) {
    }

    /** One queued gas waiting to leave the vessel. */
    public record PendingGas(String id, int ml, double purity) {
    }

    /** Gases produced by a completed reaction, parsed from its display text. */
    public static List<GasOut> producedGases(Reactions.Reaction reaction) {
        List<GasOut> out = new ArrayList<>();
        for (Map.Entry<String, String> e : GAS_NAME_TO_ID.entrySet()) {
            int count = countOccurrences(reaction.display(), e.getKey() + "↑");
            if (count > 0) {
                out.add(new GasOut(e.getValue(), ML_PER_COMPLETION * count));
            }
        }
        return out;
    }

    /** A reaction completed: queue its produced gas inside the vessel instead
     *  of teleporting it into the collector. */
    public static void enqueue(Level level, BlockPos vesselPos, ItemStack vessel,
            Reactions.Reaction completed) {
        if (level.isClientSide()) {
            return;
        }
        for (GasOut g : producedGases(completed)) {
            addPending(vessel, g.id(), g.ml(), PurityHelper.getPurity(vessel));
        }
    }

    /** Advance the pending gas one tick: push it into the connected tube and
     *  pull the same amount out of the far end into the collector. */
    public static void pump(Level level, BlockPos vesselPos, ItemStack vessel) {
        if (level.isClientSide() || vessel.isEmpty()) {
            return;
        }
        List<PendingGas> pending = readPending(vessel);
        Anchor head = vesselOutputHead(level, vesselPos);
        List<RubberTubeEntity> tubes = head != null
                ? RubberTubeItem.findTubesAt(level, head)
                : List.of();
        // Drain whatever is still travelling inside the connected tubes even
        // when the vessel's queue is empty (the last few mL must not stick).
        for (RubberTubeEntity tube : tubes) {
            if (!tube.hasTransit()) {
                continue;
            }
            String id = tube.getTransitGas();
            double transitPurity = tube.getTransitPurity();
            int out = tube.takeTransit(FLOW_RATE_ML);
            if (out > 0) {
                Anchor far = otherAnchor(tube, head);
                int accepted = far != null ? deliverTo(level, far, id, out,
                        transitPurity) : 0;
                if (accepted < out) {
                    ventFrom(level, vesselPos, out - accepted);
                }
            }
        }
        if (pending.isEmpty()) {
            return;
        }
        List<PendingGas> remaining = new ArrayList<>();
        for (PendingGas gas : pending) {
            int left = gas.ml();
            if (tubes.isEmpty()) {
                int vented = Math.min(VENT_RATE_ML, left);
                left -= vented;
                ventFrom(level, vesselPos, vented);
            } else {
                for (RubberTubeEntity tube : tubes) {
                    if (left <= 0) {
                        break;
                    }
                    left -= pumpThroughTube(level, vesselPos, tube, head, gas, left);
                }
            }
            if (left > 0) {
                remaining.add(new PendingGas(gas.id(), left, gas.purity()));
            }
        }
        writePending(vessel, remaining);
    }

    /** One tube step: gas that reached the far end this tick is delivered (or
     *  vents), then up to FLOW_RATE mL enters the source end. Returns the mL
     *  taken from the vessel this tick. */
    private static int pumpThroughTube(Level level, BlockPos vesselPos, RubberTubeEntity tube,
            Anchor head, PendingGas gas, int available) {
        // 1) Pull whatever reached the far end and deliver it.
        // Capture the id/purity BEFORE takeTransit clears them when the tube
        // empties completely this tick.
        String transitId = tube.getTransitGas();
        double transitPurity = tube.getTransitPurity();
        int out = tube.takeTransit(FLOW_RATE_ML);
        if (out > 0) {
            Anchor far = otherAnchor(tube, head);
            int accepted = far != null ? deliverTo(level, far, transitId, out,
                    transitPurity) : 0;
            if (accepted < out) {
                ventFrom(level, vesselPos, out - accepted);
            }
        }
        // 2) Push new gas into the tube (up to its remaining internal volume).
        int capacity = tubeCapacity(level, tube);
        int into = Math.min(FLOW_RATE_ML, Math.min(available, capacity - tube.getTransitMl()));
        if (into > 0) {
            int added = tube.addTransit(gas.id(), into, gas.purity());
            if (added > 0) {
                spawnFlowParticles(level, tube);
                return added;
            }
        }
        return 0;
    }

    /** Deliver mL to a gas bottle or an inverted bottle in a water trough.
     *  Returns the accepted amount. */
    private static int deliverTo(Level level, Anchor far, String id, int ml, double purity) {
        if (far == null || far.kind() != Anchor.KIND_NOZZLE || far.pos() == null) {
            return 0;
        }
        BlockPos pos = far.pos();
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.GAS_COLLECTING_BOTTLE.get())
                && state.getValue(GasCollectingBottleBlock.HAS_NOZZLE)
                && level.getBlockEntity(pos) instanceof GasCollectingBottleBlockEntity be) {
            return be.addGas(id, ml, purity);
        }
        if (state.is(ModBlocks.WATER_TROUGH.get())
                && level.getBlockEntity(pos) instanceof WaterTroughBlockEntity wbe) {
            return wbe.addGas(id, ml, purity);
        }
        return 0;
    }

    /** The glass-tube head in the vessel's stopper (its gas output port).
     *  Prefers a head that already has a rubber tube connected; otherwise the
     *  first glass tube is used (gas simply vents there without a tube). */
    private static Anchor vesselOutputHead(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        ItemStack attached1 = null;
        ItemStack attached2 = null;
        if (be instanceof IronStandBlockEntity ibe) {
            attached1 = ibe.getAttached1();
            attached2 = ibe.getAttached2();
        } else if (be instanceof PlacedVesselBlockEntity pbe) {
            attached1 = pbe.getAttached1();
            attached2 = pbe.getAttached2();
        }
        boolean aGlass = attached1 != null && isGlassTube(attached1);
        boolean bGlass = attached2 != null && isGlassTube(attached2);
        if (aGlass && RubberTubeItem.hasTubeAt(level, Anchor.stand(pos.immutable(), 1))) {
            return Anchor.stand(pos.immutable(), 1);
        }
        if (bGlass && RubberTubeItem.hasTubeAt(level, Anchor.stand(pos.immutable(), 2))) {
            return Anchor.stand(pos.immutable(), 2);
        }
        if (aGlass) {
            return Anchor.stand(pos.immutable(), 1);
        }
        if (bGlass) {
            return Anchor.stand(pos.immutable(), 2);
        }
        return null;
    }

    private static boolean isGlassTube(ItemStack stack) {
        return stack.is(com.example.chemistry.registry.ModItems.STRAIGHT_GLASS_TUBE.get())
                || stack.is(com.example.chemistry.registry.ModItems.RIGHT_ANGLE_GLASS_TUBE.get())
                || stack.is(com.example.chemistry.registry.ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get());
    }

    private static Anchor otherAnchor(RubberTubeEntity tube, Anchor head) {
        Anchor a = tube.getAnchorA();
        Anchor b = tube.getAnchorB();
        if (a != null && a.equals(head)) {
            return b;
        }
        if (b != null && b.equals(head)) {
            return a;
        }
        return null;
    }

    /** Internal volume of the tube: one mL per block of sagging length. */
    private static int tubeCapacity(Level level, RubberTubeEntity tube) {
        Vec3 a = RubberTubeItem.anchorWorldPos(level, tube.getAnchorA());
        Vec3 b = RubberTubeItem.anchorWorldPos(level, tube.getAnchorB());
        if (a == null || b == null) {
            return 1;
        }
        return Math.max(1, (int) Math.round(a.distanceTo(b) * ML_PER_BLOCK));
    }

    /** Bubbles running from the source end to the far end while gas flows. */
    private static void spawnFlowParticles(Level level, RubberTubeEntity tube) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 a = RubberTubeItem.anchorWorldPos(level, tube.getAnchorA());
        Vec3 b = RubberTubeItem.anchorWorldPos(level, tube.getAnchorB());
        if (a == null || b == null) {
            return;
        }
        double len = a.distanceTo(b);
        if (len < 1.0e-3) {
            return;
        }
        double t = (server.getGameTime() * 0.05 + tube.getId() * 0.137) % 1.0;
        double sag = Math.min(1.8, len * 0.2);
        Vec3 p = a.lerp(b, t).subtract(0, sag * 4.0 * t * (1.0 - t), 0);
        server.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z, 1,
                0.02, 0.02, 0.02, 0.0);
    }

    /** Gas escaping into the air from the vessel mouth. */
    private static void ventFrom(Level level, BlockPos pos, int ml) {
        if (!(level instanceof ServerLevel server) || ml <= 0) {
            return;
        }
        server.sendParticles(ParticleTypes.BUBBLE,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                Math.min(3, 1 + ml / 2), 0.1, 0.05, 0.1, 0.01);
        if (server.getGameTime() % 10 == 0) {
            server.sendParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    1, 0.08, 0.03, 0.08, 0.01);
            server.playSound(null, pos, SoundEvents.BUBBLE_POP,
                    SoundSource.BLOCKS, 0.6F, 1.2F);
        }
    }

    /** A tube was cut / torn down: gas inside it escapes at the break point. */
    public static void rupture(Level level, RubberTubeEntity tube) {
        if (!(level instanceof ServerLevel server) || tube == null || !tube.hasTransit()) {
            return;
        }
        Vec3 p = tube.position();
        server.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z,
                6, 0.15, 0.15, 0.15, 0.02);
        server.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z,
                4, 0.12, 0.12, 0.12, 0.01);
        server.playSound(null, BlockPos.containing(p), SoundEvents.BUBBLE_POP,
                SoundSource.BLOCKS, 0.8F, 0.9F);
    }

    // ---- pending queue stored on the vessel ----

    private static void addPending(ItemStack vessel, String id, int ml, double purity) {
        List<PendingGas> list = readPending(vessel);
        for (PendingGas g : list) {
            if (g.id().equals(id)) {
                list.remove(g);
                list.add(new PendingGas(id, g.ml() + ml, Math.min(g.purity(), purity)));
                writePending(vessel, list);
                return;
            }
        }
        list.add(new PendingGas(id, ml, purity));
        writePending(vessel, list);
    }

    private static List<PendingGas> readPending(ItemStack vessel) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = tag.getListOrEmpty(KEY_PENDING);
        List<PendingGas> out = new ArrayList<>();
        for (Tag t : list) {
            if (t instanceof CompoundTag c) {
                String id = c.getStringOr("id", "");
                int ml = c.getIntOr("ml", 0);
                if (!id.isEmpty() && ml > 0) {
                    out.add(new PendingGas(id, ml, c.getDoubleOr("purity", 1.0)));
                }
            }
        }
        return out;
    }

    private static void writePending(ItemStack vessel, List<PendingGas> list) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (list.isEmpty()) {
            tag.remove(KEY_PENDING);
        } else {
            ListTag entries = new ListTag();
            for (PendingGas g : list) {
                CompoundTag c = new CompoundTag();
                c.putString("id", g.id());
                c.putInt("ml", g.ml());
                c.putDouble("purity", g.purity());
                entries.add(c);
            }
            tag.put(KEY_PENDING, entries);
        }
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(token, idx)) >= 0) {
            count++;
            idx += token.length();
        }
        return count;
    }

    private GasFlowEngine() {
    }
}
