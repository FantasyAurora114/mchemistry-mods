package com.example.mci.data;

import java.util.List;

/** Generated ore table (see work/gen_mci_data.py). */
public final class Ores {

    public record OreDef(String id, float hardness) {
    }

    public static final List<OreDef> ALL = List.of(
        new OreDef("bauxite", 1.5F),
        new OreDef("magnesite", 1.5F),
        new OreDef("sphalerite", 1.5F),
        new OreDef("cassiterite", 1.5F),
        new OreDef("galena", 2.0F),
        new OreDef("pentlandite", 1.5F),
        new OreDef("cobaltite", 2.0F),
        new OreDef("chromite", 3.0F),
        new OreDef("pyrolusite", 2.0F),
        new OreDef("ilmenite", 3.0F),
        new OreDef("molybdenite", 2.5F),
        new OreDef("wolframite", 3.5F),
        new OreDef("argentite", 2.0F),
        new OreDef("sperrylite", 3.5F),
        new OreDef("spodumene", 2.0F),
        new OreDef("barite", 1.5F),
        new OreDef("bismuthinite", 2.5F),
        new OreDef("greenockite", 2.5F),
        new OreDef("stibnite", 2.5F),
        new OreDef("borax", 1.5F),
        new OreDef("sulfur_ore", 1.0F),
        new OreDef("apatite", 2.0F),
        new OreDef("fluorite", 1.5F),
        new OreDef("sylvite", 1.0F),
        new OreDef("halite", 1.0F),
        new OreDef("limestone", 1.0F)
    );

    private Ores() {
    }
}
