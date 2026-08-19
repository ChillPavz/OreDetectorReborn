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

import com.chillpavz.oredetectorreborn.client.ClientClock;
import com.chillpavz.oredetectorreborn.client.ResonanceChamberScreen;
import com.chillpavz.oredetectorreborn.client.ScanHighlight;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import com.chillpavz.oredetectorreborn.registry.ModMenus;

/**
 * Client-only setup: the Resonance Chamber's screen, and the scan highlight the goggles draw.
 */
public final class OreDetectorNeoForgeClient {

    private OreDetectorNeoForgeClient() {
    }

    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.RESONANCE_CHAMBER, ResonanceChamberScreen::new);
    }

    /**
     * Draws this tick's highlight. ClientTickEvent.Post fires from the tail of Minecraft.tick(),
     * which vanilla already runs inside a gizmo collector, so nothing has to be opened here.
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            ScanHighlight.clear();
            return;
        }
        ClientClock.set(client.level.getGameTime());
        ScanHighlight.tick(client.level.getGameTime(), client.player.getEyePosition(),
                client.level.dimension());
    }
}
