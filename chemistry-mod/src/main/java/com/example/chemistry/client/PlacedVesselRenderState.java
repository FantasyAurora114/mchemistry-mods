package com.example.chemistry.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class PlacedVesselRenderState extends BlockEntityRenderState {

    /** 0 = none, 2 = erlenmeyer (extensible). */
    public int vesselType;
    public int color = 0xFFFFFF;
    public boolean vesselSealed;
    public int attached1;
    public int attached2;
}
