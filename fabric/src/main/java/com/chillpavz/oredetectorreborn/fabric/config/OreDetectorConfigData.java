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
package com.chillpavz.oredetectorreborn.fabric.config;

import com.chillpavz.oredetectorreborn.config.OreDetectorConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Cloth-config screen model. Lives in the loader module because cloth isn't on the common
 * classpath. Values are pushed into the shared {@link OreDetectorConfig} via {@link #applyToRuntime}.
 */
@Config(name = "ore_detector_reborn")
public class OreDetectorConfigData implements ConfigData {

    // One option drives all six reaches: down is 3x with a single liquid, 2x with two and 1x with
    // three or more, and sideways is three quarters of down. Column width follows the same tiers
    // (3x3, 5x5, 7x7) and is not separately configurable, or the two could disagree.
    @ConfigEntry.BoundedDiscrete(min = OreDetectorConfig.REACH_STEP_MIN, max = OreDetectorConfig.REACH_STEP_MAX)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int reachStep = OreDetectorConfig.DEFAULT_REACH_STEP;

    @ConfigEntry.BoundedDiscrete(min = OreDetectorConfig.COOLDOWN_MIN, max = OreDetectorConfig.COOLDOWN_MAX)
    @ConfigEntry.Gui.Tooltip
    public int cooldownTicks = OreDetectorConfig.DEFAULT_COOLDOWN;

    // Detector beep volume as a percentage (0%..100%).
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    @ConfigEntry.Gui.Tooltip
    public int soundVolumePercent = 40;

    // What one bottle of Attunement Liquid is worth, as a percentage of the tiered default.
    // A millibucket is one ore reported, so this is how many finds a bottle carries.
    @ConfigEntry.BoundedDiscrete(min = OreDetectorConfig.BOTTLE_YIELD_MIN,
            max = OreDetectorConfig.BOTTLE_YIELD_MAX)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int bottleYieldPercent = OreDetectorConfig.DEFAULT_BOTTLE_YIELD;

    // How fast liquid moves in and out of the detector. Higher is faster.
    @ConfigEntry.BoundedDiscrete(min = OreDetectorConfig.POUR_SPEED_MIN,
            max = OreDetectorConfig.POUR_SPEED_MAX)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int pourSpeedPercent = OreDetectorConfig.DEFAULT_POUR_SPEED;

    // Seconds of Haste a successful scan grants while the goggles are worn. 0 turns it off.
    @ConfigEntry.BoundedDiscrete(min = OreDetectorConfig.HASTE_SECONDS_MIN,
            max = OreDetectorConfig.HASTE_SECONDS_MAX)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int hasteSeconds = OreDetectorConfig.DEFAULT_HASTE_SECONDS;

    // --- Goggles strain -----------------------------------------------------------------------
    // Nausea on crossing the strain ceiling, in seconds. 0 switches the effect off and leaves
    // only the refusal to visualise.
    @ConfigEntry.BoundedDiscrete(min = OreDetectorConfig.NAUSEA_SECONDS_MIN,
            max = OreDetectorConfig.NAUSEA_SECONDS_MAX)
    @ConfigEntry.Gui.Tooltip
    public int nauseaSeconds = OreDetectorConfig.DEFAULT_NAUSEA_SECONDS;

    // Strain added per visualisation, against a fixed ceiling of 100.
    @ConfigEntry.BoundedDiscrete(min = OreDetectorConfig.STRAIN_PER_SCAN_MIN,
            max = OreDetectorConfig.STRAIN_PER_SCAN_MAX)
    @ConfigEntry.Gui.Tooltip
    public int strainPerScan = OreDetectorConfig.DEFAULT_STRAIN_PER_SCAN;

    // Ticks for one point of strain to bleed off. Higher is slower.
    @ConfigEntry.BoundedDiscrete(min = OreDetectorConfig.STRAIN_DECAY_TICKS_MIN,
            max = OreDetectorConfig.STRAIN_DECAY_TICKS_MAX)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int strainDecayTicks = OreDetectorConfig.DEFAULT_STRAIN_DECAY_TICKS;

    public void applyToRuntime() {
        OreDetectorConfig.applyReachStep(reachStep);
        OreDetectorConfig.applyBottleYield(bottleYieldPercent);
        OreDetectorConfig.applyPourSpeed(pourSpeedPercent);
        OreDetectorConfig.applyHaste(hasteSeconds);
        // The legacy per-ore detectors still read the old reach fields; they keep their defaults
        // until those items are removed.
        OreDetectorConfig.apply(OreDetectorConfig.DEFAULT_DOWN_REACH, OreDetectorConfig.DEFAULT_SIDE_REACH,
                OreDetectorConfig.DEFAULT_COLUMN_RADIUS, cooldownTicks,
                1.0, soundVolumePercent / 100.0);
        // After apply(), which sets the cooldown the strain warning is measured against.
        OreDetectorConfig.applyStrain(nauseaSeconds, strainPerScan, strainDecayTicks);
    }
}
