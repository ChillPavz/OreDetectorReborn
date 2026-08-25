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
package com.chillpavz.oredetectorreborn.fabric.client;

import com.chillpavz.oredetectorreborn.client.ClientClock;
import com.chillpavz.oredetectorreborn.client.ResonanceChamberScreen;
import com.chillpavz.oredetectorreborn.client.ScanHighlight;
import com.chillpavz.oredetectorreborn.client.ShaderCompat;
import com.chillpavz.oredetectorreborn.network.ScanHighlightPayload;
import com.chillpavz.oredetectorreborn.network.ScanVolumePayload;
import com.chillpavz.oredetectorreborn.client.GrindScreen;
import com.chillpavz.oredetectorreborn.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * Client-only setup: the Resonance Chamber's screen, and the scan highlight the goggles draw.
 */
public class OreDetectorFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModMenus.RESONANCE_CHAMBER, ResonanceChamberScreen::new);
        MenuScreens.register(ModMenus.GRIND, GrindScreen::new);

        // Iris leaves the filled box pipeline unmapped, so without this the highlight loses its
        // fill the moment a shaderpack is on. Its own loader modules use this same API.
        ShaderCompat.registerFilledBoxPipeline();

        ClientPlayNetworking.registerGlobalReceiver(ScanHighlightPayload.TYPE,
                (payload, context) -> ScanHighlight.accept(payload,
                        context.player().level().getGameTime(), context.player().level().dimension()));

        ClientPlayNetworking.registerGlobalReceiver(ScanVolumePayload.TYPE,
                (payload, context) -> ScanHighlight.acceptVolume(payload,
                        context.player().level().getGameTime(), context.player().level().dimension()));

        // END_CLIENT_TICK fires at the tail of Minecraft.tick(), which vanilla already runs inside
        // a gizmo collector. That is what lets the highlight be drawn without opening one here.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) {
                return;
            }
            ClientClock.set(client.level.getGameTime());
            ShaderCompat.refresh();
            ScanHighlight.tick(client.level.getGameTime(), client.player,
                    client.level.dimension());
        });

        // Leaving a world must drop the highlight, or it would reappear over unrelated terrain in
        // the next one.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ScanHighlight.clearAll());
    }
}
