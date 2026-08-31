package com.example.chemistry.item;

import com.example.chemistry.PurityHelper;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.block.WaterTroughBlock;
import com.example.chemistry.blockentity.GasCollectingBottleBlockEntity;
import com.example.chemistry.data.GasJars;
import com.example.chemistry.registry.ModBlocks;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.storage.ChemUnits;
import com.example.chemistry.transfer.BottleCodes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Unified gas collecting bottle (集气瓶). The gas (single or mixture), sealed
 * state and water-fill live in CUSTOM_DATA. Right-click in air opens the bottle;
 * right-click on a block places it (or, against water, fills it with water).
 */
public class GasBottleItem extends Item {

    public GasBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        net.minecraft.network.chat.Component name = BottleCodes.displayName(stack);
        return name != null ? name : super.getName(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (BottleCodes.isWater(held) || !BottleCodes.isSealed(held)) {
            return InteractionResult.PASS;
        }
        String gasId = BottleCodes.gasIdOf(held);
        // NO oxidises to NO2 the moment the bottle is opened.
        if ("nitric_oxide".equals(gasId)) {
            BottleCodes.setGas(held, "nitrogen_dioxide", false);
        } else {
            BottleCodes.setSealed(held, false);
        }
        BottleCodes.refreshModel(held);
        if (!player.getInventory().add(new ItemStack(ModItems.GLASS_SHEET.get()))) {
            player.drop(new ItemStack(ModItems.GLASS_SHEET.get()), false);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var clicked = level.getBlockState(pos);
        var adj = level.getBlockState(pos.relative(context.getClickedFace()));
        boolean water = clicked.getFluidState().is(Fluids.WATER)
                || clicked.getFluidState().is(Fluids.FLOWING_WATER)
                || adj.getFluidState().is(Fluids.WATER)
                || adj.getFluidState().is(Fluids.FLOWING_WATER);
        boolean trough = clicked.is(ModBlocks.WATER_TROUGH.get())
                && clicked.getValue(WaterTroughBlock.FILLED) == WaterTroughBlock.Fill.WATER;
        // Empty bottle + water -> water-filled bottle (排水法).
        if ((water || trough) && BottleCodes.isEmpty(context.getItemInHand())) {
            if (!level.isClientSide()) {
                ItemStack held = context.getItemInHand();
                BottleCodes.setWater(held, true);
                BottleCodes.setSealed(held, true);
                BottleCodes.refreshModel(held);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return place(context, BottleCodes.gasIdOf(context.getItemInHand()), BottleCodes.isSealed(context.getItemInHand()));
    }

    /** Shared placement for gas bottles (empty / water / gas, sealed or open). */
    static InteractionResult place(UseOnContext context, String gasId, boolean hasPlate) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        BlockState target = level.getBlockState(pos);
        if (!target.isAir() && !target.canBeReplaced()) {
            if (!level.getBlockState(context.getClickedPos()).canBeReplaced()) {
                return InteractionResult.PASS;
            }
            pos = context.getClickedPos();
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        level.setBlock(pos, ModBlocks.GAS_COLLECTING_BOTTLE.get().defaultBlockState()
                .setValue(GasCollectingBottleBlock.HAS_PLATE, hasPlate)
                .setValue(GasCollectingBottleBlock.INVERTED, gasIsLighter(gasId)), 3);
        if (level.getBlockEntity(pos) instanceof GasCollectingBottleBlockEntity be) {
            ItemStack held = context.getItemInHand();
            be.setGasId(gasId == null ? "" : gasId);
            if (gasId != null && !gasId.isEmpty()) {
                be.setFill(BottleCodes.volumeOf(held), PurityHelper.getPurity(held));
            }
            be.readFromItem(held);
            CompoundTag tag = held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            String label = tag.getStringOr(LabelItem.KEY_LABEL, "");
            if (!label.isEmpty()) {
                be.setLabelName(label);
            }
        }
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    private static boolean gasIsLighter(String gasId) {
        if (gasId == null) {
            return false;
        }
        for (GasJars.GasJar gas : GasJars.ALL) {
            if (gas.id().equals(gasId)) {
                return gas.lighter();
            }
        }
        return false;
    }
}
