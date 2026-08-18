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

import java.util.LinkedHashMap;
import java.util.Map;

import com.chillpavz.oredetectorreborn.Constants;
import com.chillpavz.oredetectorreborn.menu.ResonanceChamberMenu;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** The mod's menu types. Each loader registers these; the screens are registered client-side. */
public final class ModMenus {

    public static final Map<Identifier, MenuType<?>> MENUS = new LinkedHashMap<>();

    public static final MenuType<ResonanceChamberMenu> RESONANCE_CHAMBER =
            register("resonance_chamber",
                    new MenuType<>(ResonanceChamberMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {
    }

    private static <T extends MenuType<?>> T register(String name, T type) {
        MENUS.put(Identifier.fromNamespaceAndPath(Constants.MOD_ID, name), type);
        return type;
    }

    public static void bootstrap() {
    }
}
