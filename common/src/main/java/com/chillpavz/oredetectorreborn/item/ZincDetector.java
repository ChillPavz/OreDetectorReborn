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
package com.chillpavz.oredetectorreborn.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Conditional detector for Create's zinc ore. Common code can't compile against Create, so blocks
 * are matched by their registry id — this works whether or not Create (Fly) is installed; when it
 * isn't, no block will carry these ids so the detector simply never triggers. The item is only
 * registered at all when Create is present (see the loader entrypoints).
 */
public class ZincDetector extends OreDetectorItem {

    private static final ModdedOres ZINC = ModdedOres.of("create", "zinc_ore", "deepslate_zinc_ore");

    public ZincDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return ZINC.matches(state);
    }

    @Override
    protected Component getOreName() {
        return Component.translatable("ore.ore_detector_reborn.zinc");
    }

    @Override
    protected int getOreColor() {
        return 0xB8CFCC;
    }
}
