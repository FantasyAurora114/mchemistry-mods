package com.example.chemistry.client;

import com.example.chemistry.DistillationAssembly;
import com.example.chemistry.entity.DistillationPartEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Renders each glass component in its own world-space entity frame. */
public class DistillationPartEntityRenderer
        extends EntityRenderer<DistillationPartEntity, DistillationPartRenderState> {
    public DistillationPartEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public DistillationPartRenderState createRenderState() {
        return new DistillationPartRenderState();
    }

    @Override
    public void extractRenderState(DistillationPartEntity entity,
            DistillationPartRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.kind = entity.kind();
        state.assemblyYaw = entity.assemblyYaw();
        state.steam = entity.hasSteam();
        state.flowing = entity.hasFlow();
    }

    @Override
    public void submit(DistillationPartRenderState state, PoseStack pose,
            SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0, 0.3, 0);
        pose.mulPose(Axis.YP.rotationDegrees(state.assemblyYaw));
        switch (state.kind) {
            case DistillationPartEntity.HEAD -> {
                pose.scale(DistillationAssembly.HEAD_SCALE,
                        DistillationAssembly.HEAD_SCALE, DistillationAssembly.HEAD_SCALE);
                pose.translate(-7.0 / 16.0, -0.75 / 16.0, -8.0 / 16.0);
                submitModel(ModStandaloneModels.distillationHead(), state, pose, collector);
                if (state.steam) {
                    submitModel(ModStandaloneModels.distillationSteam(), state, pose, collector);
                }
            }
            case DistillationPartEntity.CONDENSER -> {
                // Entity origin is at the lower joint for a clickable outlet.
                pose.translate(-Math.sqrt(0.5) * DistillationAssembly.MODEL_SCALE / 2,
                        Math.sqrt(0.5) * DistillationAssembly.MODEL_SCALE / 2, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(45.0F));
                pose.scale(DistillationAssembly.MODEL_SCALE,
                        DistillationAssembly.MODEL_SCALE, DistillationAssembly.MODEL_SCALE);
                pose.translate(-0.5, -0.5, -0.5);
                // The water_* layer remains hidden until cooling-water equipment exists.
                submitModel(ModStandaloneModels.condenser(), state, pose, collector);
                if(state.flowing)submitModel(ElectricalModels.get("straight_condenser_water"),state,pose,collector);
            }
            case DistillationPartEntity.ADAPTER_BENT -> {
                pose.scale(DistillationAssembly.BENT_ADAPTER_SCALE,
                        DistillationAssembly.BENT_ADAPTER_SCALE,
                        DistillationAssembly.BENT_ADAPTER_SCALE);
                pose.translate(-9.5 / 16.0, -0.75 / 16.0, -8.0 / 16.0);
                CowHornAdapterRenderer.draw(pose, collector, state.lightCoords, state.flowing);
            }
            case DistillationPartEntity.ADAPTER_STRAIGHT -> {
                pose.translate(0, 0.3375, 0);
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                pose.scale(0.6F, 0.6F, 0.6F);
                pose.translate(-8.5 / 16.0, -12.0 / 16.0, -7.5 / 16.0);
                submitModel(ModStandaloneModels.receiverAdapter(false), state, pose, collector);
            }
            case DistillationPartEntity.THERMOMETER -> {
                pose.scale(0.5F, 0.5F, 0.5F);
                submitModel(ModStandaloneModels.attachedModel(7), state, pose, collector);
            }
            default -> { }
        }
        pose.popPose();
    }

    private static void submitModel(BlockStateModel model, DistillationPartRenderState state,
            PoseStack pose, SubmitNodeCollector collector) {
        if (model != null) {
            collector.submitBlockModel(pose, CabinetGlassLayer.TYPE, model,
                    1.0F, 1.0F, 1.0F, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
    }
}
