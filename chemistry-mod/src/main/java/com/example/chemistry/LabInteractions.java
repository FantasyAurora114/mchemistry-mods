package com.example.chemistry;

import com.example.chemistry.data.Solids;
import com.example.chemistry.CombustionEngine;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.block.AlcoholLampBlock;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.blockentity.GasCollectingBottleBlockEntity;
import com.example.chemistry.blockentity.PlacedGraduatedCylinderBlockEntity;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.item.CombustionSpoonItem;
import com.example.chemistry.item.DropperHelper;
import com.example.chemistry.item.DropperItem;
import com.example.chemistry.item.GlassTubeItem;
import com.example.chemistry.item.GraduatedCylinderItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.LabelItem;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.item.SolidToolItem;
import com.example.chemistry.item.SplintItem;
import com.example.chemistry.item.TestTubeItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.registry.ModBlocks;
import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.transfer.BottleCodes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
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
        if(com.example.chemistry.transfer.BottleQuantities.refill(main,off)){event.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(GlassRodSampling.test(player,main,off)){event.setCancellationResult(InteractionResult.SUCCESS);return;}

        // 点燃手持玻璃导管喷出的气体（副手玻璃导管 + 主手打火石等）。
        if (GlassTubeIgnition.tryIgnite(player.level(), player)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        // 虚拟选中框：对空右键瞄准铁架台上的瓶口端口（瓶口悬空也能插塞/塞塞子）。
        if (main.is(ModItems.GLASS_STOPPER.get())
                || main.is(ModItems.RUBBER_STOPPER_1_HOLE.get())
                || main.is(ModItems.RUBBER_STOPPER_2_HOLE.get())
                || main.is(ModItems.RUBBER_STOPPER_3_HOLE.get())) {
            if (routeStopperViaPort(player.level(), player, main)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }

        // Gas-jar verification tests (offhand jar + mainhand splint / fire / limewater).
        String offGas = BottleCodes.gasIdOf(off);
        boolean offIsJar = offGas != null;
        if (offIsJar) {
            if (main.getItem() instanceof SplintItem splint && splint.isGlowing()) {
                if ("oxygen".equals(offGas)) {
                    if (player.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.FLAME, player.getX(), player.getEyeY(), player.getZ(),
                                10, 0.2, 0.2, 0.2, 0.02);
                    }
                    player.displayClientMessage(
                            Component.translatable("mchemistry.gas.oxygen_relight"), true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
                if ("carbon_dioxide".equals(offGas)) {
                    if (player.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getEyeY(), player.getZ(),
                                8, 0.1, 0.1, 0.1, 0.01);
                    }
                    player.displayClientMessage(
                            Component.translatable("mchemistry.gas.co2_extinguish"), true);
                    var extinguished=new ItemStack(ModItems.SPLINT.get());
                    main.shrink(1);
                    if(main.isEmpty())player.setItemInHand(InteractionHand.MAIN_HAND,extinguished);
                    else if(!player.getInventory().add(extinguished))player.drop(extinguished,false);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
            }
            if ((main.is(net.minecraft.world.item.Items.FLINT_AND_STEEL)
                    || main.getItem() instanceof com.example.chemistry.item.AlcoholLampLitItem)
                    && "hydrogen".equals(offGas)) {
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
            if ("carbon_dioxide".equals(offGas)) {
                String liquid = main.getItem() instanceof DropperItem
                        ? DropperHelper.getLiquid(main)
                        : liquidIdOf(main);
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

        if (main.getItem() instanceof com.example.chemistry.organic.PhasePipetteItem
                && off.getItem() instanceof LabVesselItem) {
            com.example.chemistry.organic.PhasePipetteItem.interact(player,main,off);
            event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);return;
        }
        if (off.getItem() instanceof com.example.chemistry.organic.PhasePipetteItem
                && main.getItem() instanceof LabVesselItem) {
            com.example.chemistry.organic.PhasePipetteItem.interact(player,off,main);
            event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);return;
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

        // 主手敞口集气瓶 + 副手反应容器：往容器里倒一部分气体。
        if (openGasIdOf(main) != null && off.getItem() instanceof LabVesselItem) {
            pourGas(main, off, player);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // 用温度计右键副手反应容器：读出当前温度。
        if (main.is(ModItems.THERMOMETER.get()) && off.getItem() instanceof LabVesselItem) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.thermometer.read",
                            String.format("%.0f", TemperatureSystem.getTemp(off))), true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // 玻璃棒搅拌副手反应容器。
        if (main.is(ModItems.GLASS_ROD.get()) && off.getItem() instanceof LabVesselItem) {
            if(player.isShiftKeyDown())GlassRodSampling.dip(player,main,off);else stir(player, off);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // 主手细口瓶 + 副手量筒：倒 5mL 液体进量筒。
        String bottleLiquid = liquidIdOf(main);
        if (bottleLiquid != null && off.is(ModItems.GRADUATED_CYLINDER.get())) {
            int amount=Math.min(Math.min(5,BottleCodes.volumeOf(main)),(int)Math.floor(GraduatedCylinderItem.CAPACITY-GraduatedCylinderItem.getMl(off)));
            if(amount>0){int accepted=(int)GraduatedCylinderItem.add(off,bottleLiquid,amount);BottleCodes.setVolume(main,BottleCodes.volumeOf(main)-accepted);}
            player.displayClientMessage(
                    Component.translatable("mchemistry.cylinder.pour5"), true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        // 主手量筒 + 副手反应容器：全部倒入。
        if (main.is(ModItems.GRADUATED_CYLINDER.get()) && off.getItem() instanceof LabVesselItem) {
            String liquid = GraduatedCylinderItem.getLiquid(main);
            if (liquid != null) {
                double ml = GraduatedCylinderItem.getMl(main);
                LabVesselItem.addLiquid(off, liquid, (int) ml);
                GraduatedCylinderItem.set(main, null, 0);
                player.displayClientMessage(
                        Component.translatable("mchemistry.cylinder.pour_all"), true);
                ReactionEngine.checkAndStart(off, player);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        // 主手烧杯（含液体）倒入副手反应容器。
        if (isBeaker(main) && off.getItem() instanceof LabVesselItem) {
            pourBeaker(main, off, player);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        // 玻璃塞连接三颈烧瓶：直接塞，无需磨口。
        if (main.is(ModItems.GLASS_STOPPER.get())
                && off.is(ModItems.THREE_NECK_FLASK.get())) {
            connectGlassStopper(main, off, player);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
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

        // Insert a burning combustion spoon into an upright oxygen jar (offhand).
        if (main.getItem() instanceof CombustionSpoonItem && CombustionEngine.isLit(main)) {
            if (BottleCodes.isGasBottle(off) && "oxygen".equals(BottleCodes.gasIdOf(off))) {
                player.setItemInHand(InteractionHand.OFF_HAND, ModItems.gasBottle("oxygen", false));
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
                if (com.example.chemistry.transfer.BottleQuantities.putTool(main,off)) {
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
                event.setCanceled(true);
                if (sealed) {
                    event.getEntity().displayClientMessage(
                            Component.translatable("mchemistry.vessel.sealed"), true);
                    event.setCancellationResult(InteractionResult.FAIL);
                    return;
                }
                if (DropperHelper.pour(main,off,player.isShiftKeyDown())) {
                    ReactionEngine.checkAndStart(off, player);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                } else {
                    event.getEntity().displayClientMessage(
                            Component.translatable("mchemistry.vessel.full"), true);
                    event.setCancellationResult(InteractionResult.FAIL);
                }
                return;
            }
            String liquidId = liquidIdOf(main);
            if (liquidId != null) {
                if (sealed) {
                    event.getEntity().displayClientMessage(
                            Component.translatable("mchemistry.vessel.sealed"), true);
                    event.setCancellationResult(InteractionResult.FAIL);
                    return;
                }
                if (com.example.chemistry.transfer.BottleQuantities.pour(main,off,25)) {
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
        String solidId = solidIdOf(off);
        if (solidId != null && main.getItem() instanceof SolidToolItem tool && SolidToolItem.isEmpty(main)) {
            boolean lumps = Solids.ALL.stream()
                    .filter(s -> s.id().equals(solidId))
                    .map(s -> s.form() == Solids.SolidForm.LUMP)
                    .findFirst().orElse(false);
            if (tool.holdsLumps() == lumps) {
                com.example.chemistry.transfer.BottleQuantities.takeSolid(off,main);
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
        // 点燃手持玻璃导管喷出的气体（右键任意方块时也优先点燃）。
        if (GlassTubeIgnition.tryIgnite(event.getLevel(), player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        // 酒精灯 / 酒精喷灯右键铁架台：挂到架子上（块物品默认会被拿去放
        // 到旁边的空气里，这里拦截并改为挂载）。
        if ((main.is(ModItems.ALCOHOL_LAMP.get()) || main.is(ModItems.ALCOHOL_BLOWTORCH.get()))
                && event.getLevel().getBlockState(event.getPos()).is(ModBlocks.IRON_STAND.get())) {
            var st = event.getLevel().getBlockState(event.getPos());
            if (!st.getValue(com.example.chemistry.block.IronStandBlock.HAS_LAMP)) {
                if (!event.getLevel().isClientSide()) {
                    boolean blowtorch = main.is(ModItems.ALCOHOL_BLOWTORCH.get());
                    event.getLevel().setBlock(event.getPos(),
                            st.setValue(com.example.chemistry.block.IronStandBlock.HAS_LAMP, true)
                                    .setValue(com.example.chemistry.block.IronStandBlock.LAMP_LIT, false),
                            3);
                    if (event.getLevel().getBlockEntity(event.getPos())
                            instanceof com.example.chemistry.blockentity.IronStandBlockEntity standBe) {
                        standBe.setLampBlowtorch(blowtorch);
                    }
                    main.shrink(1);
                    event.getLevel().playSound(null, event.getPos(), SoundEvents.GLASS_PLACE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        // Light a plain splint on fire / a lit alcohol lamp.
        if (main.getItem() instanceof SplintItem splint && !splint.isGlowing()) {
            var clicked = event.getLevel().getBlockState(event.getPos());
            boolean fire = clicked.is(Blocks.FIRE) || clicked.is(Blocks.SOUL_FIRE);
            boolean lamp = clicked.is(ModBlocks.ALCOHOL_LAMP.get())
                    && clicked.getValue(AlcoholLampBlock.LIT);
            if (fire || lamp) {
                var lit=new ItemStack(ModItems.GLOWING_SPLINT.get());
                main.shrink(1);
                if(main.isEmpty())player.setItemInHand(InteractionHand.MAIN_HAND,lit);
                else if(!player.getInventory().add(lit))player.drop(lit,false);
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
        // 潜行+右键试管架：放倒置试管（普通右键由方块处理正置）。
        // 原版潜行交互会绕过方块逻辑，这里直接拦截，避免放进不了。
        if (player.isShiftKeyDown()
                && event.getLevel().getBlockState(event.getPos()).is(ModBlocks.TEST_TUBE_RACK.get())
                && main.getItem() instanceof TestTubeItem tt && !tt.isClamped()) {
            if (event.getLevel().getBlockEntity(event.getPos())
                    instanceof com.example.chemistry.blockentity.TestTubeRackBlockEntity rbe) {
                int slot = rbe.pickSlot(player, event.getPos(), false);
                if (slot >= 0 && !rbe.getTube(slot).isEmpty()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.rack.slot_occupied"), true);
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.FAIL);
                    return;
                }
                if (slot < 0) {
                    slot = rbe.firstEmpty();
                }
                if (slot >= 0) {
                    rbe.setTube(slot, main.copy(), true);
                    main.shrink(1);
                    event.getLevel().playSound(null, event.getPos(), SoundEvents.GLASS_PLACE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        // 潜行+右键放下的量筒：倒出 1mL（原版潜行交互绕过方块逻辑，这里拦截）。
        if (player.isShiftKeyDown() && main.isEmpty()
                && event.getLevel().getBlockState(event.getPos()).is(ModBlocks.PLACED_GRADUATED_CYLINDER.get())
                && event.getLevel().getBlockEntity(event.getPos())
                        instanceof PlacedGraduatedCylinderBlockEntity cbe
                && !cbe.getCylinder().isEmpty()) {
            String liquid = GraduatedCylinderItem.getLiquid(cbe.getCylinder());
            if (liquid != null && GraduatedCylinderItem.getMl(cbe.getCylinder()) >= 1) {
                GraduatedCylinderItem.set(cbe.getCylinder(), liquid,
                        GraduatedCylinderItem.getMl(cbe.getCylinder()) - 1);
                cbe.setCylinder(cbe.getCylinder());
                player.displayClientMessage(
                        Component.translatable("mchemistry.cylinder.pour_out"), true);
                event.getLevel().playSound(null, event.getPos(), SoundEvents.BOTTLE_EMPTY,
                        SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        ItemStack placedTube = be instanceof IronStandBlockEntity ibe ? ibe.getTube() : null;
        // 敞口集气瓶右键铁架台上的试管：倒一部分气体进去。
        if (placedTube != null && !placedTube.isEmpty() && openGasIdOf(main) != null) {
            pourGas(main, placedTube, player);
            ((IronStandBlockEntity) be).setTube(placedTube);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        // Right-click the iron-stand tube with a medicine tool to fill it.
        if (placedTube != null && !placedTube.isEmpty() && isMedicineTool(main)) {
            if (tryFill(placedTube, main, player)) {
                ((IronStandBlockEntity) be).setTube(placedTube);
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
        }
    }

    /** 空集气瓶走 BlockItem 放置（不经过 GasBottleItem.place），这里把
     *  标签文字带到方块实体上，拾取时再写回物品。 */
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)
                || !event.getPlacedBlock().is(ModBlocks.GAS_COLLECTING_BOTTLE.get())
                || !(event.getEntity() instanceof Player player)
                || !(serverLevel.getBlockEntity(event.getPos())
                        instanceof GasCollectingBottleBlockEntity be)) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        CompoundTag tag = held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String label = tag.getStringOr(LabelItem.KEY_LABEL, "");
        if (!label.isEmpty()) {
            be.setLabelName(label);
        }
    }

    /** Add a held tool / dropper / liquid bottle's contents into a vessel and
     *  kick the reaction engine. Shared by held-vessel transfers (above) and
     *  placed vessels on the iron stand / tripod. */
    public static boolean addToVessel(ItemStack main, ItemStack vessel, Player player) {
        if(main.getItem() instanceof com.example.chemistry.organic.PhasePipetteItem)return com.example.chemistry.organic.PhasePipetteItem.interact(player,main,vessel);
        if (!(vessel.getItem() instanceof LabVesselItem)) {
            return false;
        }
        if (main.getItem() instanceof SolidToolItem && !SolidToolItem.isEmpty(main)) {
            if (com.example.chemistry.transfer.BottleQuantities.putTool(main,vessel)) {
                SolidToolItem.clear(main);
                if (!ReactionEngine.checkAndStart(vessel, player)) {
                    ExperimentFeedback.send(player,Component.translatable("mchemistry.no_reaction"));
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
                    ExperimentFeedback.send(player,Component.translatable("mchemistry.no_reaction"));
                }
                return true;
            }
            player.displayClientMessage(Component.translatable("mchemistry.vessel.full"), true);
            return false;
        }
        if (main.getItem() instanceof DropperItem && !DropperHelper.isEmpty(main)) {
            if (DropperHelper.pour(main,vessel,player.isShiftKeyDown())) {
                if (!ReactionEngine.checkAndStart(vessel, player)) {
                    ExperimentFeedback.send(player,Component.translatable("mchemistry.no_reaction"));
                }
                return true;
            }
            player.displayClientMessage(Component.translatable("mchemistry.vessel.full"), true);
            return false;
        }
        String liquidId = liquidIdOf(main);
        if (liquidId != null) {
            if (com.example.chemistry.transfer.BottleQuantities.pour(main,vessel,25)) {
                if (!ReactionEngine.checkAndStart(vessel, player)) {
                    ExperimentFeedback.send(player,Component.translatable("mchemistry.no_reaction"));
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
        if(main.getItem() instanceof com.example.chemistry.organic.PhasePipetteItem)return true;
        if (openGasIdOf(main) != null) {
            return true;
        }
        if (main.getItem() instanceof SolidToolItem && !SolidToolItem.isEmpty(main)) {
            return true;
        }
        if (looseSolidId(main) != null) {
            return true;
        }
        if (main.getItem() instanceof DropperItem && !DropperHelper.isEmpty(main)) {
            return true;
        }
        return liquidIdOf(main) != null;
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
        if(main.getItem() instanceof com.example.chemistry.organic.PhasePipetteItem)return com.example.chemistry.organic.PhasePipetteItem.interact(player,main,vessel);
        // 烧杯右键敞口放置容器：把烧杯里的液体倒进去。
        if (isBeaker(main)) {
            pourBeaker(main, vessel, player);
            return true;
        }
        // 玻璃棒右键敞口放置容器：搅拌。
        if (main.is(ModItems.GLASS_ROD.get())) {
            if(player.isShiftKeyDown())GlassRodSampling.dip(player,main,vessel);else stir(player, vessel);
            return true;
        }
        // 用温度计右键敞口容器：直接读出当前温度（密封容器在方块处理器里
        // 会优先把温度计插进橡胶塞）。
        if (main.is(ModItems.THERMOMETER.get())) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.thermometer.read",
                            String.format("%.0f", TemperatureSystem.getTemp(vessel))), true);
            return true;
        }
        boolean sealed = VesselHeating.isSealed(vessel);
        // 敞口集气瓶右键放置的容器：倒一部分气体进去。
        if (openGasIdOf(main) != null) {
            pourGas(main, vessel, player);
            return true;
        }
        if (!sealed) {
            return addToVessel(main, vessel, player);
        }
        boolean hasDropper = (!attached1.isEmpty() && DropperHelper.isDropper(attached1))
                || (!attached2.isEmpty() && DropperHelper.isDropper(attached2));
        boolean hasFunnel = (!attached1.isEmpty() && attached1.is(ModItems.LONG_STEM_FUNNEL.get()))
                || (!attached2.isEmpty() && attached2.is(ModItems.LONG_STEM_FUNNEL.get()));
        if (main.getItem() instanceof DropperItem && !DropperHelper.isEmpty(main)) {
            if (hasDropper) {
                if (DropperHelper.pour(main,vessel,player.isShiftKeyDown())) {
                    ReactionEngine.checkAndStart(vessel, player);
                }
                return true;
            }
            player.displayClientMessage(Component.translatable("mchemistry.vessel.sealed"), true);
            return true;
        }
        String liquidId = liquidIdOf(main);
        if (liquidId != null) {
            if (hasFunnel) {
                if (com.example.chemistry.transfer.BottleQuantities.pour(main,vessel,25)) {
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
        if (liquidIdOf(main) != null) {
            return hasFunnel;
        }
        return isTransferTool(main);
    }

    private static boolean isMedicineTool(ItemStack main) {
        if(main.getItem() instanceof com.example.chemistry.organic.PhasePipetteItem)return true;
        if (main.getItem() instanceof SolidToolItem tool) {
            return !SolidToolItem.isEmpty(main);
        }
        if (main.getItem() instanceof DropperItem) {
            return !DropperHelper.isEmpty(main);
        }
        return liquidIdOf(main) != null;
    }

    /** Fills a held/placed tube with the main-hand medicine tool. */
    private static boolean tryFill(ItemStack tube, ItemStack main, Player player) {
        if(main.getItem() instanceof com.example.chemistry.organic.PhasePipetteItem)return com.example.chemistry.organic.PhasePipetteItem.interact(player,main,tube);
        if (main.getItem() instanceof SolidToolItem tool && !SolidToolItem.isEmpty(main)) {
            if (com.example.chemistry.transfer.BottleQuantities.putTool(main,tube)) {
                SolidToolItem.clear(main);
                ReactionEngine.checkAndStart(tube, player);
                return true;
            }
            return false;
        }
        if (main.getItem() instanceof DropperItem && !DropperHelper.isEmpty(main)) {
            if (DropperHelper.pour(main,tube,player.isShiftKeyDown())) {
                ReactionEngine.checkAndStart(tube, player);
                return true;
            }
            return false;
        }
        String liquidId = liquidIdOf(main);
        if (liquidId != null && com.example.chemistry.transfer.BottleQuantities.pour(main,tube,25)) {
            ReactionEngine.checkAndStart(tube, player);
            return true;
        }
        return false;
    }

    /** 每次敞口集气瓶倒气量（mL）。 */
    private static final int GAS_POUR_ML = 25;

    /** 敞口集气瓶 → 反应容器倒气（密封/满时提示）。 */
    private static void pourGas(ItemStack bottle, ItemStack vessel, Player player) {
        String gasId = openGasIdOf(bottle);
        if (gasId == null || !(vessel.getItem() instanceof LabVesselItem)) {
            return;
        }
        if (VesselHeating.isSealed(vessel)) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.gas_pour.sealed"), true);
            return;
        }
        if (VesselGasPhase.freeVolumeMl(vessel) < 1.0) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.gas_pour.full"), true);
            return;
        }
        VesselGasPhase.pour(vessel, gasId, GAS_POUR_ML);
        player.displayClientMessage(
                Component.translatable("mchemistry.gas_pour.success",
                        Component.literal(ChemGoggleLines.gasName(gasId)), GAS_POUR_ML), true);
    }

    /** 玻璃棒搅拌：提示 + 触发反应引擎检测。 */
    private static void stir(Player player, ItemStack vessel) {
        // 玻璃棒每 tick 最多使用 1 次。
        var data = player.getPersistentData();
        long now = player.level().getGameTime();
        if (data.getLong("chem_last_stir").orElse(-1L) == now) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.stir.cooldown"), true);
            return;
        }
        data.putLong("chem_last_stir", now);
        player.displayClientMessage(
                Component.translatable("mchemistry.stir"), true);
        PhaseSystem.dissolveAndCrystallize(vessel,true);
        ReactionEngine.checkAndStart(vessel, player);
        ReactionEngine.tick(vessel, player);
    }

    /** 主手是否烧杯。 */
    private static boolean isBeaker(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()
                .startsWith("beaker_");
    }

    /** 烧杯里的液体倒入目标容器（倒空烧杯）。 */
    private static void pourBeaker(ItemStack beaker, ItemStack target, Player player) {
        if (LabVesselItem.transferLiquids(beaker, target)) {
            player.displayClientMessage(Component.translatable("mchemistry.cylinder.pour_all"), true);
            ReactionEngine.checkAndStart(target, player);
        } else {
            boolean hasLiquid = LabVesselItem.getContents(beaker).stream()
                    .anyMatch(e -> e.type().equals("liquid") && e.amount() > 0);
            player.displayClientMessage(Component.translatable(hasLiquid
                    ? "mchemistry.vessel.full" : "mchemistry.beaker.empty"), true);
        }
    }

    /** 玻璃塞连接（直接逐个塞满 3 个瓶口后密封，无需磨口）。 */
    private static void connectGlassStopper(ItemStack stopper, ItemStack flask, Player player) {
        if (VesselHeating.neckStopperCount(flask) >= 3) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.flask.sealed_all"), true);
            return;
        }
        int neck = -1;
        for (int i = 0; i < 3; i++) {
            if (!VesselHeating.neckHasStopper(flask, i)) {
                neck = i;
                break;
            }
        }
        VesselHeating.setNeckStopper(flask, neck, true);
        stopper.shrink(1);
        int count = VesselHeating.neckStopperCount(flask);
        player.displayClientMessage(
                Component.translatable("mchemistry.flask.stoppered_n",
                        count, count >= 3 ? "，三颈烧瓶已全部密封" : ""), true);
    }

    /** 在三颈瓶上按点击位置塞一个玻璃塞（放在任意挂载点上，传入该挂载点的变换）。 */
    public static boolean tryPlugNeck(ItemStack flask, ItemStack stopper, Player player,
            BlockPos pos, Vec3 click, double scale, double offX, double offY,
            double offZ, double yaw) {
        if (!VesselHeating.isThreeNeck(flask)) {
            return false;
        }
        int neck = VesselHeating.neckForRay(
                VesselHeating.neckWorldPositions(pos, scale, offX, offY, offZ, yaw), player);
        if (neck < 0) {
            // 射线没命中时退回点击点最近瓶口，保证三个颈都能塞。
            neck = VesselHeating.neckForClick(
                    VesselHeating.neckWorldPositions(pos, scale, offX, offY, offZ, yaw), click);
        }
        if (neck < 0) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.flask.aim_neck"), true);
            return true;
        }
        if (VesselHeating.neckHasStopper(flask, neck)) {
            player.displayClientMessage(Component.translatable("mchemistry.flask.neck_occupied"), true);
            return true;
        }
        VesselHeating.setNeckStopper(flask, neck, true);
        stopper.shrink(1);
        player.level().playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.9F, 1.0F);
        if (VesselHeating.neckStopperCount(flask) >= 3) {
            player.displayClientMessage(Component.translatable("mchemistry.flask.sealed_all"), true);
        } else {
            player.displayClientMessage(
                    Component.translatable("mchemistry.flask.stoppered_neck", neck + 1), true);
        }
        return true;
    }

    /** 射线选颈（三颈瓶）：命中半径 0.35，返回 -1 表示没对准任何瓶口。 */
    public static int pickNeck(Player player, BlockPos pos, Vec3 click,
            double scale, double offX, double offY, double offZ, double yaw) {
        return VesselHeating.neckForRay(
                VesselHeating.neckWorldPositions(pos, scale, offX, offY, offZ, yaw), player);
    }

    /** 拆下三颈瓶指定瓶口的玻璃塞；返回是否发生了拆塞。 */
    public static boolean tryUnplugNeck(ItemStack flask, int neck, Player player) {
        if (!VesselHeating.isThreeNeck(flask) || neck < 0 || neck > 2) {
            return false;
        }
        if ((VesselHeating.neckStopperMask(flask) & (1 << neck)) == 0) {
            return false;
        }
        VesselHeating.setNeckStopper(flask, neck, false);
        ItemStack plug = new ItemStack(ModItems.GLASS_STOPPER.get());
        if (!player.getInventory().add(plug)) {
            player.drop(plug, false);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_PLACE,
                SoundSource.BLOCKS, 0.9F, 1.0F);
        return true;
    }

    /** 在三颈瓶上按点击位置塞一个带孔橡胶塞（holes 由物品决定）。 */
    public static boolean tryPlugRubberNeck(ItemStack flask, ItemStack stopper, Player player,
            BlockPos pos, Vec3 click, int holes, double scale, double offX, double offY,
            double offZ, double yaw) {
        if (!VesselHeating.isThreeNeck(flask)) {
            return false;
        }
        int neck = VesselHeating.neckForRay(
                VesselHeating.neckWorldPositions(pos, scale, offX, offY, offZ, yaw), player);
        if (neck < 0) {
            neck = VesselHeating.neckForClick(
                    VesselHeating.neckWorldPositions(pos, scale, offX, offY, offZ, yaw), click);
        }
        if (neck < 0) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.flask.aim_neck"), true);
            return true;
        }
        if (VesselHeating.neckHasStopper(flask, neck)) {
            player.displayClientMessage(Component.translatable("mchemistry.flask.neck_occupied"), true);
            return true;
        }
        VesselHeating.sealNeck(flask, neck, holes);
        stopper.shrink(1);
        player.level().playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(
                Component.translatable("mchemistry.flask.stoppered_neck", neck + 1), true);
        return true;
    }

    /** 拆下三颈瓶指定瓶口的橡胶塞；返回是否发生了拆除。 */
    public static boolean tryUnplugRubberNeck(ItemStack flask, int neck, Player player) {
        if (!VesselHeating.isThreeNeck(flask) || neck < 0 || neck > 2) {
            return false;
        }
        int holes = VesselHeating.rubberHoles(flask, neck);
        if (holes == 0) {
            return false;
        }
        VesselHeating.unsealNeck(flask, neck);
        ItemStack stopper = new ItemStack(ModItems.stopperForHoles(holes));
        if (!player.getInventory().add(stopper)) {
            player.drop(stopper, false);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.WOOL_BREAK,
                SoundSource.BLOCKS, 0.9F, 1.0F);
        return true;
    }

    /** 敞口集气瓶 → 气体 id，非敞口集气瓶返回 null。 */
    private static String openGasIdOf(ItemStack stack) {
        if (stack.isEmpty() || !BottleCodes.isGasBottle(stack) || BottleCodes.isSealed(stack)) {
            return null;
        }
        return BottleCodes.gasIdOf(stack);
    }

    private static String liquidIdOf(ItemStack stack) {
        if (stack.isEmpty() || !BottleCodes.isLiquidBottle(stack)) {
            return null;
        }
        return BottleCodes.liquidIdOf(stack);
    }

    private static String solidIdOf(ItemStack stack) {
        if (stack.isEmpty() || !BottleCodes.isSolidJar(stack) || BottleCodes.isSealed(stack)) {
            return null;
        }
        return BottleCodes.solidIdOf(stack);
    }

    /** 对空右键：扫描附近铁架台，用瓶口端口虚拟选中框命中并插塞。 */
    private static boolean routeStopperViaPort(Level level, Player player, ItemStack stopper) {
        boolean glass = stopper.is(ModItems.GLASS_STOPPER.get());
        BlockPos c = player.blockPosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (!(level.getBlockEntity(p) instanceof IronStandBlockEntity be)) {
                        continue;
                    }
                    ItemStack flask = be.getVessel();
                    if (flask.isEmpty()) {
                        continue;
                    }
                    double yaw = -level.getBlockState(p)
                            .getValue(com.example.chemistry.block.IronStandBlock.FACING).toYRot();
                    if (glass) {
                        if (!VesselHeating.isThreeNeck(flask)
                                || !VesselHeating.isGrounded(flask)) {
                            continue;
                        }
                        int neck = VesselHeating.neckForRay(
                                VesselHeating.neckWorldPositions(p,
                                        com.example.chemistry.block.IronStandBlock.VESSEL_SCALE,
                                        com.example.chemistry.block.IronStandBlock.VESSEL_OFF_X,
                                        com.example.chemistry.block.IronStandBlock.VESSEL_OFF_Y,
                                        com.example.chemistry.block.IronStandBlock.VESSEL_OFF_Z,
                                        yaw), player);
                        if (neck < 0 || VesselHeating.neckHasStopper(flask, neck)) {
                            continue;
                        }
                        VesselHeating.setNeckStopper(flask, neck, true);
                        stopper.shrink(1);
                        be.setVessel(flask);
                        level.playSound(null, p, SoundEvents.GLASS_PLACE,
                                SoundSource.BLOCKS, 0.9F, 1.0F);
                        player.displayClientMessage(Component.translatable(
                                "mchemistry.flask.stoppered_neck", neck + 1), true);
                        return true;
                    }
                    // 橡胶塞：命中瓶口端口区域（三颈瓶需命中空闲瓶口）。
                    if (VesselHeating.isSealed(flask)) {
                        continue;
                    }
                    if (VesselHeating.isThreeNeck(flask)) {
                        int neck = VesselHeating.neckForRay(
                                VesselHeating.neckWorldPositions(p,
                                        com.example.chemistry.block.IronStandBlock.VESSEL_SCALE,
                                        com.example.chemistry.block.IronStandBlock.VESSEL_OFF_X,
                                        com.example.chemistry.block.IronStandBlock.VESSEL_OFF_Y,
                                        com.example.chemistry.block.IronStandBlock.VESSEL_OFF_Z,
                                        yaw), player);
                        if (neck < 0) {
                            continue;
                        }
                    } else {
                        // 圆底/锥形瓶：瓶口端口虚拟选中框命中才可塞。
                        if (!hitsPoint(player, vesselMouthWorld(p, flask, yaw))) {
                            continue;
                        }
                    }
                    int holes = stopper.is(ModItems.RUBBER_STOPPER_3_HOLE.get()) ? 3
                            : stopper.is(ModItems.RUBBER_STOPPER_2_HOLE.get()) ? 2 : 1;
                    VesselHeating.seal(flask, holes);
                    stopper.shrink(1);
                    be.setVessel(flask);
                    level.playSound(null, p, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return true;
                }
            }
        }
        return false;
    }

    /** 圆底/锥形瓶瓶口的世界坐标（VESSEL 变换 + 瓶口偏移，按朝向旋转）。 */
    private static net.minecraft.world.phys.Vec3 vesselMouthWorld(BlockPos pos, ItemStack flask,
            double yawDegrees) {
        double s = com.example.chemistry.block.IronStandBlock.VESSEL_SCALE;
        double x = pos.getX() + com.example.chemistry.block.IronStandBlock.VESSEL_OFF_X
                + s * 8.5 / 16.0;
        double y = pos.getY() + com.example.chemistry.block.IronStandBlock.VESSEL_OFF_Y
                + s * 10.0 / 16.0;
        double z = pos.getZ() + com.example.chemistry.block.IronStandBlock.VESSEL_OFF_Z
                + s * 8.5 / 16.0;
        double yaw = Math.toRadians(yawDegrees);
        double c = Math.cos(yaw), sn = Math.sin(yaw);
        double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
        double dx = x - cx, dz = z - cz;
        return new net.minecraft.world.phys.Vec3(cx + dx * c + dz * sn, y, cz - dx * sn + dz * c);
    }

    /** 射线是否命中一个点的虚拟选中框（±0.18 格）。 */
    private static boolean hitsPoint(Player player, net.minecraft.world.phys.Vec3 point) {
        net.minecraft.world.phys.Vec3 from = player.getEyePosition();
        net.minecraft.world.phys.Vec3 to = from.add(player.getLookAngle().scale(6.0));
        return new net.minecraft.world.phys.AABB(
                point.x - 0.18, point.y - 0.18, point.z - 0.18,
                point.x + 0.18, point.y + 0.18, point.z + 0.18).clip(from, to).isPresent();
    }
}
