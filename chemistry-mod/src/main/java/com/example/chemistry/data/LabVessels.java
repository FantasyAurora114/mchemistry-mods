package com.example.chemistry.data;

import java.util.List;

/**
 * Sized lab vessels (试管 / 烧杯) that can hold reagents (generated).
 */
public final class LabVessels {

    public record Vessel(String id, String english, String chinese, int capacity, String kind) {
    }

    public static final List<Vessel> ALL = List.of(
            new Vessel("test_tube_5ml", "50 mL Test Tube", "50mL试管", 50, "test_tube"),
            new Vessel("test_tube_10ml", "100 mL Test Tube", "100mL试管", 100, "test_tube"),
            new Vessel("test_tube_50ml", "250 mL Test Tube", "250mL试管", 250, "test_tube"),
            new Vessel("test_tube_5ml_dewar", "50 mL Dewar Test Tube", "50mL杜瓦试管", 50, "test_tube"),
            new Vessel("test_tube_10ml_dewar", "100 mL Dewar Test Tube", "100mL杜瓦试管", 100, "test_tube"),
            new Vessel("test_tube_50ml_dewar", "250 mL Dewar Test Tube", "250mL杜瓦试管", 250, "test_tube"),
            new Vessel("beaker_50ml", "250 mL Beaker", "250mL烧杯", 250, "beaker"),
            new Vessel("beaker_100ml", "500 mL Low-form Beaker", "500mL低型烧杯", 500, "beaker"),
            new Vessel("beaker_medium", "500 mL Medium-form Beaker", "500mL中型烧杯", 500, "beaker"),
            new Vessel("beaker_tall", "500 mL Tall-form Beaker", "500mL高型烧杯", 500, "beaker"),
            new Vessel("beaker_500ml", "1000 mL Beaker", "1000mL烧杯", 1000, "beaker"),
            new Vessel("beaker_1000ml", "2000 mL Beaker", "2000mL烧杯", 2000, "beaker")
    );

    private LabVessels() {
    }
}
