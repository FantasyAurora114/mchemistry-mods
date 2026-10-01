package com.example.chemistry.electrical;

import com.example.chemistry.*;
import com.example.chemistry.block.WaterTroughBlock;
import com.example.chemistry.blockentity.WaterTroughBlockEntity;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.Vec3;

public final class TroughGameTests {
    private static void check(GameTestHelper h,boolean ok,String s){h.assertTrue(ok,Component.literal(s));}
    private static void near(GameTestHelper h,double a,double b,String s){check(h,Math.abs(a-b)<1e-7,s+" "+a+" / "+b);}
    private static ElectroDeviceEntity device(GameTestHelper h,Item item,int x,int z){var e=new ElectroDeviceEntity(ModEntities.ELECTRO_DEVICE.get(),h.getLevel());e.setStack(new ItemStack(item));var p=h.absolutePos(new BlockPos(x,1,z));e.setPos(p.getX()+.5,p.getY(),p.getZ()+.5);h.getLevel().addFreshEntity(e);return e;}
    private static void fill(ElectroDeviceEntity e){var s=e.stack();LabVesselItem.addMass(s,"liquid","water",750);LabVesselItem.addMass(s,"liquid","sodium_hydroxide_solution",1);e.setStack(s);}
    private static void electrodes(ElectroDeviceEntity e,Item anode,Item cathode){TroughSystem.part(e,"clip",0,new ItemStack(ModItems.ALLIGATOR_CLIP_RED.get()));TroughSystem.part(e,"clip",1,new ItemStack(ModItems.ALLIGATOR_CLIP_BLACK.get()));TroughSystem.part(e,"mesh",0,new ItemStack(anode));TroughSystem.part(e,"mesh",1,new ItemStack(cathode));}
    private static void aim(net.minecraft.world.entity.player.Player p,Vec3 target){p.setPos(target.x,target.y-p.getEyeHeight(),target.z-2);p.setYRot(0);p.setXRot(0);}
    public static void longSeries(GameTestHelper h){h.runAtTickTime(1,()->{
        for(int count:new int[]{3,4,8}){
            var supply=device(h,ModItems.BENCH_POWER_SUPPLY.get(),0,count==3?1:count==4?3:5);
            supply.setNumber("voltage",count==8?24:12);supply.flag("on",true);
            var p=h.makeMockPlayer(GameType.SURVIVAL);var cable=new ItemStack(ModItems.ELECTRICAL_WIRE.get(),32);p.setItemInHand(InteractionHand.MAIN_HAND,cable);
            var cells=new java.util.ArrayList<ElectroDeviceEntity>();ElectroDeviceEntity previous=supply;int previousPort=0;
            for(int i=0;i<count;i++){
                int cellX=count==8?(i%4)+1:i+1;
                int cellZ=count==8?5+2*(i/4):count==3?1:3;
                var cell=device(h,ModItems.HOFMANN_VOLTAMETER.get(),cellX,cellZ);var s=cell.stack();LabVesselItem.addMass(s,"liquid","water",150);LabVesselItem.addMass(s,"liquid","sodium_hydroxide_solution",1);cell.setStack(s);cells.add(cell);
                aim(p,previous.terminal(previousPort));previous.interact(p,InteractionHand.MAIN_HAND);aim(p,cell.terminal(0));cell.interact(p,InteractionHand.MAIN_HAND);previous=cell;previousPort=1;
            }
            aim(p,previous.terminal(1));previous.interact(p,InteractionHand.MAIN_HAND);aim(p,supply.terminal(1));supply.interact(p,InteractionHand.MAIN_HAND);
            check(h,ElectricConnections.series(supply,0).size()==count,"player-connected chain not found: "+count);
            supply.tickPower();check(h,supply.current()>0,"long chain stopped: "+supply.status());double charge=cells.getFirst().number("charge_c",0);
            for(var e:cells){check(h,e.current()>0,"cell not running");near(h,e.number("charge_c",0),charge,"series charge mismatch");}
            supply.tickPower();near(h,cells.getFirst().number("charge_c",0),charge,"duplicate power tick charged twice");
            for(var e:cells)e.discard();supply.discard();p.discard();
        }
        h.succeed();
    });}
    public static void bathWater(GameTestHelper h){h.runAtTickTime(1,()->{
        var e=device(h,ModItems.DEEP_WATER_TROUGH.get(),2,2);fill(e);electrodes(e,ModItems.PLATINUM_ELECTRODE_MESH.get(),ModItems.COPPER_ELECTRODE_MESH.get());
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,ModItems.emptyGasJar());aim(p,TroughSystem.point(e,0,14));e.interact(p,InteractionHand.MAIN_HAND);
        check(h,!TroughSystem.part(e,"jar",0).isEmpty(),"left jar not installed");p.setItemInHand(InteractionHand.MAIN_HAND,ModItems.emptyGasJar());aim(p,TroughSystem.point(e,1,14));e.interact(p,InteractionHand.MAIN_HAND);check(h,!TroughSystem.part(e,"jar",1).isEmpty(),"right jar not installed");
        double before=WaterElectrolysis.water(e.stack());check(h,e.electrolyze(6,1,0,1,20)>0,"open bath water stopped");
        near(h,e.gasMoles(1),2*e.gasMoles(0),"bath gas ratio");near(h,before-WaterElectrolysis.water(e.stack()),e.gasMoles(1)*18.015,"bath water conservation");
        double q=e.number("charge_c",0);e.electrolyze(6,1,0,1,20);near(h,q,e.number("charge_c",0),"duplicate bath processing");
        near(h,TemperatureSystem.getTemp(e.stack()),20,"bath charge became 500C heat");
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());e.saveWithoutId(out);var copy=new ElectroDeviceEntity(ModEntities.ELECTRO_DEVICE.get(),h.getLevel());copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        near(h,copy.gasMoles(1),e.gasMoles(1),"gas lost on reload");check(h,ElectrodePartItem.material(TroughSystem.part(copy,"mesh",0)).equals("platinum"),"mesh lost");check(h,!TroughSystem.part(copy,"jar",0).isEmpty(),"jar lost");
        double water=WaterElectrolysis.water(e.stack());TroughSystem.takeJar(e,p,0);near(h,WaterElectrolysis.water(e.stack()),water,"lifting jar duplicates or loses bath water");
        e.discard();h.succeed();
    });}
    public static void plating(GameTestHelper h){h.runAtTickTime(1,()->{
        for(String metal:new String[]{"copper","silver"})for(boolean inert:new boolean[]{false,true}){
            var e=device(h,ModItems.DEEP_WATER_TROUGH.get(),2,2);var s=e.stack();LabVesselItem.addMass(s,"liquid","water",500);String salt=metal.equals("copper")?"copper_sulfate_solution":"silver_nitrate_solution";LabVesselItem.addMass(s,"liquid",salt,5);e.setStack(s);
            Item anode=inert?ModItems.PLATINUM_ELECTRODE_MESH.get():metal.equals("copper")?ModItems.COPPER_ELECTRODE_MESH.get():ModItems.SILVER_ELECTRODE_MESH.get();electrodes(e,anode,ModItems.PLATINUM_ELECTRODE_MESH.get());
            double total=LabVesselItem.getContents(e.stack()).stream().mapToDouble(LabVesselItem.Entry::amount).sum()+20;
            check(h,e.electrolyze(6,1,0,1,20)>0,"plating stopped: "+metal+" "+e.status());
            var cath=TroughSystem.part(e,"mesh",1);double deposit=ElectrodePartItem.coating(cath,metal);check(h,deposit>0,"no coating");
            double n=e.number("charge_c",0)/(WaterElectrolysis.FARADAY*(metal.equals("copper")?2:1));near(h,deposit,n*(metal.equals("copper")?63.546:107.8682),"Faraday plating");
            if(!inert){near(h,10-ElectrodePartItem.grams(TroughSystem.part(e,"mesh",0)),deposit,"anode/cathode imbalance");near(h,TroughSystem.mass(e.stack(),salt),5,"active anode consumes bath salt");}
            double after=LabVesselItem.getContents(e.stack()).stream().mapToDouble(LabVesselItem.Entry::amount).sum()+ElectrodePartItem.grams(TroughSystem.part(e,"mesh",0))+10+deposit+e.number("vented_oxygen",0)*31.998;
            check(h,Math.abs(total-after)<2e-6,"plating mass mismatch");e.discard();
        }
        h.succeed();
    });}
    public static void chlorine(GameTestHelper h){h.runAtTickTime(1,()->{
        for(boolean hot:new boolean[]{false,true}){
            var e=device(h,ModItems.DEEP_WATER_TROUGH.get(),2,2);var s=e.stack();LabVesselItem.addMass(s,"liquid","water",500);LabVesselItem.addMass(s,"liquid","sodium_chloride_solution",180);if(hot)LabVesselItem.addMass(s,"liquid","sodium_hydroxide_solution",100);TemperatureSystem.setTemp(s,hot?90:20);e.setStack(s);electrodes(e,ModItems.PLATINUM_ELECTRODE_MESH.get(),ModItems.COPPER_ELECTRODE_MESH.get());
            double before=LabVesselItem.getContents(e.stack()).stream().mapToDouble(LabVesselItem.Entry::amount).sum();check(h,e.electrolyze(6,1,0,1,20)>0,"brine stopped");
            check(h,TroughSystem.mass(e.stack(),hot?"sodium_chlorate_solution":"sodium_hypochlorite_solution")>0,"chlorine follow-up missing");
            double after=LabVesselItem.getContents(e.stack()).stream().mapToDouble(LabVesselItem.Entry::amount).sum()+e.number("vented_hydrogen",0)*2.016;
            near(h,before,after,"chloralkali follow-up mass balance");check(h,e.gasMoles(0)==0,"well-mixed chlorine treated as fully captured");e.discard();
        }
        var e=device(h,ModItems.DEEP_WATER_TROUGH.get(),2,2);var s=e.stack();LabVesselItem.addMass(s,"liquid","water",500);e.setStack(s);TroughSystem.chlorine(e,0,.1);near(h,TroughSystem.mass(e.stack(),"chlorine_water"),3.5,"chlorine solubility ceiling");check(h,e.number("vented_chlorine",0)>0,"excess chlorine not vented");e.discard();h.succeed();
    });}
    public static void shallow(GameTestHelper h){var pos=new BlockPos(2,1,2);h.setBlock(pos,ModBlocks.WATER_TROUGH.get().defaultBlockState().setValue(WaterTroughBlock.FILLED,WaterTroughBlock.Fill.WATER));var be=(WaterTroughBlockEntity)h.getBlockEntity(pos,WaterTroughBlockEntity.class);near(h,be.bathMl(),500,"shallow capacity");be.placeBottle();near(h,be.bathMl()+be.getWaterMl(),500,"filling jar created water");be.addGas("hydrogen",20,1);near(h,be.bathMl()+be.getWaterMl(),500,"displacement water lost");be.takeBottle();near(h,be.bathMl(),500,"jar return creates water");h.succeed();}
}
