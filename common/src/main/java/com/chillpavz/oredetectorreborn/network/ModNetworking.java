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
package com.chillpavz.oredetectorreborn.network;

import java.util.concurrent.atomic.AtomicBoolean;

import com.chillpavz.oredetectorreborn.Constants;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * The one seam between common gameplay code and each loader's networking API.
 *
 * <p>Each loader installs a {@link Sender} during init. Common code sends through here and never
 * learns which loader it is on.
 */
public final class ModNetworking {

    /** Sends one clientbound payload to one player. */
    @FunctionalInterface
    public interface Sender {
        void send(ServerPlayer player, CustomPacketPayload payload);
    }

    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private static volatile Sender sender;

    private ModNetworking() {
    }

    public static void setSender(Sender sender) {
        ModNetworking.sender = sender;
    }

    /**
     * Sends a payload, or complains once if no loader installed a sender.
     *
     * <p>The complaint matters more than it looks. A silently dropped packet would present as the
     * goggles simply never lighting anything up, with a green build, no crash and nothing in the
     * log to say why. One ERROR line turns that into a five second diagnosis.
     */
    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        Sender current = sender;
        if (current == null) {
            if (WARNED.compareAndSet(false, true)) {
                Constants.LOG.error("No network sender was installed, so {} will never reach a client. "
                        + "The loader entry point must call ModNetworking.setSender during init.",
                        payload.type().id());
            }
            return;
        }
        current.send(player, payload);
    }
}
