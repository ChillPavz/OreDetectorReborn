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
package com.chillpavz.oredetectorreborn.registry;

import com.chillpavz.oredetectorreborn.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * The mod's creative tab. Built loader-agnostically (title + icon only); the tab's contents are
 * filled in per-loader via each loader's creative-tab event, because {@code CreativeModeTab.Output}
 * is not accessible outside a tab subclass.
 */
public final class ModCreativeTabs {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "ore_detector");
    public static final ResourceKey<CreativeModeTab> KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ID);

    public static final CreativeModeTab MAIN = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.ore_detector_reborn.ore_detector"))
            .icon(() -> new ItemStack(ModItems.IRON_DETECTOR))
            .build();

    private ModCreativeTabs() {
    }

    /** Forces class initialization so the static field builds the tab. */
    public static void bootstrap() {
    }
}
