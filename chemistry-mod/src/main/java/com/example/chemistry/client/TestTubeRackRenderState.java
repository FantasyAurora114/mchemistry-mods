package com.example.chemistry.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class TestTubeRackRenderState extends BlockEntityRenderState {

    public final boolean[] hasTube = new boolean[5];
    public final boolean[] inverted = new boolean[5];
    public final int[] color = new int[5];
}
