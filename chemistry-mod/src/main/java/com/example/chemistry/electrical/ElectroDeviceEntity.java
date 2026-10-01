package com.example.chemistry.electrical;

import java.util.*;
import com.example.chemistry.*;
import com.example.chemistry.api.goggles.IChemGoggleInfo;
import com.example.chemistry.entity.*;
import com.example.chemistry.item.*;
import com.example.chemistry.registry.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Laboratory supply and authored Hofmann apparatus; chemistry travels with the item when picked up. */
public class ElectroDeviceEntity extends TechnicalEntity implements IChemGoggleInfo {
    private static final EntityDataAccessor<ItemStack> STACK=SynchedEntityData.defineId(ElectroDeviceEntity.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> CURRENT=SynchedEntityData.defineId(ElectroDeviceEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> STATUS=SynchedEntityData.defineId(ElectroDeviceEntity.class,EntityDataSerializers.STRING);
    public static final float MODEL_SCALE=.65F;
    private long lastProcessed=-1;
    private long lastPowerProcessed=-1;
    private final double[] visualGas=new double[2],previousVisualGas=new double[2];
    private boolean visualInitialized;
    public double visualGasMl(int side,float partial){
        return visualInitialized?previousVisualGas[side]+(visualGas[side]-previousVisualGas[side])*Math.clamp(partial,0,1):gasMl(side);
    }
    private void updateVisualGas(){
        for(int i=0;i<2;i++){
            if(!visualInitialized)visualGas[i]=gasMl(i);
            previousVisualGas[i]=visualGas[i];
            visualGas[i]+=(gasMl(i)-visualGas[i])*.35;
        }
        visualInitialized=true;
    }
    public ElectroDeviceEntity(EntityType<?> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(STACK,ItemStack.EMPTY);b.define(CURRENT,0F);b.define(STATUS,"未连接");}
    public ItemStack stack(){return entityData.get(STACK).copy();}
    public void setStack(ItemStack s){entityData.set(STACK,s.copy());}
    public boolean isHalfCell(){return entityData.get(STACK).is(ModItems.GALVANIC_HALF_CELL.get());}
    public boolean isResistor(){return entityData.get(STACK).is(ModItems.LAB_RESISTOR.get());}
    public boolean isVoltmeter(){return entityData.get(STACK).is(ModItems.LAB_VOLTMETER.get());}
    public boolean isTrough(){return entityData.get(STACK).is(ModItems.DEEP_WATER_TROUGH.get());}
    public boolean isPower(){return entityData.get(STACK).is(ModItems.BENCH_POWER_SUPPLY.get());}
    private static CompoundTag tag(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public double number(String key,double fallback){return tag(stack()).getDoubleOr("ec_"+key,fallback);}
    public boolean flag(String key){return tag(stack()).getBooleanOr("ec_"+key,false);}
    public void setNumber(String key,double value){var s=stack();var t=tag(s);t.putDouble("ec_"+key,value);s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));setStack(s);}
    public void flag(String key,boolean value){var s=stack();var t=tag(s);t.putBoolean("ec_"+key,value);s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));setStack(s);}
    public String gas(int side){return tag(stack()).getStringOr("ec_gas_"+side,"");}
    public double gasMoles(int side){return number("moles_"+side,0);}
    public double gasMl(int side){return gasMoles(side)*WaterElectrolysis.GAS_ML_PER_MOLE;}
    public void setGas(int side,String gas,double moles){var s=stack();var t=tag(s);t.putString("ec_gas_"+side,moles>1e-15?gas:"");if(!gas.isEmpty())t.putString("ec_last_gas_"+side,gas);t.putDouble("ec_moles_"+side,Math.max(0,moles));s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));setStack(s);}
    public float current(){return entityData.get(CURRENT);}
    public String status(){return entityData.get(STATUS);}
    public void state(double current,String status){entityData.set(CURRENT,(float)current);entityData.set(STATUS,status);}
    public Vec3 modelPoint(double x,double y,double z){double a=Math.toRadians(-getYRot()),dx=(x-8)/16*MODEL_SCALE,dz=(z-8)/16*MODEL_SCALE;return position().add(dx*Math.cos(a)+dz*Math.sin(a),y/16*MODEL_SCALE,-dx*Math.sin(a)+dz*Math.cos(a));}
    public Vec3 terminal(int slot){if(isHalfCell())return position().add(0,.6,0);if(isResistor()||isVoltmeter())return modelPoint(slot==0?3:13,4,8);if(isTrough())return TroughSystem.terminal(this,slot);return isPower()?modelPoint(switch(slot){case 0->13.6;case 1->12;case 2->2.4;default->4;},2.45,1.13):modelPoint(slot==0?.4:15.6,6.9,8);}
    public Vec3 outlet(int side){return modelPoint(side==0?3:13,26.6,8);}
    public boolean aim(Player p,Vec3 point,double radius){return new AABB(point,point).inflate(radius*MODEL_SCALE).clip(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(6))).isPresent();}
    public int pickTerminal(Player p){if(isHalfCell())return aim(p,terminal(0),.3)?0:-1;if(isTrough()){for(int i=0;i<2;i++)if(aim(p,terminal(i),.22))return i;return -1;}int result=-1;double best=Double.MAX_VALUE;for(int slot=0;slot<(isPower()?4:2);slot++){Vec3 point=terminal(slot);var hit=new AABB(point,point).inflate((isPower()?.045:.10)*MODEL_SCALE).clip(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(6)));if(hit.isPresent()&&hit.get().distanceToSqr(p.getEyePosition())<best){best=hit.get().distanceToSqr(p.getEyePosition());result=slot;}}return result;}
    @Override public AABB virtualHitbox(){if(isHalfCell())return new AABB(getX()-.3,getY(),getZ()-.3,getX()+.3,getY()+.75,getZ()+.3);if(isResistor()||isVoltmeter())return new AABB(getX()-.35,getY(),getZ()-.35,getX()+.35,getY()+.4,getZ()+.35);if(isTrough())return TroughSystem.bounds(this);return new AABB(getX()-.5*MODEL_SCALE,getY(),getZ()-.5*MODEL_SCALE,getX()+.5*MODEL_SCALE,getY()+(isPower()?.85:1.74)*MODEL_SCALE,getZ()+.5*MODEL_SCALE);}
    @Override public ItemStack toStack(){return stack();}
    @Override protected void addAdditionalSaveData(ValueOutput out){out.store("device",ItemStack.OPTIONAL_CODEC,stack());}
    @Override protected void readAdditionalSaveData(ValueInput in){setStack(in.read("device",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));}
    @Override public void tick(){super.tick();
        if(isTrough()&&flag("host_block")&&!level().isClientSide()&&!level().getBlockState(blockPosition()).is(com.example.chemistry.registry.ModBlocks.DEEP_WATER_TROUGH.get())){spawnAtLocation((ServerLevel)level(),toStack());discard();return;}setBoundingBox(virtualHitbox());if(level().isClientSide()){updateVisualGas();return;}if(tickCount%4!=0)return;
        if(isHalfCell()){GalvanicSystem.tick(this);return;}
        if(isPower())tickPower();else {if(isVoltmeter()){if(level().getGameTime()-number("measurement_tick",-100)>5){state(0,"未连接原电池测量回路");setNumber("measured_v",0);}}else if(level().getGameTime()-lastProcessed>5)state(0,"未通电");if(!isTrough()&&!isResistor()&&!isVoltmeter())tickValves();else {var bath=stack();PhaseSystem.dissolveAndCrystallize(bath,false);VesselHeating.coolGradual(bath);setStack(bath);}}
    }
    public double channelNumber(int channel,String key,double fallback){return number(channel==0?key:"ch2_"+key,fallback);}
    public boolean channelOn(int channel){return flag("on")&&!flag("disabled_"+channel);}
    public double channelCurrent(int channel){return channel==0?current():number("ch2_current",0);}
    public void tickPower(){
        boolean chargeThisTick=lastPowerProcessed!=level().getGameTime();
        lastPowerProcessed=level().getGameTime();
        StringBuilder info=new StringBuilder();
        for(int channel=0;channel<2;channel++){
            double actual=0;String status="关闭";
            var chain=ElectricConnections.series(this,channel);
            if(channelOn(channel)){
                if(ElectricConnections.shorted(this,channel))status="短路保护";
                else if(chain.isEmpty())status="回路未闭合";
                else {
                    double voltage=channelNumber(channel,"voltage",6),limit=channelNumber(channel,"limit",1);
                    double resistance=0,threshold=0;
                    for(var c:chain){var liquid=c.cell().stack();boolean brine=BrineElectrolysis.supported(liquid);
                        threshold+=c.cell().isResistor()?0:c.cell().isTrough()?TroughSystem.threshold(c.cell()):brine?2.2:1.8;resistance+=c.cell().isResistor()?100:1/Math.max(1e-9,c.cell().isTrough()?TroughSystem.conductance(c.cell()):brine?.5:WaterElectrolysis.conductance(liquid));}
                    actual=Math.min(limit,Math.max(0,(voltage-threshold)/resistance));
                    // Validate every cell before committing any charge; a full cell stops the whole loop.
                    String blocked="";int index=0;
                    for(var c:chain){index++;String reason=c.cell().stopReason(c.positive(),c.negative());
                        if(!reason.isEmpty()){blocked="第"+index+"台："+reason;actual=0;break;}
                        actual=Math.min(actual,c.cell().availableCurrent(actual,c.positive(),c.negative(),.2));
                        if(actual<=0){blocked="第"+index+"台：无可用反应量或储气空间";break;}}
                    if(voltage<=threshold)blocked=String.format(java.util.Locale.ROOT,"%d台需超过%.1f V，当前%.1f V",chain.size(),threshold,voltage);
                    if(actual>0){if(chargeThisTick){for(var c:chain)c.cell().electrolyze(12,actual,c.positive(),c.negative(),.2);
                        setNumber("energy_j",number("energy_j",0)+actual*.2*voltage);}status="串联 "+chain.size()+" 台，正在电解";
                    }else {status=blocked.isEmpty()?"电解液导电太弱或反应量不足":blocked;for(var c:chain)c.cell().state(0,"串联停止："+status);}
                }
            }
            if(channel==0)entityData.set(CURRENT,(float)actual);else setNumber("ch2_current",actual);
            if(channel>0)info.append(" | ");info.append("CH").append(channel+1).append(" ").append(status);
        }
        entityData.set(STATUS,info.toString());
    }
    @Override public boolean hurtServer(ServerLevel l,net.minecraft.world.damagesource.DamageSource d,float amount){
        if(isTrough()&&flag("host_block"))l.removeBlock(blockPosition(),false);
        return super.hurtServer(l,d,amount);
    }
    public String stopReason(int positive,int negative){
        if(isResistor())return positive>=0&&positive<2&&negative==1-positive?"":"无效负载端口";
        if(isVoltmeter())return "电压表采用高阻测量";
        if(isHalfCell())return "不能直接用实验电源驱动原电池半室";
        if(isTrough())return TroughSystem.guard(this,positive,negative);
        if(positive<0||positive>1||negative!=1-positive)return "接线端子无效";
        var s=stack();LabVesselItem.normalizeSolutions(s);boolean brine=BrineElectrolysis.supported(s);
        if(!brine&&!WaterElectrolysis.supported(s))return "不支持此电解液";
        if(WaterElectrolysis.water(s)<=0)return "没有可电解的水";
        if(LabVesselItem.usedVolume(s)<(flag("primed")?20:120))return flag("primed")?"液位过低，电极未浸没":"未预充，请加入至少120 mL液体";
        String anode=brine?"chlorine":"oxygen";
        if((gasMoles(positive)>1e-15&&!gas(positive).equals(anode))||(gasMoles(negative)>1e-15&&!gas(negative).equals("hydrogen")))return "残留气体与当前极性不符，请先排空两侧集气管";
        if(gasMl(0)>=50-1e-9||gasMl(1)>=50-1e-9)return "集气管已满，请排气";
        if(250-LabVesselItem.usedVolume(s)-gasMl(0)-gasMl(1)<=1e-9)return "储液空间不足，请减少加液量或排气";
        if(brine&&BrineElectrolysis.salt(s)<=.20*WaterElectrolysis.water(s))return "食盐水浓度过低";
        return "";
    }
    public double availableCurrent(double amps,int positive,int negative,double seconds){
        if(isResistor())return amps;
        if(isVoltmeter()||isHalfCell())return 0;
        if(isTrough())return TroughSystem.available(this,amps,positive,negative,seconds);
        if(lastProcessed==level().getGameTime())return Math.min(amps,current());
        ItemStack liquid=stack();LabVesselItem.normalizeSolutions(liquid);
        boolean brine=BrineElectrolysis.supported(liquid);
        if(!brine&&!WaterElectrolysis.supported(liquid))return 0;
        double volume=LabVesselItem.usedVolume(liquid);
        if(volume<(flag("primed")?20:120))return 0;
        String anode=brine?"chlorine":"oxygen";
        if((gasMoles(positive)>1e-15&&!gas(positive).equals(anode))||(gasMoles(negative)>1e-15&&!gas(negative).equals("hydrogen")))return 0;
        double space=250-volume-gasMl(0)-gasMl(1);
        if(brine)return BrineElectrolysis.calculate(amps,seconds,WaterElectrolysis.water(liquid),BrineElectrolysis.salt(liquid),50-gasMl(negative),50-gasMl(positive),space).charge()/seconds;
        return WaterElectrolysis.calculate(amps,seconds,WaterElectrolysis.water(liquid),50-gasMl(negative),50-gasMl(positive),space).charge()/seconds;
    }
    public double electrolyze(double voltage,double limit,int positive,int negative,double seconds){
        if(isResistor()){if(lastProcessed==level().getGameTime())return current();lastProcessed=level().getGameTime();setNumber("energy_j",number("energy_j",0)+limit*limit*100*seconds);state(limit,"负载工作中");return limit;}
        if(isVoltmeter()||isHalfCell())return 0;
        if(isTrough()){if(lastProcessed==level().getGameTime())return current();lastProcessed=level().getGameTime();return TroughSystem.run(this,Math.min(limit,Math.max(0,(voltage-TroughSystem.threshold(this))*TroughSystem.conductance(this))),positive,negative,seconds);}
        if(isPower()||positive==negative||positive<0||negative<0||positive>1||negative>1)return 0;
        if(lastProcessed==level().getGameTime())return current();lastProcessed=level().getGameTime();
        ItemStack liquid=stack();LabVesselItem.normalizeSolutions(liquid);setStack(liquid);
        boolean brine=BrineElectrolysis.supported(liquid);
        if(!brine&&!WaterElectrolysis.supported(liquid)){state(0,"目前支持水、NaOH/KOH 溶液和浓食盐水");return 0;}
        if(brine&&BrineElectrolysis.salt(liquid)<=BrineElectrolysis.MIN_SALT_WATER_RATIO*WaterElectrolysis.water(liquid)){state(0,"食盐水浓度过低，请更换浓盐水；暂不模拟稀盐水电解");return 0;}
        if(!flag("primed")){if(LabVesselItem.usedVolume(liquid)<120){state(0,"请先加入至少 120 mL 液体完成预充");return 0;}flag("primed",true);liquid=stack();}
        if(LabVesselItem.usedVolume(liquid)<20){state(0,"液位过低，电极未浸没");return 0;}
        String anodeGas=brine?"chlorine":"oxygen";
        if((gasMoles(positive)>1e-15&&!gas(positive).equals(anodeGas))||(gasMoles(negative)>1e-15&&!gas(negative).equals("hydrogen"))){state(0,"反接或更换电解液前请先排空两侧集气管");return 0;}
        double amps=Math.min(Math.max(0,Math.min(2,limit)),Math.max(0,Math.min(12,voltage)-1.8)*WaterElectrolysis.conductance(liquid));
        double space=250-LabVesselItem.usedVolume(liquid)-gasMl(0)-gasMl(1);
        if(brine){
            amps=Math.min(Math.max(0,Math.min(2,limit)),Math.max(0,Math.min(12,voltage)-2.2)*.5);
            var result=BrineElectrolysis.calculate(amps,seconds,WaterElectrolysis.water(liquid),
                    BrineElectrolysis.salt(liquid),50-gasMl(negative),50-gasMl(positive),space);
            if(result.charge()<=0){state(0,amps<=0?"电压不足":"浓度过低、集气管已满或储液空间不足");return 0;}
            LabVesselItem.consumeMass(liquid,"liquid","water",result.waterGrams());
            LabVesselItem.consumeMass(liquid,"liquid","sodium_chloride_solution",result.saltGrams());
            LabVesselItem.addMass(liquid,"liquid","sodium_hydroxide_solution",result.baseGrams());
            setStack(liquid);
            setGas(negative,"hydrogen",gasMoles(negative)+result.gasMoles());
            setGas(positive,"chlorine",gasMoles(positive)+result.gasMoles());
            setNumber("charge_c",number("charge_c",0)+result.charge());
        setNumber("water_consumed_g",number("water_consumed_g",0)+result.waterGrams());
            double actual=result.charge()/seconds;
            state(actual,"电解浓盐水（理想隔离）：2NaCl + 2H₂O → 2NaOH + H₂ + Cl₂");
            if(result.charge()>.005&&level() instanceof ServerLevel server)for(int side=0;side<2;side++){
                Vec3 p=modelPoint(side==0?3:13,10,8);server.sendParticles(ParticleTypes.BUBBLE_POP,p.x,p.y,p.z,1,.015,.12,.015,0);
            }
            return actual;
        }
        var result=WaterElectrolysis.calculate(amps,seconds,WaterElectrolysis.water(liquid),50-gasMl(negative),50-gasMl(positive),space);
        if(result.charge()<=0){state(0,amps<=0?"电压不足":"集气管已满或储液空间不足");return 0;}
        // All limits validated before the first commit; the electrolyte itself is retained.
        LabVesselItem.consumeMass(liquid,"liquid","water",result.waterGrams());setStack(liquid);
        setGas(negative,"hydrogen",gasMoles(negative)+result.hydrogenMoles());setGas(positive,"oxygen",gasMoles(positive)+result.oxygenMoles());
        setNumber("charge_c",number("charge_c",0)+result.charge());
        setNumber("water_consumed_g",number("water_consumed_g",0)+result.waterGrams());
        double actual=result.charge()/seconds;state(actual,WaterElectrolysis.conductance(liquid)<1e-5?"纯水导电很弱":"正在电解水");
        if(result.charge()>.005&&level() instanceof ServerLevel server)for(int side=0;side<2;side++){Vec3 p=modelPoint(side==0?3:13,10,8);server.sendParticles(ParticleTypes.BUBBLE_POP,p.x,p.y,p.z,side==negative?2:1,.015,.12,.015,0);}
        return actual;
    }
    public void handleGasTube(Player p,ItemStack tube){if(isPower())return;int side=aim(p,outlet(0),.18)?0:aim(p,outlet(1),.18)?1:-1;
        if(side<0){message(p,"请对准顶部的独立出气口");return;}if(!RubberTubeItem.isWet(tube)){message(p,"先将橡胶管沾湿");return;}
        var port=RubberTubeEntity.Port.stand(blockPosition(),3+side);if(RubberTubeItem.hasTubeAt(level(),port)){message(p,"该出气口已接有橡胶管");return;}
        var pending=RubberTubeItem.readPending(tube);if(pending==null)RubberTubeItem.startPending(level(),p,tube,port);else if(!RubberTubeItem.sameAnchor(pending,port))RubberTubeItem.createTube(level(),p,tube,pending,port);
    }
    void tickValves(){for(int side=0;side<2;side++)if(flag("valve_"+side))release(side);}
    private void release(int side){
        String gas=gas(side);double available=gasMl(side);if(available<=0)return;
        var port=RubberTubeEntity.Port.stand(blockPosition(),side+3);var tubes=RubberTubeItem.findTubesAt(level(),port);
        if(tubes.isEmpty()) {double ml=Math.min(available,2);setGas(side,gas,(available-ml)/WaterElectrolysis.GAS_ML_PER_MOLE);setNumber("vented_"+gas,number("vented_"+gas,0)+ml/WaterElectrolysis.GAS_ML_PER_MOLE);return;}
        var tube=tubes.getFirst();var far=RubberTubeItem.sameAnchor(tube.getAnchorA(),port)?tube.getAnchorB():tube.getAnchorA();
        if(tube.getTransitMl()>0){String transit=tube.getTransitGas();int accepted=GasFlowEngine.deliverTo(level(),far,transit,Math.min(2,tube.getTransitMl()),tube.getTransitPurity());tube.takeTransit(accepted);}
        int amount=Math.min(2,Math.min((int)Math.floor(available),Math.max(0,4-tube.getTransitMl())));
        if(amount>0){int accepted=tube.addTransit(gas,amount,1);setGas(side,gas,gasMoles(side)-accepted/WaterElectrolysis.GAS_ML_PER_MOLE);setNumber("exported_"+gas,number("exported_"+gas,0)+accepted/WaterElectrolysis.GAS_ML_PER_MOLE);}
    }
    private static void message(Player p,String s){p.displayClientMessage(Component.literal(s),true);}
    @Override public InteractionResult interact(Player p,InteractionHand hand){if(isHalfCell()||isResistor()||isVoltmeter())return GalvanicSystem.interact(this,p,hand);if(isTrough())return TroughSystem.interact(this,p,hand);ItemStack held=p.getItemInHand(hand);if(level().isClientSide())return InteractionResult.SUCCESS;
        if(held.is(ModItems.ELECTRICAL_WIRE.get())){ElectricConnections.click(this,pickTerminal(p),p,held);return InteractionResult.SUCCESS;}
        if(held.is(Items.SHEARS)){ElectricConnections.cut(p);return InteractionResult.SUCCESS;}
        if(held.getItem() instanceof RubberTubeItem){handleGasTube(p,held);return InteractionResult.SUCCESS;}
        if(isPower()) {
            if(held.isEmpty()){
                boolean handled=false;
                for(int channel=0;channel<2;channel++){
                    String prefix=channel==0?"":"ch2_";
                    if(aim(p,modelPoint(channel==0?13.35:2.65,4.45,1.3),.10)){
                        setNumber(prefix+"voltage",Math.clamp(channelNumber(channel,"voltage",6)+(p.isShiftKeyDown()?-1:1),0,24));handled=true;break;
                    }
                    if(aim(p,modelPoint(channel==0?10.65:5.35,4.45,1.3),.10)){
                        setNumber(prefix+"limit",Math.clamp(channelNumber(channel,"limit",1)+(p.isShiftKeyDown()?-.1:.1),.1,2));handled=true;break;
                    }
                    if(aim(p,modelPoint(channel==0?12:4,7,2.3),.17)){
                        flag("disabled_"+channel,!flag("disabled_"+channel));handled=true;break;
                    }
                }
                if(!handled){if(p.isShiftKeyDown()){flag("on",false);pickUp(p);return InteractionResult.SUCCESS;}flag("on",!flag("on"));}
                message(p,String.format(java.util.Locale.ROOT,"电源 %s | CH1 %.1f V / %.1f A | CH2 %.1f V / %.1f A",flag("on")?"开启":"关闭",number("voltage",6),number("limit",1),number("ch2_voltage",6),number("ch2_limit",1)));
            }
        } else {
            for(int side=0;side<2;side++)if(held.isEmpty()&&aim(p,modelPoint(side==0?3:13,24.56,6),.14)){flag("valve_"+side,!flag("valve_"+side));message(p,(side==0?"左":"右")+"旋塞"+(flag("valve_"+side)?"打开":"关闭"));return InteractionResult.SUCCESS;}
            if(held.isEmpty()&&p.isShiftKeyDown()){pickUp(p);return InteractionResult.SUCCESS;}
            if(!held.isEmpty() && gasMoles(0)+gasMoles(1)>1e-15) {
                message(p,"请先排空两侧气体，再加液或倒液");return InteractionResult.SUCCESS;
            }
            if(held.getItem() instanceof LabVesselItem && !held.is(ModItems.BENCH_POWER_SUPPLY.get())){
                ItemStack liquid=stack();boolean ok=p.isShiftKeyDown()?transferPortion(liquid,held):transferPortion(held,liquid);if(ok)setStack(liquid);message(p,ok?"已转移 25 mL 以内的液体":"没有可转移液体或容量不足");
            }else if(!held.isEmpty()) {ItemStack liquid=stack();if(LabInteractions.interactPlacedVessel(held,liquid,ItemStack.EMPTY,ItemStack.EMPTY,p))setStack(liquid);}
            else message(p,String.format(java.util.Locale.ROOT,"液体 %.3f mL | 左 %.2f mL %s | 右 %.2f mL %s | %s",LabVesselItem.usedVolume(stack()),gasMl(0),gas(0),gasMl(1),gas(1),status()));
        }
        return InteractionResult.SUCCESS;
    }
    public static boolean transferPortion(ItemStack source,ItemStack target){
        return com.example.chemistry.titration.LiquidTransfer.pour(source,target,25)>0;
    }
    private String gasLabel(int side){
        String id=gas(side);if(id.isEmpty())id=tag(stack()).getStringOr("ec_last_gas_"+side,"");
        return switch(id){case "hydrogen"->"氢气";case "oxygen"->"氧气";case "chlorine"->"氯气";default->"空";};
    }
    @Override public boolean addGoggleInfo(List<Component> lines,boolean sneaking){
        if(isHalfCell()||isResistor()||isVoltmeter()){
            lines.add(stack().getHoverName());lines.add(Component.literal(status()));
            lines.add(Component.literal(String.format(java.util.Locale.ROOT,"%.5f A；累计 %.4f C",current(),number("charge_c",0))));
            if(isHalfCell()){lines.add(Component.literal("电极："+GalvanicSystem.metal(this)));var bridge=GalvanicSystem.bridge(this);if(bridge!=null)lines.add(Component.literal(String.format(java.util.Locale.ROOT,"盐桥KNO₃：%.6f g",bridge.grams())));}
            if(isVoltmeter())lines.add(Component.literal(String.format(java.util.Locale.ROOT,"%.3f V",number("measured_v",0))));return true;
        }
        if(isTrough()){TroughSystem.info(this,lines);return true;}
        lines.add(stack().getHoverName());
        if(isPower()){
            lines.add(Component.literal(String.format(java.util.Locale.ROOT,"CH1 %.1f V / %.3f A（限流 %.1f A）",number("voltage",6),current(),number("limit",1))));
            lines.add(Component.literal(String.format(java.util.Locale.ROOT,"CH2 %.1f V / %.3f A（限流 %.1f A）",number("ch2_voltage",6),channelCurrent(1),number("ch2_limit",1))));
        }else{
            ItemStack contents=stack();
            lines.add(Component.literal(String.format(java.util.Locale.ROOT,"液体体积：%.3f mL",LabVesselItem.usedVolume(contents))));
            lines.add(Component.literal(String.format(java.util.Locale.ROOT,"温度：%.1f °C",TemperatureSystem.getTemp(contents))));
            lines.add(Component.literal(String.format(java.util.Locale.ROOT,"剩余水质量：%.6f g",WaterElectrolysis.water(contents))));
            lines.add(Component.literal(String.format(java.util.Locale.ROOT,"累计耗水：%.3f mg",number("water_consumed_g",0)*1000)));
            lines.add(Component.literal(String.format(java.util.Locale.ROOT,"累计电量：%.3f 库仑",number("charge_c",0))));
            for(int i=0;i<2;i++)lines.add(Component.literal(String.format(java.util.Locale.ROOT,"%s管：%s %.2f mL / 50 mL（25°C基准）",i==0?"左":"右",gasLabel(i),gasMl(i))));
        }
        lines.add(Component.literal(status()));return true;
    }
}
