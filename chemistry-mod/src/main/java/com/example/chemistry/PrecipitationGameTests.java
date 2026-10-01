package com.example.chemistry;

import com.example.chemistry.filtration.Filtration;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.solution.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.Map;

public final class PrecipitationGameTests {
    private static final String RESIDUE="solid:silver_chloride";
    private static void check(GameTestHelper h,boolean ok,String message) {
        h.assertTrue(ok,Component.literal(message));
    }
    private static ItemStack vessel() { return new ItemStack(ModItems.ERLENMEYER_FLASK.get()); }
    private static double mm(String id) { return SpeciesCatalog.get("solid:"+id).molarMass(); }
    private static void settle(ItemStack vessel) {
        for(int i=0;i<400;i++)PhaseSystem.tick(vessel,25);
    }
    private static double quotient(ItemStack vessel) {
        var state=SolutionSpecies.analyticalSnapshot(vessel);
        double litres=SolutionSpecies.solutionLitres(vessel);
        return state.amount("silver")*state.amount("chloride")/(litres*litres);
    }
    private static ItemStack mixture(String metal) {
        var result=vessel();LabVesselItem.addMass(result,"liquid","water",100);
        LabVesselItem.addMass(result,"solid","silver_nitrate",mm("silver_nitrate")*.001);
        LabVesselItem.addMass(result,"solid",metal+"_chloride",mm(metal+"_chloride")*.001);
        return result;
    }
    public static void solver(GameTestHelper h) { h.runAtTickTime(1,()->{
        var initial=new SpeciesInventory(Map.of("water",5.,"silver",.001,"chloride",.001,
                "sodium",.001,"nitrate",.001),Map.of());
        var result=SolubilityEquilibrium.solve(initial,RESIDUE,"silver","chloride",1.6e-10,.1);
        check(h,result.amount(RESIDUE)>0,"supersaturated ions did not precipitate");
        check(h,Math.abs(result.amount("silver")*result.amount("chloride")/.01/1.6e-10-1)<1e-8,"Q differs from Ksp");
        check(h,Conservation.compare(initial,result).conserved(),"solver changed spectators, elements or charge");
        var repeated=SolubilityEquilibrium.solve(result,RESIDUE,"silver","chloride",1.6e-10,.1);
        check(h,Math.abs(repeated.amount(RESIDUE)-result.amount(RESIDUE))<1e-15,"solver not idempotent");
        var dilute=SolubilityEquilibrium.solve(result,RESIDUE,"silver","chloride",1.6e-10,.2);
        check(h,dilute.amount(RESIDUE)<result.amount(RESIDUE),"dilution did not dissolve precipitate");
        var common=SolubilityEquilibrium.solve(result.plus(new SpeciesInventory(Map.of("sodium",.01,"chloride",.01),Map.of())),RESIDUE,"silver","chloride",1.6e-10,.1);
        check(h,common.amount(RESIDUE)>result.amount(RESIDUE),"common ion did not suppress dissolution");
        var tiny=new SpeciesInventory(Map.of("water",5.,RESIDUE,1e-8),Map.of());
        var dissolved=SolubilityEquilibrium.solve(tiny,RESIDUE,"silver","chloride",1.6e-10,.1);
        check(h,dissolved.amount(RESIDUE)==0&&dissolved.amount("silver")==1e-8,"limited solid should dissolve completely");
        for(double invalid:new double[]{Double.NaN,-1,Double.POSITIVE_INFINITY}) {
            boolean rejected=false;
            try {SolubilityEquilibrium.solve(initial,RESIDUE,"silver","chloride",1.6e-10,invalid);}
            catch(IllegalArgumentException expected){rejected=true;}
            check(h,rejected,"invalid volume accepted");
        }
        h.succeed();
    }); }
    public static void vesselDynamics(GameTestHelper h) { h.runAtTickTime(1,()->{
        for(String metal:new String[]{"sodium","potassium"}) {
            var vessel=mixture(metal);var before=SolutionSpecies.analyticalSnapshot(vessel);
            settle(vessel);var after=SolutionSpecies.analyticalSnapshot(vessel);
            check(h,after.amount(RESIDUE)>.0009,"vessel did not produce white precipitate");
            check(h,Conservation.compare(before,after).conserved(),"vessel precipitation lost mass or counterions");
            check(h,Math.abs(quotient(vessel)/PrecipitationEquilibrium.SILVER_CHLORIDE_KSP-1)<.002,"vessel not saturated");
            double original=after.amount(RESIDUE);
            LabVesselItem.addMass(vessel,"liquid","water",100);settle(vessel);
            check(h,SolutionSpecies.analyticalSnapshot(vessel).amount(RESIDUE)<original,"added water did not dissolve solid");
            LabVesselItem.addMass(vessel,"liquid",metal+"_chloride_solution",mm(metal+"_chloride")*.002);settle(vessel);
            check(h,SolutionSpecies.analyticalSnapshot(vessel).amount(RESIDUE)>original,"added chloride did not shift equilibrium");
            var copy=vessel.copy();settle(copy);
            check(h,Conservation.compare(SolutionSpecies.analyticalSnapshot(vessel),SolutionSpecies.analyticalSnapshot(copy)).conserved(),"copied vessel changed analytical totals");
            check(h,!ReactionEngine.checkAndStart(vessel,null),"legacy reaction competes with precipitation solver");
        }
        var small=vessel();LabVesselItem.addMass(small,"liquid","water",100);
        LabVesselItem.addMass(small,"solid","silver_chloride",.00001);settle(small);
        check(h,SolutionSpecies.analyticalSnapshot(small).amount(RESIDUE)<1e-10,"tiny residue did not dissolve");
        check(h,quotient(small)<PrecipitationEquilibrium.SILVER_CHLORIDE_KSP,"solid-free solution falsely forced to saturation");
        h.succeed();
    }); }
    public static void filtration(GameTestHelper h) { h.runAtTickTime(1,()->{
        var source=mixture("sodium");settle(source);
        var start=SolutionSpecies.analyticalSnapshot(source);
        var pending=new ItemStack(ModItems.FILTER_FUNNEL.get());
        var paper=new ItemStack(ModItems.USED_FILTER_PAPER.get());var receiver=vessel();
        check(h,Filtration.pour(source,pending,100),"mixture could not enter filter");
        for(int i=0;i<300;i++)Filtration.step(pending,paper,receiver);
        var combined=SolutionSpecies.analyticalSnapshot(source).plus(SolutionSpecies.analyticalSnapshot(pending))
                .plus(SolutionSpecies.analyticalSnapshot(paper)).plus(SolutionSpecies.analyticalSnapshot(receiver));
        check(h,Conservation.compare(start,combined).conserved(),"filter lost precipitate or dissolved ions");
        check(h,SolutionSpecies.analyticalSnapshot(paper).amount(RESIDUE)>0,"paper did not retain AgCl");
        check(h,SolutionSpecies.analyticalSnapshot(receiver).amount(RESIDUE)==0,"solid passed through filter");
        check(h,Math.abs(quotient(receiver)/PrecipitationEquilibrium.SILVER_CHLORIDE_KSP-1)<.002,"removal of residue changed saturated mother liquor");
        LabVesselItem.addMass(receiver,"liquid","water",50);settle(receiver);
        check(h,SolutionSpecies.analyticalSnapshot(receiver).amount(RESIDUE)==0,"diluted solid-free filtrate invented new solid");
        var redissolve=vessel();LabVesselItem.addMass(redissolve,"liquid","water",100);
        LabVesselItem.addMass(redissolve,"solid","silver_chloride",.00001);settle(redissolve);
        check(h,SolutionSpecies.analyticalSnapshot(redissolve).amount("silver")>0,"isolated residue cannot redissolve");
        h.succeed();
    }); }
    public static void feedback(GameTestHelper h) { h.runAtTickTime(1,()->{
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        check(h,!ExperimentFeedback.send(player,Component.literal("test")),"feedback visible without goggles");
        player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.CHEM_GOGGLES.get()));
        check(h,ExperimentFeedback.send(player,Component.literal("test")),"goggles did not enable feedback");
        player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
        check(h,!ExperimentFeedback.send(player,Component.literal("test")),"feedback stayed visible after goggles removal");
        var lines=new java.util.ArrayList<Component>();var sample=mixture("sodium");settle(sample);
        PrecipitationEquilibrium.appendInfo(lines,sample,true);
        check(h,lines.stream().anyMatch(c->c.getString().contains("Ksp")),"equilibrium feedback missing");
        h.succeed();
    }); }
    private static ItemStack calciumMixture(String metal) {
        var sample=vessel();LabVesselItem.addMass(sample,"liquid","water",100);
        LabVesselItem.addMass(sample,"solid","calcium_chloride",mm("calcium_chloride")*.003);
        LabVesselItem.addMass(sample,"solid",metal+"_oxalate",mm(metal+"_oxalate")*.003);
        return sample;
    }
    private static double calciumQuotient(ItemStack sample) {
        var state=SolutionSpecies.analyticalSnapshot(sample);
        double litres=SolutionSpecies.solutionLitres(sample);
        return state.amount("calcium")*state.amount("oxalate")/(litres*litres);
    }
    public static void calciumDynamics(GameTestHelper h) { h.runAtTickTime(1,()->{
        for(String metal:new String[]{"sodium","potassium"}) {
            var sample=calciumMixture(metal);var start=SolutionSpecies.analyticalSnapshot(sample);
            check(h,!ReactionEngine.checkAndStart(sample,null),"raw wet salts started legacy precipitation");
            settle(sample);var state=SolutionSpecies.analyticalSnapshot(sample);
            check(h,state.amount("solid:calcium_oxalate")>.0029,"calcium oxalate did not precipitate");
            check(h,Conservation.compare(start,state).conserved(),"Ca/oxalate/counterion mass or charge changed");
            check(h,Math.abs(calciumQuotient(sample)/PrecipitationEquilibrium.CALCIUM_OXALATE_KSP-1)<.002,"calcium Q differs from Ksp");
            double original=state.amount("solid:calcium_oxalate");
            LabVesselItem.addMass(sample,"liquid","water",100);settle(sample);
            check(h,SolutionSpecies.analyticalSnapshot(sample).amount("solid:calcium_oxalate")<original,"calcium dilution did not dissolve solid");
            LabVesselItem.addMass(sample,"liquid","calcium_chloride_solution",mm("calcium_chloride")*.003);settle(sample);
            check(h,SolutionSpecies.analyticalSnapshot(sample).amount("solid:calcium_oxalate")>original,"calcium common ion did not shift equilibrium");
            check(h,!ReactionEngine.checkAndStart(sample,null),"legacy calcium precipitation competes with Ksp");
            var lines=new java.util.ArrayList<Component>();PrecipitationEquilibrium.appendInfo(lines,sample,true);
            check(h,lines.stream().anyMatch(c->c.getString().contains("Ca²⁺")),"calcium equilibrium feedback missing");
            var reload=vessel();reload.applyComponents(sample.getComponents());settle(reload);
            check(h,Conservation.compare(SolutionSpecies.analyticalSnapshot(sample),SolutionSpecies.analyticalSnapshot(reload)).conserved(),"restored contents changed analytical totals");
        }
        var tiny=vessel();LabVesselItem.addMass(tiny,"liquid","water",100);
        LabVesselItem.addMass(tiny,"solid","calcium_oxalate",.0001);settle(tiny);
        check(h,SolutionSpecies.analyticalSnapshot(tiny).amount("solid:calcium_oxalate")<1e-10,"limited calcium residue did not dissolve");
        check(h,calciumQuotient(tiny)<PrecipitationEquilibrium.CALCIUM_OXALATE_KSP,"limited solid forced saturation");
        var before=SolutionSpecies.analyticalSnapshot(tiny);
        LabVesselItem.consumeMass(tiny,"liquid","water",100);PhaseSystem.tick(tiny,25);
        var dry=SolutionSpecies.analyticalSnapshot(tiny);
        check(h,dry.amount("solid:calcium_oxalate")>0 && dry.amount("oxalate")==0,"dry dissolved fraction did not return to solid");
        check(h,Conservation.compare(before,new SpeciesInventory(Map.of("water",before.amount("water")),Map.of()).plus(dry)).conserved(),"dry residue lost mass");
        h.succeed();
    }); }
    public static void calciumFiltration(GameTestHelper h) { h.runAtTickTime(1,()->{
        var source=calciumMixture("sodium");settle(source);var start=SolutionSpecies.analyticalSnapshot(source);
        var pending=new ItemStack(ModItems.FILTER_FUNNEL.get());
        var paper=new ItemStack(ModItems.USED_FILTER_PAPER.get());var receiver=vessel();
        check(h,Filtration.pour(source,pending,100),"calcium mixture could not enter filter");
        for(int i=0;i<300;i++)Filtration.step(pending,paper,receiver);
        var total=SolutionSpecies.analyticalSnapshot(source).plus(SolutionSpecies.analyticalSnapshot(pending))
                .plus(SolutionSpecies.analyticalSnapshot(paper)).plus(SolutionSpecies.analyticalSnapshot(receiver));
        check(h,Conservation.compare(start,total).conserved(),"calcium filter lost matter");
        check(h,SolutionSpecies.analyticalSnapshot(paper).amount("solid:calcium_oxalate")>0,"filter did not retain calcium residue");
        check(h,SolutionSpecies.analyticalSnapshot(receiver).amount("solid:calcium_oxalate")==0,"calcium solid passed filter");
        check(h,Math.abs(calciumQuotient(receiver)/PrecipitationEquilibrium.CALCIUM_OXALATE_KSP-1)<.002,"calcium mother liquor changed");
        LabVesselItem.addMass(receiver,"liquid","water",50);settle(receiver);
        check(h,SolutionSpecies.analyticalSnapshot(receiver).amount("solid:calcium_oxalate")==0,"dilute filtrate invented precipitate");
        h.succeed();
    }); }
    public static void solubleLegacy(GameTestHelper h) { h.runAtTickTime(1,()->{
        var sample=vessel();LabVesselItem.addMass(sample,"liquid","water",100);
        double calciumMass=.003*com.example.chemistry.data.ChemicalInfoProvider.molarMassOf("solid_calcium_chloride");
        LabVesselItem.addMass(sample,"liquid","calcium_chloride_solution",calciumMass);
        LabVesselItem.addMass(sample,"liquid","sodium_carbonate_solution",.003*
                com.example.chemistry.data.ChemicalInfoProvider.molarMassOf("liquid_sodium_carbonate_solution"));
        check(h,!PrecipitationEquilibrium.active(sample),"unsupported competing carbonate claimed by Ksp adapter");
        check(h,ReactionEngine.checkAndStart(sample,null),"dissolved calcium not recognized by legacy carbonate reaction");
        int completed=0;
        for(int i=0;i<180;i++)if(ReactionEngine.tickResult(sample,null)!=null)completed++;
        check(h,completed==1,"dissolved reactants generated repeated legacy products");
        double remaining=LabVesselItem.getContents(sample).stream().filter(e->e.id().equals("calcium_chloride_solution"))
                .mapToDouble(LabVesselItem.Entry::amount).sum();
        check(h,remaining<.001,"legacy reaction failed to consume actual liquid reactant");
        check(h,LabVesselItem.getContents(sample).stream().anyMatch(e->e.id().equals("calcium_carbonate")&&e.amount()>.2),"legacy carbonate product absent");
        var dry=calciumMixture("sodium");LabVesselItem.consumeMass(dry,"liquid","water",100);
        check(h,ReactionEngine.checkAndStart(dry,null),"dry legacy calcium reaction unexpectedly disabled");
        var heated=vessel();LabVesselItem.addMass(heated,"solid","calcium_oxalate",1);
        TemperatureSystem.setTemp(heated,450);
        check(h,ReactionEngine.checkAndStart(heated,null),"calcium thermal decomposition unexpectedly disabled");
        h.succeed();
    }); }
    private PrecipitationGameTests() {}
}
