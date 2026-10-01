package com.example.chemistry;

import java.util.ArrayList;
import java.util.List;

import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.entity.DistillationPartEntity;
import com.example.chemistry.entity.IronStandEntity;
import com.example.chemistry.entity.PlacedVesselEntity;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModEntities;
import com.example.chemistry.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Placement, ownership and actual fluid routing for the entity-based apparatus. */
public final class DistillationAssembly {
    public static final float MODEL_SCALE = 0.6F;
    /** The head is narrower than the mounted flask and condenser. */
    public static final float HEAD_SCALE = 0.45F;
    public static final float BENT_ADAPTER_SCALE = 0.3F;
    private static final double BENT_INLET_X = 5.414213562373094;
    private static final double BENT_INLET_Y = 13.863961030678928;
    private static final double BENT_OUTLET_X = 9.5;
    private static final double BENT_OUTLET_Y = 0.75;
    private static final double ROOT2 = Math.sqrt(0.5);

    private DistillationAssembly() {
    }

    public static IronStandEntity standFor(PlacedVesselEntity vessel) {
        if (vessel.isReceiver() || vessel.getMountScale() >= 0.9F) {
            return null;
        }
        for (IronStandEntity stand : vessel.level().getEntitiesOfClass(IronStandEntity.class,
                vessel.getBoundingBox().inflate(1.25))) {
            if (stand.findMountedVessel() == vessel) {
                return stand;
            }
        }
        return null;
    }

    public static IronStandEntity standFor(DistillationPartEntity part) {
        for (IronStandEntity stand : part.level().getEntitiesOfClass(IronStandEntity.class,
                part.getBoundingBox().inflate(2.5))) {
            if (stand.getUUID().toString().equals(part.standId())) {
                return stand;
            }
        }
        return null;
    }

    public static DistillationPartEntity part(IronStandEntity stand, int kind) {
        if (stand == null) {
            return null;
        }
        for (DistillationPartEntity candidate : stand.level().getEntitiesOfClass(
                DistillationPartEntity.class, stand.getBoundingBox().inflate(2.5))) {
            if (candidate.kind() == kind && stand.getUUID().toString().equals(candidate.standId())) {
                return candidate;
            }
        }
        return null;
    }

    public static DistillationPartEntity adapter(IronStandEntity stand) {
        DistillationPartEntity bent = part(stand, DistillationPartEntity.ADAPTER_BENT);
        return bent != null ? bent : part(stand, DistillationPartEntity.ADAPTER_STRAIGHT);
    }

    public static PlacedVesselEntity receiver(IronStandEntity stand) {
        if (stand == null) {
            return null;
        }
        for (PlacedVesselEntity candidate : stand.level().getEntitiesOfClass(
                PlacedVesselEntity.class, stand.getBoundingBox().inflate(2.5))) {
            if (stand.getUUID().toString().equals(candidate.getReceiverStandId())
                    && !candidate.getVessel().isEmpty()) {
                return candidate;
            }
        }
        return null;
    }

    public static boolean isComplete(IronStandEntity stand) {
        return part(stand, DistillationPartEntity.HEAD) != null
                && part(stand, DistillationPartEntity.CONDENSER) != null
                && adapter(stand) != null && receiver(stand) != null;
    }

    public static PlacedVesselEntity receiverForSource(PlacedVesselEntity source) {
        IronStandEntity stand = standFor(source);
        return isComplete(stand) ? receiver(stand) : null;
    }

    public static boolean hasHead(PlacedVesselEntity source) {
        return part(standFor(source), DistillationPartEntity.HEAD) != null;
    }

