package com.example.chemistry;

import com.example.chemistry.item.LabVesselItem;
import com.example.chemistry.registry.ModItems;
import com.example.chemistry.solution.SpeciesCatalog;
import com.example.chemistry.titration.LiquidTransfer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Quantitative energy and material checks, independent of rendering. */
public final class ThermalGameTests {
    private static void near(GameTestHelper h,double actual,double expected,double eps,String message){
        h.assertTrue(Math.abs(actual-expected)<eps,Component.literal(message+": "+actual+" != "+expected));
    }
    private static ItemStack water(double grams){var s=new ItemStack(ModItems.ERLENMEYER_FLASK.get());LabVesselItem.addMass(s,"liquid","water",grams);return s;}
    private static double mass(ItemStack s,String type,String id){return LabVesselItem.getContents(s).stream().filter(e->e.type().equals(type)&&e.id().equals(id)).mapToDouble(LabVesselItem.Entry::amount).sum();}
    public static void calorimetry(GameTestHelper h){h.runAtTickTime(1,()->{
        var s=water(100);double cp=ThermalSystem.capacity(s),initial=TemperatureSystem.getTemp(s);
        ThermalSystem.addHeat(s,cp*10,"test_heater");near(h,TemperatureSystem.getTemp(s),initial+10,1e-8,"sensible heat");
        var large=water(200);ThermalSystem.addHeat(large,cp*10,"test_heater");
        h.assertTrue(TemperatureSystem.getTemp(large)<initial+10,Component.literal("more water did not reduce temperature rise"));
        for(String acid:new String[]{"hydrochloric_acid","acetic_acid"}){
            var sample=water(100);String species=acid.equals("acetic_acid")?acid:"aqueous_hcl";
            LabVesselItem.addMass(sample,"liquid",acid,.001*SpeciesCatalog.get(species).molarMass());
            LabVesselItem.addMass(sample,"liquid","sodium_hydroxide_solution",.001*SpeciesCatalog.get("solid:sodium_hydroxide").molarMass());
            PhaseSystem.tick(sample,TemperatureSystem.getTemp(sample));
            double expected=acid.equals("acetic_acid")?56.1:57.9;
            near(h,ThermalSystem.accumulated(sample,"neutralization"),expected,1e-7,"neutralization joules");
            for(int i=0;i<100;i++)PhaseSystem.tick(sample,TemperatureSystem.getTemp(sample));
            near(h,ThermalSystem.accumulated(sample,"neutralization"),expected,1e-7,"neutralization repeated heat");
        }
        h.succeed();
    });}
    public static void mixing(GameTestHelper h){h.runAtTickTime(1,()->{
        var hot=water(100);var cold=water(100);TemperatureSystem.setRawTemp(hot,80);TemperatureSystem.setRawTemp(cold,20);
        double initial=ThermalSystem.capacity(hot)*80+ThermalSystem.capacity(cold)*20;
        near(h,LiquidTransfer.pour(hot,cold,50),50,1e-7,"hot portion volume");
        near(h,ThermalSystem.capacity(hot)*TemperatureSystem.getTemp(hot)+ThermalSystem.capacity(cold)*TemperatureSystem.getTemp(cold),initial,1e-5,"mixing lost sensible energy");
        near(h,TemperatureSystem.getTemp(cold),20+50*4.18*60/ThermalSystem.capacity(cold),1e-7,"receiver glass ignored");
        var full=water(250);double cp=ThermalSystem.capacity(hot),temp=TemperatureSystem.getTemp(hot);
        near(h,LiquidTransfer.pour(hot,full,10),0,1e-9,"full receiver accepted liquid");
        near(h,ThermalSystem.capacity(hot),cp,1e-9,"failed transfer lost contents");near(h,TemperatureSystem.getTemp(hot),temp,1e-9,"failed transfer lost heat");h.succeed();
    });}
    public static void latent(GameTestHelper h){h.runAtTickTime(1,()->{
        var crystal=new ItemStack(ModItems.ERLENMEYER_FLASK.get());LabVesselItem.addMass(crystal,"solid","copper",5);
        double point=1084;TemperatureSystem.setRawTemp(crystal,point);
        ThermalSystem.addHeat(crystal,2.5*205,"test_heater");near(h,TemperatureSystem.getTemp(crystal),point,1e-7,"melting plateau");
        near(h,mass(crystal,"liquid","molten_copper"),2.5,1e-7,"fusion energy not proportional to mass");
        ThermalSystem.addHeat(crystal,-1.25*205,"test_cooler");near(h,TemperatureSystem.getTemp(crystal),point,1e-7,"freezing plateau");
        near(h,mass(crystal,"liquid","molten_copper"),1.25,1e-7,"freezing energy");
        var boiling=water(1);TemperatureSystem.setRawTemp(boiling,100);ThermalSystem.addHeat(boiling,225.6,"test_heater");
        near(h,TemperatureSystem.getTemp(boiling),100,1e-7,"boiling overheated liquid");
        for(int i=0;i<10;i++)PhaseSystem.tick(boiling,TemperatureSystem.getTemp(boiling));
        near(h,mass(boiling,"liquid","water"),.9,1e-6,"latent energy evaporation mass");near(h,ThermalSystem.boilingEnergy(boiling),0,1e-6,"latent budget not debited");
        TemperatureSystem.setRawTemp(boiling,20);for(int i=0;i<10;i++)PhaseSystem.tick(boiling,TemperatureSystem.getTemp(boiling));
        near(h,mass(boiling,"liquid","water"),1,1e-6,"steam condensation mass");near(h,ThermalSystem.accumulated(boiling,"condensation"),225.6,1e-5,"condensation heat missing");h.succeed();
    });}
    private ThermalGameTests(){}
}
