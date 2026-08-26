package com.example.chemistry.item;

import java.util.List;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.blockentity.PlacedTestTubeBlockEntity;
import com.example.chemistry.registry.ModBlocks;
import com.example.chemistry.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A test tube with a temperature system. Clamped tubes are insulated; normal
 * glass cracks on thermal shock while borosilicate glass does not.
 */
public class TestTubeItem extends LabVesselItem {

    public static final int NORMAL_MELTING_POINT = 1200;
    public static final int BOROSILICATE_MELTING_POINT = 1600;

    private final int meltingPoint;
    private final boolean borosilicate;
    private final boolean clamped;
    private final int stopperHoles;

    public TestTubeItem(Properties properties, int capacity, int meltingPoint, boolean borosilicate,
            boolean clamped, int stopperHoles) {
        super(properties, capacity);
        this.meltingPoint = meltingPoint;
        this.borosilicate = borosilicate;
        this.clamped = clamped;
        this.stopperHoles = stopperHoles;
    }

    public int meltingPoint() {
        return meltingPoint;
    }

    public boolean isBorosilicate() {
        return borosilicate;
    }

    public boolean isClamped() {
        return clamped;
    }

    /** 0 = no stopper, 1 = 1-hole, 2 = 2-hole. */
    public int stopperHoles() {
        return stopperHoles;
    }

    /** Right-click the top of a normal block to stand the tube upright. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        var player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        // Let clamp-attach / pour interactions (offhand) and chemistry blocks win.
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof LabVesselItem || off.getItem() == ModItems.TEST_TUBE_CLAMP.get()
                || off.getItem() == ModItems.RUBBER_STOPPER_1_HOLE.get()
                || off.getItem() == ModItems.RUBBER_STOPPER_2_HOLE.get()) {
            return InteractionResult.PASS;
        }
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        BlockPos clicked = context.getClickedPos();
        String namespace = BuiltInRegistries.BLOCK.getKey(level.getBlockState(clicked).getBlock()).getNamespace();
        if (namespace.equals(ChemistryMod.MODID)) {
            return InteractionResult.PASS;
        }
        BlockPos target = clicked.above();
        BlockState targetState = level.getBlockState(target);
        if (!targetState.isAir() && !targetState.canBeReplaced(new BlockPlaceContext(context))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = context.getItemInHand();
        level.setBlock(target, ModBlocks.PLACED_TEST_TUBE.get().defaultBlockState(), 3);
        if (level.getBlockEntity(target) instanceof PlacedTestTubeBlockEntity be) {
            be.setTube(held.copy());
        }
        held.shrink(1);
        return InteractionResult.SUCCESS;
    }

    /** 清空试管里的全部固体；含液体的试管不响应。 */
    private static void dumpSolids(Level level, Player player, ItemStack tube) {
        List<LabVesselItem.Entry> contents = LabVesselItem.getContents(tube);
        if (contents.isEmpty()) {
            return;
        }
        List<LabVesselItem.Entry> solids = contents.stream()
                .filter(e -> e.type().equals("solid")).toList();
        if (solids.size() != contents.size()) {
            // 含有液体时不能直接倒出（液体先被倒掉/反应）。
            return;
        }
        ItemStack dump;
        if (solids.size() == 1) {
            dump = new ItemStack(ModItems.looseSolid(solids.get(0).id()));
        } else {
            dump = new ItemStack(ModItems.SOLID_MIXTURE.get());
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            for (LabVesselItem.Entry e : solids) {
                CompoundTag c = new CompoundTag();
                c.putString("type", e.type());
                c.putString("id", e.id());
                c.putDouble("amount", e.amount());
                list.add(c);
            }
            tag.put("chem_contents", list);
            dump.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        LabVesselItem.clearContents(tube);
        if (!player.getInventory().add(dump)) {
            player.drop(dump, false);
        }
        player.displayClientMessage(
                Component.translatable("mchemistry.tube.dump_solids"), true);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // 副手试管 + 主手空手：把试管里的固体全部倒出。单一固体倒出对应
        // 散装物品；多种固体倒出"固体混合物"（物品上记录组成，tooltip 显示）。
        if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().isEmpty()) {
            if (!level.isClientSide()) {
                dumpSolids(level, player, player.getOffhandItem());
            }
            return InteractionResult.SUCCESS;
        }
        // Clamped/stoppered tube: right-click in the air removes the clamp
        // first, then the stopper on a second right-click.
        if (clamped || stopperHoles > 0) {
            if (!level.isClientSide()) {
                ItemStack held = player.getItemInHand(hand);
                if (TemperatureSystem.getTemp(held) > 80) {
                    player.displayClientMessage(Component.translatable("mchemistry.tube.too_hot"), true);
                } else if (clamped) {
                    String path = BuiltInRegistries.ITEM.getKey(held.getItem()).getPath();
                    String baseId = path.replaceFirst("_clamped", "");
                    Item base = BuiltInRegistries.ITEM.getValue(
                            ResourceLocation.fromNamespaceAndPath("mchemistry", baseId));
                    ItemStack unclamped = new ItemStack(base);
                    unclamped.set(DataComponents.CUSTOM_DATA,
                            held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY));
                    player.setItemInHand(hand, unclamped);
                    ItemStack clamp = new ItemStack(ModItems.TEST_TUBE_CLAMP.get());
                    if (!player.getInventory().add(clamp)) {
                        player.drop(clamp, false);
                    }
                } else {
                    String path = BuiltInRegistries.ITEM.getKey(held.getItem()).getPath();
                    String suffix = "_stoppered_" + stopperHoles;
                    String baseId = path.substring(0, path.length() - suffix.length());
                    Item base = BuiltInRegistries.ITEM.getValue(
                            ResourceLocation.fromNamespaceAndPath("mchemistry", baseId));
                    ItemStack unstopped = new ItemStack(base);
                    unstopped.set(DataComponents.CUSTOM_DATA,
                            held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY));
                    player.setItemInHand(hand, unstopped);
                    ItemStack stopper = new ItemStack(stopperHoles == 1
                            ? ModItems.RUBBER_STOPPER_1_HOLE.get() : ModItems.RUBBER_STOPPER_2_HOLE.get());
                    if (!player.getInventory().add(stopper)) {
                        player.drop(stopper, false);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Let the client send the use packet so the server can attach the clamp or stopper.
        ItemStack off = player.getOffhandItem();
        if (off.getItem() == ModItems.TEST_TUBE_CLAMP.get()
                || off.getItem() == ModItems.RUBBER_STOPPER_1_HOLE.get()
                || off.getItem() == ModItems.RUBBER_STOPPER_2_HOLE.get()
                || com.example.chemistry.LabInteractions.isTransferTool(off)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

}
