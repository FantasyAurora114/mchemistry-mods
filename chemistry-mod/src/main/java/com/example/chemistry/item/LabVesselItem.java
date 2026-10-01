package com.example.chemistry.item;

import java.util.ArrayList;
import java.util.List;

import com.example.chemistry.data.ChemicalInfoProvider;
import com.example.chemistry.data.Liquids;
import com.example.chemistry.data.Solids;
import com.example.chemistry.data.Solutions;
import com.example.chemistry.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

/**
 * A reaction vessel (试管 / 烧杯) that holds reagents. Contents are stored as
 * custom data; the item texture shows a liquid-coloured fill when non-empty.
 */
public class LabVesselItem extends Item {

    private static final String SOLUTE_MARKS = "chem_solution_solutes";
    private final int capacity;

    public LabVesselItem(Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    /** Let the client send the use packet when the other hand holds a transfer
     *  tool / bottle, so the vessel can be filled from either hand. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack other = hand == InteractionHand.MAIN_HAND
                ? player.getOffhandItem() : player.getMainHandItem();
        if (com.example.chemistry.LabInteractions.isTransferTool(other)) {
            return InteractionResult.SUCCESS;
        }
        // 左手反应容器 + 右手空手：倒出容器内全部物质（之后容器为空）。
        if (hand == InteractionHand.OFF_HAND && other.isEmpty()) {
            if (!level.isClientSide()) {
                pourOutAll(player, player.getOffhandItem());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public int capacity() {
        return capacity;
    }

    public record Entry(String type, String id, double amount) {
    }

    /** Hydrated salts -> anhydrous product (mass preserved by molar ratio). */
    private static final java.util.Map<String, String> HYDROUS_TO_ANHYDROUS =
            java.util.Map.of(
                    "iron_sulfate_heptahydrate", "iron_sulfate",
                    "copper_sulfate_pentahydrate", "copper_sulfate_anhydrous");

    /** Solid phase change inside a vessel: hydrated salts lose their water of
     *  crystallisation (绿矾/胆矾 -> anhydrous sulfate) instead of vanishing,
     *  and only a much hotter anhydrous solid evaporates slowly. Shared by the
     *  held-vessel ticker and the placed-vessel thermodynamics. */
    public static boolean phaseChangeSolid(ItemStack stack, Entry entry, double temp, double rate) {
        if (temp <= ChemicalInfoProvider.boilingPointOf("solid_" + entry.id())) {
            return false;
        }
        String anhydrous = HYDROUS_TO_ANHYDROUS.get(entry.id());
        if (anhydrous != null) {
            double anhydrousMolar = ChemicalInfoProvider.molarMassOf("solid_" + anhydrous);
            double hydrousMolar = ChemicalInfoProvider.molarMassOf("solid_" + entry.id());
            if (anhydrousMolar > 0 && hydrousMolar > 0) {
                double converted = Math.min(entry.amount(), rate);
                consumeMass(stack, "solid", entry.id(), converted);
                addMass(stack, "solid", anhydrous, converted * anhydrousMolar / hydrousMolar);
            } else {
                consumeMass(stack, "solid", entry.id(), entry.amount());
            }
            return true;
        }
        if (ChemicalInfoProvider.boilingPointOf("solid_" + entry.id()) > 400) {
            // True boiling point: evaporate slowly. Below that, the "boiling
            // point" is a dehydration temperature, not an evaporation one.
            consumeMass(stack, "solid", entry.id(), Math.min(entry.amount(), rate));
        } else {
            consumeMass(stack, "solid", entry.id(), entry.amount());
        }
        return true;
    }

    public static List<Entry> getContents(ItemStack stack) {
        List<Entry> out = new ArrayList<>();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = tag.getListOrEmpty("chem_contents");
        for (Tag t : list) {
            CompoundTag c = (CompoundTag) t;
            out.add(new Entry(c.getStringOr("type", ""), c.getStringOr("id", ""), c.getDoubleOr("amount", 0.0)));
        }
        return out;
    }

    /** Colour of the first content entry (liquid or solid), for placed rendering. */
    public static int contentsColor(ItemStack stack) {
        if(getContents(stack).stream().anyMatch(e->com.example.chemistry.organic.Extraction.iodine(e.id()))){var phase=com.example.chemistry.organic.LiquidPhases.read(stack);if(phase.modelled()&&phase.layers().size()==1)return phase.layers().getFirst().color();}
        return com.example.chemistry.solution.FutureChemistry.color(stack,com.example.chemistry.solution.BatchChemistry.color(stack,com.example.chemistry.solution.EdtaEquilibrium.color(stack, com.example.chemistry.solution.CoordinationEquilibrium.liquidColor(stack, legacyContentsColor(stack)))));
    }

    private static int legacyContentsColor(ItemStack stack) {
        for (Entry e : getContents(stack)) {
            if (e.type().equals("liquid")) {
                for (Liquids.Liquid l : Liquids.ALL) {
                    if (l.id().equals(e.id())) {
                        return l.color();
                    }
                }
            } else {
                for (Solids.Solid s : Solids.ALL) {
                    if (s.id().equals(e.id())) {
                        return s.color();
                    }
                }
            }
        }
        return 0xFFFFFF;
    }

    public static double totalMass(ItemStack stack) {
        return getContents(stack).stream().mapToDouble(Entry::amount).sum();
    }

    /** Shared occupied volume in mL for filling and gas headspace. */
    public static double usedVolume(ItemStack stack) {
        double volume = com.example.chemistry.utility.BeakerWaterBath.displacement(stack);
        for (Entry entry : getContents(stack)) {
            if (entry.type().equals("liquid") || entry.type().equals("solid")) {
                volume += entryVolume(stack, entry);
            }
        }
        return volume;
    }

    /** Solute entries store solute grams, not a second volume of stock solution. */
    public static double entryVolume(ItemStack stack, Entry entry) {
        if (entry.type().equals("liquid") && Solutions.soluteOf(entry.id()) != null
                && isExplicitSolute(stack, entry.id())) {
            return entry.amount() * Solutions.soluteMlPerGram(entry.id());
        }
        return entry.amount() / gramsPerMl(entry.type(), entry.id());
    }

    public static boolean isExplicitSolute(ItemStack stack, String id) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getCompoundOrEmpty(SOLUTE_MARKS).getBooleanOr(id, false)
                || (id.equals("sodium_hydroxide_solution")
                    && tag.getBooleanOr("chem_naoh_explicit_water", false));
    }

