package com.example.chemistry.client;

import com.example.chemistry.VesselHeating;
import com.example.chemistry.entity.PlacedVesselEntity;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.TestTubeItem;
import com.example.chemistry.item.LabVesselItem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** 落地容器（锥形瓶等，实体版）：复用旧 PlacedVesselRenderer 的画法。 */
public class PlacedVesselEntityRenderer
        extends EntityRenderer<PlacedVesselEntity, PlacedVesselEntityRenderState> {

    public PlacedVesselEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public PlacedVesselEntityRenderState createRenderState() {
        return new PlacedVesselEntityRenderState();
    }

    @Override
    public void extractRenderState(PlacedVesselEntity entity,
            PlacedVesselEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        ItemStack vessel = entity.getVessel();
        state.vesselType = vesselType(vessel);state.watchGlass=com.example.chemistry.organic.OrganicApparatus.covered(vessel);
        var child=com.example.chemistry.utility.BeakerWaterBath.inner(entity.level(),vessel);
        state.bathType=child.isEmpty()?0:child.getItem() instanceof TestTubeItem?100:vesselType(child);
        state.bathDewar=child.getItem() instanceof TestTubeItem t&&t.isDewar();
        state.bathVisual=VesselVisualState.of(child);
        state.color = LabVesselItem.contentsColor(vessel);
        state.vesselVisual = VesselVisualState.of(vessel);
        state.garden=com.example.chemistry.garden.ChemicalGarden.stems(vessel);
        state.vesselSealed = VesselHeating.isSealed(vessel);
        state.vesselStoppers = VesselHeating.neckStopperMask(vessel);
        for (int i = 0; i < 3; i++) {
            state.rubberHoles[i] = VesselHeating.rubberHoles(vessel, i);
        }
        state.attached1 = attachedType(entity.getAttached1());
        state.attached2 = attachedType(entity.getAttached2());
        state.mountScale = entity.getMountScale();
        state.mountOffX = (float) entity.getMountOffX();
        state.mountOffY = (float) entity.getMountOffY();
        state.mountOffZ = (float) entity.getMountOffZ();
        state.mountYaw = entity.getMountYaw();
    }

    @Override
    public void submit(PlacedVesselEntityRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.vesselType == 0) {
            return;
        }
        poseStack.pushPose();
        if (state.mountScale < 0.9F) {
            // 挂载：实体位置在铁圈上方（y + mountOffY），先回到铁架台方块角。
            poseStack.translate(-0.5F, -state.mountOffY, -0.5F);
        } else {
            poseStack.translate(-0.5F, 0.0F, -0.5F);
        }
        // 挂载偏移 + 绕方块中心旋转 + 缩放（落地时 scale=1、偏移/旋转=0，退化为原地）。
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(state.mountYaw));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        poseStack.translate(state.mountOffX, state.mountOffY, state.mountOffZ);
        poseStack.scale(state.mountScale, state.mountScale, state.mountScale);
        if (state.vesselType == 1) {
                SingleNeckRenderer.draw(poseStack, collector, state.vesselVisual, state.lightCoords);
            } else if (state.vesselType == 2) {
            ErlenmeyerRenderer.draw(poseStack, collector,
                    state.vesselVisual, state.lightCoords);
        } else {
            BlockStateModel model = ModStandaloneModels.vessel(state.vesselType);
            if (model != null) {
                collector.submitBlockModel(poseStack, (state.vesselType == 6 || state.vesselType == 5 || state.vesselType >= 8) ? CabinetGlassLayer.TYPE : RenderType.cutout(), model,
                        1.0F, 1.0F, 1.0F, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
            VesselContentRenderer.draw(poseStack, collector,
                    state.vesselType, state.vesselVisual, state.lightCoords);
        }
        if(state.bathType>0){
            poseStack.pushPose();
            if(state.bathType==100){
                poseStack.translate(.5,.05,.5);poseStack.scale(.62F,.62F,.62F);
                var tube=ModStandaloneModels.testTube(0,state.bathDewar);
                poseStack.translate(-8.5/16,-5.8/16,-8.8/16);
                if(tube!=null)collector.submitBlockModel(poseStack,CabinetGlassLayer.TYPE,tube,1,1,1,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
                if(state.bathVisual.liquidFill()>0){
                    var contents=ModStandaloneModels.part(0,ModStandaloneModels.PART_CONTENTS);int c=state.bathVisual.liquidColor();
                    if(contents!=null)collector.submitBlockModel(poseStack,CabinetGlassLayer.TYPE,contents,((c>>16)&255)/255F,((c>>8)&255)/255F,(c&255)/255F,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
                }
            }else{
                poseStack.translate(.5,.04,.5);poseStack.scale(.5F,.5F,.5F);poseStack.translate(-.5,0,-.5);
                var model=ModStandaloneModels.vessel(state.bathType);
                if(model!=null)collector.submitBlockModel(poseStack,CabinetGlassLayer.TYPE,model,1,1,1,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
                VesselContentRenderer.draw(poseStack,collector,state.bathType,state.bathVisual,state.lightCoords);
            }
            poseStack.popPose();
        }
        WatchGlassRenderer.draw(poseStack,collector,state.vesselType,state.watchGlass,state.lightCoords);
        GardenRenderer.draw(poseStack,collector,state.garden,state.vesselType,state.lightCoords,state.vesselVisual.liquidFill());
        if (state.vesselSealed) {
            if (state.vesselType == 6) {
                int instrumentNeck = -1;
                for (int n = 0; n < 3; n++) {
                    if (state.rubberHoles[n] > 0) {
                        instrumentNeck = n;
                        break;
                    }
                }
                for (int n = 0; n < 3; n++) {
                    if (state.rubberHoles[n] == 0) {
                        continue;
                    }
                    poseStack.pushPose();
                    double[] neck = VesselHeating.THREE_NECK[n];
                    poseStack.translate(neck[0] / 16.0F, neck[1] / 16.0F, 8.5F / 16.0F);
                    if (n == 0) {
                        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(com.example.chemistry.ThreeNeckGeometry.SIDE_ANGLE));
                    } else if (n == 2) {
                        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-com.example.chemistry.ThreeNeckGeometry.SIDE_ANGLE));
                    }
                    submitStopper(state, poseStack, collector, n == instrumentNeck);
                    poseStack.popPose();
                }
            } else {
                poseStack.pushPose();
                poseStack.translate(8.5F / 16.0F, (float) VesselHeating.mouthTopY(state.vesselType) / 16.0F, 8.5F / 16.0F);
                submitStopper(state, poseStack, collector, true);
                poseStack.popPose();
            }
        }
        PlacedVesselRenderer.drawNeckStoppers(state.vesselStoppers, poseStack, collector,
                state.lightCoords);
        poseStack.popPose();
    }

    private void submitStopper(PlacedVesselEntityRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, boolean withInstruments) {
        BlockStateModel stopper = ModStandaloneModels.vesselStopper();
        if (stopper != null) {
            collector.submitBlockModel(poseStack, RenderType.cutout(), stopper,
                    1.0F, 1.0F, 1.0F, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        if (withInstruments && state.attached1 != 0) {
            BlockStateModel m = ModStandaloneModels.attachedModel(state.attached1);
            if (m != null) {
                poseStack.pushPose();
                poseStack.translate(0.0F, 0.0F,
                        (state.attached2 != 0 ? -0.55F : 0.0F) / 16.0F);
                collector.submitBlockModel(poseStack, RenderType.cutout(), m,
                        1.0F, 1.0F, 1.0F, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }
        if (withInstruments && state.attached2 != 0) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 0.55F / 16.0F);
            BlockStateModel m = ModStandaloneModels.attachedModel(state.attached2);
            if (m != null) {
                collector.submitBlockModel(poseStack, RenderType.cutout(), m,
                        1.0F, 1.0F, 1.0F, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
            poseStack.popPose();
        }
    }

    private static int vesselType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.equals("thermometer")) {
            return 7;
        }
        return VesselHeating.vesselType(stack);
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
            case "straight_glass_tube" -> 1;
            case "straight_glass_tube_long" -> 8;
            case "right_angle_glass_tube" -> 2;
            case "right_angle_glass_tube_long" -> 4;
            case "long_stem_funnel" -> 5;
            case "separatory_funnel" -> 6;
            default -> 0;
        };
    }
}
