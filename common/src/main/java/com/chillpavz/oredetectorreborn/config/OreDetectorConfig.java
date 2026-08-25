/*
 * This file is part of Ore Detector Reborn.
 * Copyright (c) 2026 chillpavz
 *
 * SPDX-License-Identifier: LicenseRef-PolyForm-Shield-1.0.0
 *
 * Licensed under the PolyForm Shield License 1.0.0. You may use, modify and
 * redistribute this file for any purpose EXCEPT providing a product that competes
 * with Ore Detector Reborn. See LICENSE, or
 * <https://polyformproject.org/licenses/shield/1.0.0>.
 *
 * Required Notice: Copyright chillpavz (https://github.com/ChillPavz/OreDetectorReborn)
 *
 * Portions descend from Ore Detector by restonic4, MIT licensed. See NOTICE.
 */
package com.chillpavz.oredetectorreborn.config;

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
    /**
     * Base reach. Down is 3x this with one liquid, 2x with two, 1x with three or more;
     * sideways and up are three quarters of that. One option instead of six.
     */
    public static final int REACH_STEP_MIN = 4;
    public static final int REACH_STEP_MAX = 8;
    public static final int DEFAULT_REACH_STEP = 8;

    public static volatile int reachStep = DEFAULT_REACH_STEP;

    public static volatile int cooldownTicks = DEFAULT_COOLDOWN;

    // --- Goggles strain -----------------------------------------------------------------------
    // The ceiling is fixed at 100 so the tooltip reads as a percentage. What is tunable is how
    // fast strain arrives, how fast it leaves, and how hard the Nausea bites.

    /** Nausea on crossing the ceiling, in seconds. 0 turns the effect off and leaves the refusal. */
    public static final int NAUSEA_SECONDS_MIN = 0;
    public static final int NAUSEA_SECONDS_MAX = 15;
    public static final int DEFAULT_NAUSEA_SECONDS = 10;

    /** Strain added per visualisation, against a ceiling of 100. */
    public static final int STRAIN_PER_SCAN_MIN = 5;
    public static final int STRAIN_PER_SCAN_MAX = 25;
    public static final int DEFAULT_STRAIN_PER_SCAN = 20;

    /**
     * Ticks for one point of strain to bleed off. Higher is slower.
     *
     * <p>The floor is 5 rather than 1 because below that the decay outruns any legal
     * {@code strainPerScan} at the default cooldown and strain could never reach its ceiling at
     * all. The ceiling of 100 is five seconds a point, which is 500 seconds to clear a full bar.
     */
    public static final int STRAIN_DECAY_TICKS_MIN = 5;
    public static final int STRAIN_DECAY_TICKS_MAX = 100;
    public static final int DEFAULT_STRAIN_DECAY_TICKS = 10;

    public static volatile int nauseaSeconds = DEFAULT_NAUSEA_SECONDS;
    public static volatile int strainPerScan = DEFAULT_STRAIN_PER_SCAN;
    public static volatile int strainDecayTicks = DEFAULT_STRAIN_DECAY_TICKS;

    /**
     * Clamps and stores the three strain options.
     *
     * <p>Warns when the combination makes strain unreachable. Strain only rises once per scan and
     * a scan only happens once per cooldown, so it accumulates at all only while
     * {@code strainPerScan > cooldownTicks / strainDecayTicks}. A player is entitled to switch the
     * mechanic off that way, but they should not do it by accident and then wonder why the goggles
     * never complain, so it is said out loud once.
     */
    public static void applyStrain(int nauseaSeconds, int strainPerScan, int strainDecayTicks) {
        OreDetectorConfig.nauseaSeconds =
                clampInt(nauseaSeconds, NAUSEA_SECONDS_MIN, NAUSEA_SECONDS_MAX);
        OreDetectorConfig.strainPerScan =
                clampInt(strainPerScan, STRAIN_PER_SCAN_MIN, STRAIN_PER_SCAN_MAX);
        OreDetectorConfig.strainDecayTicks =
                clampInt(strainDecayTicks, STRAIN_DECAY_TICKS_MIN, STRAIN_DECAY_TICKS_MAX);
        warnIfStrainInert();
    }

    private static void warnIfStrainInert() {
        double decayPerScan = (double) cooldownTicks / strainDecayTicks;
        if (strainPerScan <= decayPerScan) {
            com.chillpavz.oredetectorreborn.Constants.LOG.warn(
                    "Goggle strain can never reach its ceiling with these settings: a scan adds {} "
                    + "but {} decays between scans at a {} tick cooldown. The goggles will never "
                    + "refuse. Raise strainPerScan or strainDecayTicks if that was not intended.",
                    strainPerScan, Math.round(decayPerScan), cooldownTicks);
        }
    }
    // --- Goggle reward ------------------------------------------------------------------------
    // Haste on a successful scan while the goggles are worn. 0 turns it off entirely, which is the
    // setting for anyone who thinks a detector should not touch mining speed at all.

    public static final int HASTE_SECONDS_MIN = 0;
    public static final int HASTE_SECONDS_MAX = 30;
    public static final int DEFAULT_HASTE_SECONDS = 12;

    public static volatile int hasteSeconds = DEFAULT_HASTE_SECONDS;

    /** Clamps and stores how long the goggles' Haste lasts. */
    public static void applyHaste(int seconds) {
        hasteSeconds = clampInt(seconds, HASTE_SECONDS_MIN, HASTE_SECONDS_MAX);
    }

    // --- Pour and drain speed -----------------------------------------------------------------
    // One knob for both, because the two mirror each other: what a bottle takes to go in is what
    // the same amount takes to come out. Higher is faster.

    public static final int POUR_SPEED_MIN = 25;
    public static final int POUR_SPEED_MAX = 400;
    public static final int DEFAULT_POUR_SPEED = 100;

    public static volatile int pourSpeedPercent = DEFAULT_POUR_SPEED;

    /** Clamps and stores the pour and drain speed. */
    public static void applyPourSpeed(int percent) {
        pourSpeedPercent = clampInt(percent, POUR_SPEED_MIN, POUR_SPEED_MAX);
    }

    /** A hold length after the speed scalar. Higher percent means fewer ticks. */
    public static int scaleFlowTicks(int ticks) {
        return Math.max(1, Math.round(ticks * 100.0F / pourSpeedPercent));
    }

    // --- Bottle yield -------------------------------------------------------------------------
    // One millibucket is one ore reported, so the only lever left is how much a bottle is worth.
    // This scales every tier at once: below 100 the same brewing carries fewer finds, above it
    // more. It replaces the old per-block cost option, which no longer has anything to scale.
    //
    // The floor is 25 rather than 0 because a bottle worth nothing could never attune anything,
    // which is not a setting anybody wants; the way to make scanning cheap is to turn this up.

    public static final int BOTTLE_YIELD_MIN = 25;
    public static final int BOTTLE_YIELD_MAX = 400;
    public static final int DEFAULT_BOTTLE_YIELD = 100;

    public static volatile int bottleYieldPercent = DEFAULT_BOTTLE_YIELD;

    /** Clamps and stores the bottle yield scalar. */
    public static void applyBottleYield(int percent) {
        bottleYieldPercent = clampInt(percent, BOTTLE_YIELD_MIN, BOTTLE_YIELD_MAX);
    }

    /** One bottle's worth after the scalar, never rounded away to nothing. */
    public static int scaleBottleYield(int base) {
        return Math.max(1, Math.round(base * bottleYieldPercent / 100.0F));
    }

    public static volatile double durabilityMultiplier = DEFAULT_DURABILITY_MULT;
    public static volatile double soundVolume = DEFAULT_SOUND_VOLUME;

    /** Clamps and stores the one option that drives all six of the detector's reaches. */
    public static void applyReachStep(int value) {
        reachStep = Math.max(REACH_STEP_MIN, Math.min(REACH_STEP_MAX, value));
    }

    private OreDetectorConfig() {
    }

    public static void apply(int downReach, int sideReach, int columnRadius, int cooldownTicks,
                             double durabilityMultiplier, double soundVolume) {
        OreDetectorConfig.downReach = clampInt(downReach, REACH_MIN, REACH_MAX);
        OreDetectorConfig.sideReach = clampInt(sideReach, REACH_MIN, REACH_MAX);
        OreDetectorConfig.columnRadius = clampInt(columnRadius, COLUMN_RADIUS_MIN, COLUMN_RADIUS_MAX);
        OreDetectorConfig.cooldownTicks = clampInt(cooldownTicks, COOLDOWN_MIN, COOLDOWN_MAX);
        // The strain warning depends on the cooldown, so re-check it whenever the cooldown moves.
        warnIfStrainInert();
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
