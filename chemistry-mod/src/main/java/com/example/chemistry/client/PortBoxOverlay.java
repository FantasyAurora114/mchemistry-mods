package com.example.chemistry.client;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.StandPorts;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.block.WaterTroughBlock;
import com.example.chemistry.blockentity.HeatingMantleBlockEntity;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.blockentity.MagneticStirrerBlockEntity;
import com.example.chemistry.blockentity.PlacedVesselBlockEntity;
import com.example.chemistry.blockentity.TripodBlockEntity;
import com.example.chemistry.blockentity.WaterTroughBlockEntity;
import com.example.chemistry.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** 按 F9 临时显示所有反应容器的端口/接口命中盒：绿=空闲，红=占用。 */
@EventBusSubscriber(modid = ChemistryMod.MODID, value = Dist.CLIENT)
public final class PortBoxOverlay {

    public static boolean visible = false;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterEntities event) {
        if (!visible) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        // AfterEntities 的 PoseStack 是单位栈：先做相机平移，再按世界坐标画线
        // （与调试渲染器同一套路；AfterTranslucentBlocks 传的是 null 栈画不出来）。
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        BlockPos c = mc.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-8, -4, -8), c.offset(8, 8, 8))) {
            BlockState state = mc.level.getBlockState(pos);
            if (state.is(ModBlocks.IRON_STAND.get())
                    && mc.level.getBlockEntity(pos) instanceof IronStandBlockEntity be) {
                for (StandPorts.PortBox pb : StandPorts.boxes(pos, state, be)) {
                    draw(poseStack, buffer, pb.box(), pb.occupied() ? 1.0F : 0.25F,
                            pb.occupied() ? 0.2F : 1.0F, 0.2F, 0.85F);
                }
            } else if (state.is(ModBlocks.PLACED_VESSEL.get())
                    && mc.level.getBlockEntity(pos) instanceof PlacedVesselBlockEntity be
                    && !be.getVessel().isEmpty()) {
                drawGeneric(poseStack, buffer, pos, be.getVessel(), 1.0, 0.0, 0.0, 0.0, 0.0,
                        be.getAttached1(), be.getAttached2());
            } else if (state.is(ModBlocks.HEATING_MANTLE.get())
                    && mc.level.getBlockEntity(pos) instanceof HeatingMantleBlockEntity be
                    && !be.getFlask().isEmpty()) {
                double off = (8.5 - 8.5 * 0.7) / 16.0;
                drawGeneric(poseStack, buffer, pos, be.getFlask(), 0.7, off, 2.5 / 16.0, off, 0.0,
                        ItemStack.EMPTY, ItemStack.EMPTY);
            } else if (state.is(ModBlocks.MAGNETIC_STIRRER.get())
                    && mc.level.getBlockEntity(pos) instanceof MagneticStirrerBlockEntity be
                    && !be.getFlask().isEmpty()) {
                double off = (8.5 - 8.5 * 0.5) / 16.0;
                drawGeneric(poseStack, buffer, pos, be.getFlask(), 0.5, off, 5.8 / 16.0, off, 0.0,
                        ItemStack.EMPTY, ItemStack.EMPTY);
            } else if (state.is(ModBlocks.WATER_TROUGH.get())
                    && state.getValue(WaterTroughBlock.FILLED) == WaterTroughBlock.Fill.ICE
                    && mc.level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                    && !be.getFlask().isEmpty()) {
                double off = (8.5 - 8.5 * 0.7) / 16.0;
                drawGeneric(poseStack, buffer, pos, be.getFlask(), 0.7, off, 1.0 / 16.0, off, 0.0,
                        ItemStack.EMPTY, ItemStack.EMPTY);
            } else if (state.is(ModBlocks.TRIPOD.get())
                    && state.getValue(com.example.chemistry.block.TripodBlock.HAS_VESSEL)
                    && mc.level.getBlockEntity(pos) instanceof TripodBlockEntity be
                    && !be.getVessel().isEmpty()) {
                int vtype = VesselHeating.vesselType(be.getVessel());
                if (vtype != 0) {
                    Vec3 m = VesselHeating.tripodMouthWorldPosition(pos, vtype);
                    boolean occ = VesselHeating.isSealed(be.getVessel());
                    draw(poseStack, buffer, box(m, 0.4), occ ? 1.0F : 0.25F,
                            occ ? 0.2F : 1.0F, 0.2F, 0.85F);
                }
            }
        }
        buffer.endBatch();
        poseStack.popPose();
    }

    private static void drawGeneric(PoseStack poseStack, MultiBufferSource buffer, BlockPos pos,
            ItemStack vessel, double scale, double offX, double offY, double offZ, double yaw,
            ItemStack attached1, ItemStack attached2) {
        int vtype = VesselHeating.vesselType(vessel);
        if (vtype == 0) {
            return;
        }
        if (VesselHeating.isThreeNeck(vessel)) {
            Vec3[] necks = VesselHeating.neckWorldPositions(pos, scale, offX, offY, offZ, yaw);
            for (int i = 0; i < 3; i++) {
                boolean occ = VesselHeating.neckHasStopper(vessel, i);
                draw(poseStack, buffer, box(necks[i], 0.34), occ ? 1.0F : 0.25F,
                        occ ? 0.2F : 1.0F, 0.2F, 0.85F);
            }
        } else {
            Vec3 m = VesselHeating.mouthWorldPosition(pos, vtype, scale, offX, offY, offZ, yaw);
            boolean occ = VesselHeating.isSealed(vessel);
            draw(poseStack, buffer, box(m, 0.4), occ ? 1.0F : 0.25F,
                    occ ? 0.2F : 1.0F, 0.2F, 0.85F);
        }
        for (int hole = 1; hole <= 2; hole++) {
            ItemStack att = hole == 1 ? attached1 : attached2;
            if (att.isEmpty()) {
                continue;
            }
            AABB b = VesselHeating.attachedHeadBox(pos, vessel, vtype, scale, offX, offY, offZ,
                    yaw, hole, hole == 1 && !attached2.isEmpty(), att);
            if (b != null) {
                draw(poseStack, buffer, b, 0.3F, 0.5F, 1.0F, 0.9F);
            }
        }
    }

    private static void draw(PoseStack poseStack, MultiBufferSource buffer, AABB box,
            float r, float g, float b, float a) {
        DebugRenderer.renderVoxelShape(poseStack, buffer.getBuffer(RenderType.lines()),
                Shapes.create(box), 0, 0, 0, r, g, b, a, false);
    }

    private static AABB box(Vec3 c, double half) {
        return new AABB(c.x - half, c.y - half, c.z - half,
                c.x + half, c.y + half, c.z + half);
    }

    private PortBoxOverlay() {
    }
}
