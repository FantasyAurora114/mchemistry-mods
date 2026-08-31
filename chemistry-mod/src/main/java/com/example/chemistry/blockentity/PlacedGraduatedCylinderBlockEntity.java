package com.example.chemistry.blockentity;

import java.util.List;

import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.item.GraduatedCylinderItem;
import com.example.chemistry.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** 放下的量筒：保存量筒物品（含液体 NBT）。 */
public class PlacedGraduatedCylinderBlockEntity extends BlockEntity implements IChemGoggleInfo {

    private ItemStack cylinder = ItemStack.EMPTY;

    public PlacedGraduatedCylinderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLACED_GRADUATED_CYLINDER.get(), pos, state);
    }

    public ItemStack getCylinder() {
        return cylinder;
    }

    public void setCylinder(ItemStack stack) {
        this.cylinder = stack.copy();
        setChanged();
        sync();
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("量筒"));
        String liquid = GraduatedCylinderItem.getLiquid(cylinder);
        if (liquid == null) {
            tooltip.add(Component.literal("液体：空"));
        } else {
            tooltip.add(Component.literal("液体："
                    + com.example.chemistry.api.goggles.ChemGoggleLines.liquidName(liquid)
                    + " " + String.format("%.1f", GraduatedCylinderItem.getMl(cylinder)) + "mL"));
        }
        return true;
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("cylinder", ItemStack.OPTIONAL_CODEC, cylinder);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        cylinder = input.read("cylinder", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }
}
