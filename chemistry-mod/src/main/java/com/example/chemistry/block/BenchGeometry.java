package com.example.chemistry.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Shared model rotation for drawing, collision and interaction; model units are sixteenths. */
public final class BenchGeometry {
    public static float rotation(BlockState state) {
        return switch(state.getValue(LaboratoryBenchBlock.FACING)) {
            case SOUTH -> 180; case EAST -> -90; case WEST -> 90; default -> 0;
        };
    }
    public static Vec3 world(BlockState state,BlockPos pos,Vec3 model) {
        double a=Math.toRadians(rotation(state)),x=model.x/16-.5,z=model.z/16-.5;
        return new Vec3(pos.getX()+.5+x*Math.cos(a)+z*Math.sin(a),
                pos.getY()+model.y/16,pos.getZ()+.5-x*Math.sin(a)+z*Math.cos(a));
    }
    public static Vec3 local(BlockState state,BlockPos pos,Vec3 world) {
        double a=Math.toRadians(-rotation(state)),x=world.x-pos.getX()-.5,z=world.z-pos.getZ()-.5;
        return new Vec3((x*Math.cos(a)+z*Math.sin(a)+.5)*16,
                (world.y-pos.getY())*16,(-x*Math.sin(a)+z*Math.cos(a)+.5)*16);
    }
    private BenchGeometry(){}
}
