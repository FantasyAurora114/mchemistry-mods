package com.example.chemistry.entity;

import com.example.chemistry.VesselHeating;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.block.PlacedVesselBlock;
import com.example.chemistry.block.WaterTroughBlock;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.blockentity.PlacedVesselBlockEntity;
import com.example.chemistry.blockentity.WaterTroughBlockEntity;
import com.example.chemistry.entity.RubberTubeEntity.Port;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.TestTubeItem;
import com.example.chemistry.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * World positions of rubber-tube anchors, shared by the server (spawn points,
 * flow bubbles, pick boxes, rupture particles) and the client renderer, so a
 * tube always starts and ends exactly at the glass-tube head / gas-nozzle tip
 * instead of the block centre. The renderer uses the same methods, keeping the
 * two sides in lockstep.
 */
public final class AnchorPositions {

    private AnchorPositions() {
    }

    /** World position of any anchor (block face centre, glass-tube head,
     *  nozzle tip, or entity position). */
    public static Vec3 anchorWorldPos(Level level, Port anchor) {
        if (anchor == null) {
            return null;
        }
        return switch (anchor.kind()) {
            case Port.KIND_BLOCK -> blockFaceCenter(anchor);
            case Port.KIND_STAND -> standHead(level, anchor);
            case Port.KIND_NOZZLE -> nozzleTip(level, anchor);
            default -> {
                Entity e = level.getEntity(anchor.uuid());
                yield e != null ? e.position() : null;
            }
        };
    }

    /** Centre of a clicked block face. */
    private static Vec3 blockFaceCenter(Port anchor) {
        BlockPos p = anchor.pos();
        Direction f = anchor.face();
        return new Vec3(p.getX() + 0.5 + f.getStepX() * 0.5,
                p.getY() + 0.5 + f.getStepY() * 0.5,
                p.getZ() + 0.5 + f.getStepZ() * 0.5);
    }

