package com.example.chemistry.client;

import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.entity.RubberTubeEntity.Port;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.block.WaterTroughBlock;
import com.example.chemistry.blockentity.WaterTroughBlockEntity;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.entity.AnchorPositions;
import com.example.chemistry.entity.GasCollectingBottleEntity;
import com.example.chemistry.entity.PlacedVesselEntity;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a rubber tube as an octagonal hose following a cubic curve between its two anchors.
 * Block anchors stay fixed; entity anchors are re-resolved every frame, so the
 * tube follows mobs/players and droops naturally between them.
 */
public class RubberTubeRenderer extends EntityRenderer<RubberTubeEntity, RubberTubeRenderState> {

    public RubberTubeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public RubberTubeRenderState createRenderState() {
        return new RubberTubeRenderState();
    }

    @Override
    public void extractRenderState(RubberTubeEntity entity, RubberTubeRenderState rs, float partialTick) {
        super.extractRenderState(entity, rs, partialTick);
        Level level = entity.level();
        Vec3 fallbackA = rs.validA ? rs.a : null;
        Vec3 fallbackB = rs.validB ? rs.b : null;
        Vec3 a = resolveAnchor(level, entity.getAnchorA(), partialTick, fallbackA);
        Vec3 b = resolveAnchor(level, entity.getAnchorB(), partialTick, fallbackB);
        rs.a = a != null ? a : Vec3.ZERO;
        rs.b = b != null ? b : Vec3.ZERO;
        rs.validA = a != null;
        rs.validB = b != null;
        rs.sleeveA = hasSleeve(entity.getAnchorA());
        rs.sleeveB = hasSleeve(entity.getAnchorB());
        rs.axisA = rs.sleeveA ? standAxis(level, entity.getAnchorA()) : null;
        rs.axisB = rs.sleeveB ? standAxis(level, entity.getAnchorB()) : null;
        rs.supply = entity.supplyLine();
        if (!rs.validA || !rs.validB) { rs.curve = java.util.List.of(); return; }
        Vec3 axisA = applianceAxis(level, entity.getAnchorA(), rs.axisA);
        Vec3 axisB = applianceAxis(level, entity.getAnchorB(), rs.axisB);
        Vec3 delta = rs.b.subtract(rs.a); double length = delta.length();
        Vec3 dir = delta.normalize(); double k = Math.max(.15, Math.min(.9, length * .25));
        double sag = Math.min(.65, length * .12);
        Vec3 p1 = rs.a.add((axisA == null ? dir : axisA).scale(k)).subtract(0, sag, 0);
        Vec3 p2 = rs.b.add((axisB == null ? dir.scale(-1) : axisB).scale(k)).subtract(0, sag, 0);
        java.util.List<Vec3> curve = new java.util.ArrayList<>();
        for (int i = 0; i <= 32; i++) {
            double t = i / 32.0; Vec3 point = bezierCubic(rs.a, p1, p2, rs.b, t);
            Vec3 upper = rs.a.lerp(rs.b, t).add(0, .02, 0);
            if (point.y < upper.y && i > 1 && i < 31) {
                var hit = level.clip(new net.minecraft.world.level.ClipContext(upper, point.subtract(0, .045, 0),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, entity));
                if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) point = new Vec3(point.x, Math.max(point.y, hit.getLocation().y + .045), point.z);
            }
            curve.add(point);
        }
        rs.curve = java.util.List.copyOf(curve);
    }

    @Override
    public void submit(RubberTubeRenderState rs, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!rs.validA || !rs.validB) {
            return;
        }
        if (rs.curve.size() < 2) return;
        var texture = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry",
                rs.supply ? "textures/block/gas_supply_tube.png" : "textures/block/rubber_tube_side.png");
        double radius = rs.supply ? .035 : .027;
        Vec3 origin = new Vec3(rs.x, rs.y, rs.z);
        collector.submitCustomGeometry(poseStack, RenderType.entityCutoutNoCull(texture), (pose, out) -> {
            for (int i = 0; i < rs.curve.size() - 1; i++) {
                Vec3 a = rs.curve.get(i), b = rs.curve.get(i + 1);
                Vec3 tangent = b.subtract(a).normalize();
                Vec3 normal = tangent.cross(new Vec3(0, 1, 0));
                if (normal.lengthSqr() < 1e-6) normal = new Vec3(1, 0, 0);
                normal = normal.normalize();
                Vec3 side = tangent.cross(normal).normalize();
                for (int j = 0; j < 8; j++) {
                    double angle0 = j * Math.PI / 4, angle1 = (j + 1) * Math.PI / 4;
                    Vec3 n0 = normal.scale(Math.cos(angle0)).add(side.scale(Math.sin(angle0)));
                    Vec3 n1 = normal.scale(Math.cos(angle1)).add(side.scale(Math.sin(angle1)));
                    Vec3[] positions = {a.add(n0.scale(radius)), a.add(n1.scale(radius)),
                            b.add(n1.scale(radius)), b.add(n0.scale(radius))};
                    Vec3[] normals = {n0, n1, n1, n0};
                    for (int v = 0; v < 4; v++) {
                        Vec3 point = positions[v].subtract(origin), n = normals[v];
                        out.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                                .setColor(255, 255, 255, 255).setUv(v == 0 || v == 3 ? j / 8f : (j + 1) / 8f, v < 2 ? 0 : 1)
                                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(rs.lightCoords)
                                .setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
                    }
                }
            }
        });
    }

    private static Vec3 applianceAxis(Level level, Port port, Vec3 fallback) {
        if (port != null && port.kind() == Port.KIND_BLOCK && port.pos() != null) {
            var be = com.example.chemistry.block.GasApplianceBlock.device(level, port.pos());
            if (be != null) { double angle = Math.toRadians(be.rotation()); return new Vec3(Math.cos(angle), 0, -Math.sin(angle)); }
        }
        return fallback;
    }

    private static boolean hasSleeve(RubberTubeEntity.Port anchor) {
        return anchor != null && (anchor.kind() == RubberTubeEntity.Port.KIND_STAND
                || anchor.kind() == RubberTubeEntity.Port.KIND_NOZZLE);
    }

    /** Outward axis of the glass tube / nozzle at a tube head (world unit dir),
     *  or null when it cannot be determined. */
    private static Vec3 standAxis(Level level, RubberTubeEntity.Port anchor) {
        BlockPos pos = anchor.pos();
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.IRON_STAND.get())
                && level.getBlockEntity(pos) instanceof IronStandBlockEntity be) {
            ItemStack s = anchor.slot() == 1 ? be.getAttached1() : be.getAttached2();
            int type = attachedType(s);
            if (type == 0) {
                return null;
            }
            if (!be.getVessel().isEmpty()) {
                return new Vec3(0, 1, 0); // vertical flask-stopper head
            }
            int rotation = state.getValue(IronStandBlock.ROTATION);
            Direction facing = state.getValue(IronStandBlock.FACING);
            double angle = Math.toRadians(rotation * 45.0);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double ax;
            double ay;
            if (type == 1 || type == 8) {
                ax = -sin;
                ay = cos;
            } else {
                double dx = sin >= 0 ? -cos : cos;
                double dy = sin >= 0 ? -sin : sin;
                ax = dx;
                ay = dy;
            }
            double yaw = Math.toRadians(-facing.toYRot());
            double c = Math.cos(yaw);
            double s2 = Math.sin(yaw);
            return new Vec3(ax * c, ay, -ax * s2).normalize();
        }
        if (state.is(ModBlocks.PLACED_VESSEL.get())) {
            return new Vec3(0, 1, 0);
        }
        for (PlacedVesselEntity e : level.getEntitiesOfClass(PlacedVesselEntity.class,
                new AABB(pos))) {
            return new Vec3(0, 1, 0);
        }
        if (state.is(ModBlocks.GAS_COLLECTING_BOTTLE.get())) {
            return new Vec3(0, state.getValue(GasCollectingBottleBlock.INVERTED) ? -1 : 1, 0);
        }
        for (GasCollectingBottleEntity e : level.getEntitiesOfClass(GasCollectingBottleEntity.class,
                new AABB(pos))) {
            return new Vec3(0, e.isInverted() ? -1 : 1, 0);
        }
        if (state.getBlock() instanceof WaterTroughBlock && anchor.face() != null) {
            if (anchor.face() == Direction.UP
                    && level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                    && be.hasBottle()) {
                // Gas enters the inverted bottle's mouth from below.
                return new Vec3(0, -1, 0);
            }
            Direction face = anchor.face();
            return new Vec3(face.getStepX(), 1, face.getStepZ()).normalize();
        }
        return null;
    }

    private static Vec3 resolveAnchor(Level level, Port anchor, float partialTick, Vec3 fallback) {
        if (anchor == null) {
            return fallback;
        }
        if (anchor.kind() == Port.KIND_STAND) {
            Vec3 head = AnchorPositions.standHead(level, anchor);
            return head != null ? head : fallback;
        }
        if (anchor.kind() == Port.KIND_NOZZLE) {
            return AnchorPositions.nozzleTip(level, anchor);
        }
        if (anchor.kind() == Port.KIND_BLOCK) {
            return com.example.chemistry.item.RubberTubeItem.anchorWorldPos(level, anchor);
        }
        Entity e = level.getEntity(anchor.uuid());
        if (e == null) {
            return fallback;
        }
        if (e instanceof GasCollectingBottleEntity bottle) return bottle.nozzleTip();
        return e.getPosition(partialTick).add(0, e.getEyeHeight() * 0.7, 0);
    }

    private static int attachedType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof DropperItem) {
            return 3;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.equals("straight_glass_tube_long")) {
            return 8;
        }
        if (path.equals("straight_glass_tube")) {
            return 1;
        }
        if (path.equals("right_angle_glass_tube")) {
            return 2;
        }
        if (path.equals("right_angle_glass_tube_long")) {
            return 4;
        }
        return 0;
    }

    /** Smooth centerline with independent outlet directions at both ends. */
    private static Vec3 bezierCubic(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double u = 1.0 - t;
        return p0.scale(u * u * u)
                .add(p1.scale(3.0 * u * u * t))
                .add(p2.scale(3.0 * u * t * t))
                .add(p3.scale(t * t * t));
    }

    @Override
    protected AABB getBoundingBoxForCulling(RubberTubeEntity entity) {
        Vec3 a = resolveAnchor(entity.level(), entity.getAnchorA(), 0, null);
        Vec3 b = resolveAnchor(entity.level(), entity.getAnchorB(), 0, null);
        if (a == null || b == null) {
            return entity.getBoundingBox();
        }
        double minX = Math.min(a.x, b.x) - 1.0;
        double minY = Math.min(a.y, b.y) - 3.0;
        double minZ = Math.min(a.z, b.z) - 1.0;
        double maxX = Math.max(a.x, b.x) + 1.0;
        double maxY = Math.max(a.y, b.y) + 1.0;
        double maxZ = Math.max(a.z, b.z) + 1.0;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
