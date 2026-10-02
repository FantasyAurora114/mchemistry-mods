// Copyright (c) 2026 FantasyAurora (FantasyAurora114). All Rights Reserved.
// MChemistry author mark: Observer. See the repository LICENSE.
package com.example.chemistry.electrical;

import com.example.chemistry.*;
import com.example.chemistry.item.*;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.transfer.BottleCodes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.*;
import java.util.*;

/** First-stage open, mixed electrolyte bath. No diaphragm is assumed. */
public final class TroughSystem {
    public static final double MESH_SCALE=.60;
    public static ItemStack part(ElectroDeviceEntity e,String key,int side) {
        var encoded=ElectrodePartItem.data(e.stack()).get("ec_part_"+key+side);
        return encoded==null?ItemStack.EMPTY:ItemStack.CODEC.parse(e.level().registryAccess().createSerializationContext(NbtOps.INSTANCE),encoded).getOrThrow();
    }
    public static void part(ElectroDeviceEntity e,String key,int side,ItemStack value) {
        var s=e.stack();var t=ElectrodePartItem.data(s);
        if(value.isEmpty())t.remove("ec_part_"+key+side);else t.put("ec_part_"+key+side,ItemStack.CODEC.encodeStart(e.level().registryAccess().createSerializationContext(NbtOps.INSTANCE),value).getOrThrow());
        s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));e.setStack(s);
    }
    public static Vec3 point(ElectroDeviceEntity e,int side,double y) {
        double x=(side==0?4.25:11.75)-8,a=Math.toRadians(-e.getYRot());
        return e.position().add(x*Math.cos(a)/16,y/16,-x*Math.sin(a)/16);
    }
    public static Vec3 terminal(ElectroDeviceEntity e,int side) { return point(e,side,1+16.2*MESH_SCALE); }
    public static AABB bounds(ElectroDeviceEntity e) { return new AABB(e.getX()-.49,e.getY(),e.getZ()-.49,e.getX()+.49,e.getY()+1.5,e.getZ()+.49); }
    private static int side(ElectroDeviceEntity e,Player p) {
        Vec3 a=p.getEyePosition(),b=a.add(p.getLookAngle().scale(6));
        for(int i=0;i<2;i++)if(new AABB(point(e,i,1),point(e,i,20)).inflate(.18).clip(a,b).isPresent())return i;
        var hit=bounds(e).clip(a,b);if(hit.isEmpty())return -1;
        Vec3 d=hit.get().subtract(e.position());double rot=Math.toRadians(e.getYRot());return d.x*Math.cos(rot)+d.z*Math.sin(rot)<0?0:1;
    }
    private static void give(Player p,ItemStack s) { if(!s.isEmpty()&&!p.getInventory().add(s))p.drop(s,false); }
    private static void message(Player p,String s) { p.displayClientMessage(Component.literal(s),true); }
    public static InteractionResult interact(ElectroDeviceEntity e,Player p,InteractionHand hand) {
        if(e.level().isClientSide())return InteractionResult.SUCCESS;
        ItemStack held=p.getItemInHand(hand);int side=side(e,p);if(side<0)return InteractionResult.PASS;
        if(held.is(ModItems.ELECTRICAL_WIRE.get())) {
            if(part(e,"clip",side).isEmpty()){message(p,"请先安装鳄鱼夹");return InteractionResult.SUCCESS;}
            ElectricConnections.click(e,side,p,held);return InteractionResult.SUCCESS;
        }
        if(held.is(Items.SHEARS)){ElectricConnections.cut(p);return InteractionResult.SUCCESS;}
        var clip=part(e,"clip",side);var mesh=part(e,"mesh",side);var jar=part(e,"jar",side);
        if(ElectrodePartItem.clip(held)&&clip.isEmpty()) {
            part(e,"clip",side,held.copyWithCount(1));var installed=ElectrodePartItem.meshIn(e.level(),held);
            if(!installed.isEmpty()) { var bare=part(e,"clip",side);ElectrodePartItem.meshIn(e.level(),bare,ItemStack.EMPTY);part(e,"clip",side,bare);part(e,"mesh",side,installed); }
            held.shrink(1);message(p,"鳄鱼夹已安装；手持电极网点击可装入");
        } else if(ElectrodePartItem.mesh(held)&&!clip.isEmpty()&&mesh.isEmpty()) {
            part(e,"mesh",side,held.copyWithCount(1));held.shrink(1);message(p,"电极网已安装");
        } else if(BottleCodes.isGasBottle(held)&&jar.isEmpty()) {
            if(e.gasMoles(side)>0||BottleCodes.gasIdOf(held)!=null||BottleCodes.isWater(held)){message(p,"请使用空集气瓶");return InteractionResult.SUCCESS;}
            double reserved=e.number("jar_water_0",0)+e.number("jar_water_1",0);
            if(LabVesselItem.usedVolume(e.stack())<reserved+350){message(p,"请先加入足量液体：每个倒扣瓶占250 mL，槽底至少保留100 mL");return InteractionResult.SUCCESS;}
            var installed=held.copyWithCount(1);BottleCodes.setSealed(installed,false);
            e.setNumber("jar_water_"+side,250);part(e,"jar",side,installed);held.shrink(1);message(p,"已倒扣集气瓶；开放槽内氯气会先溶解并与碱反应");
        } else if(held.isEmpty()&&p.isShiftKeyDown()) {

            // The actual ray height chooses the bottle, mesh, clip, or the bath body.
            Vec3 eye=p.getEyePosition(),end=eye.add(p.getLookAngle().scale(6));
            boolean high=new AABB(point(e,side,12),point(e,side,23)).inflate(.15).clip(eye,end).isPresent();
            if(!jar.isEmpty()&&high)takeJar(e,p,side);
            else if(!mesh.isEmpty()){if(e.current()>0){message(p,"请先关闭电源再拆电极网");return InteractionResult.SUCCESS;}part(e,"mesh",side,ItemStack.EMPTY);give(p,mesh);message(p,"已取下电极网，保留余量和镀层");}
            else if(!clip.isEmpty()){if(ElectricConnections.occupied(e,side)){message(p,"请先剪下这个鳄鱼夹上的电线");return InteractionResult.SUCCESS;}part(e,"clip",side,ItemStack.EMPTY);give(p,clip);}
            else { var s=e.toStack();if(e.flag("host_block"))e.level().removeBlock(e.blockPosition(),false);e.discard();give(p,s); }
        } else if(held.getItem() instanceof LabVesselItem) {
            if(e.current()>0){message(p,"请先关闭电源再转移液体");return InteractionResult.SUCCESS;}
            var liquid=e.stack();boolean ok=p.isShiftKeyDown()?ElectroDeviceEntity.transferPortion(liquid,held):ElectroDeviceEntity.transferPortion(held,liquid);
            if(ok)e.setStack(liquid);message(p,ok?"已转移最多25 mL液体":"没有可转移液体或容量不足");
        } else if(held.is(Items.WATER_BUCKET)&&LabVesselItem.usedVolume(e.stack())<1e-8) {
            var s=e.stack();LabVesselItem.addMass(s,"liquid","water",1000);e.setStack(s);if(!p.isCreative())p.setItemInHand(hand,new ItemStack(Items.BUCKET));
        } else if(!held.isEmpty()) {
            if(e.current()>0){message(p,"请先关闭电源再加料");return InteractionResult.SUCCESS;}
            var s=e.stack();if(LabInteractions.interactPlacedVessel(held,s,ItemStack.EMPTY,ItemStack.EMPTY,p))e.setStack(s);
        } else message(p,String.format(Locale.ROOT,"高水槽 %.2f/1000 mL | %s；潜行点网片拆网，点集气瓶取瓶",LabVesselItem.usedVolume(e.stack()),e.status()));
        return InteractionResult.SUCCESS;
    }
    public static void takeJar(ElectroDeviceEntity e,Player p,int side) {
        var jar=part(e,"jar",side);if(jar.isEmpty())return;
        double exact=e.gasMl(side);int ml=(int)Math.floor(exact+1e-9);String gas=e.gas(side);
        BottleCodes.setGas(jar,ml>0?gas:null,true);var t=ElectrodePartItem.data(jar);t.putLong(BottleCodes.KEY_ML,ml);jar.set(DataComponents.CUSTOM_DATA,CustomData.of(t));BottleCodes.refreshModel(jar);
        // Bottle transfer boundary is integer mL; the sub-mL remainder escapes on lifting the mouth.
        e.setNumber("vented_"+gas,e.number("vented_"+gas,0)+Math.max(0,exact-ml)/WaterElectrolysis.GAS_ML_PER_MOLE);
        e.setGas(side,"",0);e.setNumber("jar_water_"+side,0);part(e,"jar",side,ItemStack.EMPTY);give(p,jar);
    }
    public static double mass(ItemStack s,String id) { return LabVesselItem.getContents(s).stream().filter(x->x.type().equals("liquid")&&x.id().equals(id)).mapToDouble(LabVesselItem.Entry::amount).sum(); }
    public static String mode(ItemStack s) {
        if(mass(s,"copper_sulfate_solution")>0)return "copper";
        if(mass(s,"silver_nitrate_solution")>0)return "silver";
        if(BrineElectrolysis.salt(s)>0)return "brine";
        return "water";
    }
    private static Set<String> allowed(String mode) {
        return switch(mode) {
            case "copper" -> Set.of("water","copper_sulfate_solution","sulfuric_acid","sulfuric_acid_dilute");
            case "silver" -> Set.of("water","silver_nitrate_solution","nitric_acid","nitric_acid_dilute");
            case "brine" -> Set.of("water","sodium_chloride_solution","sodium_hydroxide_solution","chlorine_water","sodium_hypochlorite_solution","sodium_chlorate_solution");
            default -> Set.of("water","sodium_hydroxide_solution","potassium_hydroxide_solution");
        };
    }
    public static String guard(ElectroDeviceEntity e,int positive,int negative) {
        var s=e.stack();LabVesselItem.normalizeSolutions(s);String mode=mode(s);
        if(!Double.isFinite(LabVesselItem.usedVolume(s)))return "液量异常";
        if(LabVesselItem.usedVolume(s)<200||WaterElectrolysis.water(s)<=0)return "请加入至少200 mL水溶液浸没电极网";
        for(int i=0;i<2;i++)if(part(e,"clip",i).isEmpty()||ElectrodePartItem.grams(part(e,"mesh",i))<=1e-12)return "两侧都需安装鳄鱼夹和有余量的电极网";
        if(LabVesselItem.getContents(s).stream().anyMatch(x->!x.type().equals("liquid")||!allowed(mode).contains(x.id())))return "该混合电解液尚未支持";
        String anode=ElectrodePartItem.material(part(e,"mesh",positive));
        if(!anode.equals("platinum")&&!anode.equals(mode))return "此电解液需铂阳极，或同种金属盐的铜/银阳极";
        if(mode.equals("brine")&&BrineElectrolysis.salt(s)<=.20*WaterElectrolysis.water(s))return "食盐水过稀，首版仅支持浓盐水";
        return "";
    }
    public static double conductance(ElectroDeviceEntity e) {
        String mode=mode(e.stack());return mode.equals("water")?WaterElectrolysis.conductance(e.stack()):.5;
    }
    public static double threshold(ElectroDeviceEntity e) { return mode(e.stack()).equals("brine")?2.2:1.8; }
    public static double available(ElectroDeviceEntity e,double amps,int pos,int neg,double seconds) {
        if(!guard(e,pos,neg).isEmpty()||amps<=0||seconds<=0)return 0;
        String mode=mode(e.stack());double q=amps*seconds,water=WaterElectrolysis.water(e.stack());
        if(mode.equals("brine"))return BrineElectrolysis.calculate(amps,seconds,water,BrineElectrolysis.salt(e.stack()),1e12,1e12,1e12).charge()/seconds;
        if(mode.equals("water"))return Math.min(q,water/18.015*2*WaterElectrolysis.FARADAY)/seconds;
        double z=mode.equals("copper")?2:1,metal=mode.equals("copper")?63.546:107.8682;
        var anode=part(e,"mesh",pos);
        if(ElectrodePartItem.material(anode).equals(mode))q=Math.min(q,ElectrodePartItem.grams(anode)/metal*z*WaterElectrolysis.FARADAY);
        else {
            double salt=mode.equals("copper")?159.609:169.8731;
            q=Math.min(q,Math.min(mass(e.stack(),mode.equals("copper")?"copper_sulfate_solution":"silver_nitrate_solution")/salt,water/(18.015*z/2))*z*WaterElectrolysis.FARADAY);
        }
        return Math.max(0,q/seconds);
    }
    public static double run(ElectroDeviceEntity e,double amps,int pos,int neg,double seconds) {
        amps=available(e,amps,pos,neg,seconds);if(amps<=0){e.state(0,guard(e,pos,neg));return 0;}
        var s=e.stack();LabVesselItem.normalizeSolutions(s);String mode=mode(s);double q=amps*seconds;
        if(mode.equals("water")) {
            double n=q/(2*WaterElectrolysis.FARADAY);LabVesselItem.consumeMass(s,"liquid","water",18.015*n);e.setStack(s);gas(e,neg,"hydrogen",n);gas(e,pos,"oxygen",n/2);
        } else if(mode.equals("brine")) {
            var r=BrineElectrolysis.calculate(amps,seconds,WaterElectrolysis.water(s),BrineElectrolysis.salt(s),1e12,1e12,1e12);
            LabVesselItem.consumeMass(s,"liquid","water",r.waterGrams());LabVesselItem.consumeMass(s,"liquid","sodium_chloride_solution",r.saltGrams());LabVesselItem.addMass(s,"liquid","sodium_hydroxide_solution",r.baseGrams());e.setStack(s);
            gas(e,neg,"hydrogen",r.gasMoles());chlorine(e,pos,r.gasMoles());q=r.charge();
        } else {
            double z=mode.equals("copper")?2:1,n=q/(z*WaterElectrolysis.FARADAY),metal=mode.equals("copper")?63.546:107.8682;
            var anode=part(e,"mesh",pos);var cathode=part(e,"mesh",neg);
            if(ElectrodePartItem.material(anode).equals(mode)) { ElectrodePartItem.grams(anode,ElectrodePartItem.grams(anode)-n*metal);part(e,"mesh",pos,anode); }
            else {
                LabVesselItem.consumeMass(s,"liquid",mode.equals("copper")?"copper_sulfate_solution":"silver_nitrate_solution",n*(mode.equals("copper")?159.609:169.8731));
                LabVesselItem.consumeMass(s,"liquid","water",n*18.015*z/2);
                LabVesselItem.addMass(s,"liquid",mode.equals("copper")?"sulfuric_acid_dilute":"nitric_acid_dilute",n*(mode.equals("copper")?98.079:63.012));e.setStack(s);gas(e,pos,"oxygen",n*z/4);
            }
            ElectrodePartItem.addCoating(cathode,mode,n*metal);part(e,"mesh",neg,cathode);
        }
        e.setNumber("charge_c",e.number("charge_c",0)+q);e.state(q/seconds,"正在电解 / "+mode+"；开放槽，无隔膜");
        if(e.level() instanceof ServerLevel l&&q>.005)for(int i=0;i<2;i++){var p=point(e,i,4);l.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP,p.x,p.y,p.z,2,.03,.05,.03,0);}
        return q/seconds;
    }
    private static void gas(ElectroDeviceEntity e,int side,String id,double n) {
        double collected=0;
        if(!part(e,"jar",side).isEmpty()&&(e.gasMoles(side)<=1e-15||e.gas(side).equals(id))) {
            collected=Math.min(n,Math.max(0,(250-e.gasMl(side))/WaterElectrolysis.GAS_ML_PER_MOLE));e.setNumber("jar_water_"+side,Math.max(0,e.number("jar_water_"+side,0)-collected*WaterElectrolysis.GAS_ML_PER_MOLE));e.setGas(side,id,e.gasMoles(side)+collected);
        }
        double escaped=Math.max(0,n-collected);e.setNumber("vented_"+id,e.number("vented_"+id,0)+escaped);
        if(escaped>0&&id.equals("chlorine")&&e.level() instanceof ServerLevel l){var p=point(e,side,12);l.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,p.x,p.y,p.z,1,.03,.05,.03,0);}
    }
    public static void chlorine(ElectroDeviceEntity e,int side,double n) {
        var s=e.stack();double old=mass(s,"chlorine_water");LabVesselItem.consumeMass(s,"liquid","chlorine_water",old);n+=old/70.9;
        double base=mass(s,"sodium_hydroxide_solution"),r=Math.min(n,base/(2*39.997));
        if(r>0) {
            boolean hot=TemperatureSystem.getTemp(s)>=80&&base/Math.max(1e-12,WaterElectrolysis.water(s))>=.15;
            LabVesselItem.consumeMass(s,"liquid","sodium_hydroxide_solution",r*2*39.997);
            LabVesselItem.addMass(s,"liquid","sodium_chloride_solution",r*58.44*(hot?5.0/3:1));
            LabVesselItem.addMass(s,"liquid",hot?"sodium_chlorate_solution":"sodium_hypochlorite_solution",r*(hot?106.437/3:74.439));
            LabVesselItem.addMass(s,"liquid","water",r*18.015);n-=r;e.flag("hot_chlorine",hot);
        }
        // NIOSH 0.7 g/100mL at 20 C, used as a provisional room-temperature ceiling.
        // This is a well-mixed approximation, not a measured mass-transfer rate or Henry curve.
        double dissolved=Math.min(n,.007*WaterElectrolysis.water(s)/70.9);
        if(dissolved>0)LabVesselItem.addMass(s,"liquid","chlorine_water",dissolved*70.9);
        e.setStack(s);gas(e,side,"chlorine",Math.max(0,n-dissolved));
    }
    public static void info(ElectroDeviceEntity e,List<Component> lines) {
        lines.add(e.stack().getHoverName());lines.add(Component.literal(String.format(Locale.ROOT,"液体 %.2f /1000 mL | 温度 %.1f°C | %.4f A",LabVesselItem.usedVolume(e.stack()),TemperatureSystem.getTemp(e.stack()),e.current())));
        for(int i=0;i<2;i++){var mesh=part(e,"mesh",i);lines.add(Component.literal((i==0?"左":"右")+"电极："+(mesh.isEmpty()?"未安装":mesh.getHoverName().getString()+String.format(Locale.ROOT," %.4fg | 铜镀层 %.4fg / 银 %.4fg",ElectrodePartItem.grams(mesh),ElectrodePartItem.coating(mesh,"copper"),ElectrodePartItem.coating(mesh,"silver")))));lines.add(Component.literal((i==0?"左":"右")+"集气："+com.example.chemistry.api.goggles.ChemGoggleLines.gasName(e.gas(i))+String.format(Locale.ROOT," %.3f mL",e.gasMl(i))));}
        lines.add(Component.literal(e.status()));com.example.chemistry.api.goggles.ChemGoggleLines.appendContents(lines,e.stack());
    }
}
