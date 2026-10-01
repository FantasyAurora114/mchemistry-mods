package com.example.chemistry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.example.chemistry.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** One independent 1-in-5 roll after a confirmed vanilla copper scraping action. */
@EventBusSubscriber(modid = ChemistryMod.MODID)
public final class CopperScrapingDrops {
    private record Pending(ServerLevel level, BlockPos pos, BlockState expected,
            BlockEvent.BlockToolModificationEvent event) {}
    private static final List<Pending> PENDING = new ArrayList<>();

    public static boolean isScrape(BlockState before, BlockState after) {
        return after != null && before.getBlock() instanceof WeatheringCopper
                && BuiltInRegistries.BLOCK.getKey(before.getBlock()).getNamespace().equals("minecraft")
                && WeatheringCopper.getPrevious(before).filter(after::equals).isPresent();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTool(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || event.isCanceled() || event.getPlayer() == null
                || event.getItemAbility() != ItemAbilities.AXE_SCRAPE
                || !(event.getLevel() instanceof ServerLevel level)
                || !event.getHeldItemStack().canPerformAction(ItemAbilities.AXE_SCRAPE)) return;
        BlockState expected = event.getFinalState() == event.getState()
                ? WeatheringCopper.getPrevious(event.getState()).orElse(null) : event.getFinalState();
        if (!isScrape(event.getState(), expected)) return;
        // The event is a proposal. Vanilla applies the state only after it returns.
        PENDING.add(new Pending(level, event.getPos().immutable(), expected, event));
    }

    @SubscribeEvent
    public static void afterTick(ServerTickEvent.Post tick) {
        Iterator<Pending> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            if (pending.level().getServer() != tick.getServer()) continue;
            iterator.remove();
            if (!pending.event().isCanceled() && pending.level().hasChunkAt(pending.pos())
                    && pending.level().getBlockState(pending.pos()).equals(pending.expected())
                    && pending.level().random.nextInt(5) == 0) {
                Block.popResource(pending.level(), pending.pos(),
                        new ItemStack(ModItems.looseSolid("basic_copper_carbonate")));
            }
        }
    }
}
