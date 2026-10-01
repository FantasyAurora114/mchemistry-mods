package com.example.chemistry.client;

import net.minecraft.core.Direction;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class MagneticStirrerEntityRenderState extends EntityRenderState {
    public VesselVisualState vesselVisual = VesselVisualState.EMPTY;

    /** 机器朝向（操作面板朝南为模型默认朝向）。 */
    public Direction facing = Direction.NORTH;
    public int vesselType;
    public int color = 0xFFFFFF;
    public boolean hasBar;
    /** 三颈瓶玻璃塞位掩码。 */
    public int vesselStoppers;
}
