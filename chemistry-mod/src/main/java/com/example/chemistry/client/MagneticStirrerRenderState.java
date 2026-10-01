package com.example.chemistry.client;

import net.minecraft.core.Direction;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class MagneticStirrerRenderState extends BlockEntityRenderState {
    public VesselVisualState vesselVisual = VesselVisualState.EMPTY;

    /** 方块朝向（操作面板朝南为模型默认朝向）。 */
    public Direction facing = Direction.NORTH;
    public int vesselType;
    public int color = 0xFFFFFF;
    public boolean hasBar;
    /** 三颈瓶玻璃塞位掩码。 */
    public int vesselStoppers;
}
