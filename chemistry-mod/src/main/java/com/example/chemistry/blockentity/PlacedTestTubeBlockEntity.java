package com.example.chemistry.blockentity;

import java.util.List;

import com.example.chemistry.TemperatureSystem;
import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.block.PlacedTestTubeBlock;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.item.TestTubeItem;
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

/**
 * Stores the test tube item placed in the world so its contents, temperature
 * and clamp state survive placement and are returned on pick-up/break.
 */
public class PlacedTestTubeBlockEntity extends BlockEntity implements IChemGoggleInfo {

    private ItemStack tube = ItemStack.EMPTY;

    public PlacedTestTubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLACED_TEST_TUBE.get(), pos, state);
    }

    public ItemStack getTube() {
        return tube;
    }

    public void setTube(ItemStack stack) {
        this.tube = stack.copy();
        setChanged();
        if (level != null && !level.isClientSide()) {
            boolean filled = !LabVesselItem.getContents(tube).isEmpty();
            boolean stoppered = tube.getItem() instanceof TestTubeItem tt && tt.stopperHoles() > 0;
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(PlacedTestTubeBlock.FILLED)
                    && (state.getValue(PlacedTestTubeBlock.FILLED) != filled
                            || state.getValue(PlacedTestTubeBlock.STOPPERED) != stoppered)) {
                level.setBlock(worldPosition, state.setValue(PlacedTestTubeBlock.FILLED, filled)
                        .setValue(PlacedTestTubeBlock.STOPPERED, stoppered), 3);
            }
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        if (tube.isEmpty()) {
            return false;
        }
        tooltip.add(tube.getHoverName().copy());
        tooltip.add(ChemGoggleLines.temp(TemperatureSystem.getTemp(tube)));
        ChemGoggleLines.appendContents(tooltip, tube);
        if (tube.getItem() instanceof TestTubeItem tt) {
            if (tt.stopperHoles() > 0) {
                tooltip.add(Component.literal("橡胶塞：" + tt.stopperHoles() + "孔"));
            }
            if (tt.isClamped()) {
                tooltip.add(Component.literal("已套试管夹"));
            }
        }
        return true;
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
        output.store("tube", ItemStack.OPTIONAL_CODEC, tube);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tube = input.read("tube", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }
}
