package com.example.chemistry.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class GasCollectingBottleRenderState extends BlockEntityRenderState {

    /** Collected gas fill (0..1) shown inside the bottle. */
    public float fill;
    /** Gas colour as packed RGB int (0xRRGGBB). */
    public int gasColor = 0xFFFFFF;
    /** True when the bottle mouth points down (向下排空气法). */
    public boolean inverted;
}
