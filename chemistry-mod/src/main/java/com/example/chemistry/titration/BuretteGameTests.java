package com.example.chemistry.titration;
import com.example.chemistry.*;
import com.example.chemistry.entity.IronStandEntity;
import com.example.chemistry.filtration.Filtration;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.*;
import com.example.chemistry.solution.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.*;
import java.util.List;

public final class BuretteGameTests {
    private static void check(GameTestHelper h,boolean ok,String msg){h.assertTrue(ok,Component.literal(msg));}
    private static void near(GameTestHelper h,double a,double b,double eps,String msg){check(h,Math.abs(a-b)<eps,msg+": "+a+" != "+b);}
    private static ItemStack cup(){return new ItemStack(ModItems.ERLENMEYER_FLASK.get());}
    private static BuretteEntity burette(GameTestHelper h){var b=new BuretteEntity(ModEntities.BURETTE.get(),h.getLevel());b.unpack(new ItemStack(ModItems.BURETTE_GLASS.get()));return b;}
    public static void flow(GameTestHelper h){h.runAtTickTime(1,()->{
        var source=cup();LabVesselItem.addMass(source,"liquid","water",100);LabVesselItem.addMass(source,"liquid","sodium_hydroxide_solution",.08);
        var start=SolutionSpecies.analyticalSnapshot(source);var b=burette(h);
        near(h,b.fillFrom(source),25,1e-7,"first fill");near(h,b.fillFrom(source),25,1e-7,"second fill");check(h,b.fillFrom(source)==0,"overfilled 50mL burette");
        var receiver=cup();LabVesselItem.addMass(receiver,"liquid","water",100);
        double acid=SolutionSpecies.analyticalSnapshot(b.device()).amount("hydroxide")/2;
        LabVesselItem.addMass(receiver,"liquid","acetic_acid",acid*SpeciesCatalog.get("acetic_acid").molarMass());
        var initial=start.plus(SolutionSpecies.analyticalSnapshot(receiver));b.receiver(receiver);
        for(int i=0;i<500;i++)near(h,b.drip(),.05,1e-7,"single drop volume");
        near(h,b.delivered(),25,.001,"delivered meter");check(h,AcidBaseEquilibrium.read(b.receiver()).ph()>8,"burette did not cross weak acid equivalence");
        check(h,Conservation.compare(initial,SolutionSpecies.analyticalSnapshot(source).plus(SolutionSpecies.analyticalSnapshot(b.device())).plus(SolutionSpecies.analyticalSnapshot(b.receiver()))).conserved(),"drips lost acid/base/solvent mass");
        var full=cup();LabVesselItem.addMass(full,"liquid","water",250);b.receiver(full);b.open(true);double remaining=Filtration.liquidVolume(b.device());
        check(h,b.drip()==0&&!b.open(),"full receiver did not stop flow");near(h,Filtration.liquidVolume(b.device()),remaining,1e-7,"full receiver consumed titrant");
        b.receiver(ItemStack.EMPTY);b.open(true);check(h,b.drip()==0&&!b.open(),"missing receiver lost liquid");
        var sealed=cup();VesselHeating.seal(sealed,1);b.receiver(sealed);b.open(true);check(h,b.drip()==0&&!b.open(),"sealed receiver accepted drip");
        b.receiver(cup());for(int i=0;i<501;i++)b.drip();near(h,b.delivered(),50,.001,"last drops accumulated wrong");check(h,Filtration.liquidVolume(b.device())<1e-7,"last remainder did not drain");h.succeed();
    });}
    public static void lifecycle(GameTestHelper h){h.runAtTickTime(1,()->{
        var stand=new IronStandEntity(ModEntities.IRON_STAND.get(),h.getLevel());var pos=h.absolutePos(new BlockPos(1,1,1));stand.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);h.getLevel().addFreshEntity(stand);
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.BURETTE_AMBER_PTFE.get()));stand.interact(player,InteractionHand.MAIN_HAND);
        var b=BuretteConnections.attached(stand);check(h,b!=null&&player.getMainHandItem().isEmpty(),"burette stand installation failed");
        var source=cup();LabVesselItem.addMass(source,"liquid","water",10);b.fillFrom(source);b.receiver(cup());b.open(true);b.drip();
        var copy=burette(h);copy.unpack(b.toStack());near(h,copy.delivered(),.05,1e-7,"pickup lost meter");check(h,copy.device().is(ModItems.BURETTE_AMBER_PTFE.get()),"variant lost on packing");
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());b.saveWithoutId(out);
        var loaded=burette(h);loaded.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        check(h,loaded.ownerId().equals(stand.getUUID().toString())&&loaded.open(),"save lost owner or valve");
        near(h,Filtration.liquidVolume(loaded.device())+Filtration.liquidVolume(loaded.receiver()),10,1e-7,"save lost stored liquid");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setPos(b.control().x,b.control().y-player.getEyeHeight(),b.control().z-2);player.setYRot(0);player.setXRot(0);player.setShiftKeyDown(true);
        double previous=b.delivered();b.interact(player,InteractionHand.MAIN_HAND);near(h,b.delivered()-previous,.05,1e-6,"sneak valve click did not deliver one drop");check(h,!b.open()&&!b.isRemoved(),"single drop picked up burette or left valve open");
        player.setShiftKeyDown(false);b.interact(player,InteractionHand.MAIN_HAND);check(h,b.open(),"valve did not open");
        for(int i=0;i<4;i++)b.tick();check(h,b.delivered()>previous+.09,"open valve did not continuously drip");
        var area=stand.getBoundingBox().inflate(3);stand.discard();check(h,b.isRemoved(),"stand break left orphan burette");
        check(h,h.getLevel().getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(ModItems.BURETTE_AMBER_PTFE.get())).count()==1,"burette duplicated on stand break");h.succeed();
    });}
    public static void variants(GameTestHelper h){h.runAtTickTime(1,()->{
        for(var item:List.of(ModItems.BURETTE_GLASS.get(),ModItems.BURETTE_ALKALI.get(),ModItems.BURETTE_PTFE.get(),ModItems.BURETTE_AMBER_GLASS.get(),ModItems.BURETTE_AMBER_PTFE.get())){
            check(h,item.capacity()==50,"variant capacity mismatch");var s=new ItemStack(item);LabVesselItem.addLiquid(s,"water",25);
            check(h,s.get(DataComponents.CUSTOM_MODEL_DATA).strings().contains("filled_050"),"burette inventory level missing");
        }
        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){var stand=new IronStandEntity(ModEntities.IRON_STAND.get(),h.getLevel());stand.setFacing(direction);var b=burette(h);b.owner(stand);
            check(h,b.virtualHitbox().contains(b.control()),"control outside rotated hitbox");check(h,b.outlet().y>stand.getY()+.39,"burette outlet intersects receiver");}
        h.succeed();
    });}
    private BuretteGameTests(){}
}
