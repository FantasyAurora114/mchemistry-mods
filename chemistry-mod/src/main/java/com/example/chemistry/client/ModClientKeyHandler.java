package com.example.chemistry.client;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.entity.PlacedVesselEntity;
import com.example.chemistry.registry.ModBlocks;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.network.ChemistryNetworking;
import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.block.TripodBlock;
import com.example.chemistry.blockentity.HeatingMantleBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class ModClientKeyHandler {

    @EventBusSubscriber(modid = ChemistryMod.MODID, value = Dist.CLIENT)
    public static class KeyRegistration {

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(ModKeyMappings.SNIFF);
            event.register(ModKeyMappings.SQUEEZE_DROPPER);
            event.register(ModKeyMappings.FIX_TEMP);
            event.register(ModKeyMappings.HEAT_UP);
            event.register(ModKeyMappings.HEAT_DOWN);
            event.register(ModKeyMappings.SHOW_PORTS);
        }
    }

    @EventBusSubscriber(modid = ChemistryMod.MODID, value = Dist.CLIENT)
    public static class KeyPressHandler {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Pre event) {
            while (ModKeyMappings.SNIFF.consumeClick()) {
                ClientPacketDistributor.sendToServer(new ChemistryNetworking.SniffPacket());
            }
            while (ModKeyMappings.SQUEEZE_DROPPER.consumeClick()) {
                HitResult hit = Minecraft.getInstance().hitResult;
                if (hit instanceof BlockHitResult blockHit) {
                    BlockState state = Minecraft.getInstance().level.getBlockState(blockHit.getBlockPos());
                    if (state.is(ModBlocks.IRON_STAND.get())) {
                        ClientPacketDistributor.sendToServer(
                                new ChemistryNetworking.SqueezeDropperPacket(blockHit.getBlockPos()));
                    }
                }
            }
            while (ModKeyMappings.SHOW_PORTS.consumeClick()) {
                PortBoxOverlay.visible = !PortBoxOverlay.visible;
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    PortBoxOverlay.visible
                                            ? "mchemistry.ports.visible"
                                            : "mchemistry.ports.hidden"),
                            true);
                }
            }
            while (ModKeyMappings.FIX_TEMP.consumeClick()) {
                HitResult hit = Minecraft.getInstance().hitResult;
                if (Minecraft.getInstance().level != null) {
                    if (hit instanceof EntityHitResult entityHit
                            && entityHit.getEntity() instanceof PlacedVesselEntity) {
                        ClientPacketDistributor.sendToServer(
                                new ChemistryNetworking.FixTempPacket(
                                        entityHit.getEntity().blockPosition()));
                    } else if (hit instanceof BlockHitResult blockHit) {
                        BlockState state = Minecraft.getInstance().level
                                .getBlockState(blockHit.getBlockPos());
                        boolean apparatus = state.is(ModBlocks.PLACED_VESSEL.get())
                                || (state.is(ModBlocks.IRON_STAND.get())
                                        && state.getValue(IronStandBlock.HAS_VESSEL))
                                || (state.is(ModBlocks.TRIPOD.get())
                                        && state.getValue(TripodBlock.HAS_VESSEL));
                        if (apparatus) {
                            ClientPacketDistributor.sendToServer(
                                    new ChemistryNetworking.FixTempPacket(blockHit.getBlockPos()));
                        }
                    }
                }
            }
            // 对着加热套按上/下键调节设定温度。
            HitResult hit = Minecraft.getInstance().hitResult;
            if (hit instanceof BlockHitResult blockHit
                    && Minecraft.getInstance().level != null
                    && Minecraft.getInstance().level.getBlockState(blockHit.getBlockPos())
                            .is(ModBlocks.HEATING_MANTLE.get())) {
                int delta = 0;
                while (ModKeyMappings.HEAT_UP.consumeClick()) {
                    delta += HeatingMantleBlockEntity.TEMP_STEP;
                }
                while (ModKeyMappings.HEAT_DOWN.consumeClick()) {
                    delta -= HeatingMantleBlockEntity.TEMP_STEP;
                }
                if (delta != 0) {
                    ClientPacketDistributor.sendToServer(
                            new ChemistryNetworking.HeatMantlePacket(blockHit.getBlockPos(), delta));
                }
            }
        }
    }

    @EventBusSubscriber(modid = ChemistryMod.MODID, value = Dist.CLIENT)
    public static class ShakeByLeftClick {

        @SubscribeEvent
        public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
            if (event.getSide().isServer()) {
                return;
            }
            if (event.getEntity().getMainHandItem().getItem() instanceof LabVesselItem
                    || event.getEntity().getOffhandItem().getItem() instanceof LabVesselItem) {
                ClientPacketDistributor.sendToServer(new ChemistryNetworking.ShakePacket());
            }
        }
    }

    private ModClientKeyHandler() {
    }
}
