package com.example.chemistry.utility;
import com.example.chemistry.entity.TechnicalEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.*;
/** Loose decorative plug anchor, owned by one IEC cable; not a power source. */
public class IECPlugEntity extends TechnicalEntity {
    public IECPlugEntity(EntityType<?> t,Level l){super(t,l);}
    @Override public void tick(){super.tick();if(!level().isClientSide()&&tickCount>40&&UtilityConnections.lines(this).isEmpty())discard();}
    @Override public boolean isPickable(){return false;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
    @Override protected void addAdditionalSaveData(ValueOutput o){}
    @Override protected void readAdditionalSaveData(ValueInput i){}
    @Override public ItemStack toStack(){return ItemStack.EMPTY;}
}
