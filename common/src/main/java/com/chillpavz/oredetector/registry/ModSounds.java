package com.chillpavz.oredetector.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import com.chillpavz.oredetector.Constants;
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