    /** Tip of a gas nozzle: a 45-degree one standing in a water trough, or one
     *  inserted in a placed gas bottle. The bottle nozzle passes through the
     *  mouth with the arm + pointed tip inside the bottle; a short stub sticks
     *  out above the mouth where the rubber tube connects. */
    public static Vec3 nozzleTip(Level level, Port anchor) {
        BlockPos pos = anchor.pos();
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.GAS_COLLECTING_BOTTLE.get())
                && state.getValue(GasCollectingBottleBlock.HAS_NOZZLE)) {
            // Bottle nozzle: stem through the mouth, connector stub above
            // (x/z 7.8, y 8.5). Upside-down bottles keep the stub sticking out
            // BELOW the mouth — the rubber tube connects to that outer end.
            if (state.getValue(GasCollectingBottleBlock.INVERTED)) {
                return new Vec3(pos.getX() + 7.8 / 16.0, pos.getY() - 1.4 / 16.0,
                        pos.getZ() + 7.8 / 16.0);
            }
            return new Vec3(pos.getX() + 7.8 / 16.0, pos.getY() + 8.5 / 16.0,
                    pos.getZ() + 7.8 / 16.0);
        }
        if (state.getBlock() instanceof WaterTroughBlock && anchor.face() == Direction.UP
                && level.getBlockEntity(pos) instanceof WaterTroughBlockEntity be
                && be.hasBottle()) {
            // 排水法: the inverted bottle's mouth sits just under the surface.
            return new Vec3(pos.getX() + 7.8 / 16.0, pos.getY() + 3.2 / 16.0,
                    pos.getZ() + 7.8 / 16.0);
        }
        Direction face = anchor.face();
        double tx = switch (face) {
            case EAST -> 1.0;
            case WEST -> -1.0;
            default -> 0.0;
        };
        double tz = switch (face) {
            case SOUTH -> 1.0;
            case NORTH -> -1.0;
            default -> 0.0;
        };
        double len = 3.0 * Math.sin(Math.toRadians(45.0)) / 16.0;
        double rise = 3.0 * Math.cos(Math.toRadians(45.0)) / 16.0;
        return new Vec3(pos.getX() + 0.5 + tx * len,
                pos.getY() + 2.5 / 16.0 + rise,
                pos.getZ() + 0.5 + tz * len);
    }

    /** World position of a glass-tube delivery head on an iron stand (tube or
     *  vessel stopper) or on a vessel placed on the ground. */
    public static Vec3 standHead(Level level, Port anchor) {
        BlockPos standPos = anchor.pos();
        BlockState state = level.getBlockState(standPos);
        int type = 0;
        int holes = 0;
        boolean vesselCase = false;
        double mouthX = 0;
        double mouthY = 0;
        double mouthZ = 0;
        if (state.is(ModBlocks.IRON_STAND.get())) {
            if (level.getBlockEntity(standPos) instanceof IronStandBlockEntity be) {
                ItemStack s = anchor.slot() == 1 ? be.getAttached1() : be.getAttached2();
                type = attachedType(s);
                if (!be.getVessel().isEmpty()) {
                    vesselCase = true;
                    holes = VesselHeating.getStopperHoles(be.getVessel());
                    int vt = vesselType(be.getVessel());
                    double baseY = vt == 1 ? 1.0 : 0.0;
                    double topY = switch (vt) {
                        case 1 -> 9.0;
                        case 2 -> 9.0;
                        case 3 -> 6.0;
                        default -> 2.0;
                    };
                    mouthX = 8.5;
                    mouthY = 9.5 + (topY - baseY) * 0.6;
                    mouthZ = 9.0;
                } else {
                    holes = be.getTube().getItem() instanceof TestTubeItem tt
                            ? tt.stopperHoles() : 0;
                }
            }
        } else if (state.is(ModBlocks.PLACED_VESSEL.get())) {
            if (level.getBlockEntity(standPos) instanceof PlacedVesselBlockEntity be) {
                ItemStack s = anchor.slot() == 1 ? be.getAttached1() : be.getAttached2();
                type = attachedType(s);
                vesselCase = true;
                holes = VesselHeating.getStopperHoles(be.getVessel());
                mouthX = 8.5;
                mouthY = 9.0;
                mouthZ = 8.5;
            }
        }
        if (type == 0) {
            return null;
        }
        if (vesselCase) {
            double hole = anchor.slot() == 1 ? (holes < 2 ? 0.0 : -0.55) : 0.55;
            double hx = mouthX;
            double hy = mouthY + 4.0;
            if (type != 1) {
                // Right-angle / long tube: the head is the outer arm tip
                // (arm 2.0 long, lifted 1.375 above the mouth).
                hx = mouthX - 2.0;
                hy = mouthY + 1.375;
            }
            double hz = mouthZ + hole;
            double lx = hx / 16.0;
            double ly = hy / 16.0;
            double lz = hz / 16.0;
            if (state.is(ModBlocks.IRON_STAND.get())) {
                Direction facing = state.getValue(IronStandBlock.FACING);
                double yaw = Math.toRadians(-facing.toYRot());
                double c = Math.cos(yaw);
                double s2 = Math.sin(yaw);
                double wx = standPos.getX() + 0.5 + (lx - 0.5) * c + (lz - 0.5) * s2;
                double wz = standPos.getZ() + 0.5 - (lx - 0.5) * s2 + (lz - 0.5) * c;
                return new Vec3(wx, standPos.getY() + ly, wz);
            }
            return new Vec3(standPos.getX() + lx, standPos.getY() + ly,
                    standPos.getZ() + lz);
        }
        int rotation = state.getValue(IronStandBlock.ROTATION);
        Direction facing = state.getValue(IronStandBlock.FACING);
        double angle = Math.toRadians(rotation * 45.0);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        // Match the rendered glass tube: slot 1 sits centred with a 1-hole stopper.
        double hole = anchor.slot() == 1 ? (holes < 2 ? 0.0 : 0.55) : -0.55;
        double mx = 8.5 + 0.3333 * cos - 2.5 * sin;
        double my = 9.5 + 0.3333 * sin + 2.5 * cos;
        double mz = 8.8333 + hole;
        double hx;
        double hy;
        double hz = mz;
        if (type == 1) {
            hx = mx - sin * 4.0;
            hy = my + cos * 4.0;
        } else {
            double dx = sin >= 0 ? -cos : cos;
            double dy = sin >= 0 ? -sin : sin;
            // The rubber tube connects to the OUTSIDE arm of the 90-degree tube
            // (2.0 units, lifted 1.375 above the mouth); the long tube's extra
            // length goes down INTO the tube.
            double arm = 2.0;
            double lift = 1.375;
            hx = mx + dx * arm;
            hy = my + lift + dy * arm;
        }
        double lx = hx / 16.0;
        double ly = hy / 16.0;
        double lz = hz / 16.0;
        double yaw = Math.toRadians(-facing.toYRot());
        double c = Math.cos(yaw);
        double s2 = Math.sin(yaw);
        double wx = standPos.getX() + 0.5 + (lx - 0.5) * c + (lz - 0.5) * s2;
        double wz = standPos.getZ() + 0.5 - (lx - 0.5) * s2 + (lz - 0.5) * c;
        return new Vec3(wx, standPos.getY() + ly, wz);
    }

    private static int attachedType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof DropperItem) {
            return 3;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.equals("straight_glass_tube")) {
            return 1;
        }
        if (path.equals("right_angle_glass_tube")) {
            return 2;
        }
        if (path.equals("right_angle_glass_tube_long")) {
            return 4;
        }
        return 0;
    }

    /** 1 = flask, 2 = erlenmeyer, 3 = crucible, 4 = evaporating dish. */
    private static int vesselType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return switch (path) {
            case "round_bottom_flask" -> 1;
            case "erlenmeyer_flask" -> 2;
            case "crucible" -> 3;
            case "evaporating_dish" -> 4;
            default -> 0;
        };
    }
}
