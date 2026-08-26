package com.example.chemistry.blockentity;

import java.util.List;

import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.data.GasJars;
import com.example.chemistry.registry.ModBlockEntities;
import com.example.chemistry.storage.ChemUnits;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Remembers which gas a placed gas-collecting bottle contains. */
public class GasCollectingBottleBlockEntity extends BlockEntity implements IChemGoggleInfo {

    public static final int CAPACITY_ML = ChemUnits.GAS_JAR_VOLUME;

    private String gasId = "";
    private int fillMl;
    private double purity = 1.0;

    public GasCollectingBottleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_COLLECTING_BOTTLE.get(), pos, state);
    }

    public String getGasId() {
        return gasId;
    }

    public void setGasId(String gasId) {
        this.gasId = gasId == null ? "" : gasId;
        setChanged();
        sync();
    }

    /** Set the collected amount + purity (used when a filled bottle is placed). */
    public void setFill(int ml, double purity) {
        this.fillMl = Math.max(0, Math.min(CAPACITY_ML, ml));
        this.purity = Math.max(0, Math.min(1, purity));
        setChanged();
        sync();
    }

    public int getFillMl() {
        return fillMl;
    }

    public double getPurity() {
        return purity;
    }

    /** Add gas flowing in through the nozzle; fills toward the capacity.
     *  Returns the accepted amount (mL) — less than ml when the bottle is
     *  already full, holds a different gas, or the mouth direction does not
     *  match the gas density (排空气法: 瓶口朝上收重气体/向上排气法,
     *  瓶口朝下收轻气体/向下排气法). */
    public int addGas(String id, int ml, double purity) {
        if (GasJars.ALL.stream().noneMatch(g -> g.id().equals(id))) {
            return 0; // not a collectable gas (vents instead)
        }
        if (!gasId.isEmpty() && fillMl > 0 && !gasId.equals(id)) {
            return 0; // already holds a different gas
        }
        boolean inverted = level != null && level.getBlockState(worldPosition)
                .getValue(GasCollectingBottleBlock.INVERTED);
        boolean lighter = GasJars.ALL.stream()
                .filter(g -> g.id().equals(id))
                .findFirst()
                .map(GasJars.GasJar::lighter)
                .orElse(false);
        if (inverted != lighter) {
            // Wrong collection side: the gas escapes through the bottle mouth.
            if (level instanceof ServerLevel server && level.getGameTime() % 5 == 0) {
                double x = worldPosition.getX() + 0.5;
                double y = worldPosition.getY() + (inverted ? 1.1 : 0.3);
                double z = worldPosition.getZ() + 0.5;
                server.sendParticles(ParticleTypes.BUBBLE, x, y, z, 3,
                        0.1, 0.05, 0.1, 0.01);
                if (level.getGameTime() % 20 == 0) {
                    server.sendParticles(ParticleTypes.CLOUD, x, y, z,
                            1, 0.08, 0.03, 0.08, 0.01);
                    server.playSound(null, worldPosition, SoundEvents.BUBBLE_POP,
                            SoundSource.BLOCKS, 0.7F, 1.1F);
                    Player p = server.getNearestPlayer(x, y, z, 8.0, false);
                    if (p != null) {
                        p.displayClientMessage(
                                Component.translatable("mchemistry.gas.wrong_side",
                                        Component.literal(inverted ? "倒立" : "正立"),
                                        Component.literal(lighter ? "向下排气法（瓶口朝下）" : "向上排气法（瓶口朝上）")),
                                true);
                    }
                }
            }
            return 0;
        }
        // 排空气法 mixes in air, so the collected gas is never full purity.
        double collectedPurity = Math.min(purity, 0.85);
        int accepted = Math.min(ml, CAPACITY_ML - fillMl);
        if (accepted <= 0) {
            return 0;
        }
        boolean wasEmpty = fillMl == 0;
        this.gasId = id;
        this.fillMl = Math.min(CAPACITY_ML, this.fillMl + accepted);
        this.purity = Math.min(this.purity, collectedPurity);
        setChanged();
        sync();
        if (wasEmpty && level != null) {
            level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                    SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        if (this.fillMl >= CAPACITY_ML && level != null && level.getGameTime() % 40 == 0) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.7F, 1.0F);
        }
        return accepted;
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean inverted = level != null && level.getBlockState(worldPosition)
                .getValue(GasCollectingBottleBlock.INVERTED);
        tooltip.add(Component.literal(inverted ? "集气瓶（倒立）" : "集气瓶（正立）"));
        if (gasId.isEmpty() || fillMl <= 0) {
            tooltip.add(Component.literal("气体：空"));
            tooltip.add(Component.literal("容量：" + CAPACITY_ML + "mL"));
            return true;
        }
        String formula = ChemGoggleLines.gasFormula(gasId);
        tooltip.add(Component.literal("气体：" + ChemGoggleLines.gasName(gasId)
                + (formula.isEmpty() ? "" : "（" + formula + "）")));
        tooltip.add(Component.literal("填充：" + fillMl + "/" + CAPACITY_ML + "mL"));
        tooltip.add(Component.literal("纯度：" + String.format("%.2f%%", purity * 100)));
        return true;
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("gas_id", gasId);
        output.putInt("fill_ml", fillMl);
        output.putDouble("purity", purity);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        gasId = input.getStringOr("gas_id", "");
        fillMl = input.getIntOr("fill_ml", 0);
        purity = input.getDoubleOr("purity", 1.0);
    }
}