    public static boolean isProducingSteam(DistillationPartEntity head) {
        IronStandEntity stand = standFor(head);
        PlacedVesselEntity source = stand == null ? null : stand.findMountedVessel();
        if (source == null || head.kind() != DistillationPartEntity.HEAD) {
            return false;
        }
        ItemStack vessel = source.getVessel();
        double temp = TemperatureSystem.getTemp(vessel);
        for (LabVesselItem.Entry entry : LabVesselItem.getContents(vessel)) {
            if (entry.type().equals("liquid") && entry.amount() > 0
                    && temp > com.example.chemistry.utility.VacuumState.boilingPoint(source.getVessel(),entry.id())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFlowing(DistillationPartEntity adapter) {
        IronStandEntity stand = standFor(adapter);
        if (stand == null || adapter.kind() != DistillationPartEntity.ADAPTER_BENT
                || !isComplete(stand)) return false;
        DistillationPartEntity head = part(stand, DistillationPartEntity.HEAD);
        return head != null && isProducingSteam(head);
    }

    /** The lower ground joint of the authored head sits on the mounted flask mouth. */
    public static Vec3 mouth(IronStandEntity stand) {
        PlacedVesselEntity source = stand == null ? null : stand.findMountedVessel();
        if (source == null) {
            return null;
        }
        ItemStack vessel = source.getVessel();
        if (VesselHeating.isThreeNeck(vessel)) {
            return VesselHeating.neckWorldPositions(stand.blockPosition(),
                    source.getMountScale(), source.getMountOffX(), source.getMountOffY(),
                    source.getMountOffZ(), source.getMountYaw())[1];
        }
        return VesselHeating.mouthWorldPosition(stand.blockPosition(),
                VesselHeating.vesselType(vessel), source.getMountScale(), source.getMountOffX(),
                source.getMountOffY(), source.getMountOffZ(), source.getMountYaw());
    }

    private static Vec3 outward(IronStandEntity stand, double x, double y) {
        double angle = Math.toRadians(-stand.getFacing().toYRot());
        return new Vec3(x * Math.cos(angle), y, -x * Math.sin(angle));
    }

    public static Vec3 headArm(IronStandEntity stand) {
        Vec3 base = mouth(stand);
        return base == null ? null : base.add(outward(stand,
                5.73 * HEAD_SCALE / 16.0, 4.02 * HEAD_SCALE / 16.0));
    }

    public static Vec3 headTop(IronStandEntity stand) {
        Vec3 base = mouth(stand);
        return base == null ? null : base.add(0, 14.5 * HEAD_SCALE / 16.0, 0);
    }

    public static Vec3 condenserEnd(IronStandEntity stand) {
        Vec3 arm = headArm(stand);
        return arm == null ? null : arm.add(outward(stand,
                ROOT2 * MODEL_SCALE, -ROOT2 * MODEL_SCALE));
    }

    public static Vec3 adapterOutlet(IronStandEntity stand) {
        DistillationPartEntity mounted = adapter(stand);
        return adapterOutletForKind(stand, mounted == null
                ? DistillationPartEntity.ADAPTER_BENT : mounted.kind());
    }

    private static Vec3 adapterOutletForKind(IronStandEntity stand, int kind) {
        Vec3 end = condenserEnd(stand);
        if (end == null) {
            return null;
        }
        return kind == DistillationPartEntity.ADAPTER_STRAIGHT
                ? end.add(0, -0.3375, 0)
                : end.add(outward(stand,
                        (BENT_OUTLET_X - BENT_INLET_X) * BENT_ADAPTER_SCALE / 16.0,
                        (BENT_OUTLET_Y - BENT_INLET_Y) * BENT_ADAPTER_SCALE / 16.0));
    }

    public static boolean nearRay(Player player, Vec3 point, double radius) {
        if (point == null) {
            return false;
        }
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(6));
        return new AABB(point.x - radius, point.y - radius, point.z - radius,
                point.x + radius, point.y + radius, point.z + radius)
                .clip(from, to).isPresent();
    }

    public static InteractionResult interactFromVessel(PlacedVesselEntity vessel,
            Player player, InteractionHand hand) {
        IronStandEntity stand = standFor(vessel);
        if (stand == null) {
            return InteractionResult.PASS;
        }
        return interact(stand, player, hand);
    }

    public static InteractionResult interact(DistillationPartEntity hit,
            Player player, InteractionHand hand) {
        IronStandEntity stand = standFor(hit);
        if (stand == null) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!player.level().isClientSide()) {
                removeWithDownstream(hit, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty()) {
            if (!player.level().isClientSide()) {
                remove(hit, player);
            }
            return InteractionResult.SUCCESS;
        }
        InteractionResult result = interact(stand, player, hand);
        return result == InteractionResult.PASS ? InteractionResult.SUCCESS : result;
    }

    public static InteractionResult interact(IronStandEntity stand,
            Player player, InteractionHand hand) {
        var sleeveResult = ThermometerSleeves.proxy(stand, player, hand);
        if (sleeveResult != InteractionResult.PASS) return sleeveResult;
        ItemStack held = player.getItemInHand(hand);
        PlacedVesselEntity source = stand.findMountedVessel();
        if (source == null) {
            return InteractionResult.PASS;
        }
        if (held.isEmpty()) {
            DistillationPartEntity target = null;
            if (part(stand, DistillationPartEntity.THERMOMETER) != null
                    && nearRay(player, headTop(stand), 0.25)) {
                target = part(stand, DistillationPartEntity.THERMOMETER);
            } else if (adapter(stand) != null
                    && nearRay(player, adapterOutlet(stand), 0.28)) {
                target = adapter(stand);
            } else if (part(stand, DistillationPartEntity.CONDENSER) != null
                    && nearRay(player, condenserEnd(stand), 0.28)) {
                target = part(stand, DistillationPartEntity.CONDENSER);
            } else if (part(stand, DistillationPartEntity.HEAD) != null
                    && nearRay(player, mouth(stand).add(0, 0.25, 0), 0.18)) {
                target = part(stand, DistillationPartEntity.HEAD);
            }
            if (target != null) {
                if (!player.level().isClientSide()) {
                    if (player.isShiftKeyDown()) removeWithDownstream(target, player);
                    else remove(target, player);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        ItemStack vessel = source.getVessel();
        int kind = 0;
        Vec3 port = null;
        if (held.is(ModItems.DISTILLATION_HEAD.get())) {
            kind = DistillationPartEntity.HEAD;
            port = mouth(stand);
            if (!GlassConnector.isOpenMouthVessel(vessel) || VesselHeating.isSealed(vessel)
                    || (VesselHeating.isThreeNeck(vessel) && VesselHeating.neckHasStopper(vessel, 1))
                    || ThermometerSleeves.occupied(source, 1) || part(stand, kind) != null) {
                return InteractionResult.SUCCESS;
            }
        } else if (held.is(ModItems.STRAIGHT_CONDENSER.get())) {
            kind = DistillationPartEntity.CONDENSER;
            port = headArm(stand);
            if (part(stand, DistillationPartEntity.HEAD) == null || part(stand, kind) != null) {
                return InteractionResult.SUCCESS;
            }
        } else if (held.is(ModItems.RECEIVER_ADAPTER_BENT.get())
                || held.is(ModItems.RECEIVER_ADAPTER_STRAIGHT.get())) {
            kind = held.is(ModItems.RECEIVER_ADAPTER_BENT.get())
                    ? DistillationPartEntity.ADAPTER_BENT : DistillationPartEntity.ADAPTER_STRAIGHT;
            port = condenserEnd(stand);
            if (part(stand, DistillationPartEntity.CONDENSER) == null || adapter(stand) != null) {
                return InteractionResult.SUCCESS;
            }
        } else if (held.is(ModItems.THERMOMETER.get())
                && part(stand, DistillationPartEntity.HEAD) != null) {
            var head = part(stand, DistillationPartEntity.HEAD);
            if (ThermometerSleeves.occupied(head, 0)) {
                return ThermometerSleeves.interact(head, player, hand);
            }
            kind = DistillationPartEntity.THERMOMETER;
            port = headTop(stand);
            if (part(stand, kind) != null) {
                if (nearRay(player, port, 0.3) && !player.level().isClientSide()) {
                    player.displayClientMessage(Component.translatable("mchemistry.thermometer.read",
                            String.format("%.0f", TemperatureSystem.getTemp(vessel))), true);
                }
                return InteractionResult.SUCCESS;
            }
        } else if (held.is(ModItems.ERLENMEYER_FLASK.get()) && adapter(stand) != null) {
            port = adapterOutlet(stand);
            if (ThermometerSleeves.occupied(adapter(stand), 0) || receiver(stand) != null) {
                return InteractionResult.SUCCESS;
            }
            if (!nearRay(player, port, 0.3)) {
                return InteractionResult.PASS;
            }
            if (!player.level().isClientSide()) {
                PlacedVesselEntity receiving = new PlacedVesselEntity(ModEntities.PLACED_VESSEL.get(),
                        stand.level());
                receiving.setPos(port.x, stand.getY(), port.z);
                double centered = (8.5 - 8.5 * MODEL_SCALE) / 16.0;
                receiving.setMount(MODEL_SCALE, centered, 0, centered, 0);
                receiving.setVessel(held.copyWithCount(1));
                receiving.setReceiverStandId(stand.getUUID().toString());
                stand.level().addFreshEntity(receiving);
                consumeAndSound(stand, player, held);
            }
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
        if (!nearRay(player, port, 0.3)) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide()) {
            DistillationPartEntity entity = new DistillationPartEntity(
                    ModEntities.DISTILLATION_PART.get(), stand.level());
            entity.setKind(kind);
            entity.setStand(stand);
            Vec3 at = switch (kind) {
                case DistillationPartEntity.HEAD -> mouth(stand);
                case DistillationPartEntity.CONDENSER -> condenserEnd(stand);
                case DistillationPartEntity.THERMOMETER -> headTop(stand);
                default -> adapterOutletForKind(stand, kind);
            };
            // Entity positions are the lower corner of AABBs. Center the pick box on the F9 port.
            entity.setPos(at.x, at.y - 0.3, at.z);
            stand.level().addFreshEntity(entity);
            consumeAndSound(stand, player, held);
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean canRemove(DistillationPartEntity part) {
        IronStandEntity stand = standFor(part);
        if (stand == null) {
            return true;
        }
        return switch (part.kind()) {
            case DistillationPartEntity.HEAD -> part(stand, DistillationPartEntity.CONDENSER) == null
                    && part(stand, DistillationPartEntity.THERMOMETER) == null;
            case DistillationPartEntity.CONDENSER -> adapter(stand) == null;
            case DistillationPartEntity.ADAPTER_BENT, DistillationPartEntity.ADAPTER_STRAIGHT ->
                    receiver(stand) == null;
            default -> true;
        };
    }

    private static void remove(DistillationPartEntity part, Player player) {
        if (!canRemove(part)) {
            player.displayClientMessage(Component.literal("请先取下后续连接的仪器"), true);
            return;
        }
        ItemStack item = part.toStack();
        part.discard();
        give(player, item);
        player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.BLOCKS, 0.5F, 1.4F);
    }

    /** Sneak-removal returns the connected tail first, including a filled receiver. */
    private static void removeWithDownstream(DistillationPartEntity target, Player player) {
        for (ItemStack item : detachWithDownstream(target)) give(player, item);
        player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.BLOCKS, 0.5F, 1.4F);
    }

    static List<ItemStack> detachWithDownstream(DistillationPartEntity target) {
        List<ItemStack> items = new ArrayList<>();
        IronStandEntity stand = standFor(target);
        if (stand == null) {
            detachPart(target, items);
            return items;
        }
        if (target.kind() == DistillationPartEntity.HEAD
                || target.kind() == DistillationPartEntity.CONDENSER
                || target.kind() == DistillationPartEntity.ADAPTER_BENT
                || target.kind() == DistillationPartEntity.ADAPTER_STRAIGHT) {
            PlacedVesselEntity receiving = receiver(stand);
            if (receiving != null) {
                items.add(receiving.getVessel());
                if (!receiving.getAttached1().isEmpty()) items.add(receiving.getAttached1());
                if (!receiving.getAttached2().isEmpty()) items.add(receiving.getAttached2());
                receiving.discard();
            }
        }
        if (target.kind() == DistillationPartEntity.HEAD
                || target.kind() == DistillationPartEntity.CONDENSER) {
            DistillationPartEntity downstream = adapter(stand);
            if (downstream != null) detachPart(downstream, items);
        }
        if (target.kind() == DistillationPartEntity.HEAD) {
            DistillationPartEntity condenser = part(stand, DistillationPartEntity.CONDENSER);
            if (condenser != null) detachPart(condenser, items);
            DistillationPartEntity thermometer = part(stand, DistillationPartEntity.THERMOMETER);
            if (thermometer != null) detachPart(thermometer, items);
        }
        detachPart(target, items);
        return items;
    }

    private static void detachPart(DistillationPartEntity part, List<ItemStack> items) {
        items.add(part.toStack());
        part.discard();
    }

    public static List<ItemStack> removeAll(IronStandEntity stand) {
        List<ItemStack> items = new ArrayList<>();
        for (DistillationPartEntity part : stand.level().getEntitiesOfClass(
                DistillationPartEntity.class, stand.getBoundingBox().inflate(2.5))) {
            if (stand.getUUID().toString().equals(part.standId())) {
                items.add(part.toStack());
                part.discard();
            }
        }
        PlacedVesselEntity receiving = receiver(stand);
        if (receiving != null) {
            items.add(receiving.getVessel().copy());
            if (!receiving.getAttached1().isEmpty()) {
                items.add(receiving.getAttached1());
            }
            if (!receiving.getAttached2().isEmpty()) {
                items.add(receiving.getAttached2());
            }
            receiving.discard();
        }
        return items;
    }

    private static void consumeAndSound(IronStandEntity stand, Player player, ItemStack held) {
        if (!player.isCreative()) {
            held.shrink(1);
        }
        stand.level().playSound(null, stand.blockPosition(), SoundEvents.GLASS_PLACE,
                SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static void give(Player player, ItemStack item) {
        if (item.isEmpty()) return;
        if (!player.getInventory().add(item)) {
            player.drop(item, false);
        }
    }
}
