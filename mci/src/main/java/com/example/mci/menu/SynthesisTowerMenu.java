/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.example.mci.registry.ModMenus
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Position
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.Container
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 */
package com.example.mci.menu;

import com.example.mci.blockentity.SynthesisTowerBlockEntity;
import com.example.mci.network.ChemistryNetworking;
import com.example.mci.registry.ModMenus;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SynthesisTowerMenu
extends AbstractContainerMenu {
    public static final int RUN_BUTTON = 100;
    public static final int CONFIG_A = 101;
    public static final int CONFIG_B = 102;
    public static final int CONFIG_C = 103;
    public static final int CONFIG_D = 104;
    public static final int TEMP_DOWN = 105;
    public static final int TEMP_UP = 106;
    public static final int PRESSURE_DOWN = 107;
    public static final int PRESSURE_UP = 108;
    private final SynthesisTowerBlockEntity blockEntity;
    private final Player player;
    private long lastSyncedVersion = -1L;

    public SynthesisTowerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, (SynthesisTowerBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public SynthesisTowerMenu(int containerId, Inventory playerInventory, SynthesisTowerBlockEntity blockEntity) {
        super((MenuType)ModMenus.SYNTHESIS_TOWER.get(), containerId);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        this.addSlot(new CatalystSlot(blockEntity, 4, 15, 26));
        this.addSlot(new Slot((Container)blockEntity, 0, 44, 26));
        this.addSlot(new Slot((Container)blockEntity, 1, 62, 26));
        this.addSlot(new Slot((Container)blockEntity, 2, 80, 26));
        this.addSlot(new Slot((Container)blockEntity, 3, 98, 26));
        this.addSlot(new Slot((Container)blockEntity, 5, 128, 8));
        this.addSlot(new Slot((Container)blockEntity, 6, 151, 8));
        this.addSlot(new Slot((Container)blockEntity, 7, 128, 84));
        this.addSlot(new Slot((Container)blockEntity, 8, 151, 84));
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot((Container)playerInventory, col + row * 9 + 9, 7 + col * 18, 116 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot((Container)playerInventory, col, 7 + col * 18, 174));
        }
    }

    public SynthesisTowerBlockEntity blockEntity() {
        return this.blockEntity;
    }

    public void broadcastChanges() {
        long version;
        super.broadcastChanges();
        if (!this.player.level().isClientSide() && (version = this.blockEntity.getSyncVersion()) != this.lastSyncedVersion) {
            this.lastSyncedVersion = version;
            ChemistryNetworking.sendTowerSync((ServerPlayer)this.player, this.blockEntity);
        }
    }

    public boolean clickMenuButton(Player player, int id) {
        if (this.handleButton(id, player)) {
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    public void clicked(int buttonId, int mode, ClickType clickType, Player player) {
        if (clickType == ClickType.PICKUP && this.handleButton(buttonId, player)) {
            return;
        }
        super.clicked(buttonId, mode, clickType, player);
    }

    private boolean handleButton(int buttonId, Player player) {
        this.clickSound(player);
        if (buttonId == 100) {
            if (!player.level().isClientSide()) {
                boolean ran = this.blockEntity.run(player);
                if (!ran) {
                    player.displayClientMessage((Component)Component.literal((String)"\u53cd\u5e94\u6761\u4ef6\u4e0d\u6ee1\u8db3\u6216\u65e0\u53ef\u7528\u53cd\u5e94\uff08\u68c0\u67e5\u6e29\u5ea6/\u538b\u529b/\u50ac\u5316\u5242/\u53cd\u5e94\u7269\uff09"), true);
                }
                this.broadcastChanges();
            }
            return true;
        }
        if (buttonId >= 101 && buttonId <= 104) {
            this.blockEntity.cycleInputType(buttonId - 101);
            this.broadcastChanges();
            return true;
        }
        if (buttonId == 105 || buttonId == 106) {
            double step = player.isShiftKeyDown() ? 100.0 : 10.0;
            this.blockEntity.addTemperature(buttonId == 106 ? step : -step);
            this.broadcastChanges();
            return true;
        }
        if (buttonId == 107 || buttonId == 108) {
            double step = player.isShiftKeyDown() ? 10000.0 : 1000.0;
            this.blockEntity.addPressure(buttonId == 108 ? step : -step);
            this.broadcastChanges();
            return true;
        }
        return false;
    }

    private void clickSound(Player player) {
        if (!player.level().isClientSide()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), (Holder)SoundEvents.UI_BUTTON_CLICK, SoundSource.BLOCKS, 0.6f, 1.0f);
        }
    }

    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            itemStack = current.copy();
            if (index < 9 ? !this.moveItemStackTo(current, 9, this.slots.size(), true) : !this.moveItemStackTo(current, 0, 9, false)) {
                return ItemStack.EMPTY;
            }
            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (current.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, current);
        }
        return itemStack;
    }

    public boolean stillValid(Player player) {
        return this.blockEntity.getBlockPos().closerToCenterThan((Position)player.position(), 8.0);
    }

    private static class CatalystSlot
    extends Slot {
        CatalystSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        public boolean mayPlace(ItemStack stack) {
            String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            return path.equals("iron_catalyst") || path.equals("vanadium_pentoxide_catalyst") || path.equals("platinum_rhodium_catalyst") || path.equals("solid_sulfuric_acid_concentrated");
        }
    }
}
