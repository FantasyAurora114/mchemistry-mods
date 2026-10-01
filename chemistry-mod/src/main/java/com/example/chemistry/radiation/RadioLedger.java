package com.example.chemistry.radiation;

import java.util.*;
import com.example.chemistry.data.*;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.transfer.BottleCodes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Separate nuclide populations in a chemical carrier. Chemical reactions never act as nuclear reactions. */
public final class RadioLedger {
 public record Nuclide(String id,String symbol,int a,int z,double halfSeconds,String daughter,String mode,double mev){}
 public static final double YEAR=365.25*86400,AVOGADRO=6.02214076e23;
 public static final Map<String,Nuclide> NUCLIDES=new LinkedHashMap<>();
 static {
  stable("H2","H",2,1);stable("He3","He",3,2);stable("C13","C",13,6);stable("N14","N",14,7);stable("O18","O",18,8);
  put("H3","H",3,1,12.32*YEAR,"He3","beta",.0186);put("C14","C",14,6,5730*YEAR,"N14","beta",.156);

  put("U238","U",238,92,4.468e9*YEAR,"Th234","alpha",4.27);put("Th234","Th",234,90,24.1*86400,"Pa234m","beta",.273);put("Pa234m","Pa",234,91,69.54,"U234","beta",2.29);
  put("U234","U",234,92,245500*YEAR,"Th230","alpha",4.86);put("Th230","Th",230,90,75380*YEAR,"Ra226","alpha",4.77);put("Ra226","Ra",226,88,1600*YEAR,"Rn222","alpha",4.87);
  put("Rn222","Rn",222,86,3.8235*86400,"Po218","alpha",5.59);put("Po218","Po",218,84,186,"Pb214","alpha",6.12);put("Pb214","Pb",214,82,1608,"Bi214","beta",1.024);
  put("Bi214","Bi",214,83,1194,"Po214","beta",3.27);put("Po214","Po",214,84,.000164,"Pb210","alpha",7.83);put("Pb210","Pb",210,82,22.2*YEAR,"Bi210","beta",.063);
  put("Bi210","Bi",210,83,5.012*86400,"Po210","beta",1.16);put("Po210","Po",210,84,138.376*86400,"Pb206","alpha",5.407);stable("Pb206","Pb",206,82);
  put("Th232","Th",232,90,1.405e10*YEAR,"Ra228","alpha",4.08);put("Ra228","Ra",228,88,5.75*YEAR,"Ac228","beta",.046);put("Ac228","Ac",228,89,6.15*3600,"Th228","beta",2.13);
  put("Th228","Th",228,90,1.9116*YEAR,"Ra224","alpha",5.52);put("Ra224","Ra",224,88,3.631*86400,"Rn220","alpha",5.79);put("Rn220","Rn",220,86,55.6,"Po216","alpha",6.405);
  put("Po216","Po",216,84,.145,"Pb212","alpha",6.91);put("Pb212","Pb",212,82,10.64*3600,"Bi212","beta",.574);put("Bi212","Bi",212,83,60.55*60,"Po212","beta",2.25);put("Po212","Po",212,84,.000000299,"Pb208","alpha",8.95);stable("Pb208","Pb",208,82);
  put("Pu239","Pu",239,94,24110*YEAR,"U235","alpha",5.245);put("U235","U",235,92,7.04e8*YEAR,"Th231","alpha",4.68);put("Th231","Th",231,90,25.52*3600,"Pa231","beta",.39);
  put("Pa231","Pa",231,91,32760*YEAR,"Ac227","alpha",5.15);put("Ac227","Ac",227,89,21.772*YEAR,"Th227","beta",.045);put("Th227","Th",227,90,18.68*86400,"Ra223","alpha",6.15);
  put("Ra223","Ra",223,88,11.43*86400,"Rn219","alpha",5.98);put("Rn219","Rn",219,86,3.96,"Po215","alpha",6.95);put("Po215","Po",215,84,.00178,"Pb211","alpha",7.53);
  put("Pb211","Pb",211,82,36.1*60,"Bi211","beta",1.37);put("Bi211","Bi",211,83,2.14*60,"Tl207","alpha",6.75);put("Tl207","Tl",207,81,4.77*60,"Pb207","beta",1.42);stable("Pb207","Pb",207,82);
  put("Am241","Am",241,95,432.2*YEAR,"Np237","alpha",5.49);put("Np237","Np",237,93,2.144e6*YEAR,"Pa233","alpha",4.96);put("Pa233","Pa",233,91,26.975*86400,"U233","beta",.57);
  put("U233","U",233,92,159200*YEAR,"Th229","alpha",4.91);put("Th229","Th",229,90,7340*YEAR,"Ra225","alpha",5.17);put("Ra225","Ra",225,88,14.9*86400,"Ac225","beta",.36);
  put("Ac225","Ac",225,89,9.92*86400,"Fr221","alpha",5.94);put("Fr221","Fr",221,87,4.9*60,"At217","alpha",6.46);put("At217","At",217,85,.0323,"Bi213","alpha",7.2);
  put("Bi213","Bi",213,83,45.6*60,"Po213","beta",1.42);put("Po213","Po",213,84,.0000042,"Pb209","alpha",8.38);put("Pb209","Pb",209,82,3.25*3600,"Bi209","beta",.64);stable("Bi209","Bi",209,83);
  put("Tc99","Tc",99,43,211100*YEAR,"Ru99","beta",.294);stable("Ru99","Ru",99,44);put("I131","I",131,53,8.02*86400,"Xe131","beta_gamma",.971);stable("Xe131","Xe",131,54);
  put("Cs137","Cs",137,55,30.05*YEAR,"Ba137m","beta",.514);put("Ba137m","Ba",137,56,153,"Ba137","gamma",.662);stable("Ba137","Ba",137,56);put("Co60","Co",60,27,5.2714*YEAR,"Ni60","beta_gamma",2.824);stable("Ni60","Ni",60,28);
 }
 private static void put(String id,String symbol,int a,int z,double t,String d,String mode,double energy){NUCLIDES.put(id,new Nuclide(id,symbol,a,z,t,d,mode,energy));}
 private static void stable(String id,String symbol,int a,int z){put(id,symbol,a,z,Double.POSITIVE_INFINITY,"","stable",0);}
 public static CompoundTag tag(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
 private static void write(ItemStack s,CompoundTag t){s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
 public static String root(String id){if(id==null)return null;if(id.equals("radon"))return "Rn222";return RadioChemicals.ROOTS.get(id.replace("_solution",""));}
 private static void add(Map<String,Double> m,String id,double grams){String root=root(id);if(root==null||grams<=0)return;var r=FutureChemicals.find(id.replace("_solution",""));double molar=r==null?222:r.mass(),count=r==null?1:r.atoms().getOrDefault(NUCLIDES.get(root).symbol(),1);m.merge(root,grams/molar*count,Double::sum);}
 public static Map<String,Double> carriers(ItemStack s){
  Map<String,Double> m=new LinkedHashMap<>();if(s.isEmpty())return m;
  if(s.getItem() instanceof IsotopeSampleItem isotope){m.put(isotope.nuclide(),5.0*s.getCount()/NUCLIDES.get(isotope.nuclide()).a());return m;}
  if(tag(s).contains("radio_surface")){var surface=new ItemStack(com.example.chemistry.registry.ModItems.RADIOACTIVE_WASTE_BOTTLE.get());surface.set(DataComponents.CUSTOM_DATA,CustomData.of(tag(s).getCompoundOrEmpty("radio_surface")));return carriers(surface);}
  if(s.getItem() instanceof LabVesselItem||tag(s).contains("chem_contents")){for(var e:LabVesselItem.getContents(s))add(m,e.id(),e.amount());double gas=com.example.chemistry.VesselGasPhase.read(s).stream().filter(p->p.id().equals("radon")).mapToDouble(p->p.ml()).sum();add(m,"radon",gas/24465*222);return m;}
  if(com.example.chemistry.item.DropperHelper.isDropper(s)){String liquid=com.example.chemistry.item.DropperHelper.getLiquid(s);if(liquid!=null){double grams=com.example.chemistry.item.DropperHelper.getMl(s)*ChemicalInfoProvider.densityOfLiquid(liquid);if(Solutions.soluteOf(liquid)!=null)grams*=Solutions.stockFraction(liquid);add(m,liquid,grams);}return m;}
  String id=BottleCodes.solidIdOf(s);if(id!=null){add(m,id,BottleCodes.solidGrams(s));return m;}
  id=BottleCodes.liquidIdOf(s);if(id!=null){double grams=BottleCodes.volumeOf(s)*ChemicalInfoProvider.densityOfLiquid(id);if(Solutions.soluteOf(id)!=null)grams*=Solutions.stockFraction(id);add(m,id,grams);return m;}
  id=BottleCodes.gasIdOf(s);if(id!=null){add(m,id,BottleCodes.volumeOf(s)/24465.0*222);return m;}
  if(s.getItem() instanceof com.example.chemistry.item.GasCylinderItem c){add(m,c.gas(s),c.remaining(s)/24465.0*222);return m;}
  String path=BottleCodes.pathOf(s);if(path.startsWith("loose_"))add(m,path.substring(6),tag(s).getDoubleOr("chem_grams",5)*s.getCount());
  else if(path.startsWith("element_")){for(String element:RadioChemicals.ROOTS.keySet())if(path.startsWith("element_"+element+"_")){add(m,element,(path.endsWith("_nugget")?5.0/9:5)*s.getCount());break;}}
  else if(s.getItem() instanceof com.example.chemistry.item.SolidToolItem)add(m,com.example.chemistry.item.SolidToolItem.getHeldSolid(s),com.example.chemistry.item.SolidToolItem.heldGrams(s));return m;
 }
 public static Map<String,Double> fractions(ItemStack s,String root){CompoundTag node=tag(s).getCompoundOrEmpty("radio_nuclides").getCompoundOrEmpty(root);Map<String,Double> m=new LinkedHashMap<>();for(String key:node.keySet())if(NUCLIDES.containsKey(key)||key.equals("He4")||key.equals("electrons"))m.put(key,node.getDoubleOr(key,0));if(m.isEmpty())m.put(root,1.0);return m;}
 public record Advance(Map<String,Double> fractions,double mev){}
 /** Chain populations advanced with bounded exponential substeps; emitted helium and electrons stay in the nuclear ledger. */
 public static Advance advance(Map<String,Double> before,double seconds){
  var values=new LinkedHashMap<>(before);double energy=0;if(seconds<=0)return new Advance(Map.copyOf(values),0);
  int steps=Math.min(256,Math.max(1,(int)Math.ceil(seconds/30)));double dt=seconds/steps;
  for(int step=0;step<steps;step++)for(var item:new ArrayList<>(values.entrySet())){var n=NUCLIDES.get(item.getKey());if(n==null||n.daughter().isEmpty()||item.getValue()<=0)continue;double decayed=item.getValue()*-Math.expm1(-Math.log(2)*dt/n.halfSeconds());
   values.merge(n.id(),-decayed,Double::sum);values.merge(n.daughter(),decayed,Double::sum);if(n.mode().equals("alpha"))values.merge("He4",decayed,Double::sum);else if(n.mode().startsWith("beta"))values.merge("electrons",decayed,Double::sum);energy+=decayed*n.mev();
  }return new Advance(Map.copyOf(values),energy);
 }
 public static double tick(ItemStack s,long time,double multiplier){
  var carriers=carriers(s);if(carriers.isEmpty())return 0;var t=tag(s);long previous=t.getLongOr("radio_tick",time);t.putLong("radio_tick",time);var all=t.getCompoundOrEmpty("radio_nuclides");double joules=0;
  for(var c:carriers.entrySet()){var old=fractions(s,c.getKey());var advanced=advance(old,Math.max(0,time-previous)/20.0*multiplier);var node=new CompoundTag();advanced.fractions().forEach(node::putDouble);all.put(c.getKey(),node);joules+=advanced.mev()*c.getValue()*AVOGADRO*1.602176634e-13;}
  t.put("radio_nuclides",all);write(s,t);
  // Heating uses physical elapsed seconds, not accelerated display time; gameplay cap prevents runaway temperatures.
  return Math.min(.2,joules/Math.max(1,multiplier));
 }
 public static double activity(ItemStack s){double bq=0;for(var c:carriers(s).entrySet())for(var f:fractions(s,c.getKey()).entrySet()){var n=NUCLIDES.get(f.getKey());if(n!=null&&Double.isFinite(n.halfSeconds()))bq+=c.getValue()*AVOGADRO*f.getValue()*Math.log(2)/n.halfSeconds();}return bq;}
 public static double betaFraction(ItemStack s){double beta=0,total=0;for(var c:carriers(s).entrySet())for(var f:fractions(s,c.getKey()).entrySet()){var n=NUCLIDES.get(f.getKey());if(n==null||!Double.isFinite(n.halfSeconds()))continue;double a=c.getValue()*f.getValue()/n.halfSeconds();total+=a;if(n.mode().startsWith("beta"))beta+=a;}return total>0?beta/total:0;}
 public static double gammaFraction(ItemStack s){double gamma=0,total=0;for(var c:carriers(s).entrySet())for(var f:fractions(s,c.getKey()).entrySet()){var n=NUCLIDES.get(f.getKey());if(n==null||!Double.isFinite(n.halfSeconds()))continue;double a=c.getValue()*f.getValue()/n.halfSeconds();total+=a;if(n.mode().contains("gamma")||Set.of("Pb214","Bi214","Ac228","Th227").contains(n.id()))gamma+=a;}return total>0?gamma/total:0;}
 /** A material transfer mixes carrier-specific fractions, including daughters, without resetting the sample age. */
 public static void inherit(ItemStack sourceBefore,ItemStack targetBefore,ItemStack targetAfter){
  var before=carriers(targetBefore);var after=carriers(targetAfter);var t=tag(targetAfter);var all=t.getCompoundOrEmpty("radio_nuclides");
  for(var c:after.entrySet()){double old=before.getOrDefault(c.getKey(),0.0),incoming=Math.max(0,c.getValue()-old);if(incoming<=1e-15)continue;Map<String,Double> merged=new LinkedHashMap<>();fractions(targetBefore,c.getKey()).forEach((k,v)->merged.merge(k,v*old/c.getValue(),Double::sum));fractions(sourceBefore,c.getKey()).forEach((k,v)->merged.merge(k,v*incoming/c.getValue(),Double::sum));var node=new CompoundTag();merged.forEach(node::putDouble);all.put(c.getKey(),node);}
  t.put("radio_nuclides",all);write(targetAfter,t);
 }
 /** Called after a mass transfer; reconstruct the receiver before mixing its nuclear populations. */
 public static void onMix(ItemStack source,ItemStack target,List<LabVesselItem.Entry> transferred){
  if(source.isEmpty()||transferred.stream().noneMatch(e->root(e.id())!=null))return;
  var before=target.copy();for(var entry:transferred)LabVesselItem.consumeMass(before,entry.type(),entry.id(),entry.amount());inherit(source,before,target);
 }
 /** Copy only nuclear metadata; quantity is always owned by the destination's normal storage. */
 public static void copyState(ItemStack source,ItemStack target){
  var before=tag(source);var after=tag(target);for(String key:List.of("radio_nuclides","radio_tick")){if(before.contains(key))after.put(key,before.get(key).copy());else after.remove(key);}write(target,after);
 }
 public static double nucleons(Map<String,Double> fractions){double n=0;for(var e:fractions.entrySet())n+=e.getValue()*(e.getKey().equals("He4")?4:NUCLIDES.containsKey(e.getKey())?NUCLIDES.get(e.getKey()).a():0);return n;}
 private RadioLedger(){}
}
