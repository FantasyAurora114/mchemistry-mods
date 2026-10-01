package com.example.chemistry.radiation;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
/** Deliberately fictional accumulation points, not real critical masses or weapon physics. */
public final class NuclearInstability {
 public static final double THRESHOLD=100;
 private static final Map<String,Long> WARNINGS=new HashMap<>();
 public static double points(ItemStack s){double points=0;
  for(var carrier:RadioLedger.carriers(s).entrySet())for(var fraction:RadioLedger.fractions(s,carrier.getKey()).entrySet())if(fraction.getKey().equals("U235")||fraction.getKey().equals("Pu239"))points+=carrier.getValue()*fraction.getValue()*RadioLedger.NUCLIDES.get(fraction.getKey()).a()*.1;
  return points;
 }
 public static void tick(ServerLevel level,List<RadiationSystem.Source> sources){
  var groups=new LinkedHashMap<String,List<RadiationSystem.Source>>();
  for(var source:sources)if(!source.plume()&&source.persist()!=null&&points(source.sample())>0){var p=source.position();String key=level.dimension().location()+":"+(int)Math.floor(p.x/3)+":"+(int)Math.floor(p.y/3)+":"+(int)Math.floor(p.z/3);groups.computeIfAbsent(key,k->new ArrayList<>()).add(source);}
  var active=new HashSet<String>();
  for(var group:groups.entrySet()){double value=group.getValue().stream().mapToDouble(s->points(s.sample())).sum();if(value<THRESHOLD)continue;active.add(group.getKey());Vec3 at=group.getValue().getFirst().position();long start=WARNINGS.computeIfAbsent(group.getKey(),k->level.getGameTime());
   if(level.getGameTime()-start<100){level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,at.x,at.y+.5,at.z,12,.5,.5,.5,.03);for(var player:level.players())if(player.position().distanceTo(at)<16)player.displayClientMessage(net.minecraft.network.chat.Component.literal("☢ 聚集失稳：请在5秒内分散样品（游戏阈值）"),true);continue;}
   for(var source:group.getValue())source.persist().accept(ItemStack.EMPTY);
   var waste=new ItemStack(com.example.chemistry.registry.ModItems.RADIOACTIVE_WASTE_BOTTLE.get());com.example.chemistry.item.LabVesselItem.addMass(waste,"solid",group.getValue().stream().anyMatch(source->RadioLedger.carriers(source.sample()).containsKey("Pu239"))?"plutonium_dioxide":"uranium_dioxide",1);RadiationContamination.spill(level,BlockPos.containing(at),waste);
   level.explode(null,at.x,at.y,at.z,8,false,net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);WARNINGS.remove(group.getKey());
  }
  String prefix=level.dimension().location()+":";WARNINGS.keySet().removeIf(k->k.startsWith(prefix)&&!active.contains(k));
 }
 private NuclearInstability(){}
}
