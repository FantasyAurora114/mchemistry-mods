package com.example.chemistry;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DATA_ON_START = BUILDER
            .comment("Whether to log loaded chemistry data (elements, compounds, synthesis recipes) on server start")
            .define("logDataOnStart", true);

    public static final ModConfigSpec.DoubleValue RADIO_DECAY_MULTIPLIER=BUILDER.comment("Radioactive decay time multiplier; 1 uses real half-lives in loaded game time. Dose rates do not multiply.").defineInRange("radioDecayMultiplier",1.0,0.0,1000000.0);
    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
