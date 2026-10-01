package com.example.chemistry.filtration;
import java.util.*;
import com.example.chemistry.*;
import com.example.chemistry.entity.*;
import com.example.chemistry.item.*;
import com.example.chemistry.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
public final class FiltrationGameTests {
    private static void check(GameTestHelper h,boolean ok,String message){h.assertTrue(ok,Component.literal(message));}
    private static void near(GameTestHelper h,double a,double b,String message){check(h,Math.abs(a-b)<1e-7,message+" "+a+" != "+b);}
    private static double mass(ItemStack s,String id){return LabVesselItem.getContents(s).stream().filter(e->e.id().equals(id)).mapToDouble(LabVesselItem.Entry::amount).sum();}
    private static ItemStack cup(){return new ItemStack(BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath("mchemistry","beaker_medium")));}
    public static void balance(GameTestHelper h){h.runAtTickTime(1,()->{
        var from=new ItemStack(ModItems.FILTER_FUNNEL.get());var paper=new ItemStack(ModItems.USED_FILTER_PAPER.get());var to=cup();
        LabVesselItem.addMass(from,"liquid","water",80);LabVesselItem.addMass(from,"liquid","sodium_chloride_solution",5);LabVesselItem.addMass(from,"solid","calcium_oxalate",2);
        for(int i=0;i<300;i++)Filtration.step(from,paper,to);
        near(h,mass(paper,"calcium_oxalate"),2,"precipitate not retained");near(h,mass(to,"calcium_oxalate"),0,"solid leaked");
        near(h,mass(from,"water")+mass(paper,"water")+mass(to,"water"),80,"water balance");near(h,mass(from,"sodium_chloride_solution")+mass(paper,"sodium_chloride_solution")+mass(to,"sodium_chloride_solution"),5,"dissolved salt balance");
        check(h,mass(to,"sodium_chloride_solution")>4.9&&mass(paper,"water")>0,"solute or retained wet cake incorrect");
        var recovery=cup();check(h,Filtration.pour(paper,recovery,Double.MAX_VALUE),"wet cake recovery failed");near(h,mass(recovery,"calcium_oxalate"),2,"cake lost on recovery");
        var noPaper=new ItemStack(ModItems.FILTER_FUNNEL.get());LabVesselItem.addMass(noPaper,"liquid","water",5);LabVesselItem.addMass(noPaper,"solid","calcium_oxalate",1);var bypass=cup();
        for(int i=0;i<20;i++)Filtration.step(noPaper,ItemStack.EMPTY,bypass);near(h,mass(bypass,"calcium_oxalate"),1,"unfiltered funnel separated solid");
        var full=cup();LabVesselItem.addMass(full,"liquid","water",500);var pending=new ItemStack(ModItems.FILTER_FUNNEL.get());LabVesselItem.addMass(pending,"liquid","water",5);
        near(h,Filtration.step(pending,new ItemStack(ModItems.USED_FILTER_PAPER.get()),full),0,"full receiver not paused");near(h,mass(pending,"water"),5,"paused filter consumed water");
        var overloaded=new ItemStack(ModItems.FILTER_FUNNEL.get());var clogged=new ItemStack(ModItems.USED_FILTER_PAPER.get());var collected=cup();LabVesselItem.addMass(overloaded,"liquid","water",80);LabVesselItem.addMass(overloaded,"solid","calcium_oxalate",30);
        for(int i=0;i<800;i++)Filtration.step(overloaded,clogged,collected);
        near(h,Filtration.solids(clogged),20,"clog limit");near(h,Filtration.solids(overloaded),10,"clog discarded excess");h.succeed();
    });}
    public static void lifecycle(GameTestHelper h){h.runAtTickTime(1,()->{
        var stand=new IronStandEntity(ModEntities.IRON_STAND.get(),h.getLevel());var pos=h.absolutePos(new BlockPos(2,1,2));stand.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);h.getLevel().addFreshEntity(stand);
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(stand.position().add(0,0,-2));player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.FILTER_FUNNEL.get()));
        stand.interact(player,InteractionHand.MAIN_HAND);var f=FilterConnections.attached(stand);check(h,f!=null&&player.getMainHandItem().isEmpty(),"stand installation failed");
        var liquid=f.contents();LabVesselItem.addMass(liquid,"liquid","water",30);LabVesselItem.addMass(liquid,"solid","calcium_oxalate",1);var initial=com.example.chemistry.solution.SolutionSpecies.analyticalSnapshot(liquid);f.contents(liquid);f.paper(new ItemStack(ModItems.USED_FILTER_PAPER.get()));f.receiver(cup());f.process();
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        player.setPos(f.getX(),f.getY()+.9-player.getEyeHeight(),f.getZ()-2);player.setYRot(0);player.setXRot(0);
        f.interact(player,InteractionHand.MAIN_HAND);check(h,f.paper().isEmpty(),"paper not removed through interaction");
        double remaining=mass(f.contents(),"water");f.process();near(h,mass(f.contents(),"water"),remaining,"paper change leaked unfiltered liquid");
        // Return the extracted wet paper rather than inventing a replacement in the conservation fixture.
        ItemStack recovered=ItemStack.EMPTY;for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(ModItems.USED_FILTER_PAPER.get())){recovered=player.getInventory().removeItemNoUpdate(i);break;}
        check(h,!recovered.isEmpty(),"removed wet paper lost");player.setItemInHand(InteractionHand.MAIN_HAND,recovered);f.interact(player,InteractionHand.MAIN_HAND);
        check(h,!f.paper().isEmpty()&&player.getMainHandItem().isEmpty(),"paper reinstall failed");
        var packed=f.toStack();var clone=new FilterFunnelEntity(ModEntities.FILTER_FUNNEL.get(),h.getLevel());clone.unpack(packed);
        near(h,mass(clone.contents(),"water")+mass(clone.paper(),"water")+mass(clone.receiver(),"water"),30,"pickup lost liquid");
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());f.saveWithoutId(out);var loaded=new FilterFunnelEntity(ModEntities.FILTER_FUNNEL.get(),h.getLevel());loaded.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        check(h,loaded.ownerId().equals(stand.getUUID().toString()),"parent UUID lost");var saved=com.example.chemistry.solution.SolutionSpecies.analyticalSnapshot(loaded.contents())
                .plus(com.example.chemistry.solution.SolutionSpecies.analyticalSnapshot(loaded.paper()))
                .plus(com.example.chemistry.solution.SolutionSpecies.analyticalSnapshot(loaded.receiver()));
        var picked=com.example.chemistry.solution.SolutionSpecies.analyticalSnapshot(clone.contents())
                .plus(com.example.chemistry.solution.SolutionSpecies.analyticalSnapshot(clone.paper()))
                .plus(com.example.chemistry.solution.SolutionSpecies.analyticalSnapshot(clone.receiver()));
        check(h,com.example.chemistry.solution.Conservation.compare(initial,saved).conserved(),"save lost solid or dissolved matter");
        check(h,com.example.chemistry.solution.Conservation.compare(initial,picked).conserved(),"pickup lost solid or dissolved matter");
        var area=stand.getBoundingBox().inflate(3);h.getLevel().getEntitiesOfClass(ItemEntity.class,area).forEach(e->e.discard());stand.discard();check(h,f.isRemoved(),"stand destruction left filter");
        check(h,h.getLevel().getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(ModItems.FILTER_FUNNEL.get())).count()==1,"filter drop duplication");h.succeed();
    });}
    public static void beakers(GameTestHelper h){h.runAtTickTime(1,()->{
        String[] ids={"beaker_100ml","beaker_medium","beaker_tall"};int[] types={5,8,9};double[] tops={7.8,10.5,14};
        for(int i=0;i<ids.length;i++){var item=BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath("mchemistry",ids[i]));check(h,item instanceof LabVesselItem v&&v.capacity()==500,"beaker capacity not 500");var stack=new ItemStack(item);LabVesselItem.addMass(stack,"liquid","water",250);check(h,stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA).strings().contains("filled_050"),"half fill model missing");near(h,VesselHeating.mouthTopY(VesselHeating.vesselType(stack)),tops[i],"mouth does not match new beaker");check(h,VesselHeating.vesselType(stack)==types[i],"beaker shape collision");check(h,com.example.chemistry.menu.ReagentCabinetMenu.accepts(stack),"cabinet rejects new beaker");}
        h.succeed();
    });}
}
