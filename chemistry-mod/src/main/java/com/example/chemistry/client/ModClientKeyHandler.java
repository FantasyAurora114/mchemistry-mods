package com.example.chemistry.client;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.registry.ModBlocks;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.network.ChemistryNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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
