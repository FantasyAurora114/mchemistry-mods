package com.example.chemistry.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class PlacedVesselRenderState extends BlockEntityRenderState {

    /** 0 = none, 2 = erlenmeyer (extensible). */
    public int vesselType;
    public int color = 0xFFFFFF;
    public boolean vesselSealed;
    /** 三颈瓶已塞的玻璃塞数量（0-3，从左到右）。 */
    public int vesselStoppers;
    /** 三颈瓶每颈的橡胶塞孔数（0 = 无）。 */
    public int[] rubberHoles = new int[3];
    public int attached1;
    public int attached2;
}