    public static void markSolute(ItemStack stack, String id) {
        if (isExplicitSolute(stack, id)) return;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag marks = tag.getCompoundOrEmpty(SOLUTE_MARKS);
        marks.putBoolean(id, true);
        tag.put(SOLUTE_MARKS, marks);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Newly formed or stirred particles gradually settle to the bottom. */
    public static double suspension(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getDoubleOr("chem_suspension", 0.0);
    }

    public static void tickSuspension(ItemStack stack, boolean stirred) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        double before = tag.getDoubleOr("chem_suspension", 0.0);
        double after = stirred ? 1.0 : Math.max(0.0, before - 0.01);
        if (Math.abs(before - after) < 1.0e-8) return;
        if (after == 0) tag.remove("chem_suspension");
        else tag.putDouble("chem_suspension", after);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static double gramsPerMl(String type, String id) {
        if (type.equals("solid") || id.startsWith("molten_")) {
            return 5.0;
        }
        return Math.max(ChemicalInfoProvider.densityOfLiquid(id), 0.001);
    }

    public static boolean canAdd(ItemStack stack, double mass) {
        return canAdd(stack, "solid", "", mass);
    }

    private static boolean canAdd(ItemStack stack, String type, String id, double mass) {
        if (!(stack.getItem() instanceof LabVesselItem vessel)) {
            return false;
        }
        if (!Double.isFinite(mass) || mass < 0) {
            return false;
        }
        return usedVolume(stack) + mass / gramsPerMl(type, id) <= vessel.capacity + 1.0e-6;
    }

    public static boolean addLiquid(ItemStack stack, String liquidId, int ml) {
        if(com.example.chemistry.organic.OrganicApparatus.covered(stack))return false;
        if (!(stack.getItem() instanceof LabVesselItem vessel) || ml <= 0) return false;
        ItemStack trial = stack.copy();
        normalizeSolutions(trial);
        if (Solutions.soluteOf(liquidId) != null) {
            double total = ml * ChemicalInfoProvider.densityOfLiquid(liquidId);
            double solute = total * Solutions.stockFraction(liquidId);
            addMass(trial, "liquid", "water", total - solute);
            addMass(trial, "liquid", liquidId, solute);
            if (usedVolume(trial) > vessel.capacity + 1.0e-6) return false;
            com.example.chemistry.ThermalSystem.mix(ItemStack.EMPTY,trial,java.util.List.of(
                    new Entry("liquid","water",total-solute),new Entry("liquid",liquidId,solute)));
            stack.set(DataComponents.CUSTOM_DATA, trial.get(DataComponents.CUSTOM_DATA));
            updateTint(stack);
            return true;
        }
        if(liquidId.equals("crude_saltwater"))return addMixture(stack,liquidId,ml,true);
        double grams = ml * ChemicalInfoProvider.densityOfLiquid(liquidId);
        if (!add(trial, "liquid", liquidId, grams)) return false;
        com.example.chemistry.ThermalSystem.mix(ItemStack.EMPTY,trial,java.util.List.of(new Entry("liquid",liquidId,grams)));
        stack.set(DataComponents.CUSTOM_DATA, trial.get(DataComponents.CUSTOM_DATA));
        updateTint(stack);
        return true;
    }

    /** One-time legacy upgrade. Explicit-water records are ambiguous: preserve their mass. */
    public static void normalizeSolutions(ItemStack stack) {
        if (!(stack.getItem() instanceof LabVesselItem)) return;
        List<Entry> entries = getContents(stack);
        boolean waterPresent = entries.stream().anyMatch(e -> e.type().equals("liquid")
                && e.id().equals("water") && e.amount() > 0);
        for (Entry entry : entries) {
            String id = entry.id();
            if (!entry.type().equals("liquid") || Solutions.soluteOf(id) == null
                    || isExplicitSolute(stack, id)) continue;
            markSolute(stack, id);
            if (!waterPresent && Solutions.isStockReagent(id)) {
                double solute = entry.amount() * Solutions.stockFraction(id);
                consumeMass(stack, "liquid", id, entry.amount());
                addMass(stack, "liquid", "water", entry.amount() - solute);
                addMass(stack, "liquid", id, solute);
            }
        }
    }

    /** Pour the whole liquid mixture atomically, without reinterpreting solutes as stock. */
    public static boolean transferLiquids(ItemStack source,ItemStack target){
        if(source==target||!(source.getItem() instanceof LabVesselItem)||!(target.getItem() instanceof LabVesselItem v))return false;
        double volume=com.example.chemistry.filtration.Filtration.liquidVolume(source);
        if(volume<=0||usedVolume(target)+volume>v.capacity()+1e-6)return false;
        return com.example.chemistry.titration.LiquidTransfer.pour(source,target,volume)>0;
    }

    public static boolean addSolid(ItemStack stack, String solidId) {
        return solidId.equals("crude_salt") ? addMixture(stack,solidId,5,true) : add(stack, "solid", solidId, 5.0);
    }

    /** Add a raw mass (grams), used when reaction products are produced. */
    public static boolean addMass(ItemStack stack, String type, String id, double grams) {
        if (!Double.isFinite(grams) || grams <= 0) return false;
        if(id.equals("crude_salt")||id.equals("crude_saltwater"))return addMixture(stack,id,grams,false);
        if (type.equals("liquid") && Solutions.soluteOf(id) != null) markSolute(stack, id);
        return add(stack, type, id, grams, false);
    }

    private static boolean addMixture(ItemStack stack,String id,double grams,boolean enforceCapacity){
        if(!(stack.getItem() instanceof LabVesselItem vessel))return false;
        ItemStack trial=stack.copy();boolean wet=id.equals("crude_saltwater");
        if(wet)addMass(trial,"liquid","water",grams*.8);
        double dry=grams*(wet?.2:1);
        addMass(trial,"solid","sodium_chloride",dry*.9);
        addMass(trial,"solid","silicon_dioxide",dry*.05);
        addMass(trial,"solid","calcium_chloride",dry*.03);
        addMass(trial,"solid","magnesium_chloride",dry*.02);
        if(enforceCapacity&&usedVolume(trial)>vessel.capacity()+1e-6)return false;
        stack.set(DataComponents.CUSTOM_DATA,trial.get(DataComponents.CUSTOM_DATA));updateTint(stack);return true;
    }

    /** Condense up to the receiver's available volume; return the mass retained. */
    public static double addLiquidMassUpToCapacity(ItemStack stack, String id, double grams) {
        if (!(stack.getItem() instanceof LabVesselItem vessel)
                || !Double.isFinite(grams) || grams <= 0) {
            return 0.0;
        }
        normalizeSolutions(stack);
        double density = gramsPerMl("liquid", id);
        double freeMl = Math.max(0.0, vessel.capacity - usedVolume(stack));
        double accepted = Math.min(grams, freeMl * density);
        if (accepted <= 1.0e-6) {
            return 0.0;
        }
        add(stack, "liquid", id, accepted, false);
        return accepted;
    }

    /** Remove a raw mass (grams) from a reactant entry; drops the entry at zero. */
    public static void consumeMass(ItemStack stack, String type, String id, double grams) {
        ListTag list = new ListTag();
        for (Entry e : getContents(stack)) {
            if (e.type().equals(type) && e.id().equals(id)) {
                double remaining = e.amount() - grams;
                if (remaining > 1.0e-9) {
                    CompoundTag c = new CompoundTag();
                    c.putString("type", type);
                    c.putString("id", id);
                    c.putDouble("amount", remaining);
                    list.add(c);
                }
            } else {
                CompoundTag c = new CompoundTag();
                c.putString("type", e.type());
                c.putString("id", e.id());
                c.putDouble("amount", e.amount());
                list.add(c);
            }
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(com.example.chemistry.ReactionEngine.KEY_EQUILIBRIUM);
        tag.put("chem_contents", list);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        com.example.chemistry.VesselGasPhase.normalize(stack);
        updateTint(stack);
    }

    /** Empties the vessel (e.g. a fully inverted test tube spills its contents). */
    public static void clearContents(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove("chem_contents");
        tag.remove("radio_nuclides");tag.remove("radio_tick");
        tag.remove("eq_cobalt_chloride");
        tag.remove("batch_completed");
        tag.remove("chem_suspension");
        tag.remove(com.example.chemistry.garden.ChemicalGarden.KEY);
        tag.remove("chem_garden_broken");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        com.example.chemistry.VesselGasPhase.normalize(stack);
        updateTint(stack);
    }

    /**
     * 左手反应容器 + 右手空手：倒出全部物质。腐蚀性液体会造成伤害；
     * 固体以物品形式归还（单一固体给散装物品并标注克数，多种固体给混合物）。
     */
    public static void pourOutAll(Player player, ItemStack vessel) {
        if (player == null || vessel.isEmpty() || !(vessel.getItem() instanceof LabVesselItem)) {
            return;
        }
        List<Entry> contents = getContents(vessel);
        if (contents.isEmpty()) {
            return;
        }
        if(player.level() instanceof net.minecraft.server.level.ServerLevel server&&!com.example.chemistry.radiation.RadioLedger.carriers(vessel).isEmpty()){
            var spill=new ItemStack(ModItems.RADIOACTIVE_WASTE_BOTTLE.get());
            for(var e:contents)if(e.type().equals("liquid")&&com.example.chemistry.radiation.RadioLedger.root(e.id())!=null)addMass(spill,e.type(),e.id(),e.amount());
            com.example.chemistry.radiation.RadioLedger.inherit(vessel,ItemStack.EMPTY,spill);
            com.example.chemistry.radiation.RadiationContamination.spill(server,player.blockPosition(),spill);
        }
        // 腐蚀性液体伤害
        float dmg = 0.0F;
        for (Entry e : contents) {
            if (!e.type().equals("liquid")) {
                continue;
            }
            ChemicalInfoProvider.ChemicalInfo info =
                    ChemicalInfoProvider.forItem("liquid_" + e.id());
            String corr = info == null ? "none" : info.corrosiveness();
            if ("strong".equals(corr)) {
                dmg = Math.max(dmg, 8.0F);
            } else if ("moderate".equals(corr)) {
                dmg = Math.max(dmg, 4.0F);
            }
        }
        if (dmg > 0.0F) {
            player.hurt(player.damageSources().generic(), dmg);
        }
        // 固体 → 物品形式
        List<Entry> solids = contents.stream().filter(e -> e.type().equals("solid")).toList();
        if (!solids.isEmpty()) {
            ItemStack dump;
            if (solids.size() == 1) {
                Entry only = solids.get(0);
                dump = new ItemStack(ModItems.looseSolid(only.id()));
                CompoundTag tag = dump.getOrDefault(
                        DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                tag.putDouble("chem_grams", only.amount());
                dump.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            } else {
                dump = new ItemStack(ModItems.SOLID_MIXTURE.get());
                CompoundTag tag = new CompoundTag();
                ListTag list = new ListTag();
                for (Entry e : solids) {
                    CompoundTag c = new CompoundTag();
                    c.putString("type", e.type());
                    c.putString("id", e.id());
                    c.putDouble("amount", e.amount());
                    list.add(c);
                }
                tag.put("chem_contents", list);
                dump.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
            com.example.chemistry.radiation.RadioLedger.inherit(vessel,ItemStack.EMPTY,dump);
            if (!player.getInventory().add(dump)) {
                player.drop(dump, false);
            }
        }
        clearContents(vessel);
        player.displayClientMessage(
                Component.translatable("mchemistry.vessel.poured_out"), true);
    }

    private static boolean add(ItemStack stack, String type, String id, double amount) {
        return add(stack, type, id, amount, true);
    }

    private static boolean add(ItemStack stack, String type, String id, double amount, boolean checkCapacity) {
        if (checkCapacity && !canAdd(stack, type, id, amount)) {
            return false;
        }
        ListTag list = new ListTag();
        boolean merged = false;
        for (Entry e : getContents(stack)) {
            CompoundTag c = new CompoundTag();
            c.putString("type", e.type());
            c.putString("id", e.id());
            if (e.type().equals(type) && e.id().equals(id)) {
                c.putDouble("amount", e.amount() + amount);
                merged = true;
            } else {
                c.putDouble("amount", e.amount());
            }
            list.add(c);
        }
        if (!merged) {
            CompoundTag c = new CompoundTag();
            c.putString("type", type);
            c.putString("id", id);
            c.putDouble("amount", amount);
            list.add(c);
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(com.example.chemistry.ReactionEngine.KEY_EQUILIBRIUM);
        tag.put("chem_contents", list);
        if (type.equals("solid") && amount > 0
                && getContents(stack).stream().anyMatch(e -> e.type().equals("liquid"))) {
            tag.putDouble("chem_suspension", 1.0);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        com.example.chemistry.VesselGasPhase.normalize(stack);
        updateTint(stack);
        return true;
    }

    public static void updateTint(ItemStack stack) {
        List<Entry> contents = getContents(stack);
        if (contents.isEmpty()) {
            if(stack.getItem() instanceof com.example.chemistry.organic.SeparatoryFunnelItem){
                boolean capped=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("separatory_capped",true);
                stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of(),List.of(),List.of(capped?"empty":"uncapped_empty"),List.of()));return;
            }
            if(com.example.chemistry.organic.OrganicApparatus.covered(stack))stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of(),List.of(false,true),List.of(),List.of()));
            else stack.remove(DataComponents.CUSTOM_MODEL_DATA);
            return;
        }
        Entry last = contents.get(contents.size() - 1);
        int color = 0xFFFFFF;
        if (last.type().equals("liquid")) {
            color = Liquids.ALL.stream().filter(l -> l.id().equals(last.id()))
                    .map(Liquids.Liquid::color).findFirst().orElse(0xFFFFFF);
        } else {
            color = Solids.ALL.stream().filter(s -> s.id().equals(last.id()))
                    .map(Solids.Solid::color).findFirst().orElse(0xFFFFFF);
        }
        color = com.example.chemistry.solution.EdtaEquilibrium.color(stack, com.example.chemistry.solution.CoordinationEquilibrium.liquidColor(stack, color));
        color = com.example.chemistry.solution.FutureChemistry.color(stack,com.example.chemistry.solution.BatchChemistry.color(stack,color));
        var liquidState=com.example.chemistry.organic.LiquidPhases.read(stack);
        if(liquidState.modelled()&&liquidState.layers().size()==1&&getContents(stack).stream().anyMatch(e->com.example.chemistry.organic.Extraction.iodine(e.id())))color=liquidState.layers().getFirst().color();
        String modelState = "filled";
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).getPath();
        if (itemId.equals("suction_flask") || itemId.equals("galvanic_half_cell") || itemId.equals("deep_water_trough") || itemId.equals("separatory_funnel") || itemId.startsWith("burette_") || itemId.startsWith("test_tube_") || itemId.startsWith("beaker_") || itemId.equals("erlenmeyer_flask") || itemId.equals("ground_glass_erlenmeyer") || itemId.equals("three_neck_flask") || itemId.equals("round_bottom_flask") || itemId.equals("ground_glass_flask")) {
            double fill = usedVolume(stack)
                    / Math.max(1.0, ((LabVesselItem) stack.getItem()).capacity());
            int step = Math.max(1, Math.min(20, (int) Math.ceil(fill * 20.0)));
            modelState = String.format(java.util.Locale.ROOT, "filled_%03d", step * 5);
        }
        var phases=com.example.chemistry.organic.LiquidPhases.read(stack);
        if(phases.separated()&&(itemId.equals("separatory_funnel")||itemId.startsWith("beaker_")||itemId.equals("erlenmeyer_flask")||itemId.equals("ground_glass_erlenmeyer"))){
            double capacity=((LabVesselItem)stack.getItem()).capacity();
            int total=Math.max(2,Math.min(20,(int)Math.ceil(usedVolume(stack)/capacity*20)));
            int lower=Math.max(1,Math.min(total-1,(int)Math.ceil(phases.bottom().ml()/capacity*20)));
            stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of(),List.of(com.example.chemistry.garden.ChemicalGarden.stems(stack).stream().anyMatch(v->v.segments()>0),com.example.chemistry.organic.OrganicApparatus.covered(stack)),
                    List.of((itemId.equals("separatory_funnel")&&!stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("separatory_capped",true)?"uncapped_":"")+String.format(java.util.Locale.ROOT,"layers_%02d_%02d",total,lower)),
                    List.of(phases.bottom().color(),phases.top().color())));return;
        }
        if(itemId.equals("separatory_funnel")&&!stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("separatory_capped",true))modelState="uncapped_"+modelState;
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(com.example.chemistry.garden.ChemicalGarden.stems(stack).stream().anyMatch(v->v.segments()>0),com.example.chemistry.organic.OrganicApparatus.covered(stack)), List.of(modelState), List.of(color)));
    }
}
