package com.example.chemistry.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class GasCollectingBottleEntityRenderState extends EntityRenderState {

    public float lift;
    public float yaw;
    public boolean inverted;
    public boolean hasPlate;
    public boolean hasNozzle;
    public int tubeType;
    public float water;
    public float fill;
    public int gasColor = 0xFFFFFF;
}
