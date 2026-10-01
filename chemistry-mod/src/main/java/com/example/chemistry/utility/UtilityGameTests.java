package com.example.chemistry.utility;
import com.example.chemistry.*;
import com.example.chemistry.entity.*;
import com.example.chemistry.electrical.*;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;
public final class UtilityGameTests {
 private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath("mchemistry",id)));}
 private static void check(GameTestHelper h,boolean test,String message){h.assertTrue(test,Component.literal(message));}
 private static void near(GameTestHelper h,double a,double b,double eps,String message){check(h,Math.abs(a-b)<eps,message+": "+a+" / "+b);}
 private static void water(ItemStack s,double grams){LabVesselItem.addMass(s,"liquid","water",grams);}
 public static void bath(GameTestHelper h){h.runAtTickTime(1,()->{
  var outer=item("beaker_medium");var inner=item("test_tube_5ml");water(outer,150);water(inner,20);TemperatureSystem.setTemp(outer,80);TemperatureSystem.setTemp(inner,20);
  check(h,BeakerWaterBath.canInstall(h.getLevel(),outer,inner),"test tube refused by bath");BeakerWaterBath.inner(h.getLevel(),outer,inner);
  double initial=TemperatureSystem.getTemp(outer)*ThermalSystem.capacity(outer)+TemperatureSystem.getTemp(inner)*ThermalSystem.capacity(inner);
  for(int i=0;i<100;i++)BeakerWaterBath.exchange(outer,inner);
  check(h,TemperatureSystem.getTemp(inner)>20&&TemperatureSystem.getTemp(inner)<=TemperatureSystem.getTemp(outer),"bath reverses temperature ordering");
  near(h,initial,TemperatureSystem.getTemp(outer)*ThermalSystem.capacity(outer)+TemperatureSystem.getTemp(inner)*ThermalSystem.capacity(inner),1e-5,"bath creates heat");
  near(h,BeakerWaterBath.water(outer)+BeakerWaterBath.water(inner),170,1e-8,"bath duplicates water");
  check(h,!BeakerWaterBath.canInstall(h.getLevel(),outer,inner),"second inner vessel allowed");
  var ops=h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);BeakerWaterBath.inner(h.getLevel(),outer,inner);var saved=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,outer).getOrThrow()).getOrThrow();var taken=BeakerWaterBath.take(h.getLevel(),saved);near(h,BeakerWaterBath.water(taken),20,1e-8,"nested contents lost");near(h,BeakerWaterBath.displacement(saved),0,1e-8,"removed vessel still displaces water");
  var dry=item("beaker_medium");near(h,BeakerWaterBath.exchange(dry,inner),0,1e-9,"dry bath transfers heat");h.succeed();
 });}
 private static WaterMachineEntity pump(GameTestHelper h,boolean vacuum,int x){var e=new WaterMachineEntity(ModEntities.WATER_MACHINE.get(),h.getLevel());var s=new ItemStack(vacuum?ModItems.CIRCULATING_WATER_VACUUM_PUMP.get():ModItems.TEMPERATURE_CONTROLLED_CIRCULATOR.get());water(s,300);e.setStack(s);e.flag("on",true);e.setPos(Vec3At(h,x));h.getLevel().addFreshEntity(e);return e;}
 private static net.minecraft.world.phys.Vec3 Vec3At(GameTestHelper h,int x){return net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(x,2,2)));}
 private static UtilityLineEntity line(GameTestHelper h,net.minecraft.world.entity.Entity a,int ap,net.minecraft.world.entity.Entity b,int bp,String kind){var w=new UtilityLineEntity(ModEntities.UTILITY_LINE.get(),h.getLevel());w.kind(kind);w.connect(a,ap,b,bp,false);h.getLevel().addFreshEntity(w);return w;}
 public static void vacuum(GameTestHelper h){h.runAtTickTime(1,()->{
  var p=pump(h,true,1);p.flag("channel_0",true);var v=new PlacedVesselEntity(ModEntities.PLACED_VESSEL.get(),h.getLevel());var s=item("round_bottom_flask");water(s,100);VesselHeating.seal(s,1);v.setVessel(s);v.setAttached1(item("straight_glass_tube"));v.setPos(Vec3At(h,3));h.getLevel().addFreshEntity(v);line(h,p,0,v,1,"vacuum");double initial=VesselGasPhase.freeVolumeMl(s);
  for(int i=0;i<500;i++)UtilityConnections.evacuate(p,0);s=v.getVessel();near(h,VacuumState.pressure(s),20,1e-7,"vacuum limit wrong");near(h,VesselGasPhase.read(s).stream().mapToDouble(VesselGasPhase.Part::ml).sum()+p.number("exhaust_0",0),initial,1e-6,"pump loses/creates gas");double pressure=VacuumState.pressure(s);VesselGasPhase.normalize(s);near(h,VacuumState.pressure(s),pressure,1e-8,"normalize refills vacuum");check(h,VacuumState.boilingPoint(s,"water")<65,"vacuum water does not boil lower");p.flag("channel_0",false);near(h,UtilityConnections.evacuate(p,0),0,1e-8,"closed channel pumps");VesselHeating.unseal(s);check(h,!VacuumState.enabled(s),"opening leaves vacuum flag");near(h,VacuumState.pressure(s),101.325,1e-6,"opening fails to admit air");h.succeed();
 });}
 public static void cooler(GameTestHelper h){h.runAtTickTime(1,()->{
  var p=pump(h,false,1);var second=pump(h,false,4);var c=new DistillationPartEntity(ModEntities.DISTILLATION_PART.get(),h.getLevel());c.setKind(DistillationPartEntity.CONDENSER);c.setPos(Vec3At(h,2));h.getLevel().addFreshEntity(c);line(h,p,0,c,0,"water");check(h,UtilityConnections.closedCoolingLoop(p)==null,"half loop supplies water");var bad=line(h,c,1,second,1,"water");check(h,UtilityConnections.closedCoolingLoop(p)==null,"different pump return accepted");bad.discard();line(h,c,1,p,1,"water");check(h,UtilityConnections.cooling(c),"complete cooling loop inactive");p.flag("on",false);check(h,!UtilityConnections.cooling(c),"off pump supplies water");p.flag("on",true);p.setStack(new ItemStack(ModItems.TEMPERATURE_CONTROLLED_CIRCULATOR.get()));check(h,!UtilityConnections.cooling(c),"empty reservoir supplies water");check(h,ElectricConnections.wires(p).isEmpty(),"utility hoses enter electrical graph");h.succeed();
 });}
 public static void stand(GameTestHelper h){h.runAtTickTime(1,()->{
  var stand=new IronStandEntity(ModEntities.IRON_STAND.get(),h.getLevel());stand.setPos(Vec3At(h,2));h.getLevel().addFreshEntity(stand);check(h,stand.addExtension()&&stand.addExtension()&&!stand.addExtension(),"extension rod limits broken");
  var source=new PlacedVesselEntity(ModEntities.PLACED_VESSEL.get(),h.getLevel());source.setPos(stand.position().add(0,.59375,0));source.setMount(.6F,.2,.59375,.2,0);source.setMountedStandId(stand.getUUID().toString());source.setVessel(item("round_bottom_flask"));h.getLevel().addFreshEntity(source);
  var part=new DistillationPartEntity(ModEntities.DISTILLATION_PART.get(),h.getLevel());part.setKind(DistillationPartEntity.HEAD);part.setStand(stand);part.setPos(source.position().add(.3,.3,0));h.getLevel().addFreshEntity(part);double y=source.getY(),py=part.getY();check(h,stand.adjustClamp(1.5),"clamp fails to rise");near(h,source.getY()-y,1.5,1e-7,"mounted vessel did not move");near(h,part.getY()-py,1.5,1e-7,"distillation head did not move");check(h,stand.findMountedVessel()==source&&DistillationAssembly.standFor(source)==stand,"raised source loses owner");check(h,!stand.removeExtension(),"rod removed from below high clamp");var restored=new IronStandEntity(ModEntities.IRON_STAND.get(),h.getLevel());restored.restoreExtensions(stand.toStack());near(h,restored.clampLift(),1.5,1e-7,"pickup loses clamp height");check(h,restored.extensionRods()==2,"pickup loses rods");check(h,stand.adjustClamp(0)&&stand.removeExtension(),"lower then remove fails");check(h,!stand.adjustClamp(Double.NaN)&&!stand.adjustClamp(4),"invalid height accepted");h.succeed();
 });}
 private UtilityGameTests(){}
}
