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
package com.chillpavz.oredetectorreborn.client;

/**
 * The client's current game time, mirrored where common code can read it.
 *
 * <p>Strain decays against the game clock, and the goggles' tooltip has to show that decay as it
 * happens. Tooltips are given no level and no time, and reaching for {@code Minecraft} from an
 * {@code Item} would put a client-only class where the dedicated server can load it. One volatile
 * long written by the client tick hook avoids both.
 *
 * <p>It stays at zero on a server, which is correct: nothing there renders a tooltip, and
 * {@link com.chillpavz.oredetectorreborn.item.Strain#currentAt} clamps a negative span to zero
 * rather than letting strain grow backwards.
 */
public final class ClientClock {

    private static volatile long gameTime;

    private ClientClock() {
    }

    public static void set(long time) {
        gameTime = time;
    }

    public static long gameTime() {
        return gameTime;
    }
}
