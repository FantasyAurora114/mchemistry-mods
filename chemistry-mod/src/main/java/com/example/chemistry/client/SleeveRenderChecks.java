package com.example.chemistry.client;

import com.example.chemistry.ChemistryMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in resource check against the real baked client model. */
@EventBusSubscriber(modid=ChemistryMod.MODID,value=Dist.CLIENT)
public final class SleeveRenderChecks {
    private static boolean completed;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(completed || !Boolean.getBoolean("mchemistry.verifySleeve") || Minecraft.getInstance().getOverlay()!=null)return;
        completed=true;
        var model=ModStandaloneModels.thermometerSleeve();
        if(model==null)throw new IllegalStateException("Missing thermometer adapter model");
        int faces=0;
        for(var part:model.collectParts(EmptyBlockAndTintGetter.INSTANCE,BlockPos.ZERO,Blocks.GLASS.defaultBlockState(),RandomSource.create(1))) {
            var quads=new java.util.ArrayList<>(part.getQuads(null));
            for(Direction direction:Direction.values())quads.addAll(part.getQuads(direction));
            for(var quad:quads) {
                if(!quad.sprite().contents().name().getPath().equals("item/thermometer_sleeve"))
                    throw new IllegalStateException("Incorrect adapter texture: "+quad.sprite().contents().name());
                faces++;
            }
        }
        if(faces!=66)throw new IllegalStateException("Lost authored faces: "+faces);
        ChemistryMod.LOGGER.info("Thermometer sleeve client model passed: {} faces, original texture",faces);
    }
}
