package com.example.chemistry.electrical;
import com.example.chemistry.*;
import com.example.chemistry.garden.ChemicalGarden;
import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.solution.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Daniell cells supply the existing terminal graph. Salt and electrode changes commit from one shared charge. */
public final class GalvanicSystem {
    public record Pair(ElectroDeviceEntity zinc,ElectroDeviceEntity copper,SaltBridgeEntity bridge){}
    public record Circuit(List<Pair> pairs,List<ElectricConnections.Connection> loads){}
    public static SaltBridgeEntity bridge(ElectroDeviceEntity e){return e.level().getEntitiesOfClass(SaltBridgeEntity.class,e.getBoundingBox().inflate(4)).stream().filter(b->b.uses(e,0)&&!b.isRemoved()).findFirst().orElse(null);}
    public static String metal(ElectroDeviceEntity e){return ElectrodePartItem.material(TroughSystem.part(e,"mesh",0));}
    private static Pair pair(ElectroDeviceEntity zinc){var bridge=bridge(zinc);if(bridge==null)return null;var other=bridge.endpoint(!bridge.a().equals(zinc.getUUID().toString()));return other instanceof ElectroDeviceEntity copper&&copper.isHalfCell()&&metal(zinc).equals("zinc")&&metal(copper).equals("copper")?new Pair(zinc,copper,bridge):null;}
    public static Circuit circuit(ElectroDeviceEntity zinc){
        Pair first=pair(zinc);if(first==null)return null;
        var pairs=new ArrayList<Pair>();pairs.add(first);var loads=new ArrayList<ElectricConnections.Connection>();Set<UUID> seen=new HashSet<>();seen.add(zinc.getUUID());seen.add(first.copper().getUUID());
        ElectroDeviceEntity at=first.copper();int port=0;
        for(int step=0;step<64;step++){
            final var here=at;final int herePort=port;
            var links=ElectricConnections.wires(at).stream().filter(w->!w.preview()&&w.uses(here,herePort)).toList();
            if(links.size()!=1)return null;var link=links.getFirst();boolean forward=link.a().equals(at.getUUID().toString());var far=link.endpoint(!forward);int entry=forward?link.bp():link.ap();
            if(far==zinc&&entry==0)return new Circuit(List.copyOf(pairs),List.copyOf(loads));
            if(!(far instanceof ElectroDeviceEntity cell)||cell.isPower()||!seen.add(cell.getUUID()))return null;
            if(cell.isHalfCell()) {if(entry!=0)return null;Pair next=pair(cell);if(next==null||!seen.add(next.copper().getUUID()))return null;pairs.add(next);at=next.copper();port=0;}
            else {if(entry<0||entry>1)return null;loads.add(new ElectricConnections.Connection(cell,entry,1-entry));at=cell;port=1-entry;}
        }return null;
    }
    public static String guard(Pair p){
        if(p.bridge().grams()<=1e-9)return "盐桥电解质耗尽";
        for(var e:List.of(p.zinc(),p.copper())){
            if(LabVesselItem.usedVolume(e.stack())<25)return "液位过低，电极未浸没";
            String allowed=e==p.zinc()?"zinc_sulfate_solution":"copper_sulfate_solution";
            if(LabVesselItem.getContents(e.stack()).stream().anyMatch(v->!v.type().equals("liquid")||!Set.of("water",allowed,"zinc_nitrate_solution","potassium_sulfate_solution").contains(v.id())))return "不支持此半电池混合物";
            if(ElectrodePartItem.grams(TroughSystem.part(e,"mesh",0))<=1e-9)return "电极已耗尽";
        }
        if(ChemicalGarden.mass(p.copper().stack(),"liquid","copper_sulfate_solution")<=1e-9)return "铜离子耗尽";
        if(ChemicalGarden.mass(p.zinc().stack(),"liquid","zinc_sulfate_solution")+ChemicalGarden.mass(p.zinc().stack(),"liquid","zinc_nitrate_solution")<=0)return "锌半电池需要锌盐溶液";
        return "";
    }
    public static double available(Pair p,double n){return Math.min(n,Math.min(p.bridge().grams()/(2*ChemicalGarden.mm("solid","potassium_nitrate")),Math.min(ElectrodePartItem.grams(TroughSystem.part(p.zinc(),"mesh",0))/65.38,ChemicalGarden.mass(p.copper().stack(),"liquid","copper_sulfate_solution")/ChemicalGarden.mm("solid","copper_sulfate_anhydrous"))));}
    public static void tick(ElectroDeviceEntity zinc){
        if(!metal(zinc).equals("zinc")){if(zinc.level().getGameTime()-zinc.number("galv_tick",-100)>5)zinc.state(0,"未供电：检查盐桥与回路");return;}
        var p=pair(zinc);if(p==null){zinc.state(0,"需要铜半电池与盐桥");return;}
        String reason=guard(p);if(!reason.isEmpty()){zinc.state(0,reason);p.copper().state(0,reason);return;}
        var c=circuit(zinc);
        if(c==null){zinc.state(0,"开路：电动势约1.10 V；未耗料");p.copper().state(0,"开路：电动势约1.10 V；未耗料");return;}
        if(c.pairs().stream().anyMatch(v->v.zinc().number("galv_tick",-1)==zinc.level().getGameTime()))return;
        for(var pair:c.pairs()){String stop=guard(pair);if(!stop.isEmpty()){stop(c,stop);return;}}
        if(c.loads().isEmpty()){stop(c,"短路保护：请接负载或电压表");return;}
        double emf=c.pairs().size()*1.10,threshold=0,resistance=c.pairs().size()*20.0;
        for(var load:c.loads()){
            var e=load.cell();if(e.isVoltmeter()){for(var pair:c.pairs()){pair.zinc().state(0,"高阻测量；未耗料");pair.copper().state(0,"高阻测量；未耗料");}e.setNumber("measurement_tick",zinc.level().getGameTime());e.setNumber("measured_v",load.positive()==0?emf:-emf);e.state(0,"测量开路电动势");return;}
            String stop=e.stopReason(load.positive(),load.negative());if(!stop.isEmpty()){stop(c,stop);return;}
            threshold+=e.isResistor()?0:e.isTrough()?TroughSystem.threshold(e):BrineElectrolysis.supported(e.stack())?2.2:1.8;
            resistance+=e.isResistor()?100:1/Math.max(1e-9,e.isTrough()?TroughSystem.conductance(e):WaterElectrolysis.conductance(e.stack()));
        }
        double amps=Math.min(.2,Math.max(0,(emf-threshold)/resistance));double n=amps*.2/(2*WaterElectrolysis.FARADAY);
        for(var pair:c.pairs()){String stop=guard(pair);if(!stop.isEmpty()){stop(c,stop);return;}n=available(pair,n);}
        amps=n*2*WaterElectrolysis.FARADAY/.2;
        for(var load:c.loads())amps=Math.min(amps,load.cell().availableCurrent(amps,load.positive(),load.negative(),.2));
        n=amps*.2/(2*WaterElectrolysis.FARADAY);
        if(n<=1e-15){stop(c,emf<=threshold?"电压不足：串联更多原电池":"回路无可用反应量");return;}
        // Preflight all chemistry, then commit. Electrons are the common 2 F n ledger.
        var trials=new ArrayList<ItemStack[]>();for(var pair:c.pairs())trials.add(transaction(pair,n));
        for(int i=0;i<c.pairs().size();i++){var pair=c.pairs().get(i);var trial=trials.get(i);pair.zinc().setStack(trial[0]);pair.copper().setStack(trial[1]);pair.bridge().setGrams(pair.bridge().grams()-n*2*ChemicalGarden.mm("solid","potassium_nitrate"));
            var zn=TroughSystem.part(pair.zinc(),"mesh",0);ElectrodePartItem.grams(zn,ElectrodePartItem.grams(zn)-n*65.38);TroughSystem.part(pair.zinc(),"mesh",0,zn);
            var cu=TroughSystem.part(pair.copper(),"mesh",0);ElectrodePartItem.addCoating(cu,"copper",n*63.546);TroughSystem.part(pair.copper(),"mesh",0,cu);
            for(var half:List.of(pair.zinc(),pair.copper())){half.setNumber("galv_tick",zinc.level().getGameTime());half.setNumber("charge_c",half.number("charge_c",0)+n*2*WaterElectrolysis.FARADAY);if(half==pair.zinc())half.setNumber("energy_j",half.number("energy_j",0)+n*2*WaterElectrolysis.FARADAY*1.1);half.state(amps,"原电池供电：Zn + Cu²⁺ → Zn²⁺ + Cu");}}
        for(var load:c.loads())load.cell().electrolyze(emf,amps,load.positive(),load.negative(),.2);
    }
    private static void stop(Circuit c,String reason){for(var pair:c.pairs()){pair.zinc().state(0,reason);pair.copper().state(0,reason);}for(var load:c.loads())load.cell().state(0,reason);}
    private static ItemStack[] transaction(Pair p,double n){
        var zn=p.zinc().stack();var cu=p.copper().stack();var before=SolutionSpecies.analyticalSnapshot(zn).plus(SolutionSpecies.analyticalSnapshot(cu));
        before=before.plus(new SpeciesInventory(Map.of("solid:zinc",n),Map.of())).plus(salt(n*2));
        LabVesselItem.consumeMass(cu,"liquid","copper_sulfate_solution",n*ChemicalGarden.mm("solid","copper_sulfate_anhydrous"));
        LabVesselItem.addMass(cu,"liquid","potassium_sulfate_solution",n*ChemicalGarden.mm("solid","potassium_sulfate"));
        LabVesselItem.addMass(zn,"liquid","zinc_nitrate_solution",n*ChemicalGarden.mm("solid","zinc_nitrate"));
        var after=SolutionSpecies.analyticalSnapshot(zn).plus(SolutionSpecies.analyticalSnapshot(cu)).plus(new SpeciesInventory(Map.of("solid:copper",n),Map.of()));
        if(!Conservation.compare(before,after).conserved())throw new IllegalStateException("Galvanic migration not conserved");return new ItemStack[]{zn,cu};
    }
    private static SpeciesInventory salt(double n){return new SpeciesInventory(Map.of("potassium",n,"nitrate",n),Map.of());}
    public static InteractionResult interact(ElectroDeviceEntity e,Player p,InteractionHand hand){
        if(e.level().isClientSide())return InteractionResult.SUCCESS;var held=p.getItemInHand(hand);
        if(held.is(ModItems.ELECTRICAL_WIRE.get())){ElectricConnections.click(e,e.isHalfCell()?0:e.pickTerminal(p),p,held);return InteractionResult.SUCCESS;}
        if(held.is(Items.SHEARS)){ElectricConnections.cut(p);return InteractionResult.SUCCESS;}
        if(e.isHalfCell()&&held.is(ModItems.SALT_BRIDGE.get())){SaltBridgeItem.click(e,p,held);return InteractionResult.SUCCESS;}
        if(e.isHalfCell()&&ElectrodePartItem.mesh(held)){
            if(!Set.of("zinc","copper").contains(ElectrodePartItem.material(held)))tell(p,"首版支持锌与铜电极");
            else if(TroughSystem.part(e,"mesh",0).isEmpty()){TroughSystem.part(e,"mesh",0,held.copyWithCount(1));held.shrink(1);}return InteractionResult.SUCCESS;
        }
        if(e.isHalfCell()&&held.getItem() instanceof LabVesselItem){var s=e.stack();if(p.isShiftKeyDown())ElectroDeviceEntity.transferPortion(s,held);else ElectroDeviceEntity.transferPortion(held,s);e.setStack(s);return InteractionResult.SUCCESS;}
        if(held.isEmpty()&&p.isShiftKeyDown()){
            var mesh=TroughSystem.part(e,"mesh",0);if(e.isHalfCell()&&!mesh.isEmpty()){TroughSystem.part(e,"mesh",0,ItemStack.EMPTY);if(!p.getInventory().add(mesh))p.drop(mesh,false);}
            else {var item=e.toStack();e.discard();if(!p.getInventory().add(item))p.drop(item,false);}return InteractionResult.SUCCESS;
        }
        tell(p,e.status());return InteractionResult.SUCCESS;
    }
    private static void tell(Player p,String s){p.displayClientMessage(Component.literal(s),true);}
    private GalvanicSystem(){}
}
