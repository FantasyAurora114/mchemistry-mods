package com.example.chemistry.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class TripodRenderState extends BlockEntityRenderState {

    public boolean hasClayTriangle;
    public boolean hasLamp;
    public boolean lampLit;
    /** 0 = none, 3 = crucible, 4 = evaporating dish. */
    public int vesselType;
    public int vesselColor = 0xFFFFFF;
}
