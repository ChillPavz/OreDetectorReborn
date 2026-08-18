/*
 * This file is part of Ore Detector Reborn.
 * Copyright (c) 2026 chillpavz
 *
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Ore Detector Reborn is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3.
 *
 * Ore Detector Reborn is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
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
