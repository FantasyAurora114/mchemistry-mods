package com.example.chemistry;

import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.blockentity.GasCollectingBottleBlockEntity;
import com.example.chemistry.blockentity.WaterTroughBlockEntity;
import com.example.chemistry.entity.GasCollectingBottleEntity;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.entity.RubberTubeEntity.Port;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/**
 * 流体网络（FlowNetwork）：气/液共用的运输核心。
 * <p>一个 {@link Packet} 表示一份流体（相态 GAS/LIQUID + 物质 id + mL + 纯度），
 * 从源 Port 沿连接（橡胶管 Connection）输送到目标 Port。
 * 目前玩法仍以气体为主（液体槽位预留给后续），内部结构统一。
 */
public final class FlowNetwork {

    public enum Phase {
        GAS, LIQUID
    }

    /** 一份流经网络的流体包。 */
    public record Packet(Phase phase, String id, int ml, double purity) {
        public static Packet gas(String id, int ml, double purity) {
            return new Packet(Phase.GAS, id, ml, purity);
        }
    }

    private FlowNetwork() {
    }

    /**
     * 把一份流体送达目标 Port：
     * 气体 → 集气瓶 / 水槽里的倒置集气瓶 / 手持导气嘴（排入空气）；
     * 液体 → 暂未开放收集（返回 0，由调用方处理）。
     * 返回实际接受量。
     */
    public static int deliver(Level level, Port far, Packet packet) {
        if (far == null || packet == null) {
            return 0;
        }
        if (packet.phase() == Phase.LIQUID) {
            // 液体管路暂未开放，返回 0（调用方按"无法送达"处理）。
            return 0;
        }
        String id = packet.id();
        int ml = packet.ml();
        double purity = packet.purity();
        if (RubberTubeEntity.AIR.equals(id)) {
            // 装置中被推出的空气：在出口冒泡，不收集，但"接受"体积让流动继续。
            if (level instanceof ServerLevel server) {
                Vec3 pos = RubberTubeItem.anchorWorldPos(level, far);
                if (pos == null) {
                    pos = far.pos() == null ? null
                            : new Vec3(far.pos().getX() + 0.5, far.pos().getY() + 0.5,
                                    far.pos().getZ() + 0.5);
                }
                if (pos != null) {
                    server.sendParticles(ParticleTypes.BUBBLE, pos.x, pos.y, pos.z,
                            2, 0.1, 0.05, 0.1, 0.01);
                    if (server.getGameTime() % 8 == 0) {
                        server.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.1,
                                pos.z, 1, 0.08, 0.03, 0.08, 0.01);
                    }
                }
            }
            return ml;
        }
        if (far.kind() == Port.KIND_BLOCK && far.pos() != null) {
            var device = com.example.chemistry.block.GasApplianceBlock.device(level, far.pos());
            return device == null ? 0 : device.receive(id, ml);
        }
        if (far.kind() == Port.KIND_ENTITY) {
            // 手持导气嘴 / 实体端：气体在此排入空气，流动不中断。
            Entity e = level.getEntity(far.uuid());
            if(e instanceof GasCollectingBottleEntity bottle)return bottle.hasNozzle()?bottle.addGas(id,ml,purity):0;
            if (e == null) {
                return 0;
            }
            if (level instanceof ServerLevel server) {
                Vec3 tip = e.position().add(0, e.getEyeHeight() * 0.7, 0);
                server.sendParticles(ParticleTypes.BUBBLE, tip.x, tip.y, tip.z,
                        2, 0.1, 0.05, 0.1, 0.01);
                if (server.getGameTime() % 8 == 0) {
                    server.sendParticles(ParticleTypes.CLOUD, tip.x, tip.y + 0.1,
                            tip.z, 1, 0.08, 0.03, 0.08, 0.01);
                }
            }
            return ml;
        }
        if (far.kind() != Port.KIND_NOZZLE || far.pos() == null) {
            return 0;
        }
        BlockPos pos = far.pos();
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.GAS_COLLECTING_BOTTLE.get())
                && state.getValue(GasCollectingBottleBlock.HAS_NOZZLE)
                && level.getBlockEntity(pos) instanceof GasCollectingBottleBlockEntity be) {
            return be.addGas(id, ml, purity);
        }
        for (GasCollectingBottleEntity be : level.getEntitiesOfClass(
                GasCollectingBottleEntity.class, new AABB(pos))) {
            if (be.hasNozzle()) {
                return be.addGas(id, ml, purity);
            }
        }
        if (state.is(ModBlocks.WATER_TROUGH.get())
                && level.getBlockEntity(pos) instanceof WaterTroughBlockEntity wbe) {
            return wbe.addGas(id, ml, purity);
        }
        return 0;
    }
}
