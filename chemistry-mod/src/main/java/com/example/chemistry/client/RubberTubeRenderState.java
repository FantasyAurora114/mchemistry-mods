package com.example.chemistry.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

public class RubberTubeRenderState extends EntityRenderState {

    public boolean supply;
    public java.util.List<Vec3> curve = java.util.List.of();
    public Vec3 a = Vec3.ZERO;
    public Vec3 b = Vec3.ZERO;
    public boolean validA;
    public boolean validB;
    /** Draw a thick rubber sleeve capping the glass-tube head / nozzle. */
    public boolean sleeveA;
    public boolean sleeveB;
    /** Glass-tube axis at each end (world unit direction), or null. */
    public Vec3 axisA;
    public Vec3 axisB;
}
