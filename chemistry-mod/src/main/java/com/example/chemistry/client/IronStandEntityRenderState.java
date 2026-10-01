package com.example.chemistry.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;

/** 铁架台本体（实体版）：底座 + 立杆 + 夹子 + 铁圈/石棉网/泥三角 + 试管。 */
public class IronStandEntityRenderState extends EntityRenderState {

    public Direction facing = Direction.SOUTH;
    public int rotation;
    public int rods;
    public float lift;
    public int attachment;
    public boolean dewar;
    public boolean hasTube;
    public boolean hasContents;
    public boolean hasStopper;
    public int contentsColor = 0xFFFFFF;
    public int stopperHoles;
    public int attached1;
    public int attached2;
}
