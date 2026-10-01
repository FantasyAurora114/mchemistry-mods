package com.example.chemistry.electrical;

import com.example.chemistry.*;
import com.example.chemistry.entity.*;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

public final class ElectricalGameTests {
    private static void check(GameTestHelper h,boolean ok,String msg){h.assertTrue(ok,Component.literal(msg));}
    private static void near(GameTestHelper h,double a,double b,String msg){check(h,Math.abs(a-b)<1e-9,msg+": "+a+" / "+b);}
    private static ElectroDeviceEntity device(GameTestHelper h,boolean power,int x){var e=new ElectroDeviceEntity(ModEntities.ELECTRO_DEVICE.get(),h.getLevel());e.setStack(new ItemStack(power?ModItems.BENCH_POWER_SUPPLY.get():ModItems.HOFMANN_VOLTAMETER.get()));var p=h.absolutePos(new BlockPos(x,1,2));e.setPos(p.getX()+.5,p.getY(),p.getZ()+.5);h.getLevel().addFreshEntity(e);return e;}
    private static void fill(ElectroDeviceEntity e){ItemStack s=e.stack();LabVesselItem.addMass(s,"liquid","water",150);LabVesselItem.addMass(s,"liquid","sodium_hydroxide_solution",1);e.setStack(s);}
    private static ElectricWireEntity wire(GameTestHelper h,ElectroDeviceEntity a,int ap,ElectroDeviceEntity b,int bp){var w=new ElectricWireEntity(ModEntities.ELECTRIC_WIRE.get(),h.getLevel());w.connect(a,ap,b,bp,false);h.getLevel().addFreshEntity(w);return w;}
    public static void balance(GameTestHelper h){h.runAtTickTime(1,()->{
        var result=WaterElectrolysis.calculate(1,2*WaterElectrolysis.FARADAY,100,50000,50000,100000);
        near(h,result.hydrogenMoles(),1,"Faraday hydrogen");near(h,result.oxygenMoles(),.5,"oxygen half");near(h,result.waterGrams(),18.015,"water consumed");near(h,result.waterGrams(),result.hydrogenMoles()*2.016+result.oxygenMoles()*31.998,"mass balance");
        var tiny=WaterElectrolysis.calculate(2,100,.000001,50,50,100);near(h,tiny.waterGrams(),.000001,"small amount cannot produce unlimited gas");
        check(h,WaterElectrolysis.calculate(1,1,10,0,50,100).charge()==0,"full side should stop");
        check(h,WaterElectrolysis.calculate(Double.NaN,1,10,50,50,100).charge()==0,"nonfinite charge");
        var cell=device(h,false,1);fill(cell);double before=WaterElectrolysis.water(cell.stack());
        double current=cell.electrolyze(6,1,0,1,.2);check(h,current>0,"base solution not conductive");
        near(h,cell.gasMoles(1),2*cell.gasMoles(0),"hydrogen/oxygen 2:1");near(h,before-WaterElectrolysis.water(cell.stack()),cell.gasMoles(1)*18.015,"liquid not deducted");
        double charge=cell.number("charge_c",0);cell.electrolyze(6,1,0,1,.2);near(h,charge,cell.number("charge_c",0),"same tick duplicated chemistry");
        double solute=LabVesselItem.getContents(cell.stack()).stream().filter(e->e.id().equals("sodium_hydroxide_solution")).mapToDouble(LabVesselItem.Entry::amount).sum();near(h,solute,1,"supporting electrolyte consumed");
        var water=new ItemStack(ModItems.HOFMANN_VOLTAMETER.get());LabVesselItem.addMass(water,"liquid","water",150);check(h,WaterElectrolysis.conductance(water)<1e-5,"pure water too conductive");
        LabVesselItem.addMass(water,"liquid","sodium_chloride_solution",1);check(h,!WaterElectrolysis.supported(water),"salt water must not silently split as pure water");
        cell.discard();h.succeed();
    });}
    public static void brine(GameTestHelper h){h.runAtTickTime(1,()->{
        var r=BrineElectrolysis.calculate(1,2*WaterElectrolysis.FARADAY,1000,360,50000,50000,100000);
        near(h,r.gasMoles(),1,"one mole each gas per 2F");
        near(h,r.saltGrams(),116.88,"salt stoichiometry");near(h,r.waterGrams(),36.03,"water stoichiometry");
        near(h,r.saltGrams()+r.waterGrams(),r.baseGrams()+2.016+70.9,"chloralkali mass balance");
        check(h,BrineElectrolysis.calculate(1,1,150,1,50,50,100).charge()==0,"dilute brine must stop");
        check(h,BrineElectrolysis.calculate(1,1,150,54,0,50,100).charge()==0,"full hydrogen side");
        check(h,BrineElectrolysis.calculate(Double.NaN,1,150,54,50,50,100).charge()==0,"nonfinite input");
        var limited=BrineElectrolysis.calculate(2,1e9,150,30.000001,50,50,100);
        check(h,limited.gasMoles()>0 && limited.gasMoles()<1e-7,"tiny excess salt not bounded");
        var cell=device(h,false,1);var s=cell.stack();LabVesselItem.addMass(s,"liquid","water",150);
        LabVesselItem.addMass(s,"liquid","sodium_chloride_solution",54);cell.setStack(s);
        check(h,cell.electrolyze(6,1,0,1,.2)>0,"saturated brine did not run");
        check(h,cell.gas(0).equals("chlorine")&&cell.gas(1).equals("hydrogen"),"wrong gases");
        double n=cell.gasMoles(0);near(h,n,cell.gasMoles(1),"gas ratio not 1:1");
        near(h,54-BrineElectrolysis.salt(cell.stack()),2*n*58.44,"salt not consumed");
        near(h,150-WaterElectrolysis.water(cell.stack()),2*n*18.015,"water not consumed");
        double base=LabVesselItem.getContents(cell.stack()).stream().filter(e->e.id().equals("sodium_hydroxide_solution")).mapToDouble(LabVesselItem.Entry::amount).sum();
        near(h,base,2*n*39.997,"NaOH product missing");
        var reversed=device(h,false,4);reversed.setStack(s);check(h,reversed.electrolyze(6,1,1,0,.2)>0,"reverse polarity failed");
        check(h,reversed.gas(1).equals("chlorine")&&reversed.gas(0).equals("hydrogen"),"reverse products wrong");
        var blocked=device(h,false,7);blocked.setStack(s);blocked.setGas(0,"oxygen",.0001);
        check(h,blocked.electrolyze(6,1,0,1,.2)==0,"residual oxygen mixed with chlorine");
        var jar=new GasCollectingBottleEntity(ModEntities.GAS_COLLECTING_BOTTLE.get(),h.getLevel());
        var pos=h.absolutePos(new BlockPos(3,1,5));jar.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        jar.setHasNozzle(true);jar.setInverted(false);h.getLevel().addFreshEntity(jar);
        var port=RubberTubeEntity.Port.stand(cell.blockPosition(),3);
        var tube=RubberTubeEntity.create(h.getLevel(),port,RubberTubeEntity.Port.nozzle(jar.blockPosition(),Direction.UP),cell.outlet(0));
        h.getLevel().addFreshEntity(tube);tube.initAir();cell.setGas(0,"chlorine",10.75/WaterElectrolysis.GAS_ML_PER_MOLE);cell.flag("valve_0",true);
        for(int i=0;i<12;i++)cell.tickValves();
        check(h,jar.getFillMl()==10&&jar.getGasId().equals("chlorine"),"chlorine not collected through hose");
        near(h,cell.gasMl(0)+jar.getFillMl()+tube.getTransitMl(),10.75,"chlorine collection loses mass");
        cell.discard();reversed.discard();blocked.discard();jar.discard();h.succeed();
    });}
    public static void visualGeometry(GameTestHelper h){h.runAtTickTime(1,()->{
        for(int x=0;x<8;x++)for(int z=0;z<5;z++)h.setBlock(new BlockPos(x,0,z),net.minecraft.world.level.block.Blocks.STONE);
        var power=device(h,true,1);var cell=device(h,false,6);
        check(h,cell.virtualHitbox().getYsize()<1.14&&power.virtualHitbox().getXsize()<.66,"apparatus hitbox not scaled");
        near(h,cell.outlet(0).y-cell.getY(),26.6/16*ElectroDeviceEntity.MODEL_SCALE,"outlet not scaled with model");
        var cable=wire(h,power,0,cell,1);var points=cable.path();
        near(h,points[0].distanceTo(power.terminal(0)),0,"wire source moved");
        near(h,points[points.length-1].distanceTo(cell.terminal(1)),0,"wire end moved");
        double floor=h.absolutePos(new BlockPos(0,1,0)).getY();
        for(Vec3 point:points)check(h,point.y>=floor+.012,"cable entered floor");
        h.setBlock(new BlockPos(3,1,2),net.minecraft.world.level.block.Blocks.STONE_SLAB);
        points=cable.path();boolean crossed=false;
        for(Vec3 point:points){BlockPos local=BlockPos.containing(point).subtract(h.absolutePos(BlockPos.ZERO));
            if(local.getX()==3&&local.getZ()==2){crossed=true;check(h,point.y>=floor+.5+.012,"cable entered slab");}}
        check(h,crossed,"route did not cross slab fixture");
        Vec3 midpoint=points[points.length/2];check(h,cable.rayHit(midpoint.add(0,1,0),midpoint.add(0,-1,0)),"scissors route differs from visible route");
        var reverse=wire(h,cell,0,power,1);check(h,reverse.wireColor()==0x30343B,"wire color must follow supply polarity when connected backwards");
        power.discard();cell.discard();h.succeed();
    });}
    public static void ventAccounting(GameTestHelper h){
        var cell=device(h,false,1);var contents=cell.stack();
        LabVesselItem.addMass(contents,"liquid","water",125);
        LabVesselItem.addMass(contents,"liquid","sodium_hydroxide_solution",1);cell.setStack(contents);
        double initial=WaterElectrolysis.water(cell.stack());
        for(int tick=1;tick<=12;tick++)h.runAtTickTime(tick,()->{
            cell.flag("valve_0",false);cell.flag("valve_1",false);
            check(h,cell.electrolyze(6,1,0,1,10)>0,"electrolysis stopped unexpectedly");
            double before=WaterElectrolysis.water(cell.stack());
            cell.flag("valve_0",true);cell.flag("valve_1",true);
            for(int i=0;i<20;i++)cell.tickValves();
            near(h,WaterElectrolysis.water(cell.stack()),before,"venting restored consumed water");
            near(h,cell.gasMl(0)+cell.gasMl(1),0,"valves not drained");
            near(h,TemperatureSystem.getTemp(cell.stack()),20,"electrolysis charge became heat");
        });
        h.runAtTickTime(13,()->{
            double consumed=initial-WaterElectrolysis.water(cell.stack());
            check(h,consumed>0,"water not consumed over repeated vent cycles");
            near(h,cell.number("water_consumed_g",0),consumed,"consumption counter incorrect");
            near(h,consumed,cell.number("charge_c",0)/(2*WaterElectrolysis.FARADAY)*18.015,"charge/water balance incorrect");
            var lines=new java.util.ArrayList<Component>();cell.setNumber("charge_c",500);cell.addGoggleInfo(lines,false);
            check(h,lines.stream().anyMatch(l->l.getString().contains("500.000 库仑")),"ambiguous charge unit");
            check(h,lines.stream().anyMatch(l->l.getString().contains("20.0 °C")),"temperature not separately displayed");
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());cell.saveWithoutId(out);
            var copy=new ElectroDeviceEntity(ModEntities.ELECTRO_DEVICE.get(),h.getLevel());copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
            near(h,copy.number("water_consumed_g",0),consumed,"consumption lost on reload");
            near(h,WaterElectrolysis.water(copy.stack()),initial-consumed,"reload refilled water");
            cell.discard();h.succeed();
        });
    }
    public static void wiring(GameTestHelper h){h.runAtTickTime(1,()->{
        var supply=device(h,true,1);var cell=device(h,false,4);fill(cell);var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(supply.position());
        for(float yaw:new float[]{0,90,180,270}) {
            supply.setYRot(yaw);
            for(int slot=0;slot<2;slot++) {
                Vec3 target=supply.terminal(slot),from=supply.modelPoint(slot==0?13.6:12,2.45,-30);
                p.setPos(from.x,from.y-p.getEyeHeight(),from.z);
                Vec3 direction=target.subtract(from);
                p.setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));p.setXRot(0);
                check(h,supply.pickTerminal(p)==slot,"terminal picking disagrees with model orientation");
            }
        }
        supply.setYRot(0);p.setPos(supply.position());
        var stack=new ItemStack(ModItems.ELECTRICAL_WIRE.get(),4);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        Vec3 terminal=supply.terminal(0);
        p.setPos(terminal.x,terminal.y-p.getEyeHeight(),terminal.z-2);p.setYRot(0);p.setXRot(0);
        supply.interact(p,InteractionHand.MAIN_HAND);check(h,stack.getCount()==4,"first click consumed wire");check(h,ElectricConnections.wires(supply).stream().anyMatch(ElectricWireEntity::preview),"missing live preview");
        ElectricConnections.cancel(p,stack);check(h,stack.getCount()==4,"cancel consumed wire");
        ElectricConnections.click(supply,0,p,stack);ElectricConnections.click(cell,0,p,stack);check(h,stack.getCount()==3,"completed link must consume exactly one");
        ElectricConnections.click(supply,0,p,stack);check(h,stack.getCount()==3,"occupied endpoint consumed item");
        ElectricConnections.click(supply,1,p,stack);ElectricConnections.click(cell,1,p,stack);check(h,stack.getCount()==2,"second link count");
        check(h,ElectricConnections.circuit(supply)!=null,"valid circuit absent");supply.flag("on",true);supply.tickPower();check(h,supply.current()>0 && cell.gasMoles(1)>0,"closed circuit not generating hydrogen");
        var cable=ElectricConnections.wires(supply).stream().filter(w->w.uses(supply,0)).findFirst().orElseThrow();cable.discard();check(h,ElectricConnections.circuit(supply)==null,"cut circuit remains active");supply.tickPower();check(h,supply.current()==0,"cut circuit current remains");
        for(var w:ElectricConnections.wires(supply))w.discard();wire(h,supply,0,supply,1);supply.tickPower();check(h,supply.current()==0 && ElectricConnections.shorted(supply),"short not protected");
        supply.discard();cell.discard();h.succeed();
    });}
    public static void persistence(GameTestHelper h){h.runAtTickTime(1,()->{
        var power=device(h,true,1);var cell=device(h,false,4);fill(cell);cell.setGas(1,"hydrogen",10/WaterElectrolysis.GAS_ML_PER_MOLE);cell.flag("valve_1",true);cell.setYRot(90);
        var w=wire(h,power,0,cell,0);var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());cell.saveWithoutId(out);
        var copy=new ElectroDeviceEntity(ModEntities.ELECTRO_DEVICE.get(),h.getLevel());copy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        near(h,copy.gasMl(1),10,"gas lost on reload");near(h,WaterElectrolysis.water(copy.stack()),150,"water lost on reload");check(h,copy.flag("valve_1"),"valve lost");near(h,copy.getYRot(),90,"orientation lost");
        var wireOut=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());w.saveWithoutId(wireOut);var wireCopy=new ElectricWireEntity(ModEntities.ELECTRIC_WIRE.get(),h.getLevel());wireCopy.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),wireOut.buildResult()));check(h,wireCopy.a().equals(power.getUUID().toString())&&wireCopy.b().equals(cell.getUUID().toString())&&!wireCopy.preview(),"wire endpoints lost");
        var area=power.getBoundingBox().inflate(6);h.getLevel().getEntitiesOfClass(ItemEntity.class,area).forEach(Entity::discard);power.discard();check(h,w.isRemoved(),"device removal left wire");int drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(ModItems.ELECTRICAL_WIRE.get())).mapToInt(e->e.getItem().getCount()).sum();check(h,drops==1,"wire removal duplicated/lost item");
        var cup=new ItemStack(ModItems.GROUND_GLASS_FLASK.get());LabVesselItem.addMass(cup,"liquid","water",100);LabVesselItem.addMass(cup,"liquid","potassium_hydroxide_solution",2);var vessel=new ItemStack(ModItems.HOFMANN_VOLTAMETER.get());check(h,ElectroDeviceEntity.transferPortion(cup,vessel),"filling failed");near(h,WaterElectrolysis.water(cup)+WaterElectrolysis.water(vessel),100,"pour loses water");check(h,LabVesselItem.usedVolume(vessel)<=25.000001,"portion exceeded 25mL");
        cell.discard();h.succeed();
    });}
    public static void gasRouting(GameTestHelper h){h.runAtTickTime(1,()->{
        var cell=device(h,false,1);fill(cell);cell.setGas(1,"hydrogen",10.75/WaterElectrolysis.GAS_ML_PER_MOLE);
        var jar=new GasCollectingBottleEntity(ModEntities.GAS_COLLECTING_BOTTLE.get(),h.getLevel());
        var pos=h.absolutePos(new BlockPos(4,1,2));jar.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);jar.setHasNozzle(true);jar.setInverted(true);h.getLevel().addFreshEntity(jar);
        var port=RubberTubeEntity.Port.stand(cell.blockPosition(),4);
        var far=RubberTubeEntity.Port.nozzle(jar.blockPosition(),Direction.UP);
        var tube=RubberTubeEntity.create(h.getLevel(),port,far,cell.outlet(1));h.getLevel().addFreshEntity(tube);tube.initAir();
        near(h,port.worldPos(h.getLevel()).distanceTo(cell.outlet(1)),0,"rubber hose outlet misaligned");
        cell.tickValves();near(h,cell.gasMl(1),10.75,"closed valve leaked");cell.flag("valve_1",true);
        for(int i=0;i<12;i++)cell.tickValves();
        check(h,jar.getFillMl()==10 && jar.getGasId().equals("hydrogen"),"hydrogen did not reach inverted collection bottle");
        near(h,cell.gasMl(1)+jar.getFillMl()+tube.getTransitMl(),10.75,"hose collection mass/volume mismatch");
        check(h,cell.gasMl(1)>0,"fractional gas was lost");
        cell.electrolyze(6,1,1,0,.2);check(h,cell.current()==0 && cell.status().contains("反接"),"polarity reversal mixed gases");
        cell.discard();check(h,tube.isRemoved(),"apparatus removal left gas hose");jar.discard();h.succeed();
    });}
    public static void modelsAndHydrides(GameTestHelper h){h.runAtTickTime(1,()->{
        for(String id:new String[]{"sodium_hydride","potassium_hydride","calcium_hydride","lithium_aluminium_hydride","sodium_borohydride"})check(h,ModItems.looseSolid(id)!=null,"missing hydride "+id);
        for(var item:new LabVesselItem[]{ModItems.ROUND_BOTTOM_FLASK.get(),ModItems.GROUND_GLASS_FLASK.get()}){var stack=new ItemStack(item);LabVesselItem.addMass(stack,"liquid","water",item.capacity()/2.0);check(h,stack.get(DataComponents.CUSTOM_MODEL_DATA).strings().contains("filled_050"),"single-neck fill height not capacity based");near(h,VesselHeating.mouthTopY(VesselHeating.vesselType(stack)),10.05,"new mouth height");}
        var bounds=VesselHeating.vesselBounds(1);near(h,bounds[0][0],5,"new body bounds");near(h,bounds[1][0],12,"new body bounds");h.succeed();
    });}
}
