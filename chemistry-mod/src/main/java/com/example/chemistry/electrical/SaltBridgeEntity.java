package com.example.chemistry.electrical;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
/** Persisted, finite KNO3; ancestry reuses attachment cleanup but wiring excludes this class. */
public class SaltBridgeEntity extends ElectricWireEntity {
    private static final EntityDataAccessor<Float> GRAMS=SynchedEntityData.defineId(SaltBridgeEntity.class,EntityDataSerializers.FLOAT);
    public SaltBridgeEntity(EntityType<?> t,Level l){super(t,l);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(GRAMS,1F);}
    private double storedGrams=1;
    public double grams(){return level().isClientSide()?entityData.get(GRAMS):storedGrams;}
    public void setGrams(double g){storedGrams=Math.clamp(g,0,1);entityData.set(GRAMS,(float)storedGrams);}
    @Override public ItemStack toStack(){return SaltBridgeItem.filled(grams());}
    @Override public Vec3 point(boolean first){var e=endpoint(first);return e instanceof ElectroDeviceEntity cell?cell.position().add(0,.3,0):null;}
    @Override public Vec3[] path(){var a=point(true);var b=point(false);if(a==null||b==null)return new Vec3[0];double y=Math.max(a.y,b.y)+.55;return new Vec3[]{a,new Vec3(a.x,y,a.z),new Vec3(b.x,y,b.z),b};}
    @Override public int wireColor(){return grams()>0?0xCCDCD8:0x879EA8;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putDouble("salt_g",grams());}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);setGrams(in.getDoubleOr("salt_g",1));}
}
