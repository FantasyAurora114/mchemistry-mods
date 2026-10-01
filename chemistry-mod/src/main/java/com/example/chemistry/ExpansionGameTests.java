package com.example.chemistry;

import com.example.chemistry.garden.ChemicalGarden;
import com.example.chemistry.organic.*;
import com.example.chemistry.electrical.*;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.*;
import com.example.chemistry.solution.*;
import com.example.chemistry.titration.LiquidTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.nbt.NbtOps;
import java.util.*;

/** Conservation and operational boundaries for the three shared-ledger expansions. */
public final class ExpansionGameTests {
    private static void check(GameTestHelper h,boolean ok,String why){h.assertTrue(ok,Component.literal(why));}
    private static void near(GameTestHelper h,double a,double b,double eps,String why){check(h,Math.abs(a-b)<eps,why+": "+a+" / "+b);}
    private static ItemStack vessel(){return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","beaker_medium")));}
    private static void add(ItemStack s,String id,double g){LabVesselItem.addMass(s,"liquid",id,g);}
    private static double mass(ItemStack s,String id){return LabVesselItem.getContents(s).stream().filter(e->e.id().equals(id)).mapToDouble(LabVesselItem.Entry::amount).sum();}
    private static ItemStack garden(String salt){var s=vessel();add(s,"water",200);add(s,"sodium_silicate_solution",10);LabVesselItem.addMass(s,"solid",salt,1);return s;}
    public static void gardenBalance(GameTestHelper h){h.runAtTickTime(1,()->{
        for(String salt:List.of("copper_sulfate_anhydrous","iron_iii_chloride")){
            var s=garden(salt);var before=SolutionSpecies.analyticalSnapshot(s);check(h,before.fullyModelled(),"garden species unmapped");
            PhaseSystem.dissolveAndCrystallize(s,false);near(h,mass(s,salt),1,1e-9,"seed prematurely dissolved");
            for(int i=0;i<800;i++)ChemicalGarden.tick(s);
            check(h,!ChemicalGarden.stems(s).isEmpty(),"garden never grows");
            check(h,mass(s,salt)<1&&mass(s,"sodium_silicate_solution")<10&&mass(s,"water")<200,"growth does not consume reactants");
            check(h,Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"garden loses material");
            check(h,ChemicalGarden.stems(s).stream().allMatch(v->v.segments()<=32),"unbounded geometry");
        }h.succeed();
    });}
    public static void gardenLifecycle(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=garden("copper_sulfate_anhydrous");for(int i=0;i<160;i++)ChemicalGarden.tick(s);
        var ops=h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var copy=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,s).getOrThrow()).getOrThrow();
        check(h,ChemicalGarden.stems(copy).equals(ChemicalGarden.stems(s)),"garden save resets structure");
        var before=SolutionSpecies.analyticalSnapshot(s);ChemicalGarden.stir(s);
        check(h,ChemicalGarden.stems(s).isEmpty()&&Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"stirring destroys material");
        for(int i=0;i<200;i++)ChemicalGarden.tick(s);check(h,ChemicalGarden.stems(s).isEmpty(),"broken garden silently regrows");
        LabVesselItem.clearContents(s);check(h,!s.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains("chem_garden_broken"),"clear does not reset garden");
        var filtered=copy.copy();LabVesselItem.consumeMass(filtered,"solid","copper_ii_hydroxide",mass(filtered,"copper_ii_hydroxide"));
        check(h,ChemicalGarden.stems(filtered).isEmpty(),"removed precipitate leaves ghost tubes");
        var plain=vessel();add(plain,"water",200);LabVesselItem.addMass(plain,"solid","copper_sulfate_anhydrous",1);for(int i=0;i<200;i++)ChemicalGarden.tick(plain);
        check(h,ChemicalGarden.stems(plain).isEmpty(),"garden grows without silicate");h.succeed();
    });}
    private static ItemStack iodine(String solvent,double grams){var s=vessel();add(s,"water",100);add(s,solvent,grams);add(s,"iodine_water",.02);for(int i=0;i<30;i++)Extraction.tick(s);return s;}
    public static void extractionBalance(GameTestHelper h){h.runAtTickTime(1,()->{
        for(String solvent:Extraction.K.keySet()){
            var s=iodine(solvent,30);var before=SolutionSpecies.analyticalSnapshot(s);var phase=LiquidPhases.read(s);check(h,phase.separated(),"solvent fails to separate "+solvent);
            var org=phase.layers().stream().filter(l->l.name().equals(solvent)).findFirst().orElseThrow();var aq=phase.layers().stream().filter(l->l.name().equals("aqueous")).findFirst().orElseThrow();
            double expected=.02*Extraction.K.get(solvent)*org.ml()/(aq.ml()+Extraction.K.get(solvent)*org.ml());
            near(h,mass(s,Extraction.organicId(solvent)),expected,1e-7,"partition wrong");near(h,mass(s,"iodine_water")+mass(s,Extraction.organicId(solvent)),.02,1e-9,"iodine duplicated");
            var out=vessel();double moved=LiquidTransfer.sample(s,out,Math.min(10,phase.bottom().ml()),true);check(h,moved>0,"phase transfer blocked");
            check(h,Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s).plus(SolutionSpecies.analyticalSnapshot(out))).conserved(),"extraction transfer loses matter");
            check(h,LabVesselItem.getContents(out).stream().anyMatch(e->Extraction.iodine(e.id())),"phase left iodine behind");
        }h.succeed();
    });}
    public static void extractionBoundaries(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=iodine("carbon_tetrachloride",30);var funnel=new SeparatoryFunnelEntity(ModEntities.SEPARATORY_FUNNEL.get(),h.getLevel());funnel.capped(false);funnel.device(s);funnel.receiver(vessel());
        var before=SolutionSpecies.analyticalSnapshot(funnel.device());check(h,funnel.openValve(),"extract drain fails");for(int i=0;i<300&&funnel.open();i++)funnel.drain();
        check(h,mass(funnel.receiver(),"iodine_carbon_tetrachloride")>0&&mass(funnel.device(),"iodine_water")>0,"separation sends wrong solute");
        check(h,Conservation.compare(before,SolutionSpecies.analyticalSnapshot(funnel.device()).plus(SolutionSpecies.analyticalSnapshot(funnel.receiver()))).conserved(),"funnel loses iodine");
        var complex=iodine("toluene",20);add(complex,"ethanol",10);before=SolutionSpecies.analyticalSnapshot(complex);check(h,!LiquidPhases.read(complex).modelled(),"cosolvent uses binary phase model");Extraction.tick(complex);check(h,Conservation.compare(before,SolutionSpecies.analyticalSnapshot(complex)).conserved(),"unsupported mixture modified");
        var trace=vessel();add(trace,"water",100);add(trace,"ethyl_acetate",1);add(trace,"iodine_ethyl_acetate",.001);
        near(h,LiquidPhases.read(trace).layers().stream().mapToDouble(LiquidPhases.Layer::ml).sum(),LabVesselItem.usedVolume(trace),1e-8,"dissolved organic iodine missing from phase volume");Extraction.tick(trace);near(h,mass(trace,"iodine_water"),.001,1e-9,"single aqueous phase retains orphan iodine");h.succeed();
    });}
    private static double extractPortion(ItemStack s,double ml){add(s,"toluene",ml*.867);for(int i=0;i<30;i++)Extraction.tick(s);var organic=LiquidPhases.read(s).top();var receiver=vessel();LiquidTransfer.sample(s,receiver,organic.ml(),false);return mass(s,"iodine_water");}
    public static void repeatedExtraction(GameTestHelper h){h.runAtTickTime(1,()->{
        var once=vessel();add(once,"water",100);add(once,"iodine_water",.02);var twice=once.copy();
        double one=extractPortion(once,20);extractPortion(twice,10);double two=extractPortion(twice,10);
        check(h,two<one,"repeated fresh small portions do not improve extraction");h.succeed();
    });}
    public static void esterification(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=vessel();add(s,"acetic_acid",6);add(s,"ethanol",4.6);add(s,"hydrochloric_acid",.01);TemperatureSystem.setTemp(s,60);
        var before=SolutionSpecies.analyticalSnapshot(s);for(int i=0;i<100;i++)OrganicChemistry.tick(s);
        check(h,mass(s,"ethyl_acetate")>0&&mass(s,"water")>0,"forward esterification failed");near(h,mass(s,"hydrochloric_acid"),.01,1e-9,"catalyst consumed");check(h,Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"esterification not balanced");
        var hydro=vessel();add(hydro,"ethyl_acetate",8.8);add(hydro,"water",100);add(hydro,"hydrochloric_acid",.01);TemperatureSystem.setTemp(hydro,60);before=SolutionSpecies.analyticalSnapshot(hydro);for(int i=0;i<100;i++)OrganicChemistry.tick(hydro);
        check(h,mass(hydro,"acetic_acid")>0&&mass(hydro,"ethanol")>0,"reversible acid hydrolysis missing");check(h,Conservation.compare(before,SolutionSpecies.analyticalSnapshot(hydro)).conserved(),"acid hydrolysis unbalanced");
        var inactive=vessel();add(inactive,"acetic_acid",6);add(inactive,"ethanol",4.6);TemperatureSystem.setTemp(inactive,60);check(h,!OrganicChemistry.tick(inactive),"no-catalyst esterifies");add(inactive,"hydrochloric_acid",.01);TemperatureSystem.setTemp(inactive,20);check(h,!OrganicChemistry.tick(inactive),"cold reaction ignores threshold");h.succeed();
    });}
    public static void alkalineHydrolysis(GameTestHelper h){h.runAtTickTime(1,()->{
        for(String metal:List.of("sodium","potassium")){
            var s=vessel();add(s,"water",100);add(s,"ethyl_acetate",1);add(s,metal+"_hydroxide_solution",.01);var before=SolutionSpecies.analyticalSnapshot(s);
            for(int i=0;i<300;i++)OrganicChemistry.tick(s);
            check(h,mass(s,metal+"_acetate_solution")>0&&mass(s,"ethanol")>0,"alkaline hydrolysis missing");near(h,mass(s,metal+"_hydroxide_solution"),0,1e-8,"base not limiting reagent");check(h,mass(s,"ethyl_acetate")>0,"hydrolysis ignores finite base");check(h,Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"alkaline hydrolysis unbalanced");
        }h.succeed();
    });}
    private static ElectroDeviceEntity device(GameTestHelper h,Item item,int x,int z){var e=new ElectroDeviceEntity(ModEntities.ELECTRO_DEVICE.get(),h.getLevel());e.setStack(new ItemStack(item));var p=h.absolutePos(new BlockPos(x,1,z));e.setPos(p.getX()+.5,p.getY(),p.getZ()+.5);h.getLevel().addFreshEntity(e);return e;}
    private static GalvanicSystem.Pair pair(GameTestHelper h,int x,int z){var zn=device(h,ModItems.GALVANIC_HALF_CELL.get(),x,z);var cu=device(h,ModItems.GALVANIC_HALF_CELL.get(),x+2,z);for(var e:List.of(zn,cu)){var s=e.stack();add(s,"water",100);add(s,e==zn?"zinc_sulfate_solution":"copper_sulfate_solution",3);e.setStack(s);TroughSystem.part(e,"mesh",0,new ItemStack(e==zn?ModItems.ZINC_ELECTRODE_MESH.get():ModItems.COPPER_ELECTRODE_MESH.get()));}var b=new SaltBridgeEntity(ModEntities.SALT_BRIDGE.get(),h.getLevel());b.connect(zn,0,cu,0,false);h.getLevel().addFreshEntity(b);return new GalvanicSystem.Pair(zn,cu,b);}
    private static ElectricWireEntity wire(GameTestHelper h,ElectroDeviceEntity a,int ap,ElectroDeviceEntity b,int bp){var w=new ElectricWireEntity(ModEntities.ELECTRIC_WIRE.get(),h.getLevel());w.connect(a,ap,b,bp,false);h.getLevel().addFreshEntity(w);return w;}
    private static SpeciesInventory inventory(GalvanicSystem.Pair p){return SolutionSpecies.analyticalSnapshot(p.zinc().stack()).plus(SolutionSpecies.analyticalSnapshot(p.copper().stack())).plus(new SpeciesInventory(Map.of("solid:zinc",ElectrodePartItem.grams(TroughSystem.part(p.zinc(),"mesh",0))/65.38,"solid:copper",(ElectrodePartItem.grams(TroughSystem.part(p.copper(),"mesh",0))+ElectrodePartItem.coating(TroughSystem.part(p.copper(),"mesh",0),"copper"))/63.546,"potassium",p.bridge().grams()/ChemicalGarden.mm("solid","potassium_nitrate"),"nitrate",p.bridge().grams()/ChemicalGarden.mm("solid","potassium_nitrate")),Map.of()));}
    public static void galvanicBalance(GameTestHelper h){h.runAtTickTime(1,()->{
        var p=pair(h,1,1);var load=device(h,ModItems.LAB_RESISTOR.get(),4,1);wire(h,p.copper(),0,load,0);wire(h,load,1,p.zinc(),0);
        var before=inventory(p);GalvanicSystem.tick(p.zinc());double q=p.zinc().number("charge_c",0);check(h,q>0&&load.current()>0,"closed Daniell circuit does not supply load");
        near(h,10-ElectrodePartItem.grams(TroughSystem.part(p.zinc(),"mesh",0)),q/(2*WaterElectrolysis.FARADAY)*65.38,1e-9,"zinc Faraday ledger");near(h,ElectrodePartItem.coating(TroughSystem.part(p.copper(),"mesh",0),"copper"),q/(2*WaterElectrolysis.FARADAY)*63.546,1e-9,"copper Faraday ledger");
        check(h,Conservation.compare(before,inventory(p)).conserved(),"electrode+bridge+solution total not conserved");
        GalvanicSystem.tick(p.zinc());near(h,p.zinc().number("charge_c",0),q,1e-12,"same tick duplicates charge");
        check(h,ElectricConnections.wires(p.zinc()).stream().noneMatch(e->e instanceof SaltBridgeEntity),"salt bridge conducts electrons");h.succeed();
    });}
    public static void galvanicOpenMeter(GameTestHelper h){h.runAtTickTime(1,()->{
        var p=pair(h,1,1);var before=inventory(p);GalvanicSystem.tick(p.zinc());check(h,p.zinc().current()==0,"open circuit current");
        var meter=device(h,ModItems.LAB_VOLTMETER.get(),4,1);wire(h,p.copper(),0,meter,0);wire(h,meter,1,p.zinc(),0);GalvanicSystem.tick(p.zinc());near(h,meter.number("measured_v",0),1.1,1e-9,"wrong emf");check(h,p.zinc().number("charge_c",0)==0&&Conservation.compare(before,inventory(p)).conserved(),"voltmeter consumes reactants");
        p.bridge().setGrams(0);GalvanicSystem.tick(p.zinc());check(h,p.zinc().current()==0,"depleted bridge conducts");h.succeed();
    });}
    public static void galvanicSeries(GameTestHelper h){h.runAtTickTime(1,()->{
        var a=pair(h,1,1);var b=pair(h,1,3);var cell=device(h,ModItems.HOFMANN_VOLTAMETER.get(),4,2);var s=cell.stack();add(s,"water",150);add(s,"sodium_hydroxide_solution",1);cell.setStack(s);
        wire(h,a.copper(),0,b.zinc(),0);wire(h,b.copper(),0,cell,0);wire(h,cell,1,a.zinc(),0);var before=inventory(a).plus(inventory(b));GalvanicSystem.tick(a.zinc());
        check(h,cell.gasMoles(1)>0,"series galvanic cells cannot drive Hofmann");near(h,a.zinc().number("charge_c",0),cell.number("charge_c",0),1e-9,"source and electrolysis charge differ");near(h,a.zinc().number("charge_c",0),b.zinc().number("charge_c",0),1e-9,"series charges differ");check(h,Conservation.compare(before,inventory(a).plus(inventory(b))).conserved(),"series batteries lose material");double q=cell.number("charge_c",0);GalvanicSystem.tick(b.zinc());near(h,cell.number("charge_c",0),q,1e-12,"second source double drives load");h.succeed();
    });}
    public static void galvanicBoundaries(GameTestHelper h){h.runAtTickTime(1,()->{
        var p=pair(h,1,1);var load=device(h,ModItems.LAB_RESISTOR.get(),4,1);wire(h,p.copper(),0,load,0);wire(h,load,1,p.zinc(),0);
        p.bridge().setGrams(1e-8);GalvanicSystem.tick(p.zinc());check(h,p.zinc().number("charge_c",0)>0,"tiny salt bridge fails bounded reaction");check(h,p.bridge().grams()<1e-12,"salt bridge not limiting");
        var s=p.copper().stack();LabVesselItem.clearContents(s);p.copper().setStack(s);p.bridge().setGrams(1);GalvanicSystem.tick(p.zinc());check(h,p.zinc().current()==0&&p.copper().current()==0,"dry half cell stays active");
        near(h,SaltBridgeItem.grams(p.bridge().toStack()),1,1e-12,"bridge item resets remaining");p.bridge().setGrams(.123456789);near(h,SaltBridgeItem.grams(p.bridge().toStack()),.123456789,1e-12,"partial bridge loses precision");h.succeed();
    });}
    private ExpansionGameTests(){}
}
