package com.example.chemistry.blockentity;

import java.util.ArrayList;
import java.util.List;

import com.example.chemistry.GasFlowEngine;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.data.GasJars;
import com.example.chemistry.entity.RubberTubeEntity;
import com.example.chemistry.entity.RubberTubeEntity.Port;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.registry.ModBlockEntities;
import com.example.chemistry.storage.ChemUnits;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A gas collecting bottle can hold a MIXTURE of gases: each gas is stored as
 *  (id, mL). The goggles overlay shows "混合气体" with the proportion of every
 *  component; the dominant gas decides the item form / render colour. */
public class GasCollectingBottleBlockEntity extends BlockEntity implements IChemGoggleInfo {

    public static final int CAPACITY_ML = ChemUnits.GAS_JAR_VOLUME;

    public record GasPart(String id, int ml) {
    }

    public static final Codec<GasPart> GAS_PART_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(GasPart::id),
            Codec.INT.fieldOf("ml").forGetter(GasPart::ml))
            .apply(i, GasPart::new));

    private final List<GasPart> parts = new ArrayList<>();
    private double purity = 1.0;
    /** 标签文字（铁砧命名的标签贴到瓶子上），放置/拾取时保留。 */
    private String labelName = "";
    /** 插在瓶口的玻璃导管类型：1=直管 2=90度管 3=90度长管。 */
    private int tubeType = 1;

    public GasCollectingBottleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_COLLECTING_BOTTLE.get(), pos, state);
    }

    /** Dominant gas (largest volume) — used for the item form and renderer. */
    public String getGasId() {
        String best = "";
        int bestMl = 0;
        for (GasPart p : parts) {
            if (p.ml() > bestMl) {
                best = p.id();
                bestMl = p.ml();
            }
        }
        return best;
    }

    public void setGasId(String gasId) {
        parts.clear();
        if (gasId != null && !gasId.isEmpty()) {
            parts.add(new GasPart(gasId, CAPACITY_ML));
        }
        setChanged();
        sync();
    }

    public int getFillMl() {
        int total = 0;
        for (GasPart p : parts) {
            total += p.ml();
        }
        return total;
    }

    public double getPurity() {
        return purity;
    }

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String name) {
        this.labelName = name == null ? "" : name;
        setChanged();
        sync();
    }

    public int getTubeType() {
        return tubeType;
    }

    public void setTubeType(int type) {
        this.tubeType = type >= 1 && type <= 3 ? type : 1;
        setChanged();
        sync();
    }

    /** 服务器 tick：瓶内气体沿连接的橡胶管流向另一端（如手持导气嘴）。 */
    public void tickServer(Level level) {
        if (level.isClientSide() || getFillMl() <= 0) {
            return;
        }
        Port head = Port.nozzle(worldPosition, Direction.UP);
        for (RubberTubeEntity tube : RubberTubeItem.findTubesAt(level, head)) {
            Port far = GasFlowEngine.otherAnchor(tube, head);
            // 只向“玩家手持导气嘴”方向供气；另一端是集气瓶/水槽等收集端时
            // 本瓶是收集器，不能反向抽气。
            if (far != null && far.kind() == Port.KIND_ENTITY) {
                pumpOut(level, tube, head);
            }
        }
    }

    /** 一步泵气：把管道另一端的气体/空气送走，再从瓶内压入等量气体。 */
    private void pumpOut(Level level, RubberTubeEntity tube, Port head) {
        String id = getGasId();
        if (id.isEmpty()) {
            return;
        }
        // 手持导气嘴使用：放慢到 1mL/tick（20mL/s），一整瓶约 5 秒放完。
        int flow = Math.min(1, getFillMl());
        // 1) 管道里已经到达远端的气体先送出去（空气冒泡排空，气体交给出口）。
        String transitId = tube.getTransitGas();
        double transitPurity = tube.getTransitPurity();
        int out = tube.takeTransit(flow);
        if (out > 0 && !transitId.isEmpty()) {
            Port far = GasFlowEngine.otherAnchor(tube, head);
            int accepted = far != null
                    ? GasFlowEngine.deliverTo(level, far, transitId, out, transitPurity)
                    : 0;
            if (accepted < out && level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.BUBBLE,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                        worldPosition.getZ() + 0.5,
                        Math.min(3, 1 + (out - accepted) / 2), 0.1, 0.05, 0.1, 0.01);
            }
        }
        // 2) 从瓶内压入等量气体（管内还是空气时会被拒收，先排空空气）。
        int capacity = GasFlowEngine.tubeCapacity(level, tube);
        int into = Math.min(flow, Math.max(0, capacity - tube.getTransitMl()));
        if (into > 0) {
            int added = tube.addTransit(id, into, purity);
            if (added > 0) {
                removeGas(id, added);
            }
        }
    }

    /** 从混合气体中扣除最多 ml mL，返回实际扣除量。 */
    public int removeGas(String id, int ml) {
        if (ml <= 0 || id == null || id.isEmpty()) {
            return 0;
        }
        int removed = 0;
        List<GasPart> remaining = new ArrayList<>();
        for (GasPart p : parts) {
            if (p.id().equals(id) && removed < ml) {
                int take = Math.min(ml - removed, p.ml());
                removed += take;
                if (p.ml() > take) {
                    remaining.add(new GasPart(id, p.ml() - take));
                }
            } else {
                remaining.add(p);
            }
        }
        if (removed > 0) {
            parts.clear();
            parts.addAll(remaining);
            setChanged();
            sync();
        }
        return removed;
    }

    /** Set the collected amount + purity (used when a filled bottle is placed). */
    public void setFill(int ml, double purity) {
        if (ml <= 0) {
            parts.clear();
        } else if (parts.size() == 1) {
            parts.set(0, new GasPart(parts.get(0).id(), Math.min(CAPACITY_ML, ml)));
        }
        this.purity = Math.max(0, Math.min(1, purity));
        setChanged();
        sync();
    }

    public List<GasPart> getParts() {
        return List.copyOf(parts);
    }

    public void setParts(List<GasPart> newParts) {
        parts.clear();
        for (GasPart p : newParts) {
            if (p.id() != null && !p.id().isEmpty() && p.ml() > 0) {
                parts.add(new GasPart(p.id(), Math.min(CAPACITY_ML, p.ml())));
            }
        }
        setChanged();
        sync();
    }

    /** Add gas flowing in through the nozzle; mixes into the bottle up to its
     *  capacity. Returns the accepted amount (mL). The mouth direction must
     *  match the gas density (排空气法); water solubility is handled upstream. */
    public int addGas(String id, int ml, double purity) {
        if (GasJars.ALL.stream().noneMatch(g -> g.id().equals(id))) {
            return 0;
        }
        boolean inverted = level != null && level.getBlockState(worldPosition)
                .getValue(GasCollectingBottleBlock.INVERTED);
        boolean lighter = GasJars.ALL.stream()
                .filter(g -> g.id().equals(id))
                .findFirst()
                .map(GasJars.GasJar::lighter)
                .orElse(false);
        if (inverted != lighter) {
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
                                        Component.literal(lighter ? "向下排气法（瓶口朝下）"
                                                : "向上排气法（瓶口朝上）")),
                                true);
                    }
                }
            }
            return 0;
        }
        int accepted = Math.min(ml, CAPACITY_ML - getFillMl());
        if (accepted <= 0) {
            return 0;
        }
        boolean wasEmpty = getFillMl() == 0;
        boolean found = false;
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).id().equals(id)) {
                parts.set(i, new GasPart(id, parts.get(i).ml() + accepted));
                found = true;
                break;
            }
        }
        if (!found) {
            parts.add(new GasPart(id, accepted));
        }
        this.purity = Math.min(this.purity, purity);
        setChanged();
        sync();
        if (wasEmpty && level != null) {
            level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                    SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        if (getFillMl() >= CAPACITY_ML && level != null && level.getGameTime() % 40 == 0) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.7F, 1.0F);
        }
        return accepted;
    }

    /** Carry the full mixture on the picked-up bottle item. */
    public void writeToItem(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (parts.size() > 1) {
            ListTag list = new ListTag();
            for (GasPart p : parts) {
                CompoundTag c = new CompoundTag();
                c.putString("id", p.id());
                c.putInt("ml", p.ml());
                list.add(c);
            }
            tag.put("chem_gas_mix", list);
        } else {
            tag.remove("chem_gas_mix");
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Restore a mixture carried on the item when the bottle is placed. */
    public void readFromItem(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag mix = tag.getListOrEmpty("chem_gas_mix");
        if (mix.isEmpty()) {
            return;
        }
        List<GasPart> restored = new ArrayList<>();
        for (Tag t : mix) {
            if (t instanceof CompoundTag c) {
                String id = c.getStringOr("id", "");
                int ml = c.getIntOr("ml", 0);
                if (!id.isEmpty() && ml > 0) {
                    restored.add(new GasPart(id, ml));
                }
            }
        }
        if (!restored.isEmpty()) {
            setParts(restored);
        }
    }

    @Override
    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean inverted = level != null && level.getBlockState(worldPosition)
                .getValue(GasCollectingBottleBlock.INVERTED);
        tooltip.add(Component.literal(inverted ? "集气瓶（倒立）" : "集气瓶（正立）"));
        int fill = getFillMl();
        if (parts.isEmpty() || fill <= 0) {
            tooltip.add(Component.literal("气体：空"));
            tooltip.add(Component.literal("容量：" + CAPACITY_ML + "mL"));
            return true;
        }
        if (parts.size() == 1) {
            String id = parts.get(0).id();
            String formula = ChemGoggleLines.gasFormula(id);
            tooltip.add(Component.literal("气体：" + ChemGoggleLines.gasName(id)
                    + (formula.isEmpty() ? "" : "（" + formula + "）")));
        } else {
            tooltip.add(Component.literal("气体：混合气体"));
            for (GasPart p : parts) {
                int pct = (int) Math.round(100.0 * p.ml() / fill);
                String formula = ChemGoggleLines.gasFormula(p.id());
                tooltip.add(Component.literal("  " + ChemGoggleLines.gasName(p.id())
                        + " " + pct + "%（" + p.ml() + "mL"
                        + (formula.isEmpty() ? "" : "，" + formula) + "）"));
            }
        }
        tooltip.add(Component.literal("填充：" + fill + "/" + CAPACITY_ML + "mL"));
        tooltip.add(Component.literal("纯度：" + String.format("%.2f%%", purity * 100)));
        return true;
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** 让客户端在放置/集气后立刻收到气体数据（否则新放置的满瓶显示为空）。 */
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
        output.store("gas_parts", GAS_PART_CODEC.listOf(), List.copyOf(parts));
        output.store("purity", Codec.DOUBLE, purity);
        output.store("label", Codec.STRING, labelName);
        output.store("tube_type", Codec.INT, tubeType);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        parts.clear();
        parts.addAll(input.read("gas_parts", GAS_PART_CODEC.listOf()).orElse(List.of()));
        purity = input.read("purity", Codec.DOUBLE).orElse(1.0);
        labelName = input.read("label", Codec.STRING).orElse("");
        tubeType = input.read("tube_type", Codec.INT).orElse(1);
        // Backward compatibility: old single-gas saves.
        if (parts.isEmpty()) {
            String old = input.getStringOr("gas_id", "");
            int fill = input.getIntOr("fill_ml", 0);
            if (!old.isEmpty() && fill > 0) {
                parts.add(new GasPart(old, fill));
            }
        }
    }
}
