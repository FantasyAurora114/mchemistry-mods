package com.example.chemistry;
import com.example.chemistry.item.*;
import com.example.chemistry.registry.*;
import com.example.chemistry.electrical.*;
import com.example.chemistry.solution.*;
import com.example.chemistry.data.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.core.*;
import net.minecraft.world.level.GameType;
import java.util.*;
public final class LabUpgradeGameTests {
    private static void check(GameTestHelper h,boolean ok,String msg){h.assertTrue(ok,Component.literal(msg));}
    private static void near(GameTestHelper h,double a,double b,String msg){check(h,Math.abs(a-b)<1e-8,msg+": "+a+" / "+b);}
    private static ItemStack cup(){return new ItemStack(ModItems.ERLENMEYER_FLASK.get());}
    private static ElectroDeviceEntity device(GameTestHelper h,boolean source,int x){var e=new ElectroDeviceEntity(ModEntities.ELECTRO_DEVICE.get(),h.getLevel());var s=new ItemStack(source?ModItems.BENCH_POWER_SUPPLY.get():ModItems.HOFMANN_VOLTAMETER.get());if(!source){LabVesselItem.addMass(s,"liquid","water",150);LabVesselItem.addMass(s,"liquid","sodium_hydroxide_solution",1);}e.setStack(s);var p=h.absolutePos(new BlockPos(x,1,2));e.setPos(p.getX()+.5,p.getY(),p.getZ()+.5);h.getLevel().addFreshEntity(e);return e;}
    private static void wire(GameTestHelper h,ElectroDeviceEntity a,int ap,ElectroDeviceEntity b,int bp){var w=new ElectricWireEntity(ModEntities.ELECTRIC_WIRE.get(),h.getLevel());w.connect(a,ap,b,bp,false);h.getLevel().addFreshEntity(w);}
    public static void series(GameTestHelper h){h.runAtTickTime(1,()->{
        var source=device(h,true,1);var a=device(h,false,2);var b=device(h,false,3);var c=device(h,false,4);
        wire(h,source,0,a,0);wire(h,a,1,b,0);wire(h,b,1,source,1);wire(h,source,2,c,1);wire(h,c,0,source,3);
        source.flag("on",true);source.tickPower();
        check(h,ElectricConnections.series(source,0).size()==2,"series circuit not discovered");
        check(h,a.current()>0&&b.current()>0&&c.current()>0,"both channels must conduct");
        near(h,a.number("charge_c",0),b.number("charge_c",0),"series charge mismatch");
        check(h,c.gas(1).equals("oxygen")&&c.gas(0).equals("hydrogen"),"CH2 reverse polarity wrong");
        check(h,source.channelCurrent(1)>0,"CH2 current missing");
        a.discard();b.discard();c.discard();source.discard();h.succeed();
    });}
    public static void mixtureAndSampling(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=cup();LabVesselItem.addMass(s,"liquid","crude_saltwater",100);
        near(h,LabVesselItem.getContents(s).stream().mapToDouble(LabVesselItem.Entry::amount).sum(),100,"crude mixture mass");
        check(h,LabVesselItem.getContents(s).stream().noneMatch(e->e.id().equals("crude_saltwater")),"mixture not expanded");
        PhaseSystem.dissolveAndCrystallize(s,false);
        check(h,LabVesselItem.getContents(s).stream().anyMatch(e->e.id().equals("silicon_dioxide")&&e.type().equals("solid")),"sand dissolved");
        var p=h.makeMockPlayer(GameType.SURVIVAL);var rod=new ItemStack(ModItems.GLASS_ROD.get());double before=LabVesselItem.getContents(s).stream().mapToDouble(LabVesselItem.Entry::amount).sum();
        check(h,GlassRodSampling.dip(p,rod,s),"sampling failed");double after=LabVesselItem.getContents(s).stream().mapToDouble(LabVesselItem.Entry::amount).sum();check(h,after<before&&before-after<.1,"sample mass not removed");
        var paperItem=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","ph_test_paper"));var paper=new ItemStack(paperItem,2);
        check(h,GlassRodSampling.test(p,rod,paper),"paper test failed");check(h,paper.getCount()==1,"paper consumption wrong");check(h,!GlassRodSampling.test(p,rod,paper),"sample reusable after consumed");h.succeed();
    });}
    public static void equilibriumDynamics(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=cup();LabVesselItem.addMass(s,"liquid","water",100);LabVesselItem.addMass(s,"liquid","iron_chloride_solution",.0162205);LabVesselItem.addMass(s,"liquid","potassium_thiocyanate_solution",.00971763);
        var totals=SolutionSpecies.analyticalSnapshot(s);double target=CoordinationEquilibrium.solve(totals,SolutionSpecies.solutionLitres(s)).amount("iron_thiocyanate");
        CoordinationEquilibrium.tick(s);double first=SolutionSpecies.snapshot(s).amount("iron_thiocyanate");check(h,first>0&&first<target,"equilibrium did not relax gradually");
        for(int i=0;i<500;i++)CoordinationEquilibrium.tick(s);near(h,SolutionSpecies.snapshot(s).amount("iron_thiocyanate"),target,"target not reached");
        LabVesselItem.addMass(s,"liquid","water",100);CoordinationEquilibrium.tick(s);check(h,SolutionSpecies.snapshot(s).amount("iron_thiocyanate")<target,"dilution did not dissociate");check(h,Conservation.compare(SolutionSpecies.analyticalSnapshot(s),SolutionSpecies.snapshot(s)).conserved(),"equilibrium created material");h.succeed();
    });}
    public static void chemistryAndHazards(GameTestHelper h){h.runAtTickTime(1,()->{
        var silver=Reactions.ALL.stream().filter(r->r.display().startsWith("3Ag + 4HNO")).findFirst().orElseThrow();check(h,silver.concentration().equals("dilute"),"silver dilute shadows concentrated branch");
        for(String base:List.of("sodium","potassium"))check(h,Reactions.ALL.stream().anyMatch(r->r.reactants().stream().anyMatch(e->e.id().equals(base+"_hydroxide_solution"))&&r.products().stream().anyMatch(e->e.id().equals(base+"_chlorate"))),"missing aqueous chlorate reaction");
        var sodium=Reactions.ALL.stream().filter(r->r.display().startsWith("2Na + 2H₂O")).findFirst().orElseThrow();var s=cup();LabVesselItem.addMass(s,"liquid","water",100);
        near(h,ContainerHazards.burstStrength(s,sodium,.0001),0,"trace sodium exploded");check(h,ContainerHazards.burstStrength(s,sodium,.2)>0,"large sodium charge did not rupture");check(h,com.example.chemistry.api.goggles.ChemGoggleLines.gasName("water_vapor").equals("水蒸气"),"raw vapor label");h.succeed();
    });}
    public static void nitricAndChlorine(GameTestHelper h){h.runAtTickTime(1,()->{
        for(String metal:List.of("silver","copper"))for(boolean concentrated:List.of(false,true)){
            var s=cup();LabVesselItem.addMass(s,"solid",metal,1);LabVesselItem.addMass(s,"liquid",concentrated?"nitric_acid_concentrated":"nitric_acid",5);
            check(h,ReactionEngine.checkAndStart(s,null),"nitric reaction not detected: "+metal+" concentrated="+concentrated);ReactionEngine.Completion completed=null;
            for(int n=0;n<500&&completed==null;n++)completed=ReactionEngine.tickResult(s,null);
            check(h,completed!=null,"nitric reaction did not finish");check(h,completed.reaction().display().contains(concentrated?"NO₂↑":"NO↑"),"wrong nitric acid gas");
        }
        for(String base:List.of("sodium","potassium"))for(boolean hot:List.of(false,true)){
            var s=cup();LabVesselItem.addMass(s,"liquid","water",10);LabVesselItem.addMass(s,"liquid",base+"_hydroxide_solution",5);LabVesselItem.addMass(s,"gas","chlorine",1);TemperatureSystem.setTemp(s,hot?90:25);
            check(h,ReactionEngine.checkAndStart(s,null),"chlorine reaction not detected");ReactionEngine.Completion completed=null;for(int n=0;n<500&&completed==null;n++)completed=ReactionEngine.tickResult(s,null);
            check(h,completed!=null,"chlorine reaction did not finish");String product=base+(hot?"_chlorate":"_hypochlorite");check(h,completed.reaction().products().stream().anyMatch(e->e.id().equals(product)),"wrong hot/cold product");
        }
        h.succeed();
    });}
    public static void rackAndBottles(GameTestHelper h){h.runAtTickTime(1,()->{
        var pos=new BlockPos(2,1,2);h.setBlock(pos,ModBlocks.TEST_TUBE_RACK.get());var absolute=h.absolutePos(pos);var rack=(com.example.chemistry.blockentity.TestTubeRackBlockEntity)h.getLevel().getBlockEntity(absolute);
        var tube=new ItemStack(ModItems.TEST_TUBES.getFirst().get());rack.setTube(0,tube,false);var state=h.getLevel().getBlockState(absolute);
        var shape=state.getShape(h.getLevel(),absolute);check(h,shape.bounds().maxY>=rack.slotBox(0).maxY,"outline misses tube");
        var target=rack.slotBox(0).getCenter().add(0,.14,0);var from=target.add(0,0,-2);var to=target.add(0,0,1);
        check(h,shape.clip(from.add(absolute.getX(),absolute.getY(),absolute.getZ()),to.add(absolute.getX(),absolute.getY(),absolute.getZ()),absolute)!=null,"tube cannot be ray selected");
        var bottle=new com.example.chemistry.entity.PlacedReagentBottleEntity(ModEntities.PLACED_REAGENT_BOTTLE.get(),h.getLevel());var stack=new ItemStack(ModItems.LIQUID_BOTTLE.get());com.example.chemistry.transfer.BottleCodes.setLiquid(stack,"water",true,100);bottle.setStack(stack);bottle.setPos(absolute.getX()+.2,absolute.getY(),absolute.getZ()+.2);bottle.setYRot(37);
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess());bottle.saveWithoutId(out);
        var loaded=new com.example.chemistry.entity.PlacedReagentBottleEntity(ModEntities.PLACED_REAGENT_BOTTLE.get(),h.getLevel());loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        near(h,loaded.getYRot(),37,"bottle angle not saved");check(h,ItemStack.matches(loaded.toStack(),bottle.toStack()),"bottle contents not saved");h.succeed();
    });}

}
