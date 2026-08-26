package com.example.chemistry.item;

import java.util.function.Supplier;

import com.example.chemistry.PurityHelper;
import com.example.chemistry.ChemistryMod;
import com.example.chemistry.block.GasCollectingBottleBlock;
import com.example.chemistry.blockentity.GasCollectingBottleBlockEntity;
import com.example.chemistry.data.GasJars;
import com.example.chemistry.registry.ModBlocks;
import com.example.chemistry.storage.ChemUnits;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;

/**
 * A gas collecting bottle that can be placed on a block (right-click or
 * sneak + right-click). Right-clicking in the air still opens the bottle.
 */
public class GasBottleItem extends GasCollectingBottleItem {

    private final String gasId;

    public GasBottleItem(net.minecraft.world.item.Item.Properties properties, String gasId,
            Supplier<Item> openVariant, Supplier<Item> glassSheet,
            Supplier<Item> immediateOpenProduct) {
        super(properties, openVariant, glassSheet, immediateOpenProduct);
        this.gasId = gasId;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return place(context, gasId, true);
    }

    /**
     * Shared placement for sealed gas bottles and open gas bottles. Sealed
     * bottles keep their glass plate; bottles of gases lighter than air are
     * placed upside down (mouth down). Plain right-click never places on this
     * mod's own machinery; sneak + right-click forces placement anywhere.
     */
    static InteractionResult place(UseOnContext context, String gasId, boolean hasPlate) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            ResourceLocation clicked = BuiltInRegistries.BLOCK.getKey(
                    context.getLevel().getBlockState(context.getClickedPos()).getBlock());
            if (clicked != null && ChemistryMod.MODID.equals(clicked.getNamespace())) {
                return InteractionResult.FAIL;
            }
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
        // A sealed bottle keeps its glass plate; lighter-than-air gases are
        // collected/stored upside down (mouth down).
        level.setBlock(pos, ModBlocks.GAS_COLLECTING_BOTTLE.get().defaultBlockState()
                .setValue(GasCollectingBottleBlock.HAS_PLATE, hasPlate)
                .setValue(GasCollectingBottleBlock.INVERTED, gasIsLighter(gasId)), 3);
        if (level.getBlockEntity(pos) instanceof GasCollectingBottleBlockEntity be) {
            be.setGasId(gasId);
            if (!gasId.isEmpty()) {
                be.setFill(ChemUnits.GAS_JAR_VOLUME, PurityHelper.getPurity(context.getItemInHand()));
            }
        }
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    private static boolean gasIsLighter(String gasId) {
        for (GasJars.GasJar gas : GasJars.ALL) {
            if (gas.id().equals(gasId)) {
                return gas.lighter();
            }
        }
        return false;
    }
}
