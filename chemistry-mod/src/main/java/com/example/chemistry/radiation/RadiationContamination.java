package com.example.chemistry.radiation;
import java.util.*;
import com.mojang.serialization.Codec;
import com.example.chemistry.item.LabVesselItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.*;
/** Persistent contamination retains actual carried material and its nuclide fractions. */
public final class RadiationContamination extends SavedData {
 private final Map<String,ItemStack> samples;
 public static final Codec<RadiationContamination> CODEC=Codec.unboundedMap(Codec.STRING,ItemStack.CODEC).xmap(RadiationContamination::new,s->s.samples);
 public static final SavedDataType<RadiationContamination> TYPE=new SavedDataType<>("mchemistry_radiation_contamination",()->new RadiationContamination(new LinkedHashMap<>()),CODEC);
 public RadiationContamination(Map<String,ItemStack> samples){this.samples=new LinkedHashMap<>(samples);}
 public static RadiationContamination get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
 public Map<String,ItemStack> samples(){return Collections.unmodifiableMap(samples);}
 public ItemStack at(BlockPos p){return samples.getOrDefault(Long.toString(p.asLong()),ItemStack.EMPTY);}
 public void deposit(BlockPos p,ItemStack sample){if(RadioLedger.carriers(sample).isEmpty())return;String key=Long.toString(p.asLong());ItemStack current=samples.computeIfAbsent(key,k->new ItemStack(com.example.chemistry.registry.ModItems.RADIOACTIVE_WASTE_BOTTLE.get()));var before=current.copy();for(var e:LabVesselItem.getContents(sample))if(RadioLedger.root(e.id())!=null)LabVesselItem.addMass(current,e.type(),e.id(),e.amount());RadioLedger.inherit(sample,before,current);setDirty();}
 public void remove(BlockPos p){samples.remove(Long.toString(p.asLong()));setDirty();}
 public static void spill(ServerLevel l,BlockPos p,ItemStack sample){get(l).deposit(p,sample);}
 private RadiationContamination(){this(Map.of());}
}
