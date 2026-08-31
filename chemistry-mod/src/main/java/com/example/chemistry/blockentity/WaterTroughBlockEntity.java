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
import net.minecraft.world.level.Level;
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
    /** 插在水槽里的玻璃导管类型：1=直管 2=90度管 3=90度长管。 */
    private int tubeType = 1;
    /** 冰浴中浸泡的烧瓶（冰块状态时可放）。 */
    private net.minecraft.world.item.ItemStack flask =
            net.minecraft.world.item.ItemStack.EMPTY;

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

    public int getTubeType() {
        return tubeType;
    }

    public void setTubeType(int type) {
        this.tubeType = type >= 1 && type <= 3 ? type : 1;
        setChanged();
        sync();
    }

    public net.minecraft.world.item.ItemStack getFlask() {
        return flask;
    }

    public void setFlask(net.minecraft.world.item.ItemStack stack) {
        this.flask = stack.copy();
        setChanged();
        sync();
    }

    /** 冰浴：把浸泡的烧瓶慢慢冷却到 0°C。 */
    public void tickServer(Level level) {
        if (level.isClientSide() || flask.isEmpty()) {
            return;
        }
        var fill = level.getBlockState(worldPosition)
                .getValue(com.example.chemistry.block.WaterTroughBlock.FILLED);
        // 烧瓶温度超过 40°C：冰块融化，水槽变成水。
        if (fill == com.example.chemistry.block.WaterTroughBlock.Fill.ICE
                && com.example.chemistry.TemperatureSystem.getTemp(flask) > 40.0) {
            level.setBlock(worldPosition, level.getBlockState(worldPosition)
                    .setValue(com.example.chemistry.block.WaterTroughBlock.FILLED,
                            com.example.chemistry.block.WaterTroughBlock.Fill.WATER), 3);
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS, 0.8F, 1.0F);
            return;
        }
        // 冰浴：把烧瓶慢慢冷却到 0°C。
        if (fill == com.example.chemistry.block.WaterTroughBlock.Fill.ICE) {
            com.example.chemistry.VesselHeating.heat(flask, 0.0);
        }
        setChanged();
    }

    /** 从集到的气体中扣除最多 ml mL，返回实际扣除量。 */
    public int removeGas(String id, int ml) {
        if (ml <= 0 || id == null || !id.equals(gasId) || fillMl <= 0) {
            return 0;
        }
        int take = Math.min(ml, fillMl);
        fillMl -= take;
        if (fillMl <= 0) {
            gasId = "";
        }
        setChanged();
        sync();
        return take;
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("水槽"));
        var fill = level != null ? level.getBlockState(worldPosition)
                .getValue(com.example.chemistry.block.WaterTroughBlock.FILLED) : null;
        if (fill == com.example.chemistry.block.WaterTroughBlock.Fill.ICE) {
            tooltip.add(Component.literal("状态：装满冰块（冰浴）"));
        } else if (fill == com.example.chemistry.block.WaterTroughBlock.Fill.WATER) {
            tooltip.add(Component.literal("状态：装满水（排水法集气）"));
        } else {
            tooltip.add(Component.literal("状态：空"));
        }
        if (!flask.isEmpty()) {
            tooltip.add(Component.literal("冰浴烧瓶：" + flask.getHoverName().getString()));
            com.example.chemistry.api.goggles.ChemGoggleLines.appendContents(tooltip, flask);
        }
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
        output.putInt("tube_type", tubeType);
        output.store("flask", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC, flask);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hasBottle = input.getBooleanOr("has_bottle", false);
        gasId = input.getStringOr("gas_id", "");
        fillMl = input.getIntOr("fill_ml", 0);
        waterMl = input.getIntOr("water_ml", 0);
        purity = input.getDoubleOr("purity", 1.0);
        tubeType = input.getIntOr("tube_type", 1);
        flask = input.read("flask", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC)
                .orElse(net.minecraft.world.item.ItemStack.EMPTY);
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
