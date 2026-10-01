package com.example.chemistry.entity;

import java.util.ArrayList;
import java.util.List;

import com.example.chemistry.GasFlowEngine;
import com.example.chemistry.PurityHelper;
import com.example.chemistry.data.GasJars;
import com.example.chemistry.entity.RubberTubeEntity.Port;
import com.example.chemistry.item.GlassTubeItem;
import com.example.chemistry.item.GlassTubeTubedItem;
import com.example.chemistry.item.LabelItem;
import com.example.chemistry.item.RubberTubeItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.storage.ChemUnits;
import com.example.chemistry.transfer.BottleCodes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * 集气瓶（技术性实体版）。气体混合物序列化进 synced data（STRING），
 * 翻倒/玻璃片/玻璃导管/橡胶管连接/取瓶与旧 {@code GasCollectingBottleBlock}
 * 保持一致。
 */
public class GasCollectingBottleEntity extends TechnicalEntity {

    public static final int CAPACITY_ML = ChemUnits.GAS_JAR_VOLUME;

    public record GasPart(String id, int ml) {
    }

    private static final EntityDataAccessor<Integer> DATA_WATER = SynchedEntityData.defineId(GasCollectingBottleEntity.class,EntityDataSerializers.INT);
    public int waterMl(){return entityData.get(DATA_WATER);}
    public void setWaterMl(int ml){entityData.set(DATA_WATER,Math.clamp(ml,0,CAPACITY_ML));}
    private static final EntityDataAccessor<Boolean> DATA_INVERTED =
            SynchedEntityData.defineId(GasCollectingBottleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_PLATE =
            SynchedEntityData.defineId(GasCollectingBottleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_NOZZLE =
            SynchedEntityData.defineId(GasCollectingBottleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_TUBE_TYPE =
            SynchedEntityData.defineId(GasCollectingBottleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_LABEL =
            SynchedEntityData.defineId(GasCollectingBottleEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_PARTS =
            SynchedEntityData.defineId(GasCollectingBottleEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_PURITY =
            SynchedEntityData.defineId(GasCollectingBottleEntity.class, EntityDataSerializers.FLOAT);

    public GasCollectingBottleEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WATER,0);
        builder.define(DATA_INVERTED, false);
        builder.define(DATA_HAS_PLATE, false);
        builder.define(DATA_HAS_NOZZLE, false);
        builder.define(DATA_TUBE_TYPE, 1);
        builder.define(DATA_LABEL, "");
        builder.define(DATA_PARTS, "");
        builder.define(DATA_PURITY, 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("radio_sample",ItemStack.OPTIONAL_CODEC,nuclearSample);
        output.putInt("water_ml",waterMl());
        output.putBoolean("inverted", isInverted());
        output.putBoolean("has_plate", hasPlate());
        output.putBoolean("has_nozzle", hasNozzle());
        output.putInt("tube_type", getTubeType());
        output.putString("label", getLabelName());
        output.putString("gas_parts", entityData.get(DATA_PARTS));
        output.putFloat("purity", (float) getPurity());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        nuclearSample=input.read("radio_sample",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        setWaterMl(input.getIntOr("water_ml",0));
        setInverted(input.getBooleanOr("inverted", false));
        setHasPlate(input.getBooleanOr("has_plate", false));
        setHasNozzle(input.getBooleanOr("has_nozzle", false));
        setTubeType(input.getIntOr("tube_type", 1));
        setLabelName(input.getStringOr("label", ""));
        setGasParts(deserializeParts(input.getStringOr("gas_parts", "")));
        setPurity(input.getFloatOr("purity", 1.0F));
    }

    public boolean isInverted() {
        return this.entityData.get(DATA_INVERTED);
    }

    public void setInverted(boolean v) {
        this.entityData.set(DATA_INVERTED, v);
    }

    public boolean hasPlate() {
        return this.entityData.get(DATA_HAS_PLATE);
    }

    public void setHasPlate(boolean v) {
        this.entityData.set(DATA_HAS_PLATE, v);
    }

    public boolean hasNozzle() {
        return this.entityData.get(DATA_HAS_NOZZLE);
    }

    public void setHasNozzle(boolean v) {
        this.entityData.set(DATA_HAS_NOZZLE, v);
    }

    public int getTubeType() {
        return this.entityData.get(DATA_TUBE_TYPE);
    }

    public void setTubeType(int t) {
        this.entityData.set(DATA_TUBE_TYPE, t >= 1 && t <= 4 ? t : 1);
    }

    public String getLabelName() {
        String s = this.entityData.get(DATA_LABEL);
        return s == null ? "" : s;
    }

    public void setLabelName(String s) {
        this.entityData.set(DATA_LABEL, s == null ? "" : s);
    }

    public double getPurity() {
        return this.entityData.get(DATA_PURITY);
    }

    public void setPurity(double p) {
        this.entityData.set(DATA_PURITY, (float) Math.max(0.0, Math.min(1.0, p)));
    }

    public List<GasPart> getGasParts() {
        return deserializeParts(this.entityData.get(DATA_PARTS));
    }

    public void setGasParts(List<GasPart> parts) {
        this.entityData.set(DATA_PARTS, serializeParts(parts));
    }

    /** Dominant gas (largest volume). */
    public String getGasId() {
        String best = "";
        int bestMl = 0;
        for (GasPart p : getGasParts()) {
            if (p.ml() > bestMl) {
                best = p.id();
                bestMl = p.ml();
            }
        }
        return best;
    }

    public int getFillMl() {
        int total = 0;
        for (GasPart p : getGasParts()) {
            total += p.ml();
        }
        return total;
    }

    public void setGasId(String gasId) {
        List<GasPart> parts = new ArrayList<>();
        if (gasId != null && !gasId.isEmpty()) {
            parts.add(new GasPart(gasId, CAPACITY_ML));
        }
        setGasParts(parts);
    }

    public void setFill(int ml, double purity) {
        List<GasPart> parts = getGasParts();
        if (ml <= 0) {
            parts.clear();
        } else if (parts.size() == 1) {
            parts.set(0, new GasPart(parts.get(0).id(), Math.min(CAPACITY_ML, ml)));
        }
        setGasParts(parts);
        setPurity(purity);
    }

    public int removeGas(String id, int ml) {
        if (ml <= 0 || id == null || id.isEmpty()) {
            return 0;
        }
        int removed = 0;
        List<GasPart> remaining = new ArrayList<>();
        for (GasPart p : getGasParts()) {
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
            setGasParts(remaining);
        }
        return removed;
    }

    public int addGas(String id, int ml, double purity) {
        if (GasJars.ALL.stream().noneMatch(g -> g.id().equals(id))) {
            return 0;
        }
        boolean lighter = GasJars.ALL.stream()
                .filter(g -> g.id().equals(id)).findFirst()
                .map(GasJars.GasJar::lighter).orElse(false);
        if (waterMl()>0 ? !isInverted() : isInverted() != lighter) {
            return 0;
        }
        int accepted = Math.min(ml, CAPACITY_ML - getFillMl());
        if (accepted <= 0) {
            return 0;
        }
        List<GasPart> parts = getGasParts();
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
        setGasParts(parts);
        if(waterMl()>0)setWaterMl(Math.max(0,waterMl()-accepted));
        setPurity(Math.min(getPurity(), purity));
        return accepted;
    }

    @Override
    public AABB virtualHitbox() {
        return new AABB(getX() - .23/2, getY(), getZ() - .23/2,
                getX() + .23/2, getY() + (hasNozzle()?.32:.27)+nozzleLift(), getZ() + .23/2);
    }

    @Override
    public ItemStack toStack() {
        String gasId = getGasId();
        ItemStack stack = hasPlate()
                ? (gasId.isEmpty() ? ModItems.emptyGasJar() : ModItems.gasBottle(gasId, true))
                : (gasId.isEmpty() ? ModItems.emptyGasJar() : ModItems.gasBottle(gasId, false));
        writeToItem(stack);
        com.example.chemistry.radiation.RadioLedger.copyState(nuclearSample,stack);
        PurityHelper.setPurity(stack, getPurity());
        applyLabel(stack, getLabelName());
        return stack;
    }

    @Override
    protected List<ItemStack> dropsOnBreak() {
        dropConnectedTubes();
        List<ItemStack> drops = new ArrayList<>();
        drops.add(toStack());
        if (hasNozzle()) {
            drops.add(new ItemStack(tubeItemFor(getTubeType())));
        }
        return drops;
    }

    public float nozzleLift(){return isInverted()&&hasNozzle()?.062F:0F;}
    public Port nozzlePort(){return Port.entity(getUUID());}
    public net.minecraft.world.phys.Vec3 nozzleTip(){
        var local=new net.minecraft.world.phys.Vec3(-.7/16*.58,(isInverted()?-.0875:.53125)*.58,-.7/16*.58);
        return position().add(local.yRot((float)Math.toRadians(-getYRot()))).add(0,nozzleLift(),0);
    }
    private void dropConnectedTubes(){
        if(!(level() instanceof net.minecraft.server.level.ServerLevel server))return;
        for(var tube:RubberTubeItem.findTubesAt(level(),nozzlePort())){
            boolean preview=false;
            for(var anchor:new Port[]{tube.getAnchorA(),tube.getAnchorB()}){
                if(anchor!=null&&anchor.kind()==Port.KIND_ENTITY&&level().getEntity(anchor.uuid()) instanceof Player player){
                    for(var hand:InteractionHand.values()){
                        var data=player.getItemInHand(hand).getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                        preview|=data.getIntOr("tube_temp_id",-1)==tube.getId();
                    }
                }
            }
            GasFlowEngine.rupture(level(),tube);
            if(!preview)tube.spawnAtLocation(server,new ItemStack(ModItems.RUBBER_TUBE.get()));
            tube.discard();
        }
    }
    @Override
    public void tick() {
        super.tick();
        setBoundingBox(virtualHitbox());
        if(!level().isClientSide()&&hasNozzle()&&tickCount==1){
            var legacy=Port.nozzle(blockPosition(),Direction.UP);
            var peers=level().getEntitiesOfClass(GasCollectingBottleEntity.class,new AABB(blockPosition()));
            if(peers.size()==1)for(var tube:RubberTubeItem.findTubesAt(level(),legacy)){
                if(legacy.equals(tube.getAnchorA()))tube.setAnchorA(nozzlePort());
                if(legacy.equals(tube.getAnchorB()))tube.setAnchorB(nozzlePort());
            }
        }
        if(!level().isClientSide()&&tickCount%20==0&&level().getBlockState(BlockPos.containing(getX(),getY()-.02,getZ())).isAir()){
            if(level() instanceof net.minecraft.server.level.ServerLevel server)for(var drop:dropsOnBreak())spawnAtLocation(server,drop);
            discard();return;
        }
        if (level().isClientSide() || getFillMl() <= 0) {
            return;
        }
        Port head = nozzlePort();
        for (RubberTubeEntity tube : RubberTubeItem.findTubesAt(level(), head)) {
            Port far = GasFlowEngine.otherAnchor(tube, head);
            if (far != null && far.kind() == Port.KIND_ENTITY && !(level().getEntity(far.uuid()) instanceof GasCollectingBottleEntity)) {
                pumpOut(tube, head);
            }
        }
    }

    private void pumpOut(RubberTubeEntity tube, Port head) {
        String id = getGasId();
        if (id.isEmpty()) {
            return;
        }
        int flow = Math.min(1, getFillMl());
        String transitId = tube.getTransitGas();
        double transitPurity = tube.getTransitPurity();
        int out = tube.takeTransit(flow);
        if (out > 0 && !transitId.isEmpty()) {
            Port far = GasFlowEngine.otherAnchor(tube, head);
            int accepted = far != null
                    ? GasFlowEngine.deliverTo(level(), far, transitId, out, transitPurity)
                    : 0;
            if (accepted < out && level() instanceof net.minecraft.server.level.ServerLevel server) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                        getX(), getY() + 1.0, getZ(),
                        Math.min(3, 1 + (out - accepted) / 2), 0.1, 0.05, 0.1, 0.01);
            }
        }
        int capacity = GasFlowEngine.tubeCapacity(level(), tube);
        int into = Math.min(flow, Math.max(0, capacity - tube.getTransitMl()));
        if (into > 0) {
            int added = tube.addTransit(id, into, getPurity());
            if (added > 0) {
                removeGas(id, added);
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!level().isClientSide()) {
            // 潜行 + 右键：翻倒（正立/倒立）。
            if (player.isShiftKeyDown() && held.isEmpty()) {
                setInverted(!isInverted());
                level().playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP,
                        SoundSource.BLOCKS, 0.5F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            // 湿橡胶管（主手或副手）：连接/锚定到瓶口导管。
            if (held.getItem() instanceof RubberTubeItem
                    || (held.isEmpty() && player.getOffhandItem().getItem() instanceof RubberTubeItem)) {
                ItemStack tube = held.isEmpty() ? player.getOffhandItem() : held;
                if (hasNozzle()) {
                    handleTubeNozzle(player, tube);
                } else {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.rubber_tube.need_bottle_nozzle"), true);
                }
                return InteractionResult.SUCCESS;
            }
            // 套管玻璃导管：把自由端连到瓶口导管。
            if (held.getItem() instanceof GlassTubeTubedItem) {
                if (!hasNozzle()) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.rubber_tube.need_bottle_nozzle"), true);
                    return InteractionResult.SUCCESS;
                }
                Port nozzle = nozzlePort();
                if (RubberTubeItem.hasTubeAt(level(), nozzle)) {
                    player.displayClientMessage(
                            Component.translatable("mchemistry.rubber_tube.occupied"), true);
                } else {
                    RubberTubeItem.createTube(level(), player, held,
                            Port.entity(player.getUUID()), nozzle);
                }
                return InteractionResult.SUCCESS;
            }
            // 玻璃导管插进玻璃片开口。
            if (held.getItem() instanceof GlassTubeItem
                    && hasPlate() && !hasNozzle()) {
                setHasNozzle(true);
                setTubeType(GlassTubeItem.tubeType(held));
                held.shrink(1);
                level().playSound(null, blockPosition(), SoundEvents.GLASS_PLACE,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            // 盖上玻璃片。
            if (held.is(ModItems.GLASS_SHEET.get()) && !hasPlate() && !hasNozzle()) {
                setHasPlate(true);
                held.shrink(1);
                level().playSound(null, blockPosition(), SoundEvents.GLASS_PLACE,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            // 空手：拆导管 → 拆玻璃片 → 取瓶。
            if (held.isEmpty()) {
                return interactEmpty(player);
            }
        } else {
            // 客户端与服务端一致返回 SUCCESS，避免 PASS 导致重试/重复返还物品。
            if (held.isEmpty()
                    || held.getItem() instanceof RubberTubeItem
                    || (held.isEmpty()
                            && player.getOffhandItem().getItem() instanceof RubberTubeItem)
                    || held.getItem() instanceof GlassTubeTubedItem
                    || (held.getItem() instanceof GlassTubeItem
                            && hasPlate() && !hasNozzle())
                    || (held.is(ModItems.GLASS_SHEET.get())
                            && !hasPlate() && !hasNozzle())) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private InteractionResult interactEmpty(Player player) {
        ItemStack off = player.getOffhandItem();
        if (hasNozzle() && off.getItem() instanceof RubberTubeItem) {
            handleTubeNozzle(player, off);
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = blockPosition();
        if (hasNozzle()) {
            dropConnectedTubes();
            giveToPlayer(player, new ItemStack(tubeItemFor(getTubeType())));
            setHasNozzle(false);
            player.displayClientMessage(
                    Component.translatable("mchemistry.gas_bottle.tube_out"), true);
        } else if (hasPlate()) {
            giveToPlayer(player, new ItemStack(ModItems.GLASS_SHEET.get()));
            setHasPlate(false);
            player.displayClientMessage(
                    Component.translatable("mchemistry.gas_bottle.plate_out"), true);
        } else {
            ItemStack bottle = toStack();
            discard();
            giveToPlayer(player, bottle);
            level().playSound(null, pos, SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        level().playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    public void handleTubeNozzle(Player player, ItemStack stack) {
        if (!RubberTubeItem.isWet(stack)) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.need_wet"), true);
            return;
        }
        Port nozzle = nozzlePort();
        if (RubberTubeItem.hasTubeAt(level(), nozzle)) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.occupied"), true);
            return;
        }
        if (GlassTubeItem.isGlassTube(player.getOffhandItem())) {
            RubberTubeItem.createTube(level(), player, stack,
                    Port.entity(player.getUUID()), nozzle);
            return;
        }
        Port pending = RubberTubeItem.readPending(stack);
        if (pending == null) {
            RubberTubeItem.startPending(level(), player, stack, nozzle);
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.start_nozzle"), true);
        } else if (RubberTubeItem.sameAnchor(pending, nozzle)) {
            player.displayClientMessage(
                    Component.translatable("mchemistry.rubber_tube.same"), true);
        } else {
            RubberTubeItem.createTube(level(), player, stack, pending, nozzle);
        }
    }

    private ItemStack nuclearSample = ItemStack.EMPTY;
    public void setRadioSample(ItemStack sample) { nuclearSample = sample.copy(); }
    public void writeToItem(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        List<GasPart> parts = getGasParts();
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
        tag.putLong(BottleCodes.KEY_ML,getFillMl()+waterMl());
        tag.putBoolean(BottleCodes.KEY_WATER,waterMl()>0);
        tag.putInt("chem_water_ml",waterMl());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        BottleCodes.setSealed(stack,hasPlate());
        BottleCodes.refreshModel(stack);
    }

    public void readFromItem(ItemStack stack) {
        nuclearSample = stack.copy();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if(BottleCodes.isWater(stack))setWaterMl(tag.getIntOr("chem_water_ml",CAPACITY_ML));
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
            setGasParts(restored);
        }
    }

    private static String serializeParts(List<GasPart> parts) {
        StringBuilder sb = new StringBuilder();
        for (GasPart p : parts) {
            if (p.id() == null || p.id().isEmpty() || p.ml() <= 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(p.id()).append(':').append(Math.min(CAPACITY_ML, p.ml()));
        }
        return sb.toString();
    }

    private static List<GasPart> deserializeParts(String s) {
        List<GasPart> parts = new ArrayList<>();
        if (s == null || s.isEmpty()) {
            return parts;
        }
        for (String seg : s.split(";")) {
            int idx = seg.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            String id = seg.substring(0, idx);
            int ml;
            try {
                ml = Integer.parseInt(seg.substring(idx + 1));
            } catch (NumberFormatException e) {
                continue;
            }
            if (!id.isEmpty() && ml > 0) {
                parts.add(new GasPart(id, ml));
            }
        }
        return parts;
    }

    private static net.minecraft.world.item.Item tubeItemFor(int type) {
        return switch (type) {
            case 2 -> ModItems.RIGHT_ANGLE_GLASS_TUBE.get();
            case 3 -> ModItems.RIGHT_ANGLE_GLASS_TUBE_LONG.get();
            case 4 -> ModItems.STRAIGHT_GLASS_TUBE_LONG.get();
            default -> ModItems.STRAIGHT_GLASS_TUBE.get();
        };
    }

    private static void applyLabel(ItemStack stack, String label) {
        if (label == null || label.isEmpty()) {
            return;
        }
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(LabelItem.KEY_LABEL, label);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private void giveToPlayer(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
