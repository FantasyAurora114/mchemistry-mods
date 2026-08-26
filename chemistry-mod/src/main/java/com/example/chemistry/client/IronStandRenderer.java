package com.example.chemistry.client;

import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.TestTubeItem;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the iron stand: base + clamp, the mounted tube (with tinted contents
 * and stopper), and the alcohol lamp on the stand. The whole model is rotated
 * by the block facing; the lamp is translated so its flame reaches the bottom
 * of the tube for the current rotation.
 */
public class IronStandRenderer implements BlockEntityRenderer<IronStandBlockEntity, IronStandRenderState> {

    private final BlockRenderDispatcher blockRenderer;
    private final net.minecraft.client.renderer.block.model.BlockStateModel planks;

    public IronStandRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.blockRenderDispatcher();
        this.planks = blockRenderer.getBlockModel(Blocks.OAK_PLANKS.defaultBlockState());
    }

    @Override
    public IronStandRenderState createRenderState() {
        return new IronStandRenderState();
    }

    @Override
    public void extractRenderState(IronStandBlockEntity blockEntity, IronStandRenderState renderState,
            float partialTick, net.minecraft.world.phys.Vec3 cameraPosition,
            @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        var state = blockEntity.getBlockState();
        renderState.facing = state.getValue(IronStandBlock.FACING);
        renderState.rotation = state.getValue(IronStandBlock.ROTATION);
        renderState.hasTube = state.getValue(IronStandBlock.HAS_TUBE);
        renderState.hasContents = state.getValue(IronStandBlock.HAS_CONTENTS);
        renderState.hasStopper = state.getValue(IronStandBlock.HAS_STOPPER);
        renderState.hasLamp = state.getValue(IronStandBlock.HAS_LAMP);
        renderState.lampLit = state.getValue(IronStandBlock.LAMP_LIT);
        renderState.attached1 = attachedType(blockEntity.getAttached1());
        renderState.attached2 = attachedType(blockEntity.getAttached2());
        renderState.stopperHoles = blockEntity.getTube().getItem() instanceof TestTubeItem tt
                ? tt.stopperHoles() : 0;
        renderState.contentsColor = LabVesselItem.contentsColor(blockEntity.getTube());
        renderState.attachment = state.getValue(IronStandBlock.ATTACHMENT);
        renderState.vesselType = vesselType(blockEntity.getVessel());
        renderState.vesselColor = LabVesselItem.contentsColor(blockEntity.getVessel());
        renderState.hasCondenser = blockEntity.hasCondenser();
        renderState.hasReceiver = !blockEntity.getReceiver().isEmpty();
        renderState.receiverColor = LabVesselItem.contentsColor(blockEntity.getReceiver());
        renderState.vesselSealed = VesselHeating.isSealed(blockEntity.getVessel());
    }

    @Override
    public void submit(IronStandRenderState renderState, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.facing.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        render(ModStandaloneModels.part(renderState.rotation, ModStandaloneModels.PART_BASE),
                poseStack, nodeCollector, renderState, 1.0F, 1.0F, 1.0F);
        if (renderState.attachment != 0) {
            render(ModStandaloneModels.attachment(renderState.attachment),
                    poseStack, nodeCollector, renderState, 1.0F, 1.0F, 1.0F);
        }
        if (renderState.vesselType != 0) {
            renderVessel(renderState, poseStack, nodeCollector);
            if (renderState.vesselSealed) {
                renderVesselStopperAndAttached(renderState, poseStack, nodeCollector);
            }
            if (renderState.hasCondenser) {
                renderCondenser(poseStack, nodeCollector, renderState);
            }
            if (renderState.hasReceiver) {
                renderReceiver(renderState, poseStack, nodeCollector);
            }
        }
        if (renderState.hasTube) {
            render(ModStandaloneModels.part(renderState.rotation, ModStandaloneModels.PART_TUBE),
                    poseStack, nodeCollector, renderState, 1.0F, 1.0F, 1.0F);
        }
        if (renderState.hasContents) {
            int c = renderState.contentsColor;
            render(ModStandaloneModels.part(renderState.rotation, ModStandaloneModels.PART_CONTENTS),
                    poseStack, nodeCollector, renderState,
                    ((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F, (c & 0xFF) / 255.0F);
        }
        if (renderState.hasStopper) {
            render(ModStandaloneModels.part(renderState.rotation, ModStandaloneModels.PART_STOPPER),
                    poseStack, nodeCollector, renderState, 1.0F, 1.0F, 1.0F);
        }
        if (renderState.hasLamp) {
            BlockStateModel lamp = blockRenderer.getBlockModel(
                    ModBlocks.ALCOHOL_LAMP.get().defaultBlockState()
                            .setValue(com.example.chemistry.block.AlcoholLampBlock.LIT, renderState.lampLit));
            double angle = Math.toRadians(renderState.rotation * 45.0);
            double ox = 0.3333;
            double oy = -2.3333;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double bx = 8.5 + ox * cos - oy * sin;
            double by = 9.5 + ox * sin + oy * cos;
            double yModel = Math.max(2.0, Math.min(4.5, by - 5.9));
            Vec3 offset = new Vec3((bx - 8.5) / 16.0, yModel / 16.0, (8.8333 - 8.5) / 16.0);
            if (yModel > 2.0) {
                // Small wooden support block so the raised lamp does not float.
                float w = 0.25F;
                float lampCenter = 8.5F / 16.0F;
                poseStack.pushPose();
                poseStack.translate(lampCenter + (float) offset.x - w / 2.0F,
                        2.0F / 16.0F, lampCenter + (float) offset.z - w / 2.0F);
                poseStack.scale(w, (float) (yModel - 2.0) / 16.0F, w);
                render(planks, poseStack, nodeCollector, renderState, 1.0F, 1.0F, 1.0F);
                poseStack.popPose();
            }
            poseStack.pushPose();
            poseStack.translate((float) offset.x, (float) offset.y, (float) offset.z);
            render(lamp, poseStack, nodeCollector, renderState, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
        if (renderState.hasTube && renderState.hasStopper) {
            renderAttached(renderState, poseStack, nodeCollector, 1, 0.55);
            renderAttached(renderState, poseStack, nodeCollector, 2, -0.55);
        }
        poseStack.popPose();
    }

    /** Rubber stopper on the flask mouth + instruments through its holes. */
    private void renderVesselStopperAndAttached(IronStandRenderState rs, PoseStack poseStack,
            SubmitNodeCollector nodeCollector) {
        // Vessel top (mouth) y in the local stand frame.
        double baseY = rs.vesselType == 1 ? 1.0 : 0.0;
        double topY = switch (rs.vesselType) {
            case 1 -> 9.0;   // round-bottom flask
            case 2 -> 9.0;   // erlenmeyer flask
            case 3 -> 6.0;   // crucible
            default -> 2.0;  // evaporating dish
        };
        double mouthY = 9.5 + (topY - baseY) * 0.6;
        poseStack.pushPose();
        poseStack.translate(8.5F / 16.0F, (float) (mouthY / 16.0), 9.0F / 16.0F);
        render(ModStandaloneModels.vesselStopper(), poseStack, nodeCollector,
                rs, 1.0F, 1.0F, 1.0F);
        int type1 = rs.attached1;
        int type2 = rs.attached2;
        if (type1 != 0) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, (type2 != 0 ? -0.55F : 0.0F) / 16.0F);
            render(ModStandaloneModels.attachedModel(type1), poseStack, nodeCollector,
                    rs, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
        if (type2 != 0) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 0.55F / 16.0F);
            render(ModStandaloneModels.attachedModel(type2), poseStack, nodeCollector,
                    rs, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private void renderVessel(IronStandRenderState rs, PoseStack poseStack,
            SubmitNodeCollector nodeCollector) {
        // Vessel sits on the ring: the ring is centred at local (8.5, 9.0)
        // with its top at y 9.5. Scale 0.6 around the MODEL centre (not the
        // origin), otherwise the scaled-down vessel drifts off the ring.
        double s = 0.6;
        double cx = (rs.vesselType == 3 || rs.vesselType == 4) ? 7.5 : 8.5;
        double cz = 8.5;
        double baseY = rs.vesselType == 1 ? 1.0 : 0.0;
        poseStack.pushPose();
        poseStack.translate((8.5 - cx * s) / 16.0, (9.5 - baseY * s) / 16.0,
                (9.0 - cz * s) / 16.0);
        poseStack.scale((float) s, (float) s, (float) s);
        render(ModStandaloneModels.vessel(rs.vesselType), poseStack, nodeCollector,
                rs, 1.0F, 1.0F, 1.0F);
        int c = rs.vesselColor;
        if (c != 0xFFFFFF) {
            render(ModStandaloneModels.vesselContents(rs.vesselType), poseStack, nodeCollector,
                    rs, ((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F, (c & 0xFF) / 255.0F);
        }
        poseStack.popPose();
    }

    /** Condenser tilted from the flask mouth down toward the front. */
    private void renderCondenser(PoseStack poseStack, SubmitNodeCollector nodeCollector,
            IronStandRenderState rs) {
        poseStack.pushPose();
        poseStack.translate(8.5F / 16.0F, 14.3F / 16.0F, 9.0F / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(18.0F));
        poseStack.translate(0.0F, -19.0F / 16.0F, 0.0F);
        render(ModStandaloneModels.condenser(), poseStack, nodeCollector, rs, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    /** Receiver Erlenmeyer flask on the ground in front of the condenser outlet. */
    private void renderReceiver(IronStandRenderState rs, PoseStack poseStack,
            SubmitNodeCollector nodeCollector) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, (2.6 - 8.5) / 16.0);
        render(ModStandaloneModels.vessel(2), poseStack, nodeCollector, rs, 1.0F, 1.0F, 1.0F);
        int c = rs.receiverColor;
        if (c != 0xFFFFFF) {
            render(ModStandaloneModels.vesselContents(2), poseStack, nodeCollector, rs,
                    ((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F, (c & 0xFF) / 255.0F);
        }
        poseStack.popPose();
    }

    /** Type code for an inserted instrument stack. */
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
        if (path.equals("long_stem_funnel")) {
            return 5;
        }
        if (path.equals("separatory_funnel")) {
            return 6;
        }
        return 0;
    }

    /** Type code for a vessel sitting on the ring (1-4). */
    private static int vesselType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return switch (path) {
            case "round_bottom_flask" -> 1;
            case "erlenmeyer_flask" -> 2;
            case "crucible" -> 3;
            case "evaporating_dish" -> 4;
            default -> 0;
        };
    }

    /** Draw one instrument through the stopper hole (slot 1 or 2). */
    private void renderAttached(IronStandRenderState rs, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, int slot, double holeOffset) {
        int type = slot == 1 ? rs.attached1 : rs.attached2;
        if (type == 0) {
            return;
        }
        double angle = Math.toRadians(rs.rotation * 45.0);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double mx = 8.5 + 0.3333 * cos - 2.5 * sin;
        double my = 9.5 + 0.3333 * sin + 2.5 * cos;
        double zOffset = (slot == 1 && rs.stopperHoles < 2) ? 0.0 : holeOffset;
        poseStack.pushPose();
        poseStack.translate((float) (mx / 16.0), (float) (my / 16.0), (float) ((8.8333 + zOffset) / 16.0));
        // Straight tube / dropper / right-angle tube: model +Y along the mouth
        // direction; the right-angle arm runs along -X so the same Z-rotation
        // maps it to the downward perpendicular of the mouth.
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(angle)));
        render(ModStandaloneModels.attachedModel(type),
                poseStack, nodeCollector, rs, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    private static void render(BlockStateModel model, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, IronStandRenderState renderState,
            float r, float g, float b) {
        if (model == null) {
            return;
        }
        nodeCollector.submitBlockModel(poseStack, RenderType.cutout(), model,
                r, g, b, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }

}
