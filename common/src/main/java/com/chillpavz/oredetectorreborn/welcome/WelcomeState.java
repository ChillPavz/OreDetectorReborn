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
package com.chillpavz.oredetectorreborn.welcome;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.chillpavz.oredetectorreborn.Constants;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Which players have already been shown the welcome message, and for which version of it.
 *
 * <p>Kept as world saved data rather than on the player, because there is no cross-loader way to
 * attach persistent data to a player without a mixin or a loader-specific attachment API. It also
 * gives the right behaviour for free: the notice explains what changed in THIS world.
 *
 * <p>The stored value is a message version, not a boolean, so bumping
 * {@link WelcomeMessage#VERSION} shows the notice again to everyone. That is what makes it an
 * update notice and not only a first-install one.
 */
public class WelcomeState extends SavedData {

    // STRING_CODEC, not CODEC: an unbounded map's keys have to encode to strings, and the
    // default UUID codec writes an int array.
    private static final Codec<Map<UUID, Integer>> SEEN_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT);

    private static final Codec<WelcomeState> CODEC = SEEN_CODEC.xmap(seen -> {
        WelcomeState state = new WelcomeState();
        state.seen.putAll(seen);
        return state;
    }, state -> state.seen);

    public static final SavedDataType<WelcomeState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "welcome"),
            WelcomeState::new, CODEC, DataFixTypes.LEVEL);

    private final Map<UUID, Integer> seen = new HashMap<>();

    /**
     * Records that this player has now seen the current message.
     *
     * @return true if they had NOT seen this version before, i.e. the message should be shown
     */
    public boolean markSeen(UUID player, int version) {
        Integer previous = seen.get(player);
        if (previous != null && previous >= version) {
            return false;
        }
        seen.put(player, version);
        setDirty();
        return true;
    }
}
