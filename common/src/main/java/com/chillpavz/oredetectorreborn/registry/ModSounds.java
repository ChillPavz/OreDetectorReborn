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
