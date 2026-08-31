package com.example.chemistry.client;

import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.entity.RubberTubeEntity.Port;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.block.WaterTroughBlock;
import com.example.chemistry.blockentity.WaterTroughBlockEntity;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.entity.AnchorPositions;
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
 * Draws a rubber tube as a sagging quadratic curve between its two anchors.
 * Block anchors stay fixed; entity anchors are re-resolved every frame, so the
 * tube follows mobs/players and droops naturally between them.
 */
public class RubberTubeRenderer extends EntityRenderer<RubberTubeEntity, RubberTubeRenderState> {

    private static final float TUBE_WIDTH = 1.0F / 16.0F; // 1 pixel
    private static final int SEGMENTS = 12;

    private final BlockRenderDispatcher blockRenderer;

    public RubberTubeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
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
    }

    @Override
    public void submit(RubberTubeRenderState rs, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!rs.validA || !rs.validB) {
            return;
        }
        Vec3 delta = rs.b.subtract(rs.a);
        double len = delta.length();
        if (len < 1.0e-4) {
            return;
        }
        Vec3 origin = new Vec3(rs.x, rs.y, rs.z);
        BlockStateModel model = blockRenderer.getBlockModel(
                ModBlocks.RUBBER_TUBE_LINK.get().defaultBlockState());
        Vec3 dir = delta.scale(1.0 / len);

        // The tube leaves each glass-tube head / nozzle along that tube's axis.
        Vec3 axisA = rs.axisA != null ? rs.axisA : dir;
        Vec3 axisB = rs.axisB != null ? rs.axisB : dir.scale(-1.0);
        Vec3 a = rs.a;
        Vec3 b = rs.b;

        // Cubic bezier: leaves each head along the glass-tube axis, sags in the middle.
        Vec3 delta2 = b.subtract(a);
        double len2 = delta2.length();
        if (len2 < 1.0e-4) {
            return;
        }
        double k = Math.max(0.35, Math.min(1.5, len2 * 0.4));
        double sag = Math.min(1.8, len2 * 0.2);
        Vec3 p1 = a.add(axisA.scale(k)).subtract(0, sag, 0);
        Vec3 p2 = b.add(axisB.scale(k)).subtract(0, sag, 0);

        for (int i = 0; i < SEGMENTS; i++) {
            double t0 = (double) i / SEGMENTS;
            double t1 = (double) (i + 1) / SEGMENTS;
            Vec3 s = bezierCubic(a, p1, p2, b, t0);
            Vec3 e = bezierCubic(a, p1, p2, b, t1);
            Vec3 d = e.subtract(s);
            double l = d.length();
            if (l < 1.0e-5) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate((float) (s.x - origin.x), (float) (s.y - origin.y), (float) (s.z - origin.z));
            double ndx = d.x / l;
            double ndy = d.y / l;
            double ndz = d.z / l;
            poseStack.mulPose(Axis.YP.rotationDegrees((float) Math.toDegrees(Math.atan2(ndx, ndz))));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, ndy))))));
            poseStack.scale(TUBE_WIDTH, (float) l, TUBE_WIDTH);
            collector.submitBlockModel(poseStack, RenderType.cutout(), model,
                    1.0F, 1.0F, 1.0F, rs.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
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
            if (type == 1) {
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
        if (state.is(ModBlocks.GAS_COLLECTING_BOTTLE.get())) {
            return new Vec3(0, state.getValue(GasCollectingBottleBlock.INVERTED) ? -1 : 1, 0);
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

    /** 1 = flask, 2 = erlenmeyer, 3 = crucible, 4 = evaporating dish. */
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
