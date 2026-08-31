package com.example.chemistry;

import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.data.Combustion;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.data.Solutions;
import com.example.chemistry.data.SubstanceVariants;
import com.example.chemistry.block.AlcoholLampBlock;
import com.example.chemistry.item.CombustionSpoonItem;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;

/**
 * Thermodynamics + phase changes + reactions for placed reaction vessels
 * (圆底烧瓶 / 锥形瓶 / 坩埚 / 蒸发皿). A lit heat source ramps the vessel's
 * temperature; liquids boil above their boiling point — either evaporating
 * away or distilling into an attached receiver flask; heat-triggered
 * reactions run via the reaction engine. Boiling vessels emit bubbles/steam.
 */
public final class VesselHeating {

    public enum Outcome {
        NONE, POPPED, CRACKED
    }

    public static final int POP_PRESSURE = 100;
    private static final int CRACK_TEMP = 700;
    private static final double AIR_BURN_RATE = 0.02;
    private static final String KEY_AIR_FUEL = "chem_air_fuel";
    private static final String KEY_TEMP_LOCKED = "chem_temp_locked";
    /** Above this temperature gas in an open vessel expands and escapes. */
    private static final double GAS_ESCAPE_TEMP = 40.0;
    /** Gentle heat of a standalone alcohol lamp under a vessel (缓慢加热). */
    public static final double LAMP_TEMP = 500.0;
    /** Alcohol blowtorch: reaches 1200 C. */
    public static final double BLOWTORCH_TEMP = 1200.0;
    /** Air cooling: 1 C per 2 seconds (40 ticks). */
    private static final double AIR_COOL_PER_TICK = 1.0 / 40.0;

    /** Ramp the vessel temperature toward the heat source temperature. */
    public static void heat(ItemStack vessel, double target) {
        if (vessel.isEmpty() || !(vessel.getItem() instanceof LabVesselItem)) {
            return;
        }
        double current = TemperatureSystem.getTemp(vessel);
        double next = current + (target - current) * 0.08;
        if (Math.abs(next - current) < 0.5) {
            next = target;
        }
        TemperatureSystem.setTemp(vessel, next);
    }

    public static void cool(ItemStack vessel) {
        heat(vessel, TemperatureSystem.ROOM_TEMP);
    }

    /** True when the player pinned the vessel's temperature (I key). */
    public static boolean isTempLocked(ItemStack vessel) {
        if (vessel.isEmpty()) {
            return false;
        }
        return vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getBooleanOr(KEY_TEMP_LOCKED, false);
    }

    public static void setTempLocked(ItemStack vessel, boolean locked) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (locked) {
            tag.putBoolean(KEY_TEMP_LOCKED, true);
        } else {
            tag.remove(KEY_TEMP_LOCKED);
        }
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Steady gentle heating: 5 C per second, capped at the target. */
    public static void heatSlow(ItemStack vessel, double target) {
        if (vessel.isEmpty() || !(vessel.getItem() instanceof LabVesselItem)) {
            return;
        }
        double current = TemperatureSystem.getTemp(vessel);
        if (current >= target) {
            return;
        }
        TemperatureSystem.setTemp(vessel, Math.min(target, current + 5.0 / 20.0));
    }

    /** Blowtorch heating: twice as fast (10 C per second). */
    public static void heatFast(ItemStack vessel, double target) {
        if (vessel.isEmpty() || !(vessel.getItem() instanceof LabVesselItem)) {
            return;
        }
        double current = TemperatureSystem.getTemp(vessel);
        if (current >= target) {
            return;
        }
        TemperatureSystem.setTemp(vessel, Math.min(target, current + 10.0 / 20.0));
    }

    /** Cool in air by 1 C every 2 seconds until room temperature (20 C). */
    public static void coolGradual(ItemStack vessel) {
        if (vessel.isEmpty() || !(vessel.getItem() instanceof LabVesselItem)) {
            return;
        }
        double current = TemperatureSystem.getTemp(vessel);
        if (current <= TemperatureSystem.ROOM_TEMP) {
            return;
        }
        TemperatureSystem.setTemp(vessel,
                Math.max(TemperatureSystem.ROOM_TEMP, current - AIR_COOL_PER_TICK));
    }

    /** True when a lit standalone alcohol lamp sits directly below the vessel. */
    public static boolean lampBelow(Level level, BlockPos pos) {
        var below = level.getBlockState(pos.below());
        return below.is(ModBlocks.ALCOHOL_LAMP.get())
                && below.getValue(AlcoholLampBlock.LIT);
    }

