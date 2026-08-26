package com.example.mci.network;

import com.example.mci.ChemistryMod;
import com.example.mci.blockentity.SynthesisTowerBlockEntity;
import com.example.chemistry.registry.ModFluids;
import com.example.mci.storage.ChemGasTank;
import com.example.mci.storage.TowerFluidTank;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** MCI networking: server->client synthesis-tower tank sync. */
public final class ChemistryNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SynthesisTowerSyncPacket.TYPE, SynthesisTowerSyncPacket.STREAM_CODEC,
                SynthesisTowerSyncPacket::handle);
    }

    public static void sendTowerSync(ServerPlayer player, SynthesisTowerBlockEntity tower) {
        ChemGasTank gas = tower.getGasTank();
        TowerFluidTank fluid = tower.getFluidTank();
        String fluidId = ModFluids.liquidIdFor(fluid.getFluid());
        PacketDistributor.sendToPlayer(player, new SynthesisTowerSyncPacket(
                tower.getBlockPos(),
                gas.getGasId(), gas.getAmount(), gas.getPurity(),
                fluidId == null ? "" : fluidId, fluid.getAmount(), fluid.getPurity(),
                tower.getTemperature(), tower.getPressure()));
    }

    public record SynthesisTowerSyncPacket(BlockPos pos, String gasId, long gasAmount, double gasPurity,
            String fluidId, int fluidAmount, double fluidPurity,
            double temperature, double pressure) implements CustomPacketPayload {

        public static final Type<SynthesisTowerSyncPacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "synthesis_tower_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SynthesisTowerSyncPacket> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, SynthesisTowerSyncPacket::pos,
                        ByteBufCodecs.STRING_UTF8, SynthesisTowerSyncPacket::gasId,
                        ByteBufCodecs.VAR_LONG, SynthesisTowerSyncPacket::gasAmount,
                        ByteBufCodecs.DOUBLE, SynthesisTowerSyncPacket::gasPurity,
                        ByteBufCodecs.STRING_UTF8, SynthesisTowerSyncPacket::fluidId,
                        ByteBufCodecs.VAR_INT, SynthesisTowerSyncPacket::fluidAmount,
                        ByteBufCodecs.DOUBLE, SynthesisTowerSyncPacket::fluidPurity,
                        ByteBufCodecs.DOUBLE, SynthesisTowerSyncPacket::temperature,
                        ByteBufCodecs.DOUBLE, SynthesisTowerSyncPacket::pressure,
                        SynthesisTowerSyncPacket::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SynthesisTowerSyncPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.flow().isClientbound() && Minecraft.getInstance().level != null) {
                    BlockEntity be = Minecraft.getInstance().level.getBlockEntity(packet.pos());
                    if (be instanceof SynthesisTowerBlockEntity tower) {
                        tower.applyClientSync(packet.gasId(), packet.gasAmount(), packet.gasPurity(),
                                packet.fluidId(), packet.fluidAmount(), packet.fluidPurity(),
                                packet.temperature(), packet.pressure());
                    }
                }
            });
        }
    }

    private ChemistryNetworking() {
    }
}
