package com.example.chemistry.blockentity;

import java.util.List;
import java.util.Set;

import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.data.GasJars;
import com.example.chemistry.registry.ModBlockEntities;
import com.example.chemistry.storage.ChemUnits;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Holds an inverted gas collecting bottle inside the water trough (排水法集气):
 *  the bottle starts full of water; incoming gas displaces the water. */
public class WaterTroughBlockEntity extends BlockEntity implements IChemGoggleInfo {

    public static final int CAPACITY_ML = ChemUnits.GAS_JAR_VOLUME;

    /** Gases that dissolve in water: they cannot be collected by 排水法
     *  (water displacement) — use upward/downward air displacement instead. */
    private static final Set<String> WATER_SOLUBLE = Set.of(
            "chlorine", "carbon_dioxide", "sulfur_dioxide", "nitrogen_dioxide",
            "ammonia", "hydrogen_chloride", "hydrogen_sulfide");

    private boolean hasBottle;
    private String gasId = "";
    private int fillMl;
    private int waterMl;
    private double purity = 1.0;

    public WaterTroughBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_TROUGH.get(), pos, state);
    }

    public boolean hasBottle() {
        return hasBottle;
    }

    public String getGasId() {
        return gasId;
    }

    public int getFillMl() {
        return fillMl;
    }

    /** Water remaining inside the inverted bottle (mL). */
    public int getWaterMl() {
        return waterMl;
    }

    public double getPurity() {
        return purity;
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("水槽"));
        tooltip.add(Component.literal("水：已注满（排水法集气）"));
        if (!hasBottle) {
            tooltip.add(Component.literal("集气瓶：未放入"));
            return true;
        }
        if (gasId.isEmpty() || fillMl <= 0) {
            tooltip.add(Component.literal("集气瓶：装满水，等待集气"));
        } else {
            tooltip.add(Component.literal("集气瓶：" + ChemGoggleLines.gasName(gasId)
                    + " " + fillMl + "/" + CAPACITY_ML + "mL"));
            tooltip.add(Component.literal("瓶内剩余水：" + waterMl + "mL"));
            tooltip.add(Component.literal("纯度：" + String.format("%.2f%%", purity * 100)));
        }
        return true;
    }

    /** Place an empty inverted bottle into the trough: it fills with water. */
    public void placeBottle() {
        hasBottle = true;
        gasId = "";
        fillMl = 0;
        waterMl = CAPACITY_ML;
        purity = 1.0;
        setChanged();
        sync();
    }

    /** Take the bottle out; returns whether it held any collected gas. */
    public boolean takeBottle() {
        boolean hadGas = hasBottle && fillMl > 0 && !gasId.isEmpty();
        hasBottle = false;
        gasId = "";
        fillMl = 0;
        waterMl = 0;
        purity = 1.0;
        setChanged();
        sync();
        return hadGas;
    }

    /** Gas bubbles up into the inverted bottle, displacing an equal volume of
     *  water. Returns the accepted amount (mL). */
    public int addGas(String id, int ml, double newPurity) {
        if (!hasBottle) {
            return 0;
        }
        if (GasJars.ALL.stream().noneMatch(g -> g.id().equals(id))) {
            return 0;
        }
        if (!gasId.isEmpty() && !gasId.equals(id)) {
            return 0;
        }
        if (WATER_SOLUBLE.contains(id)) {
            // Dissolves in the trough water instead of being collected.
            if (level instanceof ServerLevel server && level.getGameTime() % 20 == 0) {
                double x = worldPosition.getX() + 7.8 / 16.0;
                double y = worldPosition.getY() + 3.2 / 16.0;
                double z = worldPosition.getZ() + 7.8 / 16.0;
                server.sendParticles(ParticleTypes.BUBBLE, x, y, z,
                        6, 0.1, 0.05, 0.1, 0.01);
                server.playSound(null, worldPosition, SoundEvents.BUBBLE_POP,
                        SoundSource.BLOCKS, 0.7F, 1.0F);
                Player p = server.getNearestPlayer(x, y, z, 8.0, false);
                if (p != null) {
                    p.displayClientMessage(
                            Component.translatable("mchemistry.gas.water_soluble"), true);
                }
            }
            return 0;
        }
        int accepted = Math.min(ml, CAPACITY_ML - fillMl);
        if (accepted <= 0) {
            return 0;
        }
        int displaced = Math.min(accepted, waterMl);
        waterMl -= displaced;
        fillMl += accepted;
        gasId = id;
        purity = Math.min(purity, newPurity);
        setChanged();
        sync();
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.BUBBLE,
                    worldPosition.getX() + 7.8 / 16.0, worldPosition.getY() + 3.2 / 16.0,
                    worldPosition.getZ() + 7.8 / 16.0,
                    8, 0.1, 0.05, 0.1, 0.01);
        }
        return accepted;
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("has_bottle", hasBottle);
        output.putString("gas_id", gasId);
        output.putInt("fill_ml", fillMl);
        output.putInt("water_ml", waterMl);
        output.putDouble("purity", purity);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hasBottle = input.getBooleanOr("has_bottle", false);
        gasId = input.getStringOr("gas_id", "");
        fillMl = input.getIntOr("fill_ml", 0);
        waterMl = input.getIntOr("water_ml", 0);
        purity = input.getDoubleOr("purity", 1.0);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
