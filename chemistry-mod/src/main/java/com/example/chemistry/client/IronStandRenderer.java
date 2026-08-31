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
        renderState.lampBlowtorch = blockEntity.isLampBlowtorch();
        renderState.lampCapped = blockEntity.isLampCapped();
        renderState.attached1 = attachedType(blockEntity.getAttached1());
        renderState.attached2 = attachedType(blockEntity.getAttached2());
        renderState.stopperHoles = blockEntity.getTube().getItem() instanceof TestTubeItem tt
                ? tt.stopperHoles() : 0;
        renderState.contentsColor = LabVesselItem.contentsColor(blockEntity.getTube());
        renderState.attachment = state.getValue(IronStandBlock.ATTACHMENT);
        renderState.vesselType = vesselType(blockEntity.getVessel());
        renderState.vesselColor = LabVesselItem.contentsColor(blockEntity.getVessel());
        renderState.hasCondenser = blockEntity.hasCondenser();
        renderState.hasDistillationHead = blockEntity.hasDistillationHead();
        renderState.headThermometer = blockEntity.hasHeadThermometer();
        renderState.hasReceiver = !blockEntity.getReceiver().isEmpty();
        renderState.hasReceiverAdapter = blockEntity.hasReceiverAdapter();
        ItemStack adapter = blockEntity.getReceiverAdapter();
        renderState.receiverAdapterBent = adapter.is(
                com.example.chemistry.registry.ModItems.RECEIVER_ADAPTER_BENT.get());
        renderState.receiverColor = LabVesselItem.contentsColor(blockEntity.getReceiver());
        renderState.vesselSealed = VesselHeating.isSealed(blockEntity.getVessel());
        renderState.vesselStoppers = VesselHeating.neckStopperMask(blockEntity.getVessel());
        for (int i = 0; i < 3; i++) {
            renderState.rubberHoles[i] = VesselHeating.rubberHoles(blockEntity.getVessel(), i);
        }
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
            if (renderState.hasDistillationHead) {
                renderDistillationHead(renderState, poseStack, nodeCollector);
            }
            if (renderState.vesselSealed) {
                renderVesselStopperAndAttached(renderState, poseStack, nodeCollector);
            }
            if (renderState.hasCondenser) {
                renderCondenser(poseStack, nodeCollector, renderState);
                if (renderState.hasReceiverAdapter) {
                    renderReceiverAdapter(poseStack, nodeCollector, renderState);
                }
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
            BlockStateModel lamp;
            if (renderState.lampBlowtorch) {
                lamp = blockRenderer.getBlockModel(
                        ModBlocks.ALCOHOL_BLOWTORCH.get().defaultBlockState()
                                .setValue(com.example.chemistry.block.AlcoholLampBlock.LIT,
                                        renderState.lampLit));
            } else {
                lamp = blockRenderer.getBlockModel(
                        ModBlocks.ALCOHOL_LAMP.get().defaultBlockState()
                                .setValue(com.example.chemistry.block.AlcoholLampBlock.LIT,
                                        renderState.lampLit)
                                .setValue(com.example.chemistry.block.AlcoholLampBlock.CAPPED,
                                        renderState.lampCapped));
            }
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
        if (rs.vesselType == 6) {
            int instrumentNeck = -1;
            for (int n = 0; n < 3; n++) {
                if (rs.rubberHoles[n] > 0) {
                    instrumentNeck = n;
                    break;
                }
            }
            for (int n = 0; n < 3; n++) {
                if (rs.rubberHoles[n] == 0) {
                    continue;
                }
                poseStack.pushPose();
                double[] neck = VesselHeating.THREE_NECK[n];
                double s = 0.6;
                double x = (8.5 - 8.5 * s + s * neck[0]) / 16.0;
                double y = (9.5 + s * neck[1]) / 16.0;
                poseStack.translate((float) x, (float) y, 9.0F / 16.0F);
                if (n == 0) {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(22.5F));
                } else if (n == 2) {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-22.5F));
                }
                renderStopper(rs, poseStack, nodeCollector, n == instrumentNeck);
                poseStack.popPose();
            }
        } else {
            poseStack.pushPose();
            double baseY = 0.0;
            double topY = switch (rs.vesselType) {
                case 1 -> 9.0;   // round-bottom flask
                case 2 -> 9.0;   // erlenmeyer flask
                case 3 -> 6.0;   // crucible
                case 5 -> 6.0;   // beaker
                case 6 -> 10.0;  // three-neck flask centre neck
                case 7 -> 10.0;  // flat-bottom flask
                default -> 2.0;  // evaporating dish
            };
            double mouthY = 9.5 + (topY - baseY) * 0.6;
            poseStack.translate(8.5F / 16.0F, (float) (mouthY / 16.0), 9.0F / 16.0F);
            renderStopper(rs, poseStack, nodeCollector, true);
            poseStack.popPose();
        }
    }

    private void renderStopper(IronStandRenderState rs, PoseStack poseStack,
            SubmitNodeCollector nodeCollector, boolean withInstruments) {
        render(ModStandaloneModels.vesselStopper(), poseStack, nodeCollector,
                rs, 1.0F, 1.0F, 1.0F);
        int type1 = rs.attached1;
        int type2 = rs.attached2;
        if (withInstruments && type1 != 0) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, (type2 != 0 ? -0.55F : 0.0F) / 16.0F);
            render(ModStandaloneModels.attachedModel(type1), poseStack, nodeCollector,
                    rs, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
        if (withInstruments && type2 != 0) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 0.55F / 16.0F);
            render(ModStandaloneModels.attachedModel(type2), poseStack, nodeCollector,
                    rs, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
    }

    private void renderVessel(IronStandRenderState rs, PoseStack poseStack,
            SubmitNodeCollector nodeCollector) {
        // Vessel sits on the ring: the ring is centred at local (8.5, 9.0)
        // with its top at y 9.5. Scale 0.6 around the MODEL centre (not the
        // origin), otherwise the scaled-down vessel drifts off the ring.
        double s = 0.6;
        double cx = (rs.vesselType == 3 || rs.vesselType == 4) ? 7.5 : 8.5;
        double cz = 8.5;
        // 圆底烧瓶与锥形瓶一样底座从 y=0 开始，正好坐在石棉网上（原来低 0.6/16 会沉进网里）。
        double baseY = 0.0;
        poseStack.pushPose();
        poseStack.translate((8.5 - cx * s) / 16.0, (9.5 - baseY * s) / 16.0,
                (9.0 - cz * s) / 16.0);
        poseStack.scale((float) s, (float) s, (float) s);
        if (rs.vesselType == 2) {
            ErlenmeyerRenderer.draw(poseStack, nodeCollector,
                    ModStandaloneModels.vessel(2),
                    ModStandaloneModels.erlenmeyerBodyUnit(),
                    ModStandaloneModels.erlenmeyerLiquidUnit(),
                    rs.vesselColor, rs.lightCoords);
        } else {
            render(ModStandaloneModels.vessel(rs.vesselType), poseStack, nodeCollector,
                    rs, 1.0F, 1.0F, 1.0F);
            int c = rs.vesselColor;
            if (c != 0xFFFFFF) {
                BlockStateModel contents = ModStandaloneModels.vesselContents(rs.vesselType);
                if (contents != null) {
                    nodeCollector.submitBlockModel(poseStack,
                            RenderType.translucentMovingBlock(), contents,
                            ((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F,
                            (c & 0xFF) / 255.0F, rs.lightCoords,
                            OverlayTexture.NO_OVERLAY, 0);
                }
            }
            PlacedVesselRenderer.drawNeckStoppers(rs.vesselStoppers, poseStack,
                    nodeCollector, rs.lightCoords);
        }
        poseStack.popPose();
    }

    /** Condenser tilted from the flask mouth down toward the front. */
    private void renderCondenser(PoseStack poseStack, SubmitNodeCollector nodeCollector,
            IronStandRenderState rs) {
        poseStack.pushPose();
        // 有蒸馏头时冷凝管从蒸馏头的侧管伸出（位置略高）。
        float cy = rs.hasDistillationHead ? 17.5F : 14.3F;
        poseStack.translate(8.5F / 16.0F, cy / 16.0F, 9.0F / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(18.0F));
        poseStack.translate(0.0F, -19.0F / 16.0F, 0.0F);
        render(ModStandaloneModels.condenser(), poseStack, nodeCollector, rs, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    /** 牛角管：从冷凝管出口伸向接收瓶（倾斜约 35°）。 */
    private void renderReceiverAdapter(PoseStack poseStack, SubmitNodeCollector nodeCollector,
            IronStandRenderState rs) {
        BlockStateModel model = ModStandaloneModels.receiverAdapter(rs.receiverAdapterBent);
        if (model == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(8.5F / 16.0F, 8.0F / 16.0F, 4.5F / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(35.0F));
        poseStack.translate(-8.5F / 16.0F, -7.0F / 16.0F, -4.5F / 16.0F);
        render(model, poseStack, nodeCollector, rs, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    /** 蒸馏头：接在圆底烧瓶瓶口上，竖直颈 + 侧向出气管。 */
    private void renderDistillationHead(IronStandRenderState rs, PoseStack poseStack,
            SubmitNodeCollector nodeCollector) {
        double baseY = 0.0;
        // 三颈瓶中心瓶口在模型 y=10，平底烧瓶瓶口在 y=10，圆底烧瓶瓶口在 y=9。
        double topY = switch (rs.vesselType) {
            case 6 -> 10.0;   // three-neck flask centre neck
            case 7 -> 10.0;   // flat-bottom flask
            default -> 9.0;   // round-bottom / ground-glass / erlenmeyer
        };
        double mouthY = 9.5 + (topY - baseY) * 0.6;
        poseStack.pushPose();
        // 模型底口中心在 (9, 0, 9)，缩放 0.5 后是 (4.5, 0, 4.5)，把它平移到烧瓶口 (8.5, mouthY, 9)。
        poseStack.translate(4.0F / 16.0F, (float) (mouthY / 16.0), 4.5F / 16.0F);
        // 蒸馏头体积缩小 2 倍，与烧瓶比例协调。
        poseStack.scale(0.5F, 0.5F, 0.5F);
        render(ModStandaloneModels.distillationHead(), poseStack, nodeCollector,
                rs, 1.0F, 1.0F, 1.0F);
        // 侧管带 115° 角（bbmodel 原值），方块元素不支持任意角度，用 PoseStack 绕其原点旋转。
        poseStack.pushPose();
        poseStack.translate(9.14154F / 16.0F, 2.36155F / 16.0F, 8.0F / 16.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(115.0F));
        poseStack.translate(-9.14154F / 16.0F, -2.36155F / 16.0F, -8.0F / 16.0F);
        render(ModStandaloneModels.distillationHeadArm(), poseStack, nodeCollector,
                rs, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        // 温度计插在蒸馏头顶端接口里（模型帧 (9,6,9) 向上伸出）。
        if (rs.headThermometer) {
            poseStack.pushPose();
            poseStack.translate(9.0F / 16.0F, 6.0F / 16.0F, 9.0F / 16.0F);
            render(ModStandaloneModels.attachedModel(7), poseStack, nodeCollector,
                    rs, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
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
        if (path.equals("thermometer")) {
            return 7;
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
        return VesselHeating.vesselType(stack);
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
