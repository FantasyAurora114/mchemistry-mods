package com.example.chemistry;

import com.example.chemistry.block.AlcoholLampBlock;
import com.example.chemistry.block.IronStandBlock;
import com.example.chemistry.registry.ModBlocks;
import com.example.chemistry.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Left-click (empty hand or cap in hand) snuffs a lit alcohol lamp. */
@EventBusSubscriber(modid = ChemistryMod.MODID)
public class AlcoholLampEvents {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getSide().isClient()) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.IRON_STAND.get()) && state.getValue(IronStandBlock.HAS_LAMP)
                && state.getValue(IronStandBlock.LAMP_LIT)) {
            Player player = event.getEntity();
            ItemStack main = player.getMainHandItem();
            if (main.isEmpty() || main.is(ModItems.ALCOHOL_LAMP_CAP.get())) {
                level.setBlock(pos, state.setValue(IronStandBlock.LAMP_LIT, false), 3);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return;
        }
        if (!state.is(ModBlocks.ALCOHOL_LAMP.get()) || !state.getValue(AlcoholLampBlock.LIT)) {
            return;
        }
        Player player = event.getEntity();
        ItemStack main = player.getMainHandItem();
        if (main.isEmpty() || main.is(ModItems.ALCOHOL_LAMP_CAP.get())) {
            level.setBlock(pos, state.setValue(AlcoholLampBlock.LIT, false), 3);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
