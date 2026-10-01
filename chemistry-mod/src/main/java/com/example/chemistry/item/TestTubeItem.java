package com.example.chemistry.item;

import java.util.List;

import com.example.chemistry.ChemistryMod;
import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.registry.ModItems;

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
import net.minecraft.world.level.Level;

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

    public boolean isDewar() {
        return BuiltInRegistries.ITEM.getKey(this).getPath().contains("_dewar");
    }
    public static double exchangeFactor(ItemStack stack) {
        return stack.getItem() instanceof TestTubeItem tube && tube.isDewar() ? .125 : 1;
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

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // 副手反应容器 + 主手空手：倒出全部物质（腐蚀液体受伤、固体以物品归还）。
        if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().isEmpty()) {
            if (!level.isClientSide()) {
                LabVesselItem.pourOutAll(player, player.getOffhandItem());
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
