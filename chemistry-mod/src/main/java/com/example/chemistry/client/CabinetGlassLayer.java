package com.example.chemistry.client;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
/** Transparent doors must not mask translucent inventory that flushes in a later batch. */
public final class CabinetGlassLayer {
    public static final RenderType TYPE=RenderType.create("mchemistry_cabinet_glass",1536,false,true,
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("mchemistry","pipeline/cabinet_glass"))
                    .withVertexShader("core/rendertype_translucent_moving_block")
                    .withFragmentShader("core/rendertype_translucent_moving_block")
                    .withSampler("Sampler0").withSampler("Sampler2")
                    .withBlend(BlendFunction.TRANSLUCENT).withDepthWrite(false)
                    .withVertexFormat(DefaultVertexFormat.BLOCK,VertexFormat.Mode.QUADS).build(),
            RenderType.CompositeState.builder().setLightmapState(RenderStateShard.LIGHTMAP)
                    .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
                    .setOutputState(RenderStateShard.MAIN_TARGET).createCompositeState(false));
    public static final RenderType ITEM = RenderType.create("mchemistry_lab_glass_item",1536,false,true,
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("mchemistry","pipeline/lab_glass_item"))
                    .withSampler("Sampler1").withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthWrite(false).withCull(false).build(),
            RenderType.CompositeState.builder().setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
                    .setOutputState(RenderStateShard.MAIN_TARGET).createCompositeState(false));
    private CabinetGlassLayer(){}
}
