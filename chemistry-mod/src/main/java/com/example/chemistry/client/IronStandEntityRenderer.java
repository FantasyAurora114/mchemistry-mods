package com.example.chemistry.client;

import com.example.chemistry.entity.IronStandEntity;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.TestTubeItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** 铁架台本体（实体版）：底座 + 立杆 + 夹子 + 铁圈/石棉网/泥三角 + 试管。 */
public class IronStandEntityRenderer
        extends EntityRenderer<IronStandEntity, IronStandEntityRenderState> {

    public IronStandEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public IronStandEntityRenderState createRenderState() {
        return new IronStandEntityRenderState();
    }

    @Override
    public void extractRenderState(IronStandEntity entity,
            IronStandEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.facing = entity.getFacing();
        state.rotation = entity.getRotation();
        state.rods = entity.extensionRods();
        state.lift = (float) entity.clampLift();
        state.attachment = entity.getAttachment();
        ItemStack tube = entity.getTube();
        state.hasTube = !tube.isEmpty();
        state.dewar=tube.getItem() instanceof TestTubeItem t&&t.isDewar();
        state.hasContents = state.hasTube && !LabVesselItem.getContents(tube).isEmpty();
        state.hasStopper = state.hasTube && tube.getItem() instanceof TestTubeItem tt
                && tt.stopperHoles() > 0;
        state.contentsColor = LabVesselItem.contentsColor(tube);
        state.stopperHoles = tube.getItem() instanceof TestTubeItem tt ? tt.stopperHoles() : 0;
        state.attached1 = attachedType(entity.getAttached1());
        state.attached2 = attachedType(entity.getAttached2());
    }

    @Override
    public void submit(IronStandEntityRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        render(ElectricalModels.get("iron_stand_base_"+state.rotation),
                poseStack, collector, state, 1.0F, 1.0F, 1.0F);
        for (int i=0;i<state.rods;i++) {
            ElectroDeviceRenderer.box(poseStack,collector,state.lightCoords,8.5,16+i*16,11.5,1,16,1,0x9AA8AE,false);
            ElectroDeviceRenderer.box(poseStack,collector,state.lightCoords,8.5,15.5+i*16,11.5,1.5,1.5,1.5,0x53656F,false);
        }
        poseStack.translate(0,state.lift,0);
        render(ElectricalModels.get("iron_stand_clip_"+state.rotation),poseStack,collector,state,1,1,1);
        if (state.attachment != 0) {
            render(ModStandaloneModels.attachment(state.attachment),
                    poseStack, collector, state, 1.0F, 1.0F, 1.0F);
        }
        if (state.hasTube) {
            renderGlass(ModStandaloneModels.testTube(state.rotation,state.dewar),
                    poseStack, collector, state, 1.0F, 1.0F, 1.0F);
        }
        if (state.hasContents) {
            int c = state.contentsColor;
            renderGlass(ModStandaloneModels.part(state.rotation, ModStandaloneModels.PART_CONTENTS),
                    poseStack, collector, state,
                    ((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F, (c & 0xFF) / 255.0F);
        }
        if (state.hasStopper) {
            render(ModStandaloneModels.part(state.rotation, ModStandaloneModels.PART_STOPPER),
                    poseStack, collector, state, 1.0F, 1.0F, 1.0F);
        }
        if (state.hasTube && state.hasStopper) {
            renderAttached(state, poseStack, collector, 1, 0.55);
            renderAttached(state, poseStack, collector, 2, -0.55);
        }
        poseStack.popPose();
    }

    private void renderAttached(IronStandEntityRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, int slot, double holeOffset) {
        int type = slot == 1 ? state.attached1 : state.attached2;
        if (type == 0) {
            return;
        }
        double angle = Math.toRadians(state.rotation * 45.0);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double mx = 8.5 + 0.3333 * cos - 2.5 * sin;
        double my = 9.5 + 0.3333 * sin + 2.5 * cos;
        double zOffset = (slot == 1 && state.stopperHoles < 2) ? 0.0 : holeOffset;
        poseStack.pushPose();
        poseStack.translate((float) (mx / 16.0), (float) (my / 16.0),
                (float) ((8.8333 + zOffset) / 16.0));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(angle)));
        render(ModStandaloneModels.attachedModel(type),
                poseStack, collector, state, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    private static int attachedType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof DropperItem) {
            return 3;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return switch (path) {
            case "thermometer" -> 7;
            case "straight_glass_tube" -> 1;
            case "straight_glass_tube_long" -> 8;
            case "right_angle_glass_tube" -> 2;
            case "right_angle_glass_tube_long" -> 4;
            case "long_stem_funnel" -> 5;
            case "separatory_funnel" -> 6;
            default -> 0;
        };
    }

    private static void renderGlass(BlockStateModel model, PoseStack poseStack,
            SubmitNodeCollector collector, IronStandEntityRenderState state,
            float r, float g, float b) {
        if (model == null) {
            return;
        }
        collector.submitBlockModel(poseStack, CabinetGlassLayer.TYPE, model,
                r, g, b, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }
    private static void render(BlockStateModel model, PoseStack poseStack,
            SubmitNodeCollector collector, IronStandEntityRenderState state,
            float r, float g, float b) {
        if (model == null) {
            return;
        }
        collector.submitBlockModel(poseStack, RenderType.cutout(), model,
                r, g, b, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }
}
