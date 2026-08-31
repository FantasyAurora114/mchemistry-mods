package com.example.chemistry.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class WaterTroughRenderState extends BlockEntityRenderState {

    public boolean hasBottle;
    /** Water remaining in the inverted bottle (0..1). */
    public float waterFill;
    /** 冰浴中浸泡的烧瓶类型：1=圆底烧瓶，2=锥形瓶，0=无。 */
    public int vesselType;
    /** 内容物颜色（0xRRGGBB）。 */
    public int color = 0xFFFFFF;
}
