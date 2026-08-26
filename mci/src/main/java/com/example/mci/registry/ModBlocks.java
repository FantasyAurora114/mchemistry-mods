package com.example.mci.registry;

import java.util.ArrayList;
import java.util.List;

import com.example.mci.block.SynthesisTowerBlock;
import com.example.mci.data.Ores;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * MCI blocks: ore blocks (stone + deepslate) and the industrial synthesis
 * tower. Resource namespace is "mchemistry" (assets live in assets/mchemistry),
 * even though the mod id is mci.
 */
public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("mchemistry");

    public static final DeferredBlock<SynthesisTowerBlock> SYNTHESIS_TOWER =
            BLOCKS.registerBlock("synthesis_tower", SynthesisTowerBlock::new,
                    props -> props.mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.METAL));

    public static final List<DeferredBlock<Block>> ORE_BLOCKS = new ArrayList<>();
    public static final List<DeferredBlock<Block>> DEEPSLATE_ORE_BLOCKS = new ArrayList<>();

    static {
        for (Ores.OreDef ore : Ores.ALL) {
            String id = "ore_" + ore.id();
            ORE_BLOCKS.add(BLOCKS.registerBlock(id, Block::new,
                    props -> props.mapColor(MapColor.STONE)
                            .strength(ore.hardness(), 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));
            DEEPSLATE_ORE_BLOCKS.add(BLOCKS.registerBlock("deepslate_" + id, Block::new,
                    props -> props.mapColor(MapColor.DEEPSLATE)
                            .strength(ore.hardness() + 0.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.DEEPSLATE)));
        }
    }

    private ModBlocks() {
    }
}
