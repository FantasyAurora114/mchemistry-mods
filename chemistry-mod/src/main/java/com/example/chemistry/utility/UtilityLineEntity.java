package com.example.chemistry.utility;
import com.example.chemistry.electrical.ElectricWireEntity;
import com.example.chemistry.registry.ModItems;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
public class UtilityLineEntity extends ElectricWireEntity {
    private static final EntityDataAccessor<String> KIND=SynchedEntityData.defineId(UtilityLineEntity.class,EntityDataSerializers.STRING);
    public UtilityLineEntity(EntityType<?> t,Level l){super(t,l);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(KIND,"water");}
    public String kind(){return entityData.get(KIND);}public void kind(String k){entityData.set(KIND,k);}
    @Override public Vec3 point(boolean first){Entity e=endpoint(first);return e==null?null:e instanceof net.minecraft.world.entity.player.Player?e.position().add(0,1.0,0):UtilityConnections.port(e,first?ap():bp());}
    @Override public int wireColor(){return kind().equals("iec")?0x252A2D:kind().equals("vacuum")?0x4E565B:0x4D8AAB;}
    @Override public ItemStack toStack(){return preview()?ItemStack.EMPTY:new ItemStack(kind().equals("iec")?ModItems.IEC_CABLE.get():ModItems.RUBBER_TUBE.get());}
    @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putString("utility_kind",kind());}
    @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);kind(i.getStringOr("utility_kind","water"));}
}
