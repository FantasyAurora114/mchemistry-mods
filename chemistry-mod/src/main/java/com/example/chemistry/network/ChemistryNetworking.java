package com.example.chemistry.network;

// Valer1ya: keep the registrar version in sync with every codec change.

import java.util.Set;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.ReactionEngine;
import com.example.chemistry.api.ReactionUnlocks;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.blockentity.GasCollectingBottleBlockEntity;
import com.example.chemistry.blockentity.HeatingMantleBlockEntity;
import com.example.chemistry.blockentity.IronStandBlockEntity;
import com.example.chemistry.blockentity.PlacedVesselBlockEntity;
import com.example.chemistry.blockentity.TripodBlockEntity;
import com.example.chemistry.entity.PlacedVesselEntity;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModBlocks;
import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.VesselHeating;
import com.example.chemistry.transfer.BottleCodes;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

    /** 协议修订标记（Valer1ya）。 */
    private static final String PROTOCOL_REV = "Valer1ya-1";

    private static final Set<String> TOXIC_GAS_JARS = Set.of(
            "open_gas_collecting_bottle_hydrogen_sulfide",
            "open_gas_collecting_bottle_sulfur_dioxide",
            "open_gas_collecting_bottle_ammonia");

    /** 手册解锁响应的客户端处理器（由客户端入口注入，服务端为空实现）。 */
    private static java.util.function.BiConsumer<HandbookUnlockResponsePacket, IPayloadContext>
            unlockResponseHandler = (packet, context) -> { };

    public static void setUnlockResponseHandler(
            java.util.function.BiConsumer<HandbookUnlockResponsePacket, IPayloadContext> handler) {
        if (handler != null) {
            unlockResponseHandler = handler;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SniffPacket.TYPE, SniffPacket.STREAM_CODEC, SniffPacket::handle);
        registrar.playToServer(ShakePacket.TYPE, ShakePacket.STREAM_CODEC, ShakePacket::handle);
        registrar.playToServer(SqueezeDropperPacket.TYPE, SqueezeDropperPacket.STREAM_CODEC,
                SqueezeDropperPacket::handle);
        registrar.playToServer(FixTempPacket.TYPE, FixTempPacket.STREAM_CODEC, FixTempPacket::handle);
        registrar.playToServer(HeatMantlePacket.TYPE, HeatMantlePacket.STREAM_CODEC,
                HeatMantlePacket::handle);
        registrar.playToServer(HandbookUnlockRequestPacket.TYPE, HandbookUnlockRequestPacket.STREAM_CODEC,
                HandbookUnlockRequestPacket::handle);
        registrar.playToClient(HandbookUnlockResponsePacket.TYPE, HandbookUnlockResponsePacket.STREAM_CODEC,
                HandbookUnlockResponsePacket::handle);
    }

    /** 客户端请求：化学手册打开时向服务器索取已解锁反应列表。 */
    public record HandbookUnlockRequestPacket() implements CustomPacketPayload {
        public static final Type<HandbookUnlockRequestPacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "handbook_unlock_request"));
        public static final StreamCodec<ByteBuf, HandbookUnlockRequestPacket> STREAM_CODEC =
                StreamCodec.unit(new HandbookUnlockRequestPacket());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(HandbookUnlockRequestPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    PacketDistributor.sendToPlayer(player,
                            buildUnlockResponse(player));
                }
            });
        }
    }

    /** 向某玩家主动推送当前手册解锁/发现状态（指令等场景使用）。 */
    public static void sendHandbookUnlockTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildUnlockResponse(player));
    }

    private static HandbookUnlockResponsePacket buildUnlockResponse(ServerPlayer player) {
        return new HandbookUnlockResponsePacket(
                ReactionUnlocks.all(player), ReactionUnlocks.allSubstances(player));
    }

    /** 服务器响应：已解锁反应 + 已发现物质。 */
    public record HandbookUnlockResponsePacket(
            java.util.List<String> unlocked, java.util.List<String> discovered)
            implements CustomPacketPayload {
        public static final Type<HandbookUnlockResponsePacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "handbook_unlock_response"));
        public static final StreamCodec<ByteBuf, HandbookUnlockResponsePacket> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                        HandbookUnlockResponsePacket::unlocked,
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                        HandbookUnlockResponsePacket::discovered,
                        HandbookUnlockResponsePacket::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(HandbookUnlockResponsePacket packet, IPayloadContext context) {
            unlockResponseHandler.accept(packet, context);
        }
    }

    /** 加热套上/下键：调节设定温度。 */
    public record HeatMantlePacket(BlockPos pos, int delta) implements CustomPacketPayload {
        public static final Type<HeatMantlePacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "heat_mantle"));
        public static final StreamCodec<ByteBuf, HeatMantlePacket> STREAM_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, HeatMantlePacket::pos,
                        ByteBufCodecs.VAR_INT, HeatMantlePacket::delta,
                        HeatMantlePacket::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(HeatMantlePacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                        && player.level().getBlockEntity(packet.pos())
                                instanceof HeatingMantleBlockEntity be) {
                    be.adjustSetTemp(packet.delta());
                }
            });
        }
    }

    public record FixTempPacket(BlockPos pos) implements CustomPacketPayload {
        public static final Type<FixTempPacket> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ChemistryMod.MODID, "fix_temp"));
        public static final StreamCodec<ByteBuf, FixTempPacket> STREAM_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, FixTempPacket::pos,
                        FixTempPacket::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(FixTempPacket packet, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    toggleTempLock(player.level(), packet.pos(), player);
                }
            });
        }
    }

    /** I key: pin / unpin the temperature of the reaction vessel the player
     *  is looking at (placed vessel, iron-stand flask or tripod vessel). */
    private static void toggleTempLock(Level level, BlockPos pos, ServerPlayer player) {
        BlockEntity be = level.getBlockEntity(pos);
        ItemStack vessel = null;
        if (be instanceof PlacedVesselBlockEntity pbe) {
            vessel = pbe.getVessel();
        } else if (be instanceof IronStandBlockEntity ibe) {
            vessel = ibe.getVessel();
        } else if (be instanceof TripodBlockEntity tbe) {
            vessel = tbe.getVessel();
        } else {
            for (PlacedVesselEntity pve : level.getEntitiesOfClass(PlacedVesselEntity.class,
                    new net.minecraft.world.phys.AABB(pos))) {
                vessel = pve.getVessel();
                break;
            }
        }
        if (vessel == null || vessel.isEmpty() || !(vessel.getItem() instanceof LabVesselItem)) {
            return;
        }
        boolean locked = !VesselHeating.isTempLocked(vessel);
        VesselHeating.setTempLocked(vessel, locked);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                locked ? "mchemistry.vessel.temp_locked" : "mchemistry.vessel.temp_unlocked",
                String.format("%.0f", TemperatureSystem.getTemp(vessel))), true);
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
        String gas = BottleCodes.isGasBottle(held) ? BottleCodes.gasIdOf(held) : null;
        String liquid = BottleCodes.isLiquidBottle(held) ? BottleCodes.liquidIdOf(held) : null;

        if ("chlorine".equals(gas)) {
            player.kill((net.minecraft.server.level.ServerLevel) player.level());
            return;
        }
        if ("ammonia_water_concentrated".equals(liquid)) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            return;
        }
        if ("ammonia_water".equals(liquid)) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            return;
        }
        if (gas != null && Set.of("hydrogen_sulfide", "sulfur_dioxide", "ammonia").contains(gas)) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            return;
        }
        taste(player, BottleCodes.substanceKeyOf(held));

        // Nothing sniffable in hand: sniff a placed OPEN bottle being looked at.
        HitResult hit = player.pick(5.0D, 1.0F, false);
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = player.level().getBlockState(pos);
            if (state.is(ModBlocks.GAS_COLLECTING_BOTTLE.get())
                    && !state.getValue(GasCollectingBottleBlock.HAS_PLATE)
                    && player.level().getBlockEntity(pos) instanceof GasCollectingBottleBlockEntity be) {
                String placedGas = be.getGasId();
                if (placedGas.equals("chlorine")) {
                    player.kill((net.minecraft.server.level.ServerLevel) player.level());
                } else if (Set.of("hydrogen_sulfide", "sulfur_dioxide", "ammonia").contains(placedGas)) {
                    player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                }
            }
        }
    }

    /** R 键品尝：按毒性/腐蚀性施加效果（固体、液体等非气体试剂）。 */
    private static void taste(ServerPlayer player, String key) {
        if (key.startsWith("gas_")
                || key.startsWith("empty_")
                || key.endsWith("_bucket")
                || key.endsWith("_tubed")
                || key.contains("nozzle")) {
            return;
        }
        ChemicalInfoProvider.ChemicalInfo info = ChemicalInfoProvider.forItem(key);
        if (info == null) {
            return;
        }
        String tox = info.toxicity();
        String corr = info.corrosiveness();
        if (tox.equals("high") || corr.equals("strong")) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 1));
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 400, 0));
        } else if (tox.equals("moderate") || tox.equals("medium") || corr.equals("moderate")) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        } else if (tox.equals("low") || corr.equals("weak")) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
        }
        player.displayClientMessage(
                Component.translatable("mchemistry.taste"), true);
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
