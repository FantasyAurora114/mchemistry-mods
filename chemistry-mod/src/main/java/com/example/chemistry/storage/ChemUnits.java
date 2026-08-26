package com.example.chemistry.storage;

/**
 * Volume units for the Mekanism-style tank system.
 * One mB equals one millilitre; bottles, droppers and vessels all map to mB.
 */
public final class ChemUnits {

    /** Capacity of one gas collecting bottle (集气瓶), in mB. */
    public static final int GAS_JAR_VOLUME = 250;
    /** Capacity of one narrow-mouth reagent bottle (细口瓶), in mB. */
    public static final int LIQUID_BOTTLE_VOLUME = 250;
    /** Capacity of a dropper bottle (滴瓶), in mB. */
    public static final int DROPPER_BOTTLE_VOLUME = 100;
    /** Capacity of a hand dropper (胶头滴管), in mB. */
    public static final int DROPPER_VOLUME = 25;
    /** Capacity of a bucket (桶), in mB. */
    public static final int BUCKET_VOLUME = 1000;

    /** Internal gas tank capacity of the synthesis tower, in mB. */
    public static final long TOWER_GAS_CAPACITY = 64_000;
    /** Internal fluid tank capacity of the synthesis tower, in mB. */
    public static final int TOWER_FLUID_CAPACITY = 64_000;

    /** Capacity of the portable gas canister, in mB. */
    public static final long CANISTER_CAPACITY = 16_000;

    private ChemUnits() {
    }
}
