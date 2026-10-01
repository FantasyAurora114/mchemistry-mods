package com.example.chemistry.client;
import com.example.chemistry.ChemistryMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid=ChemistryMod.MODID,value=Dist.CLIENT)
public final class ElectricalRenderChecks {
    private static boolean done;
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){if(done||!Boolean.getBoolean("mchemistry.verifyElectrical")||Minecraft.getInstance().getOverlay()!=null)return;done=true;int count=0;
        for(String id:new String[]{"bench_power_supply","hofmann_voltameter"}){int size=id.equals("bench_power_supply")?ElectricalModelGeometry.BENCH_POWER_SUPPLY.length:ElectricalModelGeometry.HOFMANN_VOLTAMETER.length;
            for(int i=0;i<size;i++){var model=ElectricalModels.get(id+"/part_"+i);if(model==null)throw new IllegalStateException("Missing "+id+i);int faces=0;
                for(var part:model.collectParts(EmptyBlockAndTintGetter.INSTANCE,BlockPos.ZERO,Blocks.GLASS.defaultBlockState(),RandomSource.create(1))){var quads=new java.util.ArrayList<>(part.getQuads(null));for(Direction direction:Direction.values())quads.addAll(part.getQuads(direction));for(var q:quads){if(!q.sprite().contents().name().getPath().equals("item/"+id))throw new IllegalStateException("Incorrect texture "+id);faces++;}}
                if(faces==0)throw new IllegalStateException("Empty part "+id+i);count++;
            }
        }
        var unit=ElectricalModels.get("electrical_unit");if(unit==null)throw new IllegalStateException("Missing tinted unit");
        int tintedFaces=0;
        for(var part:unit.collectParts(EmptyBlockAndTintGetter.INSTANCE,BlockPos.ZERO,Blocks.GLASS.defaultBlockState(),RandomSource.create(1))){
            var quads=new java.util.ArrayList<>(part.getQuads(null));for(Direction d:Direction.values())quads.addAll(part.getQuads(d));
            for(var q:quads){if(q.tintIndex()!=0)throw new IllegalStateException("Wire/liquid face ignores color");tintedFaces++;}
        }
        if(tintedFaces!=6)throw new IllegalStateException("Incomplete tinted unit");
        ChemistryMod.LOGGER.info("Electrical tint check passed: all six wire/liquid faces accept tint");
        if(ModStandaloneModels.singleGround()==null || ModStandaloneModels.vessel(1)==null)throw new IllegalStateException("Missing new flasks");
        ChemistryMod.LOGGER.info("Electrical render check passed: {} authored parts and both single-neck flasks",count);
    }
}
