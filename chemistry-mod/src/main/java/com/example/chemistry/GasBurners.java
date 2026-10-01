package com.example.chemistry;

import com.example.chemistry.block.GasApplianceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** All receivers share the burner's finite heat budget for the tick. */
public final class GasBurners {
    public static boolean heat(Level level, BlockPos vesselPos, ItemStack vessel) {
        if(vessel.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBooleanOr("chem_bath_inner",false))return false;
        if (level.isClientSide() || VesselHeating.isTempLocked(vessel)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(vesselPos.offset(-1, -1, -1), vesselPos.offset(1, 0, 1))) {
            var burner = GasApplianceBlock.device(level, pos);
            if (burner != null && burner.kind() == 0 && burner.burning()
                    && pos.getCenter().distanceToSqr(vesselPos.getCenter()) <= 2.05 && burner.heat(vessel)) return true;
        }
        return false;
    }
    private GasBurners() { }
}
