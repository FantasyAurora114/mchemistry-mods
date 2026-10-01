package com.example.chemistry.radiation;
import java.util.*;
import com.example.chemistry.ChemistryMod;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.transfer.BottleCodes;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
@EventBusSubscriber(modid=ChemistryMod.MODID)
public final class RadiationSystem {
 public record Source(Vec3 position,ItemStack sample,double shielding,boolean plume,java.util.function.Consumer<ItemStack> persist){
  public Source(Vec3 p,ItemStack s,double shield,boolean plume){this(p,s,shield,plume,null);}
 }
 public record Reading(double cps,double doseRate){}

 public static double transmission(double distance,double material,double gamma,boolean sealed,boolean plume,int suit){
  double external=(sealed&&!plume)?gamma+(1-gamma)*.015:1;
  double clothes=gamma+(1-gamma)*Math.pow(plume?.55:.8,suit);
  return external*clothes*material/(1+distance*distance);
 }
 /** Shield thickness is sampled along the ray; beta prefers plastic, gamma benefits from dense shielding. */
 public static double shielding(ServerLevel l,Vec3 from,Vec3 to,double gamma){double out=1;var seen=new HashSet<BlockPos>();double distance=from.distanceTo(to);int steps=Math.min(96,Math.max(1,(int)Math.ceil(distance*4)));
  for(int i=1;i<steps;i++){var p=BlockPos.containing(from.lerp(to,(double)i/steps));if(p.equals(BlockPos.containing(from))||!seen.add(p)||!l.hasChunkAt(p))continue;var s=l.getBlockState(p);String id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
   if(id.equals("radiation_shield_box")||id.equals("lead_lined_cabinet"))out*=s.getValue(BlockStateProperties.OPEN)?1:.02;
   else if(id.contains("lead")||id.contains("iron_block"))out*=.08;
   else if(id.contains("concrete"))out*=.25;
   else if(id.contains("water"))out*=.75;
   else if(id.contains("glass"))out*=gamma*.98+(1-gamma)*.35;
   else if(s.isSolidRender())out*=gamma*.85+(1-gamma)*.2;
  }return out;
 }
 public static Reading measure(ServerLevel level,Vec3 position,List<Source> sources,int suit){double cps=0,dose=0;for(var source:sources){double bq=RadioLedger.activity(source.sample());if(bq<=0)continue;double gamma=RadioLedger.gammaFraction(source.sample());double path=source.shielding()*shielding(level,source.position(),position,gamma),d=source.position().distanceTo(position);
   double enclosure=sealed(source.sample())&&!source.plume()?gamma+RadioLedger.betaFraction(source.sample())*.05:1;double signal=Math.log1p(bq)*transmission(d,path,gamma,false,source.plume(),0)*enclosure;cps+=signal*.6;
   dose+=Math.log1p(bq)*.004*transmission(d,path,gamma,false,source.plume(),suit)*enclosure;
  }return new Reading(cps+.15,dose);}
 public static boolean sealed(ItemStack s){return s.getItem() instanceof LabVesselItem?com.example.chemistry.VesselHeating.isSealed(s):BottleCodes.isSealed(s)||s.getItem() instanceof com.example.chemistry.item.GasCylinderItem;}
 private static void add(List<Source> out,ItemStack s,Vec3 p,double shield,ServerLevel l,java.util.function.Consumer<ItemStack> persist){if(s.isEmpty()||RadioLedger.carriers(s).isEmpty())return;double heat=RadioLedger.tick(s,l.getGameTime(),com.example.chemistry.Config.RADIO_DECAY_MULTIPLIER.get());if(s.getItem() instanceof LabVesselItem&&heat>0)com.example.chemistry.TemperatureSystem.setTemp(s,com.example.chemistry.PhaseSystem.applyHeatJoules(s,com.example.chemistry.TemperatureSystem.getTemp(s),heat));persist.accept(s);out.add(new Source(p,s.copy(),shield,false,persist));}
 private static List<Source> sources(ServerLevel l){List<Source> out=new ArrayList<>();Set<BlockPos> blocks=new HashSet<>();Set<Integer> entities=new HashSet<>();
  for(var player:l.players()){
   for(int i=0;i<player.getInventory().getContainerSize();i++){int slot=i;
    var held=player.getInventory().getItem(i);String path=BottleCodes.pathOf(held);
    if(java.util.Set.of("straight_glass_tube_tubed","straight_glass_tube_long_tubed","right_angle_glass_tube_tubed","right_angle_glass_tube_long_tubed").contains(path)){
     String base=path.substring(0,path.length()-6);var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry",base));player.getInventory().setItem(i,new ItemStack(item,held.getCount()));}
    add(out,player.getInventory().getItem(i),player.position(),1,l,s->player.getInventory().setItem(slot,s));}
   if(player.getPersistentData().contains("chem_radio_skin")){var skin=new ItemStack(com.example.chemistry.registry.ModItems.RADIOACTIVE_WASTE_BOTTLE.get());skin.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(player.getPersistentData().getCompoundOrEmpty("chem_radio_skin")));RadioLedger.tick(skin,l.getGameTime(),com.example.chemistry.Config.RADIO_DECAY_MULTIPLIER.get());player.getPersistentData().put("chem_radio_skin",RadioLedger.tag(skin));out.add(new Source(player.position(),skin,1,true));}
   int cx=player.chunkPosition().x,cz=player.chunkPosition().z;
   for(int x=cx-1;x<=cx+1;x++)for(int z=cz-1;z<=cz+1;z++){var chunk=l.getChunkSource().getChunk(x,z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);if(!(chunk instanceof net.minecraft.world.level.chunk.LevelChunk loaded))continue;
    for(var be:loaded.getBlockEntities().values()){if(!blocks.add(be.getBlockPos()))continue;Vec3 p=Vec3.atCenterOf(be.getBlockPos());
     if(be instanceof net.minecraft.world.Container container){double factor=be instanceof ShieldedStorageBlockEntity shield&&shield.closed()?.02:1;for(int i=0;i<container.getContainerSize();i++){int slot=i;add(out,container.getItem(i),p,factor,l,s->{container.setItem(slot,s);container.setChanged();});}}
     else if(be instanceof com.example.chemistry.blockentity.TestTubeRackBlockEntity rack){for(int i=0;i<com.example.chemistry.blockentity.TestTubeRackBlockEntity.SLOTS;i++){int slot=i;add(out,rack.getTube(i),p,1,l,s->{rack.setTube(slot,s,rack.isInverted(slot));});}}
     else if(be instanceof com.example.chemistry.blockentity.GasApplianceBlockEntity gas&&gas.gas().equals("radon")){add(out,gas.dropStack(),p,1,l,gas::setRadioSample);}
     else if(be instanceof com.example.chemistry.blockentity.LaboratoryBenchBlockEntity bench){for(int b=0;b<bench.bays();b++)for(int i=0;i<9;i++){int bay=b,slot=i;add(out,bench.item(b,i),p,1,l,s->bench.item(bay,slot,s));}}
     else if(be instanceof com.example.chemistry.blockentity.PlacedVesselBlockEntity vessel)add(out,vessel.getVessel(),p,1,l,s->{vessel.setVessel(s);});
     else if(be instanceof com.example.chemistry.blockentity.IronStandBlockEntity stand)add(out,stand.getVessel(),p,1,l,s->stand.setVessel(s));
    }
   }
   for(var e:l.getEntities(player,new AABB(player.position(),player.position()).inflate(24))){if(!entities.add(e.getId()))continue;
    if(e instanceof net.minecraft.world.entity.item.ItemEntity dropped)add(out,dropped.getItem(),e.position(),1,l,dropped::setItem);
    else if(e instanceof com.example.chemistry.entity.PlacedReagentBottleEntity bottle)add(out,bottle.toStack(),e.position(),1,l,bottle::setStack);
    else if(e instanceof com.example.chemistry.entity.GasCollectingBottleEntity gas){add(out,gas.toStack(),e.position(),1,l,gas::setRadioSample);}
    else if(e instanceof com.example.chemistry.entity.PlacedVesselEntity vessel)add(out,vessel.getVessel(),e.position(),1,l,vessel::setVessel);
    else if(e instanceof com.example.chemistry.entity.IronStandEntity stand&&stand.findMountedVessel()==null)add(out,stand.getVessel(),e.position(),1,l,stand::setVessel);
   }
  }
  var contamination=RadiationContamination.get(l);for(var e:contamination.samples().entrySet()){var p=BlockPos.of(Long.parseLong(e.getKey()));if(!l.hasChunkAt(p))continue;RadioLedger.tick(e.getValue(),l.getGameTime(),com.example.chemistry.Config.RADIO_DECAY_MULTIPLIER.get());out.add(new Source(Vec3.atCenterOf(p),e.getValue().copy(),1,true));}if(!contamination.samples().isEmpty())contamination.setDirty();return out;
 }
 @SubscribeEvent public static void tick(LevelTickEvent.Post event){if(!(event.getLevel() instanceof ServerLevel level)||level.players().isEmpty())return;
  for(var player:level.players())if(player.getMainHandItem().is(com.example.chemistry.registry.ModItems.GEIGER_COUNTER.get())||player.getOffhandItem().is(com.example.chemistry.registry.ModItems.GEIGER_COUNTER.get())){
   double cps=player.getPersistentData().getDoubleOr("chem_geiger_cps",.15);int interval=Math.max(1,(int)Math.round(20/Math.max(.15,cps)));if(level.getGameTime()%interval==0)level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.LEVER_CLICK,net.minecraft.sounds.SoundSource.PLAYERS,.15F,1.4F);
  }
  if(level.getGameTime()%20!=0)return;
  for(var player:level.players())handleOpenSamples(level,player);
  var sources=sources(level);NuclearInstability.tick(level,sources);
  for(var player:level.players()){var reading=measure(level,player.position().add(0,1,0),sources,RadiationGear.pieces(player));var data=player.getPersistentData();double dose=data.getDoubleOr("chem_radiation_dose",0)+reading.doseRate();data.putDouble("chem_radiation_dose",dose);data.putDouble("chem_geiger_cps",reading.cps());data.putDouble("chem_radiation_rate",reading.doseRate());
   if(!player.isCreative()&&!player.isSpectator()){if(dose>=10)player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS,60,0));if(dose>=30)player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NAUSEA,60,0));if(dose>=80&&level.getGameTime()%100==0)player.hurtServer(level,level.damageSources().magic(),1);}
   if(player.getMainHandItem().is(com.example.chemistry.registry.ModItems.GEIGER_COUNTER.get())||player.getOffhandItem().is(com.example.chemistry.registry.ModItems.GEIGER_COUNTER.get())){player.displayClientMessage(net.minecraft.network.chat.Component.literal(reading(player)),true);}
  }
 }
 private static void handleOpenSamples(ServerLevel l,Player p){
  for(var held:List.of(p.getMainHandItem(),p.getOffhandItem())){
   if(sealed(held))continue;String gas=BottleCodes.gasIdOf(held);
   if("radon".equals(gas)&&BottleCodes.volumeOf(held)>0){var plume=new ItemStack(com.example.chemistry.registry.ModItems.RADIOACTIVE_WASTE_BOTTLE.get());LabVesselItem.addMass(plume,"gas","radon",222.0/24465);RadioLedger.inherit(held,ItemStack.EMPTY,plume);BottleCodes.setVolume(held,BottleCodes.volumeOf(held)-1);RadiationContamination.spill(l,p.blockPosition(),plume);}
   String solid=BottleCodes.solidIdOf(held);if(RadioLedger.root(solid)==null||BottleCodes.solidGrams(held)<=0)continue;
   double grams=Math.min(BottleCodes.solidGrams(held),.00001*Math.pow(.55,RadiationGear.pieces(p)));var skin=new ItemStack(com.example.chemistry.registry.ModItems.RADIOACTIVE_WASTE_BOTTLE.get());
   net.minecraft.world.item.ItemStack clothing=p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);boolean covered=RadiationGear.isSuit(clothing);
   var stored=covered?RadioLedger.tag(clothing).getCompoundOrEmpty("radio_surface"):p.getPersistentData().getCompoundOrEmpty("chem_radio_skin");skin.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(stored));var before=skin.copy();LabVesselItem.addMass(skin,"solid",solid,grams);RadioLedger.inherit(held,before,skin);BottleCodes.setSolidGrams(held,BottleCodes.solidGrams(held)-grams);
   if(covered){var t=RadioLedger.tag(clothing);t.put("radio_surface",RadioLedger.tag(skin));clothing.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(t));}else p.getPersistentData().put("chem_radio_skin",RadioLedger.tag(skin));
  }
 }
 public static String reading(Player p){var t=p.getPersistentData();return String.format(java.util.Locale.ROOT,"盖革计数：%.1f cps（模拟）　剂量率 %.3f /s　累计 %.2f（游戏单位）",t.getDoubleOr("chem_geiger_cps",.15),t.getDoubleOr("chem_radiation_rate",0),t.getDoubleOr("chem_radiation_dose",0));}
 @SubscribeEvent public static void clone(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone e){if(e.getOriginal().getPersistentData().contains("chem_radio_skin"))e.getEntity().getPersistentData().put("chem_radio_skin",e.getOriginal().getPersistentData().getCompoundOrEmpty("chem_radio_skin").copy());for(String key:List.of("chem_radiation_dose","chem_geiger_cps","chem_radiation_rate"))e.getEntity().getPersistentData().putDouble(key,e.getOriginal().getPersistentData().getDoubleOr(key,0));}
 private RadiationSystem(){}
}
