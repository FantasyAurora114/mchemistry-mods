package com.example.chemistry.network;

import java.util.Set;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.ReactionEngine;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.blockentity.GasCollectingBottleBlockEntity;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModBlocks;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Network messages: client->server key/action packets.
 */
public final class ChemistryNetworking {

    private static final Set<String> TOXIC_GAS_JARS = Set.of(
            "open_gas_collecting_bottle_hydrogen_sulfide",
            "open_gas_collecting_bottle_sulfur_dioxide",
            "open_gas_collecting_bottle_ammonia");

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SniffPacket.TYPE, SniffPacket.STREAM_CODEC, SniffPacket::handle);
        registrar.playToServer(ShakePacket.TYPE, ShakePacket.STREAM_CODEC, ShakePacket::handle);
        registrar.playToServer(SqueezeDropperPacket.TYPE, SqueezeDropperPacket.STREAM_CODEC,
                SqueezeDropperPacket::handle);
    }

    public record SqueezeDropperPacket(BlockPos pos) implements CustomPacketPayload {
        public static final Type<SqueezeDropperPacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "squeeze_dropper"));
        public static final StreamCodec<ByteBuf, SqueezeDropperPacket> STREAM_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, SqueezeDropperPacket::pos,
                        SqueezeDropperPacket::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SqueezeDropperPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    IronStandBlock.squeezeAt(player.level(), packet.pos(), player);
                }
            });
        }
    }
    public record SniffPacket() implements CustomPacketPayload {
        public static final Type<SniffPacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "sniff"));
        public static final StreamCodec<ByteBuf, SniffPacket> STREAM_CODEC =
                StreamCodec.unit(new SniffPacket());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SniffPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    sniff(player);
                }
            });
        }
    }

    public record ShakePacket() implements CustomPacketPayload {
        public static final Type<ShakePacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "shake"));
        public static final StreamCodec<ByteBuf, ShakePacket> STREAM_CODEC =
                StreamCodec.unit(new ShakePacket());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ShakePacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    shake(player);
                }
            });
        }
    }

    private static void sniff(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        String path = BuiltInRegistries.ITEM.getKey(held.getItem()).getPath();

        if (path.equals("open_gas_collecting_bottle_chlorine")) {
            player.kill((net.minecraft.server.level.ServerLevel) player.level());
            return;
        }
        if (path.equals("open_liquid_ammonia_water_concentrated")) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            return;
        }
        if (path.equals("open_liquid_ammonia_water")) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            return;
        }
        if (TOXIC_GAS_JARS.contains(path)) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            return;
        }
        // Nothing sniffable in hand: sniff a placed OPEN bottle being looked at.
        HitResult hit = player.pick(5.0D, 1.0F, false);
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = player.level().getBlockState(pos);
            if (state.is(ModBlocks.GAS_COLLECTING_BOTTLE.get())
                    && !state.getValue(GasCollectingBottleBlock.HAS_PLATE)
                    && player.level().getBlockEntity(pos) instanceof GasCollectingBottleBlockEntity be) {
                String gas = be.getGasId();
                if (gas.equals("chlorine")) {
                    player.kill((net.minecraft.server.level.ServerLevel) player.level());
                } else if (Set.of("hydrogen_sulfide", "sulfur_dioxide", "ammonia").contains(gas)) {
                    player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                }
            }
        }
    }

    private static void shake(ServerPlayer player) {
        ItemStack vessel = player.getMainHandItem();
        if (!(vessel.getItem() instanceof LabVesselItem)) {
            vessel = player.getOffhandItem();
        }
        if (vessel.getItem() instanceof LabVesselItem) {
            ReactionEngine.checkAndStart(vessel, player);
        }
    }

    private ChemistryNetworking() {
    }
}
