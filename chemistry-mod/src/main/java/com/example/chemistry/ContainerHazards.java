package com.example.chemistry;
import com.example.chemistry.data.Reactions;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
/** Gameplay rupture thresholds, based on actual reacted amount and the mixture's heat capacity. */
public final class ContainerHazards {
    public static double burstStrength(ItemStack vessel,Reactions.Reaction reaction,double extent){
        if(!(vessel.getItem() instanceof LabVesselItem)||extent<=0)return 0;
        boolean water=reaction.reactants().stream().anyMatch(e->e.id().equals("water"));if(!water)return 0;
        double energy=ThermalSystem.reactionJoules(reaction,extent);
        double rise=energy/ThermalSystem.capacity(vessel),threshold=VesselHeating.isSealed(vessel)?25:80;
        return rise<threshold?0:Math.clamp(.5+Math.sqrt(energy/20000),.5,2);
    }
    public static void record(ItemStack vessel,Reactions.Reaction reaction,double extent){
        double power=burstStrength(vessel,reaction,extent);if(power<=0)return;
        var t=vessel.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();t.putDouble("chem_pending_burst",power);vessel.set(DataComponents.CUSTOM_DATA,CustomData.of(t));
    }
    public static boolean discharge(ItemStack vessel,Level level,Vec3 pos){
        var t=vessel.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();double power=t.getDoubleOr("chem_pending_burst",0);
        if(power<=0||level.isClientSide())return false;t.remove("chem_pending_burst");vessel.set(DataComponents.CUSTOM_DATA,CustomData.of(t));
        if(level instanceof net.minecraft.server.level.ServerLevel server){
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,pos.x,pos.y,pos.z,1,0,0,0,0);
            server.playSound(null,net.minecraft.core.BlockPos.containing(pos),net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.BLOCKS,1,.9F);
            double radius=power*2;
            for(var living:server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(pos,pos).inflate(radius))){
                double distance=living.position().distanceTo(pos);if(distance<radius)living.hurtServer(server,server.damageSources().explosion(null,null),(float)((1-distance/radius)*power*4));
            }
        }
        return true;
    }
    private ContainerHazards(){}
}
