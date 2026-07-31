package com.chillpavz.oredetector.config;

/**
 * Loader-agnostic holder for the mod's tunable values. Gameplay code reads the {@code volatile}
 * fields directly; each loader's config screen (cloth-config) pushes user values in via
 * {@link #apply}. Reach / column / cooldown take effect live; the durability multiplier is read
 * when items are created, so it only changes after a restart.
 */
public final class OreDetectorConfig {

    // Bounds (min/max) enforced regardless of what a config file contains.
    public static final int REACH_MIN = 1;
    public static final int REACH_MAX = 32;
    public static final int COLUMN_RADIUS_MIN = 0;   // 0 -> 1x1
    public static final int COLUMN_RADIUS_MAX = 3;   // 3 -> 7x7
    public static final int COOLDOWN_MIN = 0;
    public static final int COOLDOWN_MAX = 200;
    public static final double DURABILITY_MULT_MIN = 0.25;
    public static final double DURABILITY_MULT_MAX = 4.0;
    public static final double SOUND_VOLUME_MIN = 0.0;
    public static final double SOUND_VOLUME_MAX = 1.0;

    // Defaults.
    public static final int DEFAULT_DOWN_REACH = 16;
    public static final int DEFAULT_SIDE_REACH = 8;
    public static final int DEFAULT_COLUMN_RADIUS = 1;   // 3x3
    public static final int DEFAULT_COOLDOWN = 100;
    public static final double DEFAULT_DURABILITY_MULT = 1.0;
    public static final double DEFAULT_SOUND_VOLUME = 0.4;

    // Live values.
    public static volatile int downReach = DEFAULT_DOWN_REACH;
    public static volatile int sideReach = DEFAULT_SIDE_REACH;
    public static volatile int columnRadius = DEFAULT_COLUMN_RADIUS;
    public static volatile int cooldownTicks = DEFAULT_COOLDOWN;
    public static volatile double durabilityMultiplier = DEFAULT_DURABILITY_MULT;
    public static volatile double soundVolume = DEFAULT_SOUND_VOLUME;

    private OreDetectorConfig() {
    }

    public static void apply(int downReach, int sideReach, int columnRadius, int cooldownTicks,
                             double durabilityMultiplier, double soundVolume) {
        OreDetectorConfig.downReach = clampInt(downReach, REACH_MIN, REACH_MAX);
        OreDetectorConfig.sideReach = clampInt(sideReach, REACH_MIN, REACH_MAX);
        OreDetectorConfig.columnRadius = clampInt(columnRadius, COLUMN_RADIUS_MIN, COLUMN_RADIUS_MAX);
        OreDetectorConfig.cooldownTicks = clampInt(cooldownTicks, COOLDOWN_MIN, COOLDOWN_MAX);
        OreDetectorConfig.durabilityMultiplier = clampDouble(durabilityMultiplier, DURABILITY_MULT_MIN, DURABILITY_MULT_MAX);
        OreDetectorConfig.soundVolume = clampDouble(soundVolume, SOUND_VOLUME_MIN, SOUND_VOLUME_MAX);
    }

    /**
     * Applies only the durability multiplier, as a percentage. Durability is baked into the items
     * when they are created, so a loader whose config system is not guaranteed to have loaded by
     * then needs to set this on its own, ahead of registration.
     */
    public static void applyDurabilityPercent(int percent) {
        durabilityMultiplier = clampDouble(percent / 100.0, DURABILITY_MULT_MIN, DURABILITY_MULT_MAX);
    }

    /** Scales a base durability by the configured multiplier (never below 1). */
    public static int scaleDurability(int base) {
        return Math.max(1, (int) Math.round(base * durabilityMultiplier));
    }

    private static int clampInt(int value, int lo, int hi) {
        return Math.max(lo, Math.min(hi, value));
    }

    private static double clampDouble(double value, double lo, double hi) {
        return Math.max(lo, Math.min(hi, value));
    }
}
