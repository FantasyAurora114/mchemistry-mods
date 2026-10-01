package com.example.chemistry.solution;
import java.util.Map;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.data.BatchChemicals;
import com.example.chemistry.data.BatchReactions;
import com.example.chemistry.PhaseSystem;
import com.example.chemistry.TemperatureSystem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.gametest.framework.GameTestHelper;
public final class BatchGameTests {
    private static ItemStack vessel(){return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mchemistry","beaker_medium")));}
    private static void add(ItemStack s,String id,double grams){LabVesselItem.addMass(s,"liquid",id,grams);}
    private static void check(boolean v,String message){if(!v)throw new IllegalStateException(message);}
    public static void formulas(GameTestHelper h){h.runAtTickTime(1,()->{
        for(var r:BatchChemicals.ALL){check(Math.abs(BatchChemistry.molar("solid",r.id())-r.mass())<1e-7,"formula mass "+r.id());}
        for(var reaction:BatchReactions.ALL){
            var s=vessel();add(s,"water",100);var lhs=new java.util.HashMap<String,Integer>();var rhs=new java.util.HashMap<String,Integer>();
            reaction.reactants().forEach(e->{lhs.put(e.type()+":"+e.id(),e.coefficient());LabVesselItem.addMass(s,e.type(),e.id(),e.coefficient()*BatchChemistry.molar(e.type(),e.id())*.001);});
            reaction.products().forEach(e->rhs.put(e.type()+":"+e.id(),e.coefficient()));
            check(BatchChemistry.transact(s,lhs,rhs,.0005),"reaction inactive "+reaction.display());
            check(SolutionSpecies.analyticalSnapshot(s).fullyModelled(),"unmapped reaction "+reaction.display());
        }h.succeed();
    });}
    public static void cobalt(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=vessel();add(s,"water",100);add(s,"cobalt_chloride_solution",1);add(s,"hydrochloric_acid",12);
        var total=SolutionSpecies.analyticalSnapshot(s);double l=SolutionSpecies.solutionLitres(s);
        double cold=BatchCoordination.cobaltTarget(total,l,20),hot=BatchCoordination.cobaltTarget(total,l,80),dilute=BatchCoordination.cobaltTarget(total,l*2,20);
        check(hot>cold&&dilute<cold,"cobalt temperature/dilution direction");
        check(Conservation.compare(total,SolutionSpecies.snapshot(s)).conserved(),"complex duplicates chloride");
        for(int i=0;i<100;i++)BatchCoordination.tick(s);
        LabVesselItem.consumeMass(s,"liquid","cobalt_chloride_solution",1);check(SolutionSpecies.snapshot(s).amount("cobalt_tetrachloride")==0,"stale cobalt partition");h.succeed();
    });}
    public static void nickel(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=vessel();add(s,"water",100);add(s,"nickel_chloride_solution",1);add(s,"ethylenediamine_solution",5);add(s,"ammonia_water",3);
        var total=SolutionSpecies.analyticalSnapshot(s);var view=SolutionSpecies.snapshot(s);
        check(view.amount("nickel_en")>0&&view.amount("nickel_ammine")>0,"nickel ligands inactive");
        check(view.amount("nickel_en")+view.amount("nickel_ammine")<=total.amount("nickel_ii")+1e-12,"ligands double nickel");
        check(Conservation.compare(total,view).conserved(),"nickel complex atom/charge loss");h.succeed();
    });}
    public static void precipitation(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=vessel();add(s,"water",100);add(s,"nickel_chloride_solution",1);add(s,"dimethylglyoxime_solution",1);add(s,"sodium_hydroxide_solution",.65);
        var before=SolutionSpecies.analyticalSnapshot(s);for(int i=0;i<100;i++)BatchChemistry.tick(s);
        check(BatchChemistry.mass(s,"solid","nickel_dimethylglyoximate")>0,"Ni-DMG red precipitate missing");check(Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"precipitation loses atoms");h.succeed();
    });}
    public static void amphoteric(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=vessel();add(s,"water",100);LabVesselItem.addMass(s,"solid","zinc_hydroxide",1);add(s,"sodium_hydroxide_solution",2);
        var before=SolutionSpecies.analyticalSnapshot(s);for(int i=0;i<100;i++)BatchChemistry.tick(s);
        check(BatchChemistry.mass(s,"liquid","sodium_tetrahydroxozincate_solution")>0,"excess alkali fails");check(Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"amphoteric loss");
        add(s,"hydrochloric_acid_concentrated",3);for(int i=0;i<200;i++)BatchChemistry.tick(s);check(BatchChemistry.mass(s,"liquid","zinc_chloride_solution")>0,"acid return fails");h.succeed();
    });}
    public static void hydrates(GameTestHelper h){h.runAtTickTime(1,()->{
        for(var r:BatchChemicals.ALL){if(r.waters()==0)continue;
            var s=vessel();add(s,"water",100);LabVesselItem.addMass(s,"solid",r.id(),1);var before=SolutionSpecies.analyticalSnapshot(s);
            for(int i=0;i<5;i++)HydrateChemistry.dissolve(s);
            check(Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"hydrate dissolution loss "+r.id());
            check(BatchChemistry.mass(s,"solid",r.id())<1,"hydrate will not dissolve "+r.id());
            var dry=vessel();LabVesselItem.addMass(dry,"solid",r.id(),1);before=SolutionSpecies.analyticalSnapshot(dry);
            double point=HydrateChemistry.transition(r.id());double temperature=PhaseSystem.applyHeatJoules(dry,point,1);
            check(Math.abs(temperature-point)<1e-8,"dehydration plateau broken");check(Conservation.compare(before,SolutionSpecies.analyticalSnapshot(dry)).conserved(),"dehydration loses water");
        }h.succeed();
    });}
    public static void crystallization(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=vessel();add(s,"water",10);add(s,"sodium_sulfate_solution",5);var before=SolutionSpecies.analyticalSnapshot(s);
        for(int i=0;i<100;i++){HydrateChemistry.dissolve(s);PhaseSystem.dissolveAndCrystallize(s,false);}
        check(BatchChemistry.mass(s,"solid","sodium_sulfate_decahydrate")>0,"no hydrated crystals");check(BatchChemistry.mass(s,"solid","sodium_sulfate")==0,"generic crystallizer steals hydration");
        check(Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"crystals manufacture water");h.succeed();
    });}
    public static void indicators(GameTestHelper h){h.runAtTickTime(1,()->{
        for(String indicator:java.util.List.of("methyl_red","bromothymol_blue","bromocresol_green","thymol_blue")){
            var acid=vessel();add(acid,"water",100);add(acid,indicator+"_solution",.01);add(acid,"hydrochloric_acid",.01);
            var base=vessel();add(base,"water",100);add(base,indicator+"_solution",.01);add(base,"sodium_hydroxide_solution",.01);
            check(AcidBaseEquilibrium.read(acid)!=null&&AcidBaseEquilibrium.read(base)!=null,"indicator disables pH");check(LabVesselItem.contentsColor(acid)!=LabVesselItem.contentsColor(base),"indicator colour static");
        }h.succeed();
    });}
    public static void chromate(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=vessel();add(s,"water",100);add(s,"potassium_chromate_solution",1);add(s,"hydrochloric_acid",.5);var before=SolutionSpecies.analyticalSnapshot(s);
        for(int i=0;i<100;i++)BatchChemistry.tick(s);check(BatchChemistry.mass(s,"liquid","potassium_dichromate_solution")>0,"acid chromate shift fails");check(Conservation.compare(before,SolutionSpecies.analyticalSnapshot(s)).conserved(),"chromate atom loss");
        add(s,"sodium_hydroxide_solution",2);for(int i=0;i<200;i++)BatchChemistry.tick(s);check(BatchChemistry.mass(s,"liquid","potassium_chromate_solution")>0,"alkali chromate shift fails");h.succeed();
    });}
    public static void solubility(GameTestHelper h){h.runAtTickTime(1,()->{
        var pure=vessel();add(pure,"water",100);LabVesselItem.addMass(pure,"solid","lead_iodide",1);
        var common=pure.copy();add(common,"potassium_iodide_solution",1);
        var before=SolutionSpecies.analyticalSnapshot(pure);for(int i=0;i<100;i++){BatchPrecipitation.tick(pure);BatchPrecipitation.tick(common);}
        check(BatchChemistry.mass(pure,"liquid","lead_iodide_solution")>BatchChemistry.mass(common,"liquid","lead_iodide_solution"),"common ion effect absent");
        check(Conservation.compare(before,SolutionSpecies.analyticalSnapshot(pure)).conserved(),"Ksp dissolution loses atoms");
        var trace=vessel();add(trace,"water",100);add(trace,"nickel_chloride_solution",1e-9);add(trace,"sodium_hydroxide_solution",1e-9);BatchChemistry.tick(trace);
        check(BatchChemistry.mass(trace,"solid","nickel_hydroxide")==0,"precipitates below Ksp");h.succeed();
    });}
    private BatchGameTests(){}
}
