package com.example.chemistry;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntConsumer;

import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.blockentity.GasCollectingBottleBlockEntity;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.blockentity.PlacedVesselBlockEntity;
import com.example.chemistry.blockentity.WaterTroughBlockEntity;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.entity.RubberTubeEntity.Port;
import com.example.chemistry.item.AlcoholLampLitItem;
import com.example.chemistry.item.GlassTubeItem;
import com.example.chemistry.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 玻璃导管点燃：当玩家一手拿火源（打火石/火焰弹/点燃的酒精灯）、另一手拿
 * 玻璃导管（作为导气出口）时，右键会点燃从导管口喷出的气体。
 * 气源沿橡胶管追溯：另一端可以是集气瓶、水槽（排水法集气瓶）或铁架台/
 * 落地容器里的气体发生器。每次点燃消耗少量气体并喷出火焰。
 */
public final class GlassTubeIgnition {

    /** 可燃气体：点燃时喷出火焰。 */
    private static final Set<String> FLAMMABLE = Set.of(
            "hydrogen", "carbon_monoxide", "hydrogen_sulfide", "ammonia",
            "methane", "ethane", "propane", "butane",
            "ethylene", "propylene", "butene",
            "acetylene", "propyne", "butyne",
            "cyanogen", "chloromethane");

    /** 爆鸣气：点燃时发出爆鸣声、喷出更猛的火舌。 */
    private static final Set<String> POP = Set.of(
            "hydrogen", "acetylene", "propyne", "butyne");

    /** 可燃气体燃烧方程式（点燃时显示）。 */
    private static final Map<String, String> EQUATION = Map.ofEntries(
            Map.entry("hydrogen", "2H₂ + O₂ →(点燃) 2H₂O"),
            Map.entry("carbon_monoxide", "2CO + O₂ →(点燃) 2CO₂"),
            Map.entry("hydrogen_sulfide", "2H₂S + 3O₂ →(点燃) 2SO₂ + 2H₂O"),
            Map.entry("ammonia", "4NH₃ + 3O₂ →(点燃) 2N₂ + 6H₂O"),
            Map.entry("methane", "CH₄ + 2O₂ →(点燃) CO₂ + 2H₂O"),
            Map.entry("ethane", "2C₂H₆ + 7O₂ →(点燃) 4CO₂ + 6H₂O"),
            Map.entry("propane", "C₃H₈ + 5O₂ →(点燃) 3CO₂ + 4H₂O"),
            Map.entry("butane", "2C₄H₁₀ + 13O₂ →(点燃) 8CO₂ + 10H₂O"),
            Map.entry("ethylene", "C₂H₄ + 3O₂ →(点燃) 2CO₂ + 2H₂O"),
            Map.entry("propylene", "2C₃H₆ + 9O₂ →(点燃) 6CO₂ + 6H₂O"),
            Map.entry("butene", "C₄H₈ + 6O₂ →(点燃) 4CO₂ + 4H₂O"),
            Map.entry("acetylene", "2C₂H₂ + 5O₂ →(点燃) 4CO₂ + 2H₂O"),
            Map.entry("propyne", "C₃H₄ + 4O₂ →(点燃) 3CO₂ + 2H₂O"),
            Map.entry("butyne", "2C₄H₆ + 11O₂ →(点燃) 8CO₂ + 6H₂O"),
            Map.entry("cyanogen", "(CN)₂ + 2O₂ →(点燃) 2CO₂ + N₂"),
            Map.entry("chloromethane", "2CH₃Cl + 3O₂ →(点燃) 2CO₂ + 2H₂O + 2HCl"));

    /** 每点燃一次消耗的气体量（mL）。 */
    private static final int BURN_ML = 5;

    private GlassTubeIgnition() {
    }

