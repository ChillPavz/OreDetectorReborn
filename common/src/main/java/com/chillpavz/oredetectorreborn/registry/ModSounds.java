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
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Defines the mod's sound events. Instances are created here (loader-agnostic); each loader module
 * registers the {@link #SOUND_EVENTS} entries into the game registry.
 */
public final class ModSounds {

    public static final Map<Identifier, SoundEvent> SOUND_EVENTS = new LinkedHashMap<>();

    public static final SoundEvent NOT_FOUND = create("not_found");
    public static final SoundEvent FOUND = create("found");

    private ModSounds() {
    }

    private static SoundEvent create(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, name);
        SoundEvent sound = SoundEvent.createVariableRangeEvent(id);
        SOUND_EVENTS.put(id, sound);
        return sound;
    }

    /** Forces class initialization so the static fields populate {@link #SOUND_EVENTS}. */
    public static void bootstrap() {
    }
}
