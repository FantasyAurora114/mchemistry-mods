package com.example.chemistry.data;
import java.util.List;
/** Explicit anhydrous additions; ceilings are provisional gameplay parameters. */
public final class ExpansionCompounds {
    public record Compound(String id,String formula,String chinese,String english,int color,double mass,double ceiling){}
    public static final List<Compound> ALL=List.of(
        new Compound("sodium_silicate","Na2SiO3","硅酸钠","Sodium Silicate",0xE5E9DF,122.063,50),
        new Compound("zinc_nitrate","Zn(NO3)2","硝酸锌","Zinc Nitrate",0xE6EBE9,189.388,100));
    private ExpansionCompounds(){}
}