    /** True when a lit alcohol blowtorch sits directly below the vessel. */
    public static boolean blowtorchBelow(Level level, BlockPos pos) {
        var below = level.getBlockState(pos.below());
        return below.is(ModBlocks.ALCOHOL_BLOWTORCH.get())
                && below.getValue(AlcoholLampBlock.LIT);
    }

    /** One tick of a heated placed vessel. */
    public static Outcome tick(ItemStack vessel, Level level, BlockPos pos,
            ItemStack distillateTarget, boolean hasCondenser, boolean gasOutlet, Player player) {
        if (vessel.isEmpty() || !(vessel.getItem() instanceof LabVesselItem)) {
            return Outcome.NONE;
        }
        // 敞口容器里比空气轻的气体慢慢逸出（被空气取代）。
        if (!isSealed(vessel)) {
            VesselGasPhase.tickLeak(vessel);
        }
        double temp = TemperatureSystem.getTemp(vessel);
        PhaseSystem.tick(vessel, temp);
        ReactionEngine.checkAndStart(vessel, player);
        Reactions.Reaction completed = ReactionEngine.tick(vessel, player);
        if (completed != null) {
            ReactionEngine.applyReactionHeat(vessel, completed);
            ReactionPhenomena.spawn(level,
                    new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                    ReactionPhenomena.detect(completed, vessel));
            GasFlowEngine.enqueue(level, pos, vessel, completed);
        }
        // NB-style gas flow: queue gas first, then pump it along the rubber
        // tube at a visible speed (bubbles run along the tube, the water level
        // in a 排水法 trough drops live, cutting the tube stops the flow).
        GasFlowEngine.pump(level, pos, vessel);
        combustInAir(vessel, level, pos, player);

        // Pressure: a sealed vessel with NO gas outlet heated above boiling
        // pressurises until the stopper pops off; extreme heat cracks it.
        // When a glass tube / funnel / dropper passes through the stopper the
        // produced gas escapes through it, so pressure never builds (加热制取
        // 气体时瓶塞不会被崩飞).
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean sealed = tag.getBooleanOr("chem_sealed", false);
        int pressure = tag.getIntOr("chem_pressure", 0);
        Outcome outcome = Outcome.NONE;
        if (sealed && !gasOutlet) {
            if (temp > CRACK_TEMP) {
                outcome = Outcome.CRACKED;
            } else if (temp > 100) {
                pressure += 2;
                if (pressure >= POP_PRESSURE) {
                    pressure = 0;
                    tag.remove("chem_sealed");
                    tag.remove("chem_stopper_holes");
                    outcome = Outcome.POPPED;
                }
            }
        } else if (pressure > 0) {
            pressure -= 1;
        }
        tag.putInt("chem_pressure", Math.max(0, pressure));
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        boolean boiling = false;
        for (LabVesselItem.Entry entry : LabVesselItem.getContents(vessel)) {
            if (entry.type().equals("liquid")) {
                String solute = Solutions.soluteOf(entry.id());
                if (solute != null) {
                    // Solutions lose their water above 100 C; solute crystallises.
                    if (temp > 100) {
                        if (entry.amount() <= 0.05) {
                            LabVesselItem.consumeMass(vessel, "liquid", entry.id(), entry.amount());
                            LabVesselItem.addMass(vessel, "solid", solute, 1.0);
                        } else {
                            LabVesselItem.consumeMass(vessel, "liquid", entry.id(), 0.05);
                        }
                        boiling = true;
                    }
                    continue;
                }
                double bp = ChemicalInfoProvider.boilingPointOf("liquid_" + entry.id());
                if (temp > bp) {
                    double transfer = Math.min(entry.amount(), 0.2);
                    if (hasCondenser && distillateTarget != null && !distillateTarget.isEmpty()
                            && distillateTarget.getItem() instanceof LabVesselItem
                            && LabVesselItem.addMass(distillateTarget, "liquid", entry.id(), transfer)) {
                        LabVesselItem.consumeMass(vessel, "liquid", entry.id(), transfer);
                    } else {
                        LabVesselItem.consumeMass(vessel, "liquid", entry.id(), transfer);
                    }
                    boiling = true;
                }
            } else if (entry.type().equals("solid")
                    && LabVesselItem.phaseChangeSolid(vessel, entry, temp, 0.05)) {
                boiling = true;
            }
        }
        if (boiling && level instanceof ServerLevel server) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5;
            server.sendParticles(ParticleTypes.BUBBLE, x, y, z, 2, 0.15, 0.05, 0.15, 0.01);
            server.sendParticles(ParticleTypes.CLOUD, x, y + 0.12, z, 1, 0.1, 0.05, 0.1, 0.01);
        }
        // Thermal expansion: gas in an OPEN heated vessel escapes gradually.
        if (!sealed && temp > GAS_ESCAPE_TEMP && level instanceof ServerLevel server) {
            boolean escaped = false;
            for (LabVesselItem.Entry entry : LabVesselItem.getContents(vessel)) {
                if (!entry.type().equals("gas") || entry.amount() <= 0) {
                    continue;
                }
                double escape = Math.min(entry.amount(),
                        0.02 + (temp - GAS_ESCAPE_TEMP) * 0.002);
                if (escape <= 0) {
                    continue;
                }
                LabVesselItem.consumeMass(vessel, "gas", entry.id(), escape);
                escaped = true;
            }
            if (escaped) {
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 1.0;
                double z = pos.getZ() + 0.5;
                server.sendParticles(ParticleTypes.BUBBLE, x, y, z, 2, 0.15, 0.05, 0.15, 0.01);
                if (server.getGameTime() % 8 == 0) {
                    server.sendParticles(ParticleTypes.CLOUD, x, y + 0.12, z,
                            1, 0.1, 0.05, 0.1, 0.01);
                }
            }
        }
        return outcome;
    }

    /** Open heated vessels burn combustible solids with oxygen from the air
     *  (坩埚/蒸发皿灼烧): each fuel catches fire at its own ignition point,
     *  producing the oxide (or just burning away) with flame + smoke. */
    private static void combustInAir(ItemStack vessel, Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || isSealed(vessel)) {
            return;
        }
        if (vessel.getItem() instanceof CombustionSpoonItem) {
            // The spoon burns through its own lit mechanism instead.
            return;
        }
        double temp = TemperatureSystem.getTemp(vessel);
        if (temp < 30) {
            return;
        }
        LabVesselItem.Entry fuel = null;
        for (LabVesselItem.Entry entry : LabVesselItem.getContents(vessel)) {
            if (entry.type().equals("solid")
                    && Combustion.byFuel(SubstanceVariants.canonicalOf(entry.id())) != null) {
                fuel = entry;
                break;
            }
        }
        if (fuel == null) {
            return;
        }
        String canonical = SubstanceVariants.canonicalOf(fuel.id());
        Combustion.Burn burn = Combustion.byFuel(canonical);
        if (burn == null || temp < burn.ignition()) {
            return;
        }
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.getStringOr(KEY_AIR_FUEL, "").equals(canonical)) {
            tag.putString(KEY_AIR_FUEL, canonical);
            vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            if (player != null) {
                player.displayClientMessage(Component.literal(burn.airMessage()), true);
            }
        }
        double consumed = Math.min(AIR_BURN_RATE, fuel.amount());
        LabVesselItem.consumeMass(vessel, fuel.type(), fuel.id(), consumed);
        if (!burn.vent()) {
            double fuelMolar = ChemicalInfoProvider.molarMassOf("solid_" + canonical);
            double productMolar = ChemicalInfoProvider.molarMassOf("solid_" + burn.product());
            if (fuelMolar > 0 && productMolar > 0) {
                LabVesselItem.addMass(vessel, "solid", burn.product(),
                        consumed * productMolar / fuelMolar);
            }
        }
        if (level instanceof ServerLevel server) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.85;
            double z = pos.getZ() + 0.5;
            server.sendParticles(ParticleTypes.FLAME, x, y, z, 2, 0.12, 0.08, 0.12, 0.01);
            server.sendParticles(ParticleTypes.SMOKE, x, y + 0.15, z, 1, 0.1, 0.05, 0.1, 0.01);
        }
    }

    /** Seal a vessel with a rubber stopper (holes 1 or 2). */
    public static void seal(ItemStack vessel, int holes) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean("chem_sealed", true);
        tag.putInt("chem_stopper_holes", holes);
        tag.putInt("chem_pressure", 0);
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isSealed(ItemStack vessel) {
        return vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getBooleanOr("chem_sealed", false);
    }

    public static int getStopperHoles(ItemStack vessel) {
        if (isThreeNeck(vessel)) {
            int max = 0;
            for (int i = 0; i < 3; i++) {
                max = Math.max(max, rubberHoles(vessel, i));
            }
            return max;
        }
        return vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getIntOr("chem_stopper_holes", 0);
    }

    /** Passivation (钝化): concentrated oxidizing acids make Fe/Al inert. */
    public static final double PASSIVATION_BREAK_TEMP = 150.0;
    private static final String KEY_PASSIVATED = "chem_passivated";

    public static void markPassivated(ItemStack vessel, String canonicalId) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = tag.getListOrEmpty(KEY_PASSIVATED);
        for (Tag t : list) {
            if (t instanceof StringTag st && st.value().equals(canonicalId)) {
                return;
            }
        }
        list.add(StringTag.valueOf(canonicalId));
        tag.put(KEY_PASSIVATED, list);
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isPassivated(ItemStack vessel, String canonicalId) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        for (Tag t : tag.getListOrEmpty(KEY_PASSIVATED)) {
            if (t instanceof StringTag st && st.value().equals(canonicalId)) {
                return true;
            }
        }
        return false;
    }

    public static void clearPassivated(ItemStack vessel) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(KEY_PASSIVATED);
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Clear the seal (stopper popped off). */
    public static void unseal(ItemStack vessel) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove("chem_sealed");
        tag.remove("chem_stopper_holes");
        tag.remove(KEY_RUBBER_HOLES);
        tag.putInt("chem_pressure", 0);
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // --- Three-neck flask (三颈烧瓶): per-neck glass + rubber stoppers ---

    /** 每颈橡胶塞孔数（ListTag，3 个 int：0 = 无橡胶塞，1/2/3 = 孔数）。 */
    public static final String KEY_RUBBER_HOLES = "chem_rubber_holes";

    public static int rubberHoles(ItemStack vessel, int neck) {
        if (neck < 0 || neck > 2) {
            return 0;
        }
        ListTag list = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getListOrEmpty(KEY_RUBBER_HOLES);
        if (neck >= list.size()) {
            return 0;
        }
        return list.get(neck) instanceof IntTag it ? it.intValue() : 0;
    }

    public static boolean hasAnyRubber(ItemStack vessel) {
        for (int i = 0; i < 3; i++) {
            if (rubberHoles(vessel, i) > 0) {
                return true;
            }
        }
        return false;
    }

    /** 在某个瓶口塞一个带孔橡胶塞（与该瓶口玻璃塞互斥）。 */
    public static void sealNeck(ItemStack vessel, int neck, int holes) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int mask = tag.getIntOr("chem_stoppers", 0) & ~(1 << neck);
        tag.putInt("chem_stoppers", mask);
        tag.put(KEY_RUBBER_HOLES, rubberList(tag, neck, holes));
        recomputeSealed(tag);
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** 拆下某个瓶口的橡胶塞。 */
    public static void unsealNeck(ItemStack vessel, int neck) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.put(KEY_RUBBER_HOLES, rubberList(tag, neck, 0));
        recomputeSealed(tag);
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static ListTag rubberList(CompoundTag tag, int setNeck, int setValue) {
        ListTag old = tag.getListOrEmpty(KEY_RUBBER_HOLES);
        ListTag out = new ListTag();
        for (int i = 0; i < 3; i++) {
            int h = 0;
            if (i < old.size() && old.get(i) instanceof IntTag it) {
                h = it.intValue();
            }
            if (i == setNeck) {
                h = setValue;
            }
            out.add(IntTag.valueOf(h));
        }
        return out;
    }

    /** 按“任一橡胶塞 / 三个玻璃塞全塞”重算 chem_sealed。 */
    private static void recomputeSealed(CompoundTag tag) {
        int mask = tag.getIntOr("chem_stoppers", 0);
        boolean rubber = false;
        for (Tag t : tag.getListOrEmpty(KEY_RUBBER_HOLES)) {
            if (t instanceof IntTag it && it.intValue() > 0) {
                rubber = true;
            }
        }
        if (mask == 0b111 || rubber) {
            tag.putBoolean("chem_sealed", true);
        } else {
            tag.remove("chem_sealed");
        }
    }

    /** Neck (x, y) positions in block-model units; z is always 8.5. Left, centre, right. */
    /** 三颈瓶三个瓶口（模型帧）：两侧颈向外张开 22.5°，y 略高于瓶口让塞子坐在上面。 */
    public static final double[][] THREE_NECK = {{5.0, 9.3}, {8.5, 10.4}, {12.0, 9.34}};

    public static boolean isThreeNeck(ItemStack vessel) {
        if (vessel.isEmpty()) {
            return false;
        }
        return net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(vessel.getItem()).getPath().equals("three_neck_flask");
    }

    /** 容器类型码（与 ModStandaloneModels.VESSELS 索引一致）：0 = 非容器。 */
    public static int vesselType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).getPath();
        return switch (path) {
            case "round_bottom_flask", "ground_glass_flask" -> 1;
            case "erlenmeyer_flask", "ground_glass_erlenmeyer" -> 2;
            case "crucible" -> 3;
            case "evaporating_dish" -> 4;
            case "beaker_50ml", "beaker_100ml", "beaker_500ml", "beaker_1000ml" -> 5;
            case "three_neck_flask" -> 6;
            case "flat_bottom_flask", "ground_glass_flat_bottom_flask" -> 7;
            default -> 0;
        };
    }

    /** 单口容器瓶口在模型帧里的 y（与各渲染器塞子位置一致）。 */
    public static double mouthTopY(int vesselType) {
        return switch (vesselType) {
            case 3 -> 6.0;    // crucible
            case 4 -> 2.0;    // evaporating dish
            case 5 -> 6.0;    // beaker
            case 6 -> 10.4;   // three-neck flask centre neck
            case 7 -> 10.0;   // flat-bottom flask
            default -> 9.0;   // round-bottom / erlenmeyer / others
        };
    }

    /** 单口容器瓶口的世界坐标（与渲染器塞子位置一致；镜像 neckWorldPositions）。 */
    public static Vec3 mouthWorldPosition(BlockPos pos, int vesselType, double scale,
            double offX, double offY, double offZ, double yawDegrees) {
        double lx = offX + scale * 8.5 / 16.0;
        double ly = offY + scale * mouthTopY(vesselType) / 16.0;
        double lz = offZ + scale * 8.5 / 16.0;
        double wx = pos.getX() + lx;
        double wz = pos.getZ() + lz;
        if (yawDegrees != 0.0) {
            double yaw = Math.toRadians(yawDegrees);
            double c = Math.cos(yaw);
            double s2 = Math.sin(yaw);
            double cx = pos.getX() + 0.5;
            double cz = pos.getZ() + 0.5;
            double dx = wx - cx;
            double dz = wz - cz;
            wx = cx + dx * c + dz * s2;
            wz = cz - dx * s2 + dz * c;
        }
        return new Vec3(wx, pos.getY() + ly, wz);
    }

    /** 玩家视线是否命中单口容器的瓶口（半径 0.4 格）。 */
    public static boolean mouthForRay(BlockPos pos, int vesselType, double scale,
            double offX, double offY, double offZ, double yawDegrees, Player player) {
        if (player == null) {
            return false;
        }
        Vec3 m = mouthWorldPosition(pos, vesselType, scale, offX, offY, offZ, yawDegrees);
        Vec3 from = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        Vec3 v = m.subtract(from);
        double t = v.dot(dir);
        Vec3 closest = t > 0 ? from.add(dir.scale(t)) : from;
        return m.distanceTo(closest) <= 0.4;
    }

    /** 容器在模型帧(0..16)里的大致包围盒：{{minX,minY,minZ},{maxX,maxY,maxZ}}。 */
    public static double[][] vesselBounds(int vesselType) {
        return switch (vesselType) {
            case 1 -> new double[][] {{6, 0, 6}, {11, 10, 11}};      // round-bottom flask
            case 2 -> new double[][] {{6, 0, 6}, {11, 9, 11}};       // erlenmeyer flask
            case 3 -> new double[][] {{6, 0, 6}, {11, 6, 11}};       // crucible
            case 4 -> new double[][] {{6, 0, 6}, {11, 2, 11}};       // evaporating dish
            case 5 -> new double[][] {{6, 0, 5}, {11, 6, 10}};       // beaker
            case 6 -> new double[][] {{5, 0, 7.5}, {12, 10.4, 9.5}}; // three-neck flask (含两侧颈)
            case 7 -> new double[][] {{6, 0, 6}, {11, 10, 11}};      // flat-bottom flask
            default -> null;
        };
    }

    /** 通用挂载点容器碰撞盒：点 = off + scale*模型/16，再绕方块中心旋转 yaw。 */
    public static net.minecraft.world.phys.shapes.VoxelShape vesselCollisionShape(
            BlockPos pos, int vesselType, double scale,
            double offX, double offY, double offZ, double yawDegrees) {
        double[][] b = vesselBounds(vesselType);
        if (b == null) {
            return net.minecraft.world.phys.shapes.Shapes.empty();
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    double mx = b[i][0], my = b[j][1], mz = b[k][2];
                    double lx = offX + scale * mx / 16.0;
                    double ly = offY + scale * my / 16.0;
                    double lz = offZ + scale * mz / 16.0;
                    double wx = pos.getX() + lx;
                    double wz = pos.getZ() + lz;
                    if (yawDegrees != 0.0) {
                        double yaw = Math.toRadians(yawDegrees);
                        double c = Math.cos(yaw);
                        double s2 = Math.sin(yaw);
                        double cx = pos.getX() + 0.5;
                        double cz = pos.getZ() + 0.5;
                        double dx = wx - cx;
                        double dz = wz - cz;
                        wx = cx + dx * c + dz * s2;
                        wz = cz - dx * s2 + dz * c;
                    }
                    double wy = pos.getY() + ly;
                    minX = Math.min(minX, wx); maxX = Math.max(maxX, wx);
                    minY = Math.min(minY, wy); maxY = Math.max(maxY, wy);
                    minZ = Math.min(minZ, wz); maxZ = Math.max(maxZ, wz);
                }
            }
        }
        return clampToBlock(minX, minY, minZ, maxX, maxY, maxZ, pos);
    }

    /** 三脚架容器碰撞盒：三脚架整体 0.75 缩放（绕地面中心），容器原点在三角环上方。 */
    public static net.minecraft.world.phys.shapes.VoxelShape tripodVesselCollisionShape(
            BlockPos pos, int vesselType) {
        double[][] b = vesselBounds(vesselType);
        if (b == null) {
            return net.minecraft.world.phys.shapes.Shapes.empty();
        }
        double scale = 0.75;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    double mx = b[i][0], my = b[j][1], mz = b[k][2];
                    // 渲染：先平移到 (0.5+mx, 11.17+my, mz-0.5)/16，再整体 0.75 缩放。
                    double lx = (0.5 + mx) / 16.0;
                    double ly = (11.17 + my) / 16.0;
                    double lz = (mz - 0.5) / 16.0;
                    double wx = pos.getX() + 0.5 + scale * (lx - 0.5);
                    double wy = pos.getY() + scale * ly;
                    double wz = pos.getZ() + 0.5 + scale * (lz - 0.5);
                    minX = Math.min(minX, wx); maxX = Math.max(maxX, wx);
                    minY = Math.min(minY, wy); maxY = Math.max(maxY, wy);
                    minZ = Math.min(minZ, wz); maxZ = Math.max(maxZ, wz);
                }
            }
        }
        return clampToBlock(minX, minY, minZ, maxX, maxY, maxZ, pos);
    }

    private static net.minecraft.world.phys.shapes.VoxelShape clampToBlock(
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            BlockPos pos) {
        double x0 = Mth.clamp(minX - pos.getX(), 0.0, 1.0);
        double y0 = Mth.clamp(minY - pos.getY(), 0.0, 1.0);
        double z0 = Mth.clamp(minZ - pos.getZ(), 0.0, 1.0);
        double x1 = Mth.clamp(maxX - pos.getX(), 0.0, 1.0);
        double y1 = Mth.clamp(maxY - pos.getY(), 0.0, 1.0);
        double z1 = Mth.clamp(maxZ - pos.getZ(), 0.0, 1.0);
        if (x1 - x0 < 1.0e-4 || y1 - y0 < 1.0e-4 || z1 - z0 < 1.0e-4) {
            return net.minecraft.world.phys.shapes.Shapes.empty();
        }
        return net.minecraft.world.phys.shapes.Shapes.box(x0, y0, z0, x1, y1, z1);
    }

    /** 三脚架上容器瓶口的世界坐标（与 tripodVesselCollisionShape 同一变换）。 */
    public static Vec3 tripodMouthWorldPosition(BlockPos pos, int vesselType) {
        double topY = mouthTopY(vesselType);
        double scale = 0.75;
        double lx = (0.5 + 8.5) / 16.0;
        double ly = (11.17 + topY) / 16.0;
        double lz = (8.5 - 0.5) / 16.0;
        double wx = pos.getX() + 0.5 + scale * (lx - 0.5);
        double wy = pos.getY() + scale * ly;
        double wz = pos.getZ() + 0.5 + scale * (lz - 0.5);
        return new Vec3(wx, wy, wz);
    }

    /** 玩家视线是否命中三脚架上容器的瓶口（半径 0.4）。 */
    public static boolean tripodMouthForRay(BlockPos pos, int vesselType, Player player) {
        if (player == null) {
            return false;
        }
        Vec3 m = tripodMouthWorldPosition(pos, vesselType);
        Vec3 from = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        Vec3 v = m.subtract(from);
        double t = v.dot(dir);
        Vec3 closest = t > 0 ? from.add(dir.scale(t)) : from;
        return m.distanceTo(closest) <= 0.4;
    }

    /** 附件类型码（与各渲染器 attachedType 一致）：1/2/4 玻璃导管，3 滴管，
     *  5/6 漏斗，7 温度计。 */
    public static int attachedType(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof com.example.chemistry.item.DropperItem) {
            return 3;
        }
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).getPath();
        return switch (path) {
            case "straight_glass_tube" -> 1;
            case "right_angle_glass_tube" -> 2;
            case "right_angle_glass_tube_long" -> 4;
            case "long_stem_funnel" -> 5;
            case "separatory_funnel" -> 6;
            case "thermometer" -> 7;
            default -> 0;
        };
    }

    /** 附件插在塞子上的基准点：三颈瓶在带塞子的那个颈，其它容器在瓶口。 */
    public static Vec3 attachedBase(BlockPos pos, ItemStack vessel, int vesselType,
            double scale, double offX, double offY, double offZ, double yawDegrees) {
        if (isThreeNeck(vessel)) {
            Vec3[] necks = neckWorldPositions(pos, scale, offX, offY, offZ, yawDegrees);
            for (int n = 0; n < 3; n++) {
                if (rubberHoles(vessel, n) > 0) {
                    return necks[n];
                }
            }
            return necks[1];
        }
        return mouthWorldPosition(pos, vesselType, scale, offX, offY, offZ, yawDegrees);
    }

    /** 插在塞子里的附件头部命中盒（hole=1/2，hasOther=另一孔是否占用）。 */
    @org.jetbrains.annotations.Nullable
    public static net.minecraft.world.phys.AABB attachedHeadBox(BlockPos pos,
            ItemStack vessel, int vesselType, double scale,
            double offX, double offY, double offZ, double yawDegrees,
            int hole, boolean hasOther, ItemStack attached) {
        int type = attachedType(attached);
        if (type == 0) {
            return null;
        }
        Vec3 m = attachedBase(pos, vessel, vesselType, scale, offX, offY, offZ, yawDegrees);
        double zOff = hole == 2 ? 0.55 / 16.0 : (hasOther ? -0.55 / 16.0 : 0.0);
        double y0, y1, half;
        switch (type) {
            case 1, 2, 4 -> { y0 = 0.38; y1 = 1.2; half = 0.24; }  // 玻璃导管
            case 3 -> { y0 = 0.38; y1 = 1.05; half = 0.26; }       // 胶头滴管
            case 5, 6 -> { y0 = 0.38; y1 = 1.3; half = 0.34; }     // 长颈/分液漏斗
            case 7 -> { y0 = 0.38; y1 = 1.3; half = 0.2; }         // 温度计
            default -> {
                return null;
            }
        }
        return new net.minecraft.world.phys.AABB(
                m.x - half, m.y + y0, m.z + zOff - half,
                m.x + half, m.y + y1, m.z + zOff + half);
    }

    /** 射线选中附件：返回 1/2（塞子的哪个孔），-1 = 没对准任何附件。 */
    public static int pickAttached(Player player, BlockPos pos, ItemStack vessel,
            int vesselType, double scale, double offX, double offY, double offZ,
            double yawDegrees, ItemStack attached1, ItemStack attached2) {
        if (player == null) {
            return -1;
        }
        Vec3 from = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        int best = -1;
        double bestD = Double.MAX_VALUE;
        net.minecraft.world.phys.AABB[] boxes = {
                attachedHeadBox(pos, vessel, vesselType, scale, offX, offY, offZ, yawDegrees,
                        1, !attached2.isEmpty(), attached1),
                attachedHeadBox(pos, vessel, vesselType, scale, offX, offY, offZ, yawDegrees,
                        2, false, attached2)};
        for (int i = 0; i < 2; i++) {
            net.minecraft.world.phys.AABB box = boxes[i];
            if (box == null) {
                continue;
            }
            java.util.Optional<Vec3> hit = box.clip(from, from.add(dir.scale(6.0)));
            if (hit.isPresent()) {
                double d = hit.get().distanceToSqr(from);
                if (d < bestD) {
                    bestD = d;
                    best = i + 1;
                }
            }
        }
        return best;
    }

    /** 射线是否命中蒸馏头（位于瓶口上方）。 */
    public static boolean pickDistillationHead(Player player, BlockPos pos,
            ItemStack vessel, int vesselType, double scale,
            double offX, double offY, double offZ, double yawDegrees) {
        if (player == null) {
            return false;
        }
        Vec3 m = attachedBase(pos, vessel, vesselType, scale, offX, offY, offZ, yawDegrees);
        // 蒸馏头本体：从瓶口向上约 3/16（模型缩放 0.5 后），温度计再向上伸。
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                m.x - 0.32, m.y + 0.05, m.z - 0.32,
                m.x + 0.32, m.y + 0.55, m.z + 0.32);
        Vec3 from = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        return box.clip(from, from.add(dir.scale(6.0))).isPresent();
    }

    public static boolean isGrounded(ItemStack vessel) {
        return vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getBooleanOr("chem_grounded", false);
    }

    /** Bitmask of plugged necks (bit 0 = left, 1 = centre, 2 = right). */
    public static int neckStopperMask(ItemStack vessel) {
        return vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getIntOr("chem_stoppers", 0);
    }

    public static boolean neckHasStopper(ItemStack vessel, int neck) {
        return rubberHoles(vessel, neck) > 0 || (neckStopperMask(vessel) & (1 << neck)) != 0;
    }

    public static int neckStopperCount(ItemStack vessel) {
        int count = Integer.bitCount(neckStopperMask(vessel));
        for (int i = 0; i < 3; i++) {
            if (rubberHoles(vessel, i) > 0) {
                count++;
            }
        }
        return count;
    }

    /** Put / remove a glass stopper on one neck (与同颈橡胶塞互斥)。 */
    public static void setNeckStopper(ItemStack vessel, int neck, boolean plugged) {
        CompoundTag tag = vessel.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int mask = tag.getIntOr("chem_stoppers", 0);
        if (plugged) {
            mask |= (1 << neck);
        } else {
            mask &= ~(1 << neck);
        }
        tag.putInt("chem_stoppers", mask);
        if (plugged && rubberHoles(vessel, neck) > 0) {
            tag.put(KEY_RUBBER_HOLES, rubberList(tag, neck, 0));
        }
        recomputeSealed(tag);
        vessel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** World positions of the three necks for a flask drawn with the given
     *  scale, block-local offset and yaw (0 for upright holders). Mirrors the
     *  renderer transform: point = offset + scale * model / 16. */
    public static Vec3[] neckWorldPositions(BlockPos pos, double scale,
            double offX, double offY, double offZ, double yawDegrees) {
        Vec3[] out = new Vec3[3];
        for (int i = 0; i < 3; i++) {
            double lx = offX + scale * THREE_NECK[i][0] / 16.0;
            double ly = offY + scale * THREE_NECK[i][1] / 16.0;
            double lz = offZ + scale * 8.5 / 16.0;
            double wx = pos.getX() + lx;
            double wz = pos.getZ() + lz;
            if (yawDegrees != 0.0) {
                double yaw = Math.toRadians(yawDegrees);
                double c = Math.cos(yaw);
                double s2 = Math.sin(yaw);
                double cx = pos.getX() + 0.5;
                double cz = pos.getZ() + 0.5;
                double dx = wx - cx;
                double dz = wz - cz;
                wx = cx + dx * c + dz * s2;
                wz = cz - dx * s2 + dz * c;
            }
            out[i] = new Vec3(wx, pos.getY() + ly, wz);
        }
        return out;
    }

    /** Nearest neck to a click point (horizontal x/z distance only — the
     *  centre neck sits higher than the two side necks, so including y would
     *  always bias the selection toward the centre). */
    public static int neckForClick(Vec3[] necks, Vec3 click) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < necks.length; i++) {
            double dx = click.x - necks[i].x;
            double dz = click.z - necks[i].z;
            double d = dx * dx + dz * dz;
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    /**
     * 用玩家视线选颈：取视线到各瓶口点的最近距离，命中半径 0.6 格。
     * 判定框比点击点大、且各颈互不冲突（返回 -1 表示没对准任何瓶口）。
     */
    public static int neckForRay(Vec3[] necks, net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return -1;
        }
        Vec3 from = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        // 用"射线到瓶口中心的最近距离"选颈：三个瓶口离得很近，AABB 命中框会
        // 互相重叠导致选错颈；点到射线距离没有歧义，命中半径 0.35 格。
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < necks.length; i++) {
            Vec3 n = necks[i];
            Vec3 v = n.subtract(from);
            double t = v.dot(dir);
            Vec3 closest = t > 0 ? from.add(dir.scale(t)) : from;
            double d = n.distanceTo(closest);
            if (d <= 0.35 && d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private VesselHeating() {
    }
}
