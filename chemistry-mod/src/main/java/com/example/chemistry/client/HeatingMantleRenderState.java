package com.example.chemistry.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class HeatingMantleRenderState extends BlockEntityRenderState {
    public VesselVisualState vesselVisual = VesselVisualState.EMPTY;

    /** 烧瓶类型：1=圆底烧瓶，2=锥形瓶，0=无。 */
    public int vesselType;
    /** 内容物颜色（0xRRGGBB）。 */
    public int color = 0xFFFFFF;
    /** 三颈瓶玻璃塞位掩码（0 = 无）。 */
    public int vesselStoppers;
}
