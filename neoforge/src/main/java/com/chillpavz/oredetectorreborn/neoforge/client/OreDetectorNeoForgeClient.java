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
package com.chillpavz.oredetectorreborn.neoforge.client;

import com.chillpavz.oredetectorreborn.client.ResonanceChamberScreen;
import com.chillpavz.oredetectorreborn.registry.ModMenus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only setup: binds the Resonance Chamber's menu to its screen. */
public final class OreDetectorNeoForgeClient {

    private OreDetectorNeoForgeClient() {
    }

    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.RESONANCE_CHAMBER, ResonanceChamberScreen::new);
    }
}
