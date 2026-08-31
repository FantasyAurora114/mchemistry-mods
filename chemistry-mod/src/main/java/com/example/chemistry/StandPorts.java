package com.example.chemistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.blockentity.IronStandBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 铁架台上的"接口/插头"虚拟命中盒：一个端口一个盒，射线选中后即可对接/拆件。
 * 与橡胶管可剪选中同一原理——端口是一个可点的虚框。
 */
public final class StandPorts {

    public static final int MOUTH = 1;          // 瓶口接口（单口瓶 / 三颈瓶中间颈）
    public static final int NECK_LEFT = 2;      // 三颈瓶左颈
    public static final int NECK_RIGHT = 3;     // 三颈瓶右颈
    public static final int ATTACHED_1 = 4;     // 塞子孔 1 上的附件
    public static final int ATTACHED_2 = 5;     // 塞子孔 2 上的附件
    public static final int HEAD = 6;           // 蒸馏头本体 + 顶端接口（温度计）
    public static final int HEAD_ARM = 7;       // 蒸馏头侧管接口（冷凝管）
    public static final int CONDENSER_END = 8;  // 冷凝管远端接口（牛角管）
    public static final int RECEIVER = 9;       // 接收瓶瓶口

    public record PortBox(int id, AABB box, boolean occupied, String label) {
    }

    private StandPorts() {
    }

    public static List<PortBox> boxes(BlockPos pos, BlockState state, IronStandBlockEntity be) {
        List<PortBox> out = new ArrayList<>();
        if (be == null || be.getVessel().isEmpty()) {
            return out;
        }
        ItemStack vessel = be.getVessel();
        int vtype = VesselHeating.vesselType(vessel);
        if (vtype == 0) {
            return out;
        }
        double scale = IronStandBlock.VESSEL_SCALE;
        double offX = IronStandBlock.VESSEL_OFF_X;
        double offY = IronStandBlock.VESSEL_OFF_Y;
        double offZ = IronStandBlock.VESSEL_OFF_Z;
        double yaw = -state.getValue(IronStandBlock.FACING).toYRot();
        boolean three = VesselHeating.isThreeNeck(vessel);
        Vec3[] necks = three
                ? VesselHeating.neckWorldPositions(pos, scale, offX, offY, offZ, yaw)
                : null;
        Vec3 mouth = three ? necks[1]
                : VesselHeating.mouthWorldPosition(pos, vtype, scale, offX, offY, offZ, yaw);
        boolean headOn = be.hasDistillationHead();
        if (three) {
            for (int i = 0; i < 3; i++) {
                boolean occupied = VesselHeating.neckHasStopper(vessel, i)
                        || (i == 1 && headOn);
                out.add(new PortBox(i == 0 ? NECK_LEFT : i == 2 ? NECK_RIGHT : MOUTH,
                        boxAround(necks[i], 0.34), occupied, "neck" + (i + 1)));
            }
        } else {
            out.add(new PortBox(MOUTH, boxAround(mouth, 0.4),
                    VesselHeating.isSealed(vessel) || headOn
                            || (be.hasCondenser() && !headOn),
                    "mouth"));
        }
        if (!be.getAttached1().isEmpty()) {
            AABB b = VesselHeating.attachedHeadBox(pos, vessel, vtype, scale, offX, offY, offZ,
                    yaw, 1, !be.getAttached2().isEmpty(), be.getAttached1());
            if (b != null) {
                out.add(new PortBox(ATTACHED_1, b, true, "attached1"));
            }
        }
        if (!be.getAttached2().isEmpty()) {
            AABB b = VesselHeating.attachedHeadBox(pos, vessel, vtype, scale, offX, offY, offZ,
                    yaw, 2, false, be.getAttached2());
            if (b != null) {
                out.add(new PortBox(ATTACHED_2, b, true, "attached2"));
            }
        }
        if (headOn) {
            out.add(new PortBox(HEAD,
                    new AABB(mouth.x - 0.32, mouth.y + 0.05, mouth.z - 0.32,
                            mouth.x + 0.32, mouth.y + (be.hasHeadThermometer() ? 0.7 : 0.45),
                            mouth.z + 0.32),
                    be.hasHeadThermometer(), "head"));
            out.add(new PortBox(HEAD_ARM,
                    boxAround(mouth.add(0.0, 0.2, -0.12), 0.3),
                    be.hasCondenser(), "head_arm"));
        }
        if (be.hasCondenser()) {
            double cy = headOn ? 17.5 : 14.3;
            Vec3 pivot = rotateYaw(new Vec3(pos.getX() + 8.5 / 16.0,
                    pos.getY() + cy / 16.0, pos.getZ() + 9.0 / 16.0), pos, yaw);
            Vec3 end = pivot.add(0.0, -0.9, -0.28);
            out.add(new PortBox(CONDENSER_END, boxAround(end, 0.35),
                    be.hasReceiverAdapter(), "condenser_end"));
        }
        if (be.hasReceiverAdapter()) {
            Vec3 outCenter = rotateYaw(new Vec3(pos.getX() + 8.5 / 16.0,
                    pos.getY() + 8.0 / 16.0, pos.getZ() + 4.5 / 16.0), pos, yaw);
            out.add(new PortBox(RECEIVER, boxAround(outCenter.add(0.0, 0.15, -0.1), 0.32),
                    !be.getReceiver().isEmpty(), "receiver"));
        }
        return out;
    }

    /** 射线选中端口：返回最近命中的端口 id，0 = 没对准任何端口。 */
    public static int pick(Level level, Player player, BlockPos pos, BlockState state,
            IronStandBlockEntity be) {
        if (player == null) {
            return 0;
        }
        Vec3 from = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        Vec3 to = from.add(dir.scale(6.0));
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (PortBox pb : boxes(pos, state, be)) {
            Optional<Vec3> hit = pb.box().clip(from, to);
            if (hit.isPresent()) {
                // 相邻盒（三颈瓶三个瓶口）会互相重叠：命中后按"射线到盒中心的
                // 最近距离"取最贴合的端口，避免选错瓶口。
                Vec3 center = pb.box().getCenter();
                Vec3 v = center.subtract(from);
                double t = v.dot(dir);
                Vec3 closest = t > 0 ? from.add(dir.scale(t)) : from;
                double d = center.distanceTo(closest);
                if (d < bestD) {
                    bestD = d;
                    best = pb.id();
                }
            }
        }
        return best;
    }

    private static AABB boxAround(Vec3 c, double half) {
        return new AABB(c.x - half, c.y - half, c.z - half,
                c.x + half, c.y + half, c.z + half);
    }

    private static Vec3 rotateYaw(Vec3 w, BlockPos pos, double yawDegrees) {
        if (yawDegrees == 0.0) {
            return w;
        }
        double yaw = Math.toRadians(yawDegrees);
        double c = Math.cos(yaw);
        double s = Math.sin(yaw);
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        double dx = w.x - cx;
        double dz = w.z - cz;
        return new Vec3(cx + dx * c + dz * s, w.y, cz - dx * s + dz * c);
    }
}
