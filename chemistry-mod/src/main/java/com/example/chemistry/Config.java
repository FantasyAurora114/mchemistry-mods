package com.example.chemistry;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DATA_ON_START = BUILDER
            .comment("Whether to log loaded chemistry data (elements, compounds, synthesis recipes) on server start")
            .define("logDataOnStart", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