    public static boolean isFireSource(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE)
                || stack.getItem() instanceof AlcoholLampLitItem;
    }

    public static boolean isGlassTube(ItemStack stack) {
        return GlassTubeItem.isGlassTube(stack) || GlassTubeItem.isTubedGlassTube(stack);
    }

    /** 尝试点燃手持玻璃导管喷出的气体。返回 true 表示这次右键已被消耗
     *  （手里有玻璃导管 + 火源的组合，无论是否真的点着）。 */
    public static boolean tryIgnite(Level level, Player player) {
        if (level.isClientSide()) {
            return false;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean fireMain = isFireSource(main);
        boolean fireOff = isFireSource(off);
        boolean tubeMain = isGlassTube(main);
        boolean tubeOff = isGlassTube(off);
        if (!((fireMain && tubeOff) || (fireOff && tubeMain))) {
            return false;
        }
        IgniteTarget target = findTarget(level, player);
        if (target == null) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.nozzle.no_gas"), true);
            return true;
        }
        String gas = target.gasId();
        String name = ChemGoggleLines.gasName(gas);
        if (!FLAMMABLE.contains(gas)) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.nozzle.not_flammable", name), true);
            return true;
        }
        target.consumer().accept(BURN_ML);
        Vec3 tip = player.position().add(0, player.getEyeHeight() * 0.7, 0)
                .add(player.getViewVector(1.0F).scale(0.25));
        if (level instanceof ServerLevel server) {
            if (POP.contains(gas)) {
                server.sendParticles(ParticleTypes.FLAME, tip.x, tip.y, tip.z, 18,
                        0.15, 0.15, 0.15, 0.05);
                server.sendParticles(ParticleTypes.LARGE_SMOKE, tip.x, tip.y, tip.z, 6,
                        0.1, 0.1, 0.1, 0.02);
                server.playSound(null, player.blockPosition(),
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.8F, 1.6F);
                player.displayClientMessage(
                        combustionMessage(gas, "mchemistry.nozzle.pop", name), true);
            } else {
                server.sendParticles(ParticleTypes.FLAME, tip.x, tip.y, tip.z, 12,
                        0.12, 0.12, 0.12, 0.04);
                server.sendParticles(ParticleTypes.SMOKE, tip.x, tip.y, tip.z, 4,
                        0.1, 0.1, 0.1, 0.01);
                server.playSound(null, player.blockPosition(),
                        SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
                player.displayClientMessage(
                        combustionMessage(gas, "mchemistry.nozzle.ignite", name), true);
            }
        }
        return true;
    }

    /** "⚗ 方程式　现象" 一条消息。 */
    private static Component combustionMessage(String gas, String key, String name) {
        String equation = EQUATION.getOrDefault(gas, "");
        Component phenomenon = Component.translatable(key, name);
        if (equation.isEmpty()) {
            return phenomenon;
        }
        return Component.translatable("mchemistry.combustion",
                Component.literal(equation), phenomenon);
    }

    /** 在玩家锚定的橡胶管另一端找到可点燃的气体（管道内气体 + 气源）。 */
    private static IgniteTarget findTarget(Level level, Player player) {
        IgniteTarget best = null;
        for (RubberTubeEntity tube : level.getEntitiesOfClass(RubberTubeEntity.class,
                new AABB(player.blockPosition()).inflate(64.0))) {
            Port a = tube.getAnchorA();
            Port b = tube.getAnchorB();
            Port other = isPlayerAnchor(a, player.getUUID()) ? b
                    : isPlayerAnchor(b, player.getUUID()) ? a : null;
            if (other == null) {
                continue;
            }
            String transit = tube.hasTransit() ? tube.getTransitGas() : "";
            if (RubberTubeEntity.AIR.equals(transit)) {
                transit = "";
            }
            GasSource source = resolveSource(level, other);
            String sourceGas = source != null ? source.gasId() : "";
            String gas = FLAMMABLE.contains(sourceGas) ? sourceGas
                    : FLAMMABLE.contains(transit) ? transit
                    : !sourceGas.isEmpty() ? sourceGas
                    : transit;
            if (gas.isEmpty()) {
                continue;
            }
            final GasSource src = source;
            IgniteTarget target = new IgniteTarget(gas, ml -> {
                int left = ml;
                if (tube.hasTransit() && !RubberTubeEntity.AIR.equals(tube.getTransitGas())) {
                    left -= tube.takeTransit(left);
                }
                if (left > 0 && src != null) {
                    src.consumer().accept(left);
                }
            });
            if (best == null || (FLAMMABLE.contains(gas) && !FLAMMABLE.contains(best.gasId()))) {
                best = target;
            }
        }
        return best;
    }

    /** 橡胶管另一端对应的气源（集气瓶 / 水槽集气瓶 / 气体发生器）。 */
    private static GasSource resolveSource(Level level, Port anchor) {
        if (anchor == null || anchor.pos() == null) {
            return null;
        }
        BlockPos pos = anchor.pos();
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.GAS_COLLECTING_BOTTLE.get())
                && level.getBlockEntity(pos) instanceof GasCollectingBottleBlockEntity be) {
            String id = be.getGasId();
            if (id.isEmpty() || be.getFillMl() <= 0) {
                return null;
            }
            return new GasSource(id, ml -> be.removeGas(id, ml));
        }
        if (state.is(ModBlocks.WATER_TROUGH.get())
                && level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be) {
            String id = be.getGasId();
            if (id.isEmpty() || be.getFillMl() <= 0) {
                return null;
            }
            return new GasSource(id, ml -> be.removeGas(id, ml));
        }
        if (state.is(ModBlocks.IRON_STAND.get())
                && level.getBlockEntity(pos) instanceof IronStandBlockEntity be
                && !be.getVessel().isEmpty()) {
            ItemStack vessel = be.getVessel();
            String id = GasFlowEngine.dominantPendingGas(vessel);
            if (id.isEmpty()) {
                return null;
            }
            return new GasSource(id, ml -> {
                GasFlowEngine.consumePending(vessel, id, ml);
                be.setVessel(vessel);
            });
        }
        if (state.is(ModBlocks.PLACED_VESSEL.get())
                && level.getBlockEntity(pos) instanceof PlacedVesselBlockEntity be
                && !be.getVessel().isEmpty()) {
            ItemStack vessel = be.getVessel();
            String id = GasFlowEngine.dominantPendingGas(vessel);
            if (id.isEmpty()) {
                return null;
            }
            return new GasSource(id, ml -> {
                GasFlowEngine.consumePending(vessel, id, ml);
                be.setVessel(vessel);
            });
        }
        return null;
    }

    private static boolean isPlayerAnchor(Port anchor, UUID playerId) {
        return anchor != null && anchor.kind() == Port.KIND_ENTITY
                && playerId.equals(anchor.uuid());
    }

    /** 一个气源：气体 id + 从该气源扣除气体的方法。 */
    private record GasSource(String gasId, IntConsumer consumer) {
    }

    /** 一次点燃目标：选中的气体 + 从管道/气源扣减的方法。 */
    private record IgniteTarget(String gasId, IntConsumer consumer) {
    }
}
