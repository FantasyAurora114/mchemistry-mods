package com.example.chemistry.electrical;

import java.util.*;
import com.example.chemistry.entity.TechnicalEntity;
import com.example.chemistry.registry.ModItems;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

public class ElectricWireEntity extends TechnicalEntity {
    private static final EntityDataAccessor<String> A=SynchedEntityData.defineId(ElectricWireEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> B=SynchedEntityData.defineId(ElectricWireEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> AP=SynchedEntityData.defineId(ElectricWireEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BP=SynchedEntityData.defineId(ElectricWireEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PREVIEW=SynchedEntityData.defineId(ElectricWireEntity.class,EntityDataSerializers.BOOLEAN);
    private net.minecraft.core.BlockPos lastA, lastB;
    private int missingTicks;
    public ElectricWireEntity(EntityType<?> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(A,"");b.define(B,"");b.define(AP,0);b.define(BP,0);b.define(PREVIEW,false);}
    public String a(){return entityData.get(A);} public String b(){return entityData.get(B);}
    public int ap(){return entityData.get(AP);} public int bp(){return entityData.get(BP);} public boolean preview(){return entityData.get(PREVIEW);}
    public void connect(Entity a,int ap,Entity b,int bp,boolean preview){entityData.set(A,a.getUUID().toString());entityData.set(B,b.getUUID().toString());entityData.set(AP,ap);entityData.set(BP,bp);entityData.set(PREVIEW,preview);lastA=a.blockPosition();lastB=b.blockPosition();setPos(a.position());}
    public Entity endpoint(boolean first){try{return level().getEntity(UUID.fromString(first?a():b()));}catch(IllegalArgumentException e){return null;}}
    public Vec3 point(boolean first){Entity e=endpoint(first);if(e instanceof ElectroDeviceEntity device)return device.terminal(first?ap():bp());if(e instanceof net.minecraft.world.entity.player.Player p)return p.getEyePosition().add(p.getLookAngle().scale(.4)).add(0,-.3,0);return null;}
    public boolean uses(Entity e,int port){String id=e.getUUID().toString();return a().equals(id)&&ap()==port || b().equals(id)&&bp()==port;}
    public static Vec3 curve(Vec3 a,Vec3 b,double t){return a.lerp(b,t).add(0,-4*t*(1-t)*Math.min(1.2,a.distanceTo(b)*.2),0);}
    public Vec3[] path(){
        Vec3 a=point(true),b=point(false);if(a==null||b==null)return new Vec3[0];
        java.util.List<Vec3> anchors=new java.util.ArrayList<>();anchors.add(a);
        Entity ae=endpoint(true),be=endpoint(false);
        if(ae instanceof ElectroDeviceEntity d&&d.isTrough()&&!TroughSystem.part(d,"jar",ap()).isEmpty()){
            anchors.add(TroughSystem.point(d,ap(),.7));anchors.add(rim(d,ap()));
        }
        if(be instanceof ElectroDeviceEntity d&&d.isTrough()&&!TroughSystem.part(d,"jar",bp()).isEmpty()){
            anchors.add(rim(d,bp()));anchors.add(TroughSystem.point(d,bp(),.7));
        }
        anchors.add(b);java.util.List<Vec3> result=new java.util.ArrayList<>();
        for(int segment=0;segment<anchors.size()-1;segment++){
            Vec3 from=anchors.get(segment),to=anchors.get(segment+1);int count=Math.max(8,(int)Math.ceil(from.distanceTo(to)*16));
            boolean internal=segment==0&&ae instanceof ElectroDeviceEntity d&&d.isTrough()&&anchors.size()>2
                    ||segment==anchors.size()-2&&be instanceof ElectroDeviceEntity secondDevice&&secondDevice.isTrough()&&anchors.size()>2;
            for(int i=segment==0?0:1;i<=count;i++){
                double t=i/(double)count;Vec3 point=internal?from.lerp(to,t):curve(from,to,t);
                if(i>0&&i<count){double top=point.y,radius=.02+from.distanceTo(to)/count;
                    AABB scan=new AABB(point.x-radius,point.y-.025,point.z-radius,point.x+radius,Math.max(point.y+.025,from.lerp(to,t).y+.025),point.z+radius);
                    for(var shape:level().getBlockCollisions(this,scan))for(AABB box:shape.toAabbs()){
                        boolean bath=ae instanceof ElectroDeviceEntity d&&d.isTrough()&&box.intersects(new AABB(d.blockPosition()))
                                ||be instanceof ElectroDeviceEntity secondDevice&&secondDevice.isTrough()&&box.intersects(new AABB(secondDevice.blockPosition()));
                        if(!bath)top=Math.max(top,box.maxY+.02);
                    }
                    point=new Vec3(point.x,top,point.z);
                }
                result.add(point);
            }
        }
        return result.toArray(Vec3[]::new);
    }
    private static Vec3 rim(ElectroDeviceEntity d,int side){
        double angle=Math.toRadians(-d.getYRot()),x=side==0?-.47:.47;
        return d.position().add(x*Math.cos(angle),.62,-x*Math.sin(angle));
    }
    public int wireColor(){
        Entity first=endpoint(true),second=endpoint(false);
        int polarity=first instanceof ElectroDeviceEntity d&&d.isPower()?ap():
                second instanceof ElectroDeviceEntity d&&d.isPower()?bp():ap();
        return polarity%2==0?0xC43B3B:0x30343B;
    }
    public boolean rayHit(Vec3 from,Vec3 to){Vec3[] points=path();for(int i=0;i<points.length-1;i++)if(new AABB(points[i],points[i+1]).inflate(.055).clip(from,to).isPresent())return true;return false;}
    @Override public boolean isPickable(){return false;} // Accurate scissors ray test, not the curve's large enclosing box.
    @Override public ItemStack toStack(){return preview()?ItemStack.EMPTY:new ItemStack(ModItems.ELECTRICAL_WIRE.get());}
    @Override public void tick(){super.tick();Vec3 a=point(true),b=point(false);
        if(a!=null)lastA=net.minecraft.core.BlockPos.containing(a);
        if(b!=null)lastB=net.minecraft.core.BlockPos.containing(b);if(a!=null&&b!=null){setPos(a);setBoundingBox(new AABB(a,b).inflate(.1).expandTowards(0,-1.2,0));}
        if(level().isClientSide())return;
        if(preview()) {Entity player=endpoint(false);if(!(player instanceof net.minecraft.world.entity.player.Player p)||!ElectricConnections.isPending(p,this)){discard();return;}}
        if(!preview() && level() instanceof net.minecraft.server.level.ServerLevel server) {
            net.minecraft.core.BlockPos missing=a==null?lastA:b==null?lastB:null;
            if(missing!=null && server.hasChunkAt(missing) && server.isPositionEntityTicking(missing)) {
                if(++missingTicks>100){spawnAtLocation(server,toStack());discard();return;}
            } else missingTicks=0;
        }
        if(a!=null&&b!=null&&a.distanceTo(b)>ElectricConnections.MAX_LENGTH){if(!preview())spawnAtLocation((net.minecraft.server.level.ServerLevel)level(),toStack());discard();}
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){out.putString("a",a());out.putString("b",b());out.putInt("ap",ap());out.putInt("bp",bp());out.putBoolean("preview",preview());
        if(lastA!=null)out.store("last_a",net.minecraft.core.BlockPos.CODEC,lastA);
        if(lastB!=null)out.store("last_b",net.minecraft.core.BlockPos.CODEC,lastB);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){entityData.set(A,in.getStringOr("a",""));entityData.set(B,in.getStringOr("b",""));entityData.set(AP,in.getIntOr("ap",0));entityData.set(BP,in.getIntOr("bp",0));entityData.set(PREVIEW,in.getBooleanOr("preview",false));
        lastA=in.read("last_a",net.minecraft.core.BlockPos.CODEC).orElse(null);
        lastB=in.read("last_b",net.minecraft.core.BlockPos.CODEC).orElse(null);
    }
}
