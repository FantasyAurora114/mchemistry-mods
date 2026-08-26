/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.example.chemistry.PurityHelper
 *  com.example.chemistry.api.ChemistryAPI
 *  com.example.chemistry.api.ReactionUnlocks
 *  com.example.chemistry.api.goggles.ChemGoggleLines
 *  com.example.chemistry.api.goggles.IChemGoggleInfo
 *  com.example.chemistry.data.Reactions$Ingredient
 *  com.example.chemistry.data.Reactions$Product
 *  com.example.chemistry.data.Reactions$Reaction
 *  com.example.chemistry.data.SubstanceVariants
 *  com.example.mci.registry.ModBlockEntities
 *  com.example.chemistry.registry.ModFluids
 *  com.example.chemistry.registry.ModItems
 *  com.example.chemistry.registry.SimpleFluid
 *  com.example.chemistry.transfer.BottleCodes
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.NonNullList
 *  net.minecraft.core.Position
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.ContainerHelper
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.ContainerUser
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.material.Fluid
 *  net.minecraft.world.level.storage.ValueInput
 *  net.minecraft.world.level.storage.ValueOutput
 *  net.neoforged.neoforge.common.extensions.IMenuProviderExtension
 *  net.neoforged.neoforge.fluids.FluidStack
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 */
package com.example.mci.blockentity;

