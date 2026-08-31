package com.example.chemistry.data;

import java.util.List;

/**
 * Gas-filled collecting bottles (集气瓶) registered by the mod (generated).
 */
public final class GasJars {

    public record GasJar(String id, String formula, String name, String chinese, int color, boolean lighter) {
    }

    public static final List<GasJar> ALL = List.of(
            new GasJar("oxygen", "O2", "Oxygen", "氧气", 0x8FD0F0, false),
            new GasJar("hydrogen", "H2", "Hydrogen", "氢气", 0xF0C8E0, true),
            new GasJar("nitrogen", "N2", "Nitrogen", "氮气", 0xD0C8F0, true),
            new GasJar("chlorine", "Cl2", "Chlorine", "氯气", 0xC0DC30, false),
            new GasJar("helium", "He", "Helium", "氦气", 0xF8D8B8, true),
            new GasJar("neon", "Ne", "Neon", "氖气", 0xF09868, true),
            new GasJar("argon", "Ar", "Argon", "氩气", 0xC0A8E8, false),
            new GasJar("krypton", "Kr", "Krypton", "氪气", 0xD8E8B0, false),
            new GasJar("xenon", "Xe", "Xenon", "氙气", 0xA8D0F8, false),
            new GasJar("carbon_dioxide", "CO2", "Carbon Dioxide", "二氧化碳", 0xD8D8D8, false),
            new GasJar("carbon_monoxide", "CO", "Carbon Monoxide", "一氧化碳", 0xC8D8E0, true),
            new GasJar("sulfur_dioxide", "SO2", "Sulfur Dioxide", "二氧化硫", 0xF0E080, false),
            new GasJar("nitric_oxide", "NO", "Nitric Oxide", "一氧化氮", 0xE8A880, false),
            new GasJar("nitrogen_dioxide", "NO2", "Nitrogen Dioxide", "二氧化氮", 0xC04820, false),
            new GasJar("ammonia", "NH3", "Ammonia", "氨气", 0xE0E8C0, true),
            new GasJar("hydrogen_chloride", "HCl", "Hydrogen Chloride", "氯化氢", 0xE8E0E0, false),
            new GasJar("hydrogen_sulfide", "H2S", "Hydrogen Sulfide", "硫化氢", 0xF0E8A0, false),
            new GasJar("methane", "CH4", "Methane", "甲烷", 0xA0C8E0, true),
            new GasJar("ethane", "C2H6", "Ethane", "乙烷", 0xA8C8D8, true),
            new GasJar("propane", "C3H8", "Propane", "丙烷", 0xB0C8D0, true),
            new GasJar("butane", "C4H10", "Butane", "丁烷", 0xB8C8C8, true),
            new GasJar("cyanogen", "(CN)2", "Cyanogen", "氰气", 0xC0B8A8, false),
            new GasJar("ethylene", "C2H4", "Ethylene", "乙烯", 0xB8D8E8, true),
            new GasJar("propylene", "C3H6", "Propylene", "丙烯", 0xC0D8E0, false),
            new GasJar("butene", "C4H8", "Butene", "丁烯", 0xC8D8D8, false),
            new GasJar("acetylene", "C2H2", "Acetylene", "乙炔", 0xD8E0E8, true),
            new GasJar("propyne", "C3H4", "Propyne", "丙炔", 0xD8E0E0, false),
            new GasJar("butyne", "C4H6", "Butyne", "丁炔", 0xE0E0E0, false),
            new GasJar("fluorine", "F2", "Fluorine", "氟气", 0xE8F0A0, false),
            new GasJar("chloromethane", "CH3Cl", "Chloromethane", "氯甲烷", 0xD0E0E8, false),
            new GasJar("nitrous_oxide", "N2O", "Nitrous Oxide", "一氧化二氮（笑气）", 0xE0E8F0, false)
    );

    private GasJars() {
    }
}
