package com.example.chemistry.client;

import com.example.chemistry.blockentity.RubberTubeLinkBlockEntity;
import com.example.chemistry.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the straight rubber tube. Each link block renders only the slice of
 * the A->B segment that passes through its own cell, so consecutive cells
 * form one continuous tube.
 */
public class RubberTubeLinkRenderer implements BlockEntityRenderer<RubberTubeLinkBlockEntity, RubberTubeLinkRenderState> {

    /** Cross-section side length of the tube in blocks. */
    private static final float TUBE_WIDTH = 0.3F;

    private final BlockRenderDispatcher blockRenderer;

    public RubberTubeLinkRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.blockRenderDispatcher();
    }

    @Override
    public RubberTubeLinkRenderState createRenderState() {
        return new RubberTubeLinkRenderState();
    }

    @Override
    public void extractRenderState(RubberTubeLinkBlockEntity blockEntity, RubberTubeLinkRenderState renderState,
            float partialTick, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.ax = blockEntity.getAx();
        renderState.ay = blockEntity.getAy();
        renderState.az = blockEntity.getAz();
        renderState.bx = blockEntity.getBx();
        renderState.by = blockEntity.getBy();
        renderState.bz = blockEntity.getBz();
    }

    @Override
    public void submit(RubberTubeLinkRenderState renderState, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        double dx = renderState.bx - renderState.ax;
        double dy = renderState.by - renderState.ay;
        double dz = renderState.bz - renderState.az;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-4) {
            return;
        }
        // Clip the A->B segment to this block cell: find the t range inside
        // [cellX, cellX+1] x [cellY, cellY+1] x [cellZ, cellZ+1].
        int cx = renderState.blockPos.getX();
        int cy = renderState.blockPos.getY();
        int cz = renderState.blockPos.getZ();
        double[] range = {0.0, 1.0};
        if (!clipAxis(renderState.ax, dx, cx, range)) {
            return;
        }
        if (!clipAxis(renderState.ay, dy, cy, range)) {
            return;
        }
        if (!clipAxis(renderState.az, dz, cz, range)) {
            return;
        }
        double t0 = range[0];
        double t1 = range[1];
        if (t0 >= t1) {
            return;
        }
        double sx = renderState.ax + dx * t0;
        double sy = renderState.ay + dy * t0;
        double sz = renderState.az + dz * t0;
        double ex = renderState.ax + dx * t1;
        double ey = renderState.ay + dy * t1;
        double ez = renderState.az + dz * t1;
        double lx = ex - sx;
        double ly = ey - sy;
        double lz = ez - sz;
        double len = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (len < 1.0e-4) {
            return;
        }

        BlockStateModel model = blockRenderer.getBlockModel(
                ModBlocks.RUBBER_TUBE_LINK.get().defaultBlockState());
        poseStack.pushPose();
        // The submit pose stack origin is at the block's corner, so offset by
        // the world position relative to that corner.
        poseStack.translate((float) (sx - renderState.blockPos.getX()),
                (float) (sy - renderState.blockPos.getY()),
                (float) (sz - renderState.blockPos.getZ()));
        // Rotate the unit tube (axis = +Y) to point along the slice direction.
        double ndx = lx / len;
        double ndy = ly / len;
        double ndz = lz / len;
        float yaw = (float) Math.toDegrees(Math.atan2(ndx, ndz));
        float pitch = (float) Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, ndy))));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.scale(TUBE_WIDTH, (float) len, TUBE_WIDTH);
        nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), model,
                1.0F, 1.0F, 1.0F, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /** Slab-clip one axis; narrows {@code range} in place and returns false when the segment misses the cell. */
    private static boolean clipAxis(double a, double d, int cellMin, double[] range) {
        double cellMax = cellMin + 1.0;
        if (Math.abs(d) < 1.0e-9) {
            return a > cellMin && a < cellMax;
        }
        double ta = (cellMin - a) / d;
        double tb = (cellMax - a) / d;
        double lo = Math.min(ta, tb);
        double hi = Math.max(ta, tb);
        range[0] = Math.max(range[0], lo);
        range[1] = Math.min(range[1], hi);
        return range[0] < range[1];
    }
}
