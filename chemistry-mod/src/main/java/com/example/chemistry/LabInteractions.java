package com.example.chemistry;

import com.example.chemistry.data.Solids;
import com.example.chemistry.CombustionEngine;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.block.AlcoholLampBlock;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.blockentity.PlacedTestTubeBlockEntity;
import com.example.chemistry.item.CombustionSpoonItem;
import com.example.chemistry.item.DropperHelper;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.item.SolidToolItem;
import com.example.chemistry.item.SplintItem;
import com.example.chemistry.item.TestTubeItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.registry.ModBlocks;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Reagent transfer mechanics:
 *  - offhand open wide-mouth bottle (solid source): tweezers/spatula take out.
 *  - offhand reaction vessel (试管/烧杯): tools put solids in, bottles and
 *    droppers pour liquids in.
 */
@EventBusSubscriber(modid = ChemistryMod.MODID)
public class LabInteractions {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        // The client must let the use-item packet reach the server; only the
        // server applies the transfer and cancels the item's own use().
        if (event.getSide().isClient()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack main = event.getItemStack();
        ItemStack off = player.getOffhandItem();

        // Gas-jar verification tests (offhand jar + mainhand splint / fire / limewater).
        String offPathJar = BuiltInRegistries.ITEM.getKey(off.getItem()).getPath();
        boolean offIsJar = offPathJar.startsWith("gas_collecting_bottle_")
                || offPathJar.startsWith("open_gas_collecting_bottle_");
        if (offIsJar) {
            if (main.getItem() instanceof SplintItem splint && splint.isGlowing()) {
                if (offPathJar.contains("oxygen")) {
                    if (player.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.FLAME, player.getX(), player.getEyeY(), player.getZ(),
                                10, 0.2, 0.2, 0.2, 0.02);
                    }
                    player.displayClientMessage(
                            Component.translatable("mchemistry.gas.oxygen_relight"), true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
                if (offPathJar.contains("carbon_dioxide")) {
                    if (player.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getEyeY(), player.getZ(),
                                8, 0.1, 0.1, 0.1, 0.01);
                    }
                    player.displayClientMessage(
                            Component.translatable("mchemistry.gas.co2_extinguish"), true);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SPLINT.get()));
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
            }
            if ((main.is(net.minecraft.world.item.Items.FLINT_AND_STEEL)
                    || main.getItem() instanceof com.example.chemistry.item.AlcoholLampLitItem)
                    && offPathJar.contains("hydrogen")) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                        SoundSource.BLOCKS, 0.8F, 1.4F);
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.FLAME, player.getX(), player.getEyeY(), player.getZ(),
                            16, 0.3, 0.3, 0.3, 0.05);
                }
                player.displayClientMessage(Component.translatable("mchemistry.gas.h2_pop"), true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
            if (offPathJar.contains("carbon_dioxide")) {
                String liquid = main.getItem() instanceof DropperItem
                        ? DropperHelper.getLiquid(main)
                        : liquidIdOf(BuiltInRegistries.ITEM.getKey(main.getItem()).getPath());
                if ("limewater_clear".equals(liquid)) {
                    if (player.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getEyeY(), player.getZ(),
                                10, 0.1, 0.1, 0.1, 0.01);
                    }
                    player.displayClientMessage(
                            Component.translatable("mchemistry.gas.co2_limewater"), true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
            }
        }

        // The vessel can be held in either hand: if the main hand is a vessel
        // and the offhand is a transfer tool / bottle, swap the roles.
        if (!(off.getItem() instanceof LabVesselItem)
                && main.getItem() instanceof LabVesselItem
                && isTransferTool(off)) {
            ItemStack tmp = main;
            main = off;
            off = tmp;
        }

        // Attach a test tube clamp (offhand) onto a test tube (mainhand).
        if (off.getItem() == ModItems.TEST_TUBE_CLAMP.get()
                && main.getItem() instanceof TestTubeItem tube && !tube.isClamped()) {
            String path = BuiltInRegistries.ITEM.getKey(main.getItem()).getPath();
            Item clamped;
            if (tube.stopperHoles() > 0) {
                clamped = ModItems.clampedStopperedVariant(path, tube.stopperHoles());
            } else {
                DeferredItem<Item> deferred = ModItems.TEST_TUBE_CLAMPED_BY_ID.get(path);
                clamped = deferred != null ? deferred.get() : null;
            }
            if (clamped != null) {
                ItemStack clampedStack = new ItemStack(clamped);
                clampedStack.set(DataComponents.CUSTOM_DATA,
                        main.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY));
                player.setItemInHand(InteractionHand.MAIN_HAND, clampedStack);
                off.shrink(1);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }

        // Attach a rubber stopper (offhand, 1-hole / 2-hole) onto a test tube.
        if (main.getItem() instanceof TestTubeItem tube && tube.stopperHoles() == 0) {
            int holes = off.getItem() == ModItems.RUBBER_STOPPER_1_HOLE.get() ? 1
                    : off.getItem() == ModItems.RUBBER_STOPPER_2_HOLE.get() ? 2 : 0;
            if (holes > 0) {
                String path = BuiltInRegistries.ITEM.getKey(main.getItem()).getPath();
                Item stoppered = ModItems.stopperedVariant(path, holes);
                if (stoppered != net.minecraft.world.item.Items.AIR) {
                    ItemStack stopperedStack = new ItemStack(stoppered);
                    stopperedStack.set(DataComponents.CUSTOM_DATA,
                            main.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY));
                    player.setItemInHand(InteractionHand.MAIN_HAND, stopperedStack);
                    off.shrink(1);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
            }
        }

        // Slip a wet rubber tube onto a gas nozzle (offhand nozzle + mainhand tube).
        if (off.getItem() == ModItems.GAS_NOZZLE.get() && main.getItem() instanceof RubberTubeItem) {
            if (RubberTubeItem.isWet(main)) {
                RubberTubeItem.discardTempTube(player.level(), main);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GAS_NOZZLE_TUBED.get()));
                off.shrink(1);
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else {
                player.displayClientMessage(Component.translatable("mchemistry.rubber_tube.need_wet"), true);
                event.setCancellationResult(InteractionResult.FAIL);
            }
            return;
        }

        // Insert a burning combustion spoon into an upright oxygen jar (offhand).
        if (main.getItem() instanceof CombustionSpoonItem && CombustionEngine.isLit(main)) {
            String offPath = BuiltInRegistries.ITEM.getKey(off.getItem()).getPath();
            if (offPath.equals("gas_collecting_bottle_oxygen")) {
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.openGasJar("oxygen")));
                CombustionEngine.setInBottle(main, true);
                player.displayClientMessage(Component.translatable("mchemistry.spoon.insert"), true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }

        // Put or pour into a reaction vessel held in the offhand.
        if (off.getItem() instanceof LabVesselItem) {
            boolean sealed = VesselHeating.isSealed(off);
            if (main.getItem() instanceof SolidToolItem tool && !SolidToolItem.isEmpty(main)) {
                if (sealed) {
                    event.getEntity().displayClientMessage(
                            Component.translatable("mchemistry.vessel.sealed"), true);
                    event.setCancellationResult(InteractionResult.FAIL);
                    return;
                }
                if (LabVesselItem.addSolid(off, SolidToolItem.getHeldSolid(main))) {
                    SolidToolItem.clear(main);
                    ReactionEngine.checkAndStart(off, player);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                } else {
                    event.getEntity().displayClientMessage(
                            Component.translatable("mchemistry.vessel.full"), true);
                    event.setCancellationResult(InteractionResult.FAIL);
                }
                return;
            }
            if (main.getItem() instanceof DropperItem && !DropperHelper.isEmpty(main)) {
                if (sealed) {
                    event.getEntity().displayClientMessage(
                            Component.translatable("mchemistry.vessel.sealed"), true);
                    event.setCancellationResult(InteractionResult.FAIL);
                    return;
                }
                if (LabVesselItem.addLiquid(off, DropperHelper.getLiquid(main), 5)) {
                    DropperHelper.setMl(main, DropperHelper.getMl(main) - 5);
                    ReactionEngine.checkAndStart(off, player);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                } else {
                    event.getEntity().displayClientMessage(
                            Component.translatable("mchemistry.vessel.full"), true);
                    event.setCancellationResult(InteractionResult.FAIL);
                }
                return;
            }
            String liquidId = liquidIdOf(BuiltInRegistries.ITEM.getKey(main.getItem()).getPath());
            if (liquidId != null) {
                if (sealed) {
                    event.getEntity().displayClientMessage(
                            Component.translatable("mchemistry.vessel.sealed"), true);
                    event.setCancellationResult(InteractionResult.FAIL);
                    return;
                }
                if (LabVesselItem.addLiquid(off, liquidId, 25)) {
                    ReactionEngine.checkAndStart(off, player);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                } else {
                    event.getEntity().displayClientMessage(
                            Component.translatable("mchemistry.vessel.full"), true);
                    event.setCancellationResult(InteractionResult.FAIL);
                }
                return;
            }
        }

        // Take a solid out of an open wide-mouth bottle (only take-out).
        String solidId = solidIdOf(BuiltInRegistries.ITEM.getKey(off.getItem()).getPath());
        if (solidId != null && main.getItem() instanceof SolidToolItem tool && SolidToolItem.isEmpty(main)) {
            boolean lumps = Solids.ALL.stream()
                    .filter(s -> s.id().equals(solidId))
                    .map(s -> s.form() == Solids.SolidForm.LUMP)
                    .findFirst().orElse(false);
            if (tool.holdsLumps() == lumps) {
                SolidToolItem.pickUp(main, solidId);
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else {
                event.setCancellationResult(InteractionResult.FAIL);
            }
        }
    }

    /**
     * Right-clicking heat/cooling blocks with a test tube changes its
     * temperature; sneak + fire toggles the sustained heating mode.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getSide().isClient()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack main = event.getItemStack();
        // Light a plain splint on fire / a lit alcohol lamp.
        if (main.getItem() instanceof SplintItem splint && !splint.isGlowing()) {
            var clicked = event.getLevel().getBlockState(event.getPos());
            boolean fire = clicked.is(Blocks.FIRE) || clicked.is(Blocks.SOUL_FIRE);
            boolean lamp = clicked.is(ModBlocks.ALCOHOL_LAMP.get())
                    && clicked.getValue(AlcoholLampBlock.LIT);
            if (fire || lamp) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GLOWING_SPLINT.get()));
                event.getLevel().playSound(null, event.getPos(), SoundEvents.FLINTANDSTEEL_USE,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
        // Sneak + right-click a placed gas bottle flips its up/down orientation
        // (the vanilla sneak bypass skips the block's own useItemOn). Holding a
        // rubber tube must NOT toggle it — the tube connection wins instead.
        if (player.isShiftKeyDown() && !(main.getItem() instanceof RubberTubeItem)
                && event.getLevel().getBlockState(event.getPos()).is(ModBlocks.GAS_COLLECTING_BOTTLE.get())) {
            GasCollectingBottleBlock.toggleOrientation(event.getLevel(), event.getPos(),
                    event.getLevel().getBlockState(event.getPos()));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        ItemStack placedTube = null;
        if (be instanceof PlacedTestTubeBlockEntity pbe) {
            placedTube = pbe.getTube();
        } else if (be instanceof IronStandBlockEntity ibe) {
            placedTube = ibe.getTube();
        }
        // Right-click a placed test tube with a medicine tool to fill it.
        if (placedTube != null && !placedTube.isEmpty() && isMedicineTool(main)) {
            if (tryFill(placedTube, main, player)) {
                if (be instanceof PlacedTestTubeBlockEntity pbe) {
                    pbe.setTube(placedTube);
                } else {
                    ((IronStandBlockEntity) be).setTube(placedTube);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else {
                event.setCancellationResult(InteractionResult.FAIL);
            }
            return;
        }
        if (!(main.getItem() instanceof TestTubeItem)) {
            return;
        }
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        var clickedState = event.getLevel().getBlockState(event.getPos());
        boolean lampLit = clickedState.is(ModBlocks.ALCOHOL_LAMP.get())
                && clickedState.getValue(AlcoholLampBlock.LIT);
        if (player.isShiftKeyDown() && (block == Blocks.FIRE || block == Blocks.SOUL_FIRE || lampLit)) {
            TemperatureSystem.toggleHeatmode(main, block == Blocks.SOUL_FIRE ? 1200 : 600);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        double delta = 0;
        boolean extinguish = false;
        if (block == Blocks.FIRE || lampLit) {
            TemperatureSystem.addTempCapped(main, 50, 600);
            delta = 1;
        } else if (block == Blocks.SOUL_FIRE) {
            TemperatureSystem.addTempCapped(main, 100, 1200);
            delta = 1;
        } else if (event.getLevel().getBlockState(event.getPos()).getFluidState().is(Fluids.WATER)) {
            TemperatureSystem.addTemp(main, -50);
            delta = 1;
        } else if (block == Blocks.ICE) {
            TemperatureSystem.addTempFloored(main, -100, 0);
            extinguish = true;
            delta = 1;
        } else if (block == Blocks.PACKED_ICE) {
            TemperatureSystem.addTempFloored(main, -200, -10);
            extinguish = true;
            delta = 1;
        } else if (block == Blocks.BLUE_ICE) {
            TemperatureSystem.addTempFloored(main, -300, -40);
            extinguish = true;
            delta = 1;
        }
        if (delta != 0) {
            TemperatureSystem.addTemp(main, delta);
            if (extinguish) {
                player.level().playSound(null, event.getPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Add a held tool / dropper / liquid bottle's contents into a vessel and
     *  kick the reaction engine. Shared by held-vessel transfers (above) and
     *  placed vessels on the iron stand / tripod. */
    public static boolean addToVessel(ItemStack main, ItemStack vessel, Player player) {
        if (!(vessel.getItem() instanceof LabVesselItem)) {
            return false;
        }
        if (main.getItem() instanceof SolidToolItem && !SolidToolItem.isEmpty(main)) {
            if (LabVesselItem.addSolid(vessel, SolidToolItem.getHeldSolid(main))) {
                SolidToolItem.clear(main);
                if (!ReactionEngine.checkAndStart(vessel, player)) {
                    player.displayClientMessage(Component.translatable("mchemistry.no_reaction"), true);
                }
                return true;
            }
            player.displayClientMessage(Component.translatable("mchemistry.vessel.full"), true);
            return false;
        }
        // 手持散装固体（loose_<id>）直接装填，不需要广口瓶/药匙。
        String looseId = looseSolidId(main);
        if (looseId != null) {
            if (LabVesselItem.addSolid(vessel, looseId)) {
                main.shrink(1);
                if (!ReactionEngine.checkAndStart(vessel, player)) {
                    player.displayClientMessage(Component.translatable("mchemistry.no_reaction"), true);
                }
                return true;
            }
            player.displayClientMessage(Component.translatable("mchemistry.vessel.full"), true);
            return false;
        }
        if (main.getItem() instanceof DropperItem && !DropperHelper.isEmpty(main)) {
            if (LabVesselItem.addLiquid(vessel, DropperHelper.getLiquid(main), 5)) {
                DropperHelper.setMl(main, DropperHelper.getMl(main) - 5);
                if (!ReactionEngine.checkAndStart(vessel, player)) {
                    player.displayClientMessage(Component.translatable("mchemistry.no_reaction"), true);
                }
                return true;
            }
            player.displayClientMessage(Component.translatable("mchemistry.vessel.full"), true);
            return false;
        }
        String liquidId = liquidIdOf(BuiltInRegistries.ITEM.getKey(main.getItem()).getPath());
        if (liquidId != null) {
            if (LabVesselItem.addLiquid(vessel, liquidId, 25)) {
                if (!ReactionEngine.checkAndStart(vessel, player)) {
                    player.displayClientMessage(Component.translatable("mchemistry.no_reaction"), true);
                }
                return true;
            }
            player.displayClientMessage(Component.translatable("mchemistry.vessel.full"), true);
            return false;
        }
        return false;
    }

    /** Whether the held item is a transfer tool / dropper / liquid bottle. */
    public static boolean isTransferTool(ItemStack main) {
        if (main.getItem() instanceof SolidToolItem && !SolidToolItem.isEmpty(main)) {
            return true;
        }
        if (looseSolidId(main) != null) {
            return true;
        }
        if (main.getItem() instanceof DropperItem && !DropperHelper.isEmpty(main)) {
            return true;
        }
        return liquidIdOf(BuiltInRegistries.ITEM.getKey(main.getItem()).getPath()) != null;
    }

    /** 散装固体物品（loose_<id>）对应的固体 id，非散装返回 null。 */
    public static String looseSolidId(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.startsWith("loose_") ? path.substring("loose_".length()) : null;
    }

    /**
     * Right-click on a PLACED vessel. Open vessels accept tools / droppers /
     * bottles as usual. A rubber-stoppered vessel rejects additions unless a
     * dropper or long-stem funnel is already inserted through the stopper:
     * the dropper admits 1 mL of liquid, the funnel admits liquid bottles.
     */
    public static boolean interactPlacedVessel(ItemStack main, ItemStack vessel,
            ItemStack attached1, ItemStack attached2, Player player) {
        if (!(vessel.getItem() instanceof LabVesselItem)) {
            return false;
        }
        boolean sealed = VesselHeating.isSealed(vessel);
        if (!sealed) {
            return addToVessel(main, vessel, player);
        }
        boolean hasDropper = (!attached1.isEmpty() && DropperHelper.isDropper(attached1))
                || (!attached2.isEmpty() && DropperHelper.isDropper(attached2));
        boolean hasFunnel = (!attached1.isEmpty() && attached1.is(ModItems.LONG_STEM_FUNNEL.get()))
                || (!attached2.isEmpty() && attached2.is(ModItems.LONG_STEM_FUNNEL.get()));
        if (main.getItem() instanceof DropperItem && !DropperHelper.isEmpty(main)) {
            if (hasDropper) {
                if (LabVesselItem.addLiquid(vessel, DropperHelper.getLiquid(main), 5)) {
                    DropperHelper.setMl(main, DropperHelper.getMl(main) - 5);
                    ReactionEngine.checkAndStart(vessel, player);
                }
                return true;
            }
            player.displayClientMessage(Component.translatable("mchemistry.vessel.sealed"), true);
            return true;
        }
        String liquidId = liquidIdOf(BuiltInRegistries.ITEM.getKey(main.getItem()).getPath());
        if (liquidId != null) {
            if (hasFunnel) {
                if (LabVesselItem.addLiquid(vessel, liquidId, 25)) {
                    ReactionEngine.checkAndStart(vessel, player);
                }
                return true;
            }
            player.displayClientMessage(Component.translatable("mchemistry.vessel.sealed"), true);
            return true;
        }
        if (isTransferTool(main)) {
            player.displayClientMessage(Component.translatable("mchemistry.vessel.sealed"), true);
            return true;
        }
        return false;
    }

    /** Whether the click should be consumed for a placed vessel. */
    public static boolean isVesselRelevant(ItemStack main, ItemStack vessel,
            ItemStack attached1, ItemStack attached2) {
        if (!(vessel.getItem() instanceof LabVesselItem)) {
            return false;
        }
        boolean sealed = VesselHeating.isSealed(vessel);
        if (!sealed) {
            return isTransferTool(main);
        }
        boolean hasDropper = (!attached1.isEmpty() && DropperHelper.isDropper(attached1))
                || (!attached2.isEmpty() && DropperHelper.isDropper(attached2));
        boolean hasFunnel = (!attached1.isEmpty() && attached1.is(ModItems.LONG_STEM_FUNNEL.get()))
                || (!attached2.isEmpty() && attached2.is(ModItems.LONG_STEM_FUNNEL.get()));
        if (main.getItem() instanceof DropperItem) {
            return hasDropper;
        }
        if (liquidIdOf(BuiltInRegistries.ITEM.getKey(main.getItem()).getPath()) != null) {
            return hasFunnel;
        }
        return isTransferTool(main);
    }

    private static boolean isMedicineTool(ItemStack main) {
        if (main.getItem() instanceof SolidToolItem tool) {
            return !SolidToolItem.isEmpty(main);
        }
        if (main.getItem() instanceof DropperItem) {
            return !DropperHelper.isEmpty(main);
        }
        return liquidIdOf(BuiltInRegistries.ITEM.getKey(main.getItem()).getPath()) != null;
    }

    /** Fills a held/placed tube with the main-hand medicine tool. */
    private static boolean tryFill(ItemStack tube, ItemStack main, Player player) {
        if (main.getItem() instanceof SolidToolItem tool && !SolidToolItem.isEmpty(main)) {
            if (LabVesselItem.addSolid(tube, SolidToolItem.getHeldSolid(main))) {
                SolidToolItem.clear(main);
                ReactionEngine.checkAndStart(tube, player);
                return true;
            }
            return false;
        }
        if (main.getItem() instanceof DropperItem && !DropperHelper.isEmpty(main)) {
            if (LabVesselItem.addLiquid(tube, DropperHelper.getLiquid(main), 5)) {
                DropperHelper.setMl(main, DropperHelper.getMl(main) - 5);
                ReactionEngine.checkAndStart(tube, player);
                return true;
            }
            return false;
        }
        String liquidId = liquidIdOf(BuiltInRegistries.ITEM.getKey(main.getItem()).getPath());
        if (liquidId != null && LabVesselItem.addLiquid(tube, liquidId, 25)) {
            ReactionEngine.checkAndStart(tube, player);
            return true;
        }
        return false;
    }

    private static String liquidIdOf(String path) {
        if (path.startsWith("liquid_")) {
            return path.substring("liquid_".length());
        }
        if (path.startsWith("open_liquid_")) {
            return path.substring("open_liquid_".length());
        }
        return null;
    }

    private static String solidIdOf(String path) {
        return path.startsWith("open_solid_") ? path.substring("open_solid_".length()) : null;
    }
}
