package com.example.chemistry;

import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.item.GasNozzleTubedItem;
import com.example.chemistry.entity.RubberTubeEntity.Anchor;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Entity interactions for rubber tubes: set entity anchors, remove tubes. */
@EventBusSubscriber(modid = ChemistryMod.MODID)
public class RubberTubeEvents {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getSide().isClient()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack main = player.getMainHandItem();
        Entity target = event.getTarget();

        if (target instanceof RubberTubeEntity tube) {
            if (main.isEmpty() || main.is(Items.SHEARS)) {
                GasFlowEngine.rupture(event.getLevel(), tube);
                tube.discard();
                ItemStack item = new ItemStack(ModItems.RUBBER_TUBE.get());
                if (!player.getInventory().add(item)) {
                    player.drop(item, false);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }

        if (main.getItem() instanceof RubberTubeItem) {
            RubberTubeItem.onEntityClicked(event.getLevel(), player, main, target);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        } else if (main.getItem() instanceof GasNozzleTubedItem) {
            RubberTubeItem.createTube(event.getLevel(), player, main,
                    Anchor.entity(player.getUUID()), Anchor.entity(target.getUUID()));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Shears cut through old straight-tube segments. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide().isClient()) {
            return;
        }
        Player player = event.getEntity();
        if (!player.getMainHandItem().is(Items.SHEARS)) {
            return;
        }
        Level level = event.getLevel();
        // Cutting a sagging tube (entity) takes priority: right-click ANY
        // visible segment with shears and the tube drops.
        if (level instanceof ServerLevel serverLevel) {
            double dist = player.getEyePosition(1.0F)
                    .distanceTo(event.getHitVec().getLocation()) + 0.5;
            if (tryCutTubeAt(serverLevel, player, dist)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
        BlockPos pos = event.getPos();
        if (level.getBlockState(pos).is(ModBlocks.RUBBER_TUBE_LINK.get())) {
            level.destroyBlock(pos, false);
            ItemEntity drop = new ItemEntity(
                    level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                    new ItemStack(ModItems.RUBBER_TUBE.get()));
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Right-clicking in the air with shears also cuts a tube in front of you. */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide().isClient()) {
            return;
        }
        Player player = event.getEntity();
        if (!player.getMainHandItem().is(Items.SHEARS)) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel serverLevel
                && tryCutTubeAt(serverLevel, player, 5.0)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Ray-cast for a rubber tube along the player's look direction and cut it. */
    private static boolean tryCutTubeAt(ServerLevel level, Player player, double maxDistance) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(maxDistance));
        double best = maxDistance * maxDistance;
        RubberTubeEntity found = null;
        for (RubberTubeEntity tube : level.getEntitiesOfClass(RubberTubeEntity.class,
                new AABB(eye, end).inflate(1.0))) {
            java.util.Optional<Vec3> hit = RubberTubeItem.tubePickBox(level, tube).clip(eye, end);
            if (hit.isPresent()) {
                double d = eye.distanceToSqr(hit.get());
                if (d < best) {
                    best = d;
                    found = tube;
                }
            }
        }
        if (found != null) {
            GasFlowEngine.rupture(level, found);
            found.discard();
            ItemEntity drop = new ItemEntity(level, found.getX(), found.getY(), found.getZ(),
                    new ItemStack(ModItems.RUBBER_TUBE.get()));
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
            return true;
        }
        return false;
    }
}
