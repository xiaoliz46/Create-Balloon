package com.createballoon;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModConfigs {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue LIFT_FORCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        LIFT_FORCE = b.comment("Lift capacity per balloon (kpg). Each balloon can lift this much mass.")
                .defineInRange("liftForce", 100.0, 1.0, 10000.0);
        SPEC = b.build();
    }
}