import com.example.chemistry.PurityHelper;
import com.example.chemistry.api.ChemistryAPI;
import com.example.chemistry.api.ReactionUnlocks;
import com.example.chemistry.api.goggles.ChemGoggleLines;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.data.SubstanceVariants;
import com.example.mci.menu.SynthesisTowerMenu;
import com.example.mci.network.ChemistryNetworking;
import com.example.mci.registry.ModBlockEntities;
import com.example.chemistry.registry.ModFluids;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.registry.SimpleFluid;
import com.example.mci.storage.ChemGasTank;
import com.example.mci.storage.TowerFluidTank;
import com.example.chemistry.transfer.BottleCodes;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Position;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.extensions.IMenuProviderExtension;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class SynthesisTowerBlockEntity
extends BlockEntity
implements MenuProvider,
IMenuProviderExtension,
Container,
IChemGoggleInfo {
    public static final int INPUT_A = 0;
    public static final int INPUT_B = 1;
    public static final int INPUT_C = 2;
    public static final int INPUT_D = 3;
    public static final int CATALYST = 4;
    public static final int TANK_IN_GAS = 5;
    public static final int TANK_IN_LIQUID = 6;
    public static final int TANK_OUT_GAS = 7;
    public static final int TANK_OUT_LIQUID = 8;
    public static final int SLOT_COUNT = 9;
    public static final int INPUT_SLOT_COUNT = 4;
    public static final int TYPE_GAS = 0;
    public static final int TYPE_LIQUID = 1;
    public static final int TYPE_SOLID = 2;
    public static final double MIN_TEMPERATURE = -273.0;
    public static final double MAX_TEMPERATURE = 2000.0;
    public static final double MIN_PRESSURE = 1.0;
    public static final double MAX_PRESSURE = 100000.0;
    private static final String KEY_TYPES = "chem_input_types";
    private static final String KEY_TEMP = "chem_temperature";
    private static final String KEY_PRESSURE = "chem_pressure";
    private final SimpleContainer inventory = new SimpleContainer(9);
    private int[] inputTypes = new int[]{0, 1, 2, 2};
    private double temperature = 25.0;
    private double pressure = 101.0;
    private final ChemGasTank gasTank = new ChemGasTank(64000L);
    private final TowerFluidTank fluidTank = new TowerFluidTank(this, 64000);
    private long syncVersion = 0L;
    private boolean hasClientSync = false;
    private String syncedGasId = "";
    private long syncedGasAmount = 0L;
    private double syncedGasPurity = 1.0;
    private String syncedFluidId = "";
    private int syncedFluidAmount = 0;
    private double syncedFluidPurity = 1.0;
    private double syncedTemperature = 25.0;
    private double syncedPressure = 101.0;

    public SynthesisTowerBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlockEntities.SYNTHESIS_TOWER.get(), pos, state);
        this.inventory.addListener(container -> this.setChanged());
    }

    public ChemGasTank getGasTank() {
        return this.gasTank;
    }

    public TowerFluidTank getFluidTank() {
        return this.fluidTank;
    }

    public long getSyncVersion() {
        return this.syncVersion;
    }

    public double getTemperature() {
        return this.temperature;
    }

    public double getPressure() {
        return this.pressure;
    }

    public void setTemperature(double value) {
        this.temperature = Math.max(-273.0, Math.min(2000.0, value));
        this.setChanged();
    }

    public void setPressure(double value) {
        this.pressure = Math.max(1.0, Math.min(100000.0, value));
        this.setChanged();
    }

    public void addTemperature(double delta) {
        this.setTemperature(this.temperature + delta);
    }

    public void addPressure(double delta) {
        this.setPressure(this.pressure + delta);
    }

    public void tickServer() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        this.bufferInputs();
        this.tankInputs();
        this.fillBottleFromGasTank(7);
        this.fillBottleFromFluidTank(8);
    }

    public boolean addGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add((Component)Component.literal((String)"\u5de5\u4e1a\u5408\u6210\u5854"));
        tooltip.add((Component)Component.literal((String)("\u6e29\u5ea6\uff1a" + String.format("%.0f", this.temperature) + "\u00b0C")));
        tooltip.add((Component)Component.literal((String)("\u538b\u529b\uff1a" + String.format("%.0f", this.pressure) + "kPa")));
        if (this.gasTank.isEmpty()) {
            tooltip.add((Component)Component.literal((String)"\u6c14\u4f53\u7f50\uff1a\u7a7a"));
        } else {
            tooltip.add((Component)Component.literal((String)("\u6c14\u4f53\u7f50\uff1a" + ChemGoggleLines.gasName((String)this.gasTank.getGasId()) + " " + this.gasTank.getAmount() + "/64000mB")));
            tooltip.add((Component)Component.literal((String)("\u6c14\u4f53\u7eaf\u5ea6\uff1a" + String.format("%.2f%%", this.gasTank.getPurity() * 100.0))));
        }
        if (this.fluidTank.isEmpty()) {
            tooltip.add((Component)Component.literal((String)"\u6db2\u4f53\u7f50\uff1a\u7a7a"));
        } else {
            String liquidId = ModFluids.liquidIdFor((Fluid)this.fluidTank.getFluid());
            String name = liquidId != null ? ChemGoggleLines.liquidName((String)liquidId) : "?";
            tooltip.add((Component)Component.literal((String)("\u6db2\u4f53\u7f50\uff1a" + name + " " + this.fluidTank.getAmount() + "/64000mB")));
        }
        StringBuilder config = new StringBuilder("\u8f93\u5165\u69fd\uff1a");
        for (int i = 0; i < 4; ++i) {
            if (i > 0) {
                config.append("/");
            }
            config.append(switch (this.inputTypes[i]) {
                case 0 -> "\u6c14";
                case 1 -> "\u6db2";
                default -> "\u56fa";
            });
        }
        tooltip.add((Component)Component.literal((String)config.toString()));
        ItemStack catalyst = this.getItem(4);
        tooltip.add((Component)Component.literal((String)(catalyst.isEmpty() ? "\u50ac\u5316\u5242\uff1a\u65e0" : "\u50ac\u5316\u5242\uff1a" + catalyst.getHoverName().getString())));
        return true;
    }

    private void tankInputs() {
        this.drainToGasTank(5);
        this.drainToLiquidTank(6);
    }

    private void drainToGasTank(int slot) {
        ItemStack stack = this.getItem(slot);
        if (stack.isEmpty()) {
            return;
        }
        String gasId = BottleCodes.gasIdOfAny((ItemStack)stack);
        if (gasId == null || this.gasTank.insert(gasId, 1L, true) <= 0L) {
            return;
        }
        long accepted = this.gasTank.addWithPurity(gasId, 250L, PurityHelper.getPurity((ItemStack)stack));
        if (accepted >= 250L) {
            this.setItem(slot, new ItemStack(ModItems.EMPTY_GAS_JAR.get()));
        } else if (accepted > 0L) {
            this.setChanged();
        }
    }

    private void drainToLiquidTank(int slot) {
        ItemStack stack = this.getItem(slot);
        if (stack.isEmpty()) {
            return;
        }
        String liquidId = BottleCodes.liquidIdOf((ItemStack)stack);
        if (liquidId == null && BottleCodes.bucketIdOf((ItemStack)stack) != null) {
            liquidId = BottleCodes.bucketIdOf((ItemStack)stack);
        }
        if (liquidId == null) {
            return;
        }
        SimpleFluid fluid = ModFluids.liquidFluid((String)liquidId);
        if (fluid == null) {
            return;
        }
        int volume = BottleCodes.liquidVolumeOf((ItemStack)stack);
        if (volume <= 0 || !this.fluidTank.canAccept((Fluid)fluid, volume)) {
            return;
        }
        int accepted = this.fluidTank.addWithPurity(new FluidStack((Fluid)fluid, volume), PurityHelper.getPurity((ItemStack)stack));
        if (accepted >= volume) {
            String path = BottleCodes.pathOf((ItemStack)stack);
            Item empty = path.startsWith("dropper_bottle_") ? (Item)ModItems.EMPTY_DROPPER_BOTTLE.get() : (path.endsWith("_bucket") || path.equals("water_bucket") ? Items.BUCKET : (Item)ModItems.EMPTY_NARROW_BOTTLE.get());
            this.setItem(slot, new ItemStack(empty));
        } else if (accepted > 0) {
            this.setChanged();
        }
    }

    private void bufferInputs() {
        for (int i = 0; i < 4; ++i) {
            String liquidId;
            ItemStack stack = this.getItem(i);
            if (stack.isEmpty()) continue;
            if (this.inputTypes[i] == 0) {
                String gasId = BottleCodes.gasIdOf((ItemStack)stack);
                if (gasId == null || this.gasTank.insert(gasId, 1L, true) <= 0L) continue;
                long accepted = this.gasTank.addWithPurity(gasId, 250L, PurityHelper.getPurity((ItemStack)stack));
                if (accepted >= 250L) {
                    this.setItem(i, new ItemStack(ModItems.EMPTY_GAS_JAR.get()));
                    continue;
                }
                if (accepted <= 0L) continue;
                this.setChanged();
                continue;
            }
            if (this.inputTypes[i] != 1 || (liquidId = BottleCodes.liquidIdOf((ItemStack)stack)) == null) continue;
            SimpleFluid fluid = ModFluids.liquidFluid((String)liquidId);
            int volume = BottleCodes.liquidVolumeOf((ItemStack)stack);
            if (fluid == null || volume <= 0 || !this.fluidTank.canAccept((Fluid)fluid, 1)) continue;
            int accepted = this.fluidTank.addWithPurity(new FluidStack((Fluid)fluid, volume), PurityHelper.getPurity((ItemStack)stack));
            if (accepted >= volume) {
                String path = BottleCodes.pathOf((ItemStack)stack);
                this.setItem(i, new ItemStack(path.startsWith("dropper_bottle_") ? ModItems.EMPTY_DROPPER_BOTTLE.get() : ModItems.EMPTY_NARROW_BOTTLE.get()));
                continue;
            }
            if (accepted <= 0) continue;
            this.setChanged();
        }
    }

    private void fillBottleFromGasTank(int slot) {
        ItemStack stack = this.getItem(slot);
        if (!BottleCodes.isEmptyGasJar((ItemStack)stack) || this.gasTank.isEmpty()) {
            return;
        }
        String gasId = this.gasTank.getGasId();
        long extracted = this.gasTank.extract(250L, false);
        if (extracted >= 250L) {
            ItemStack jar = new ItemStack(SynthesisTowerBlockEntity.openGasJarItem(gasId));
            PurityHelper.setPurity((ItemStack)jar, (double)this.gasTank.getPurity());
            this.setItem(slot, jar);
        } else if (extracted > 0L) {
            this.gasTank.addWithPurity(gasId, extracted, this.gasTank.getPurity());
            this.setChanged();
        }
    }

    private void fillBottleFromFluidTank(int slot) {
        ItemStack stack = this.getItem(slot);
        if (!BottleCodes.isEmptyLiquidBottle((ItemStack)stack) || this.fluidTank.isEmpty()) {
            return;
        }
        String path = BottleCodes.pathOf((ItemStack)stack);
        int volume = BottleCodes.isEmptyBucket((ItemStack)stack) ? 1000 : (path.equals("empty_dropper_bottle") ? 100 : 250);
        if (this.fluidTank.getAmount() < volume) {
            return;
        }
        Fluid fluid = this.fluidTank.getFluid();
        FluidStack taken = this.fluidTank.take(fluid, volume);
        if (taken.getAmount() >= volume) {
            String liquidId = ModFluids.liquidIdFor((Fluid)fluid);
            Item item = Items.AIR;
            if (liquidId != null) {
                item = BottleCodes.isEmptyBucket((ItemStack)stack) ? ModItems.liquidBucket((String)liquidId) : (path.equals("empty_dropper_bottle") ? ModItems.dropperBottle((String)liquidId) : ModItems.openLiquidItem((String)liquidId));
            }
            ItemStack bottle = new ItemStack(item);
            PurityHelper.setPurity((ItemStack)bottle, (double)this.fluidTank.getPurity());
            this.setItem(slot, bottle);
        } else if (!taken.isEmpty()) {
            this.fluidTank.addWithPurity(taken, this.fluidTank.getPurity());
            this.setChanged();
        }
    }

    public int inputType(int index) {
        return index >= 0 && index < 4 ? this.inputTypes[index] : 2;
    }

    public void cycleInputType(int index) {
        if (index >= 0 && index < 4) {
            this.inputTypes[index] = (this.inputTypes[index] + 1) % 3;
            this.setChanged();
        }
    }

    public void setChanged() {
        super.setChanged();
        ++this.syncVersion;
    }

    public Component getDisplayName() {
        return Component.translatable((String)"container.mchemistry.synthesis_tower");
    }

    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SynthesisTowerMenu(containerId, playerInventory, this);
    }

    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.getBlockPos());
    }

    public void startOpen(ContainerUser user) {
        if (this.level != null && !this.level.isClientSide() && user instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)user;
            String fluidId = ModFluids.liquidIdFor((Fluid)this.fluidTank.getFluid());
            PacketDistributor.sendToPlayer((ServerPlayer)serverPlayer, (CustomPacketPayload)new ChemistryNetworking.SynthesisTowerSyncPacket(this.getBlockPos(), this.gasTank.getGasId(), this.gasTank.getAmount(), this.gasTank.getPurity(), fluidId == null ? "" : fluidId, this.fluidTank.getAmount(), this.fluidTank.getPurity(), this.temperature, this.pressure), (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
    }

    public boolean run(Player player) {
        if (this.level == null || this.level.isClientSide()) {
            return false;
        }
        for (Reactions.Reaction reaction : ChemistryAPI.allReactions()) {
            if (!ReactionUnlocks.isUnlocked((Player)player, (String)reaction.display()) || !this.tryRun(reaction)) continue;
            return true;
        }
        if (player != null) {
            player.displayClientMessage((Component)Component.translatable((String)"mchemistry.tower.no_unlocked_reaction"), true);
        }
        return false;
    }

    private boolean tryRun(Reactions.Reaction reaction) {
        double purity;
        if ((double)reaction.requiredTemp() > this.temperature || (double)reaction.requiredPressure() > this.pressure) {
            return false;
        }
        int n = reaction.reactants().size();
        if (n == 0 || n > 3) {
            return false;
        }
        Input[] inputs = new Input[4];
        for (int i = 0; i < 4; ++i) {
            inputs[i] = SynthesisTowerBlockEntity.inputOf(this.getItem(i), this.inputTypes[i]);
        }
        int[] inputIndex = new int[n];
        boolean[] fromTank = new boolean[n];
        double puritySum = 0.0;
        for (int r = 0; r < n; ++r) {
            Reactions.Ingredient ing = (Reactions.Ingredient)reaction.reactants().get(r);
            int found = -1;
            for (int i = 0; i < 4; ++i) {
                if (inputs[i] == null || !inputs[i].type().equals(ing.type()) || !SubstanceVariants.canonicalOf((String)inputs[i].id()).equals(ing.id())) continue;
                found = i;
                break;
            }
            if (found < 0) {
                SimpleFluid fluid;
                if (ing.type().equals("gas") && this.gasTank.isGas(ing.id()) && this.gasTank.getAmount() >= 250L) {
                    inputIndex[r] = -1;
                    fromTank[r] = true;
                    puritySum += this.gasTank.getPurity();
                    continue;
                }
                if (ing.type().equals("liquid") && (fluid = ModFluids.liquidFluid((String)ing.id())) != null && this.fluidTank.getFluid() == fluid && this.fluidTank.getAmount() >= 250) {
                    inputIndex[r] = -1;
                    fromTank[r] = true;
                    puritySum += this.fluidTank.getPurity();
                    continue;
                }
                return false;
            }
            inputIndex[r] = found;
            puritySum += inputs[found].purity();
        }
        if (!SynthesisTowerBlockEntity.catalystMatches(this.getItem(4), reaction.catalyst())) {
            return false;
        }
        double d = purity = n > 0 ? puritySum / (double)n : 0.9999999;
        if (!this.canPlaceGasAndLiquidProducts(reaction)) {
            return false;
        }
        HashSet<Integer> consumedSlots = new HashSet<Integer>();
        for (int r = 0; r < n; ++r) {
            if (fromTank[r]) continue;
            consumedSlots.add(inputIndex[r]);
        }
        int solidProducts = 0;
        for (Reactions.Product product : reaction.products()) {
            if (!product.type().equals("solid")) continue;
            ++solidProducts;
        }
        int availableSolidSlots = this.countEmptyInputSlots() + consumedSlots.size();
        if (solidProducts > availableSolidSlots) {
            return false;
        }
        for (int r = 0; r < n; ++r) {
            if (fromTank[r]) {
                Reactions.Ingredient ing = (Reactions.Ingredient)reaction.reactants().get(r);
                if (ing.type().equals("gas")) {
                    this.gasTank.extract(250L, false);
                    continue;
                }
                SimpleFluid fluid = ModFluids.liquidFluid((String)ing.id());
                if (fluid == null) continue;
                this.fluidTank.take((Fluid)fluid, 250);
                continue;
            }
            this.removeItem(inputIndex[r], 1);
        }
        for (Reactions.Product product : reaction.products()) {
            if (product.type().equals("gas")) {
                this.placeGas(product.id(), purity);
                continue;
            }
            if (product.type().equals("liquid")) {
                this.placeLiquid(product.id(), purity);
                continue;
            }
            if (!product.type().equals("solid")) continue;
            this.placeSolid(product.id(), purity);
        }
        this.setChanged();
        return true;
    }

    private boolean canPlaceGasAndLiquidProducts(Reactions.Reaction reaction) {
        for (Reactions.Product product : reaction.products()) {
            SimpleFluid fluid;
            if (!(product.type().equals("gas") ? this.gasTank.insert(product.id(), 250L, true) < 250L : product.type().equals("liquid") && ((fluid = ModFluids.liquidFluid((String)product.id())) == null || !this.fluidTank.canAccept((Fluid)fluid, 250)))) continue;
            return false;
        }
        return true;
    }

    private int countEmptyInputSlots() {
        int count = 0;
        for (int i = 0; i < 4; ++i) {
            if (!this.getItem(i).isEmpty()) continue;
            ++count;
        }
        return count;
    }

    private void placeGas(String gasId, double purity) {
        this.gasTank.addWithPurity(gasId, 250L, purity);
    }

    private void placeLiquid(String liquidId, double purity) {
        SimpleFluid fluid = ModFluids.liquidFluid((String)liquidId);
        if (fluid != null) {
            this.fluidTank.addWithPurity(new FluidStack((Fluid)fluid, 250), purity);
        }
    }

    private void placeSolid(String solidId, double purity) {
        ItemStack loose = new ItemStack(SynthesisTowerBlockEntity.looseItem(solidId));
        PurityHelper.setPurity((ItemStack)loose, (double)purity);
        for (int i = 0; i < 4; ++i) {
            ItemStack current = this.getItem(i);
            if (current.isEmpty()) {
                this.setItem(i, loose);
                return;
            }
            if (!ItemStack.isSameItem((ItemStack)current, (ItemStack)loose) || current.getCount() >= current.getMaxStackSize()) continue;
            current.grow(1);
            return;
        }
    }

    private static Input inputOf(ItemStack stack, int configuredType) {
        if (stack.isEmpty()) {
            return null;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String id = null;
        String type = null;
        if (configuredType == 0 && path.startsWith("gas_collecting_bottle_") && !path.startsWith("open_")) {
            id = path.substring("gas_collecting_bottle_".length());
            type = "gas";
        } else if (configuredType == 1) {
            if (path.startsWith("liquid_")) {
                id = path.substring("liquid_".length());
            } else if (path.startsWith("open_liquid_")) {
                id = path.substring("open_liquid_".length());
            } else if (path.startsWith("dropper_bottle_")) {
                id = path.substring("dropper_bottle_".length());
            }
            type = "liquid";
        } else if (configuredType == 2) {
            if (path.startsWith("solid_")) {
                id = path.substring("solid_".length());
            } else if (path.startsWith("loose_")) {
                id = path.substring("loose_".length());
            }
            type = "solid";
        }
        return id == null ? null : new Input(type, id, PurityHelper.getPurity((ItemStack)stack));
    }

    private static boolean catalystMatches(ItemStack catalyst, String required) {
        boolean any;
        if (required == null || required.isEmpty()) {
            return true;
        }
        if (catalyst.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(catalyst.getItem()).getPath();
        boolean bl = any = path.equals("iron_catalyst") || path.equals("vanadium_pentoxide_catalyst") || path.equals("platinum_rhodium_catalyst") || path.equals("solid_sulfuric_acid_concentrated");
        if (required.equals("any")) {
            return any;
        }
        return switch (required) {
            case "iron" -> path.equals("iron_catalyst");
            case "vanadium" -> path.equals("vanadium_pentoxide_catalyst");
            case "platinum" -> path.equals("platinum_rhodium_catalyst");
            case "acid" -> path.equals("solid_sulfuric_acid_concentrated");
            default -> false;
        };
    }

    private static Item openGasJarItem(String gasId) {
        return (Item)BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath((String)"mchemistry", (String)("open_gas_collecting_bottle_" + gasId)));
    }

    private static Item looseItem(String solidId) {
        return (Item)BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath((String)"mchemistry", (String)("loose_" + solidId)));
    }

    public void applyClientSync(String gasId, long gasAmount, double gasPurity, String fluidId, int fluidAmount, double fluidPurity, double temperature, double pressure) {
        this.hasClientSync = true;
        this.syncedGasId = gasId;
        this.syncedGasAmount = gasAmount;
        this.syncedGasPurity = gasPurity;
        this.syncedFluidId = fluidId;
        this.syncedFluidAmount = fluidAmount;
        this.syncedFluidPurity = fluidPurity;
        this.syncedTemperature = temperature;
        this.syncedPressure = pressure;
    }

    public String syncedGasId() {
        return this.syncedGasId;
    }

    public boolean hasClientSync() {
        return this.hasClientSync;
    }

    public long syncedGasAmount() {
        return this.syncedGasAmount;
    }

    public double syncedGasPurity() {
        return this.syncedGasPurity;
    }

    public String syncedFluidId() {
        return this.syncedFluidId;
    }

    public int syncedFluidAmount() {
        return this.syncedFluidAmount;
    }

    public double syncedFluidPurity() {
        return this.syncedFluidPurity;
    }

    public double syncedTemperature() {
        return this.syncedTemperature;
    }

    public double syncedPressure() {
        return this.syncedPressure;
    }

    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems((ValueOutput)output, (NonNullList)this.inventory.getItems());
        output.putIntArray(KEY_TYPES, this.inputTypes);
        output.putDouble(KEY_TEMP, this.temperature);
        output.putDouble(KEY_PRESSURE, this.pressure);
        this.gasTank.serialize(output, "chem_gas_tank");
        this.fluidTank.serialize(output.child("chem_fluid_tank"));
        output.putDouble("chem_fluid_purity", this.fluidTank.getPurity());
    }

    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems((ValueInput)input, (NonNullList)this.inventory.getItems());
        input.getIntArray(KEY_TYPES).ifPresent(types -> {
            this.inputTypes = types;
            if (this.inputTypes.length < 4) {
                int[] fixed = new int[]{0, 1, 2, 2};
                System.arraycopy(this.inputTypes, 0, fixed, 0, this.inputTypes.length);
                this.inputTypes = fixed;
            }
        });
        this.temperature = Math.max(-273.0, Math.min(2000.0, input.getDoubleOr(KEY_TEMP, 25.0)));
        this.pressure = Math.max(1.0, Math.min(100000.0, input.getDoubleOr(KEY_PRESSURE, 101.0)));
        this.gasTank.deserialize(input, "chem_gas_tank");
        input.child("chem_fluid_tank").ifPresent(arg_0 -> ((TowerFluidTank)this.fluidTank).deserialize(arg_0));
        this.fluidTank.setPurity(input.getDoubleOr("chem_fluid_purity", 1.0));
    }

    public int getContainerSize() {
        return this.inventory.getContainerSize();
    }

    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    public ItemStack getItem(int index) {
        return this.inventory.getItem(index);
    }

    public ItemStack removeItem(int index, int count) {
        return this.inventory.removeItem(index, count);
    }

    public ItemStack removeItemNoUpdate(int index) {
        return this.inventory.removeItemNoUpdate(index);
    }

    public void setItem(int index, ItemStack stack) {
        this.inventory.setItem(index, stack);
    }

    public void clearContent() {
        this.inventory.clearContent();
    }

    public boolean stillValid(Player player) {
        return this.getBlockPos().closerToCenterThan((Position)player.position(), 8.0);
    }

    private record Input(String type, String id, double purity) {
    }
}
