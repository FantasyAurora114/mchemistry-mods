package com.example.chemistry.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class WaterTroughRenderState extends BlockEntityRenderState {

    public boolean hasBottle;
    /** Water remaining in the inverted bottle (0..1). */
    public float waterFill;
}
