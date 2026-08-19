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
package com.chillpavz.oredetectorreborn.neoforge.config;

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
    public int reachStep = OreDetectorConfig.DEFAULT_REACH_STEP;

    @ConfigEntry.BoundedDiscrete(min = OreDetectorConfig.COOLDOWN_MIN, max = OreDetectorConfig.COOLDOWN_MAX)
    public int cooldownTicks = OreDetectorConfig.DEFAULT_COOLDOWN;

    // Durability as a percentage (25%..400%); applied at item creation, so a restart is required.
    @ConfigEntry.BoundedDiscrete(min = 25, max = 400)
    public int durabilityPercent = 100;

    // Detector beep volume as a percentage (0%..100%).
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int soundVolumePercent = 40;

    public void applyToRuntime() {
        OreDetectorConfig.applyReachStep(reachStep);
        // The legacy per-ore detectors still read the old reach fields; they keep their defaults
        // until those items are removed.
        OreDetectorConfig.apply(OreDetectorConfig.DEFAULT_DOWN_REACH, OreDetectorConfig.DEFAULT_SIDE_REACH,
                OreDetectorConfig.DEFAULT_COLUMN_RADIUS, cooldownTicks,
                durabilityPercent / 100.0, soundVolumePercent / 100.0);
    }
}
