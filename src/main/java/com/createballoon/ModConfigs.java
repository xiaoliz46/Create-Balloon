package com.createballoon;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModConfigs {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue LIFT_FORCE;
    public static final ModConfigSpec.DoubleValue ASCEND_SPEED;
    public static final ModConfigSpec.DoubleValue DESCEND_SPEED;
    public static final ModConfigSpec.DoubleValue FORWARD_SPEED;
    public static final ModConfigSpec.DoubleValue BACKWARD_SPEED;
    public static final ModConfigSpec.DoubleValue TURN_SPEED;
    public static final ModConfigSpec.DoubleValue GYRO_OMEGA;
    public static final ModConfigSpec.DoubleValue GYRO_DAMPING;
    public static final ModConfigSpec.DoubleValue GYRO_AUTHORITY;
    public static final ModConfigSpec.BooleanValue GYRO_ENABLED;
    public static final ModConfigSpec.DoubleValue GYRO_STRENGTH;
    public static final ModConfigSpec.DoubleValue GYRO_YAW_DAMPING;
    public static final ModConfigSpec.EnumValue<LogLevel> LOG_LEVEL;

    public enum LogLevel { OFF, SIMPLE, DETAILED, DIAGNOSTIC }

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        // ===== 升力 / Lift =====
        b.comment("--- 升力 / Lift ---");
        LIFT_FORCE = b.comment("单个气球的升力容量 (kpg)。总升力 = 此值 × 气球数量；仅当 总升力 ≥ 结构总质量 时才可起飞。",
                        "Lift capacity per balloon (kpg). Total lift = this × balloon count; the craft can only",
                        "fly when total lift >= total structure mass (otherwise 'insufficient lift').")
                .defineInRange("liftForce", 100.0, 0.0, 10000.0);
        ASCEND_SPEED = b.comment("目标上升速度 (m/s)。按住上升 (空格) 时结构伺服到此垂直速度。",
                        "Target ascend speed (m/s). Holding ascend (Space) servos the craft to this climb rate.")
                .defineInRange("ascendSpeed", 5.0, 0.0, 100.0);
        DESCEND_SPEED = b.comment("目标下降速度 (m/s)。按住下降 (V) 时结构伺服到此下沉速度。",
                        "Target descend speed (m/s). Holding descend (V) servos the craft to this sink rate.")
                .defineInRange("descendSpeed", 5.0, 0.0, 100.0);
        FORWARD_SPEED = b.comment("目标前进速度 (m/s)。按住 W 时加速到此水平速度。",
                        "Target forward speed (m/s). Hold W to reach this horizontal speed.")
                .defineInRange("forwardSpeed", 10.0, 0.0, 100.0);
        BACKWARD_SPEED = b.comment("目标后退速度 (m/s)。按住 S 时加速到此水平速度。",
                        "Target backward speed (m/s). Hold S to reach this horizontal speed.")
                .defineInRange("backwardSpeed", 10.0, 0.0, 100.0);
        TURN_SPEED = b.comment("转向速度 (°/s)。按住 A/D 时旋转到此角速度。",
                        "Turn speed (degrees/s). Hold A/D to rotate at this angular velocity.")
                .defineInRange("turnSpeed", 30.0, 0.0, 360.0);

        // ===== 陀螺仪 / Gyroscope =====
        b.comment("--- 陀螺仪 / Gyroscope ---");
        GYRO_OMEGA = b.comment("目标自然频率 (rad/s)。值越大修正越迅猛。",
                        "Target natural frequency (rad/s). Higher = snappier correction.")
                .defineInRange("gyroOmega", 3.0, 0.1, 10.0);
        GYRO_DAMPING = b.comment("阻尼比。0.6=弹性(推荐), 0.9=平衡, 1.0=临界阻尼。",
                        "Damping ratio. Lower = more P authority. 0.6 recommended for balloon.")
                .defineInRange("gyroDamping", 0.6, 0.1, 2.0);
        GYRO_AUTHORITY = b.comment("每个气球提供的陀螺仪容量 (kg*m²)。结构总转动惯量除以总容量 = 有效增益比例。",
                        "Gyro authority per balloon (kg*m²). Total inertia / total capacity = effective gain scale.")
                .defineInRange("gyroAuthority", 5000.0, 10.0, 100000.0);
        GYRO_ENABLED = b.comment("是否启用陀螺仪姿态稳定。关闭后结构不自动回正。",
                        "Enable gyro stabilization. Turn off to disable auto-leveling.")
                .define("gyroEnabled", true);
        GYRO_STRENGTH = b.comment("陀螺仪整体强度系数。>10 时每多 1 增加一个虚拟陀螺仪，上限 10000。",
                        "Overall gyro strength. >10 adds virtual gyros. Max 10000.")
                .defineInRange("gyroStrength", 10.0, 0.0, 10010.0);
        GYRO_YAW_DAMPING = b.comment("偏航阻尼。0=关。",
                        "Yaw damping. 0=off.")
                .defineInRange("gyroYawDamping", 0.8, 0.0, 50.0);

        // ===== 调试 / Debug =====
        b.comment("--- 调试 / Debug ---");
        LOG_LEVEL = b.comment("调试日志级别：OFF=关闭, SIMPLE=简要, DETAILED=完整物理数据",
                        "Debug log: OFF=disabled, SIMPLE=basic, DETAILED=full physics data")
                .defineEnum("logLevel", LogLevel.SIMPLE);

        SPEC = b.build();
    }
}
