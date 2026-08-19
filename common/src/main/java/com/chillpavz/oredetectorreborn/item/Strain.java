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
package com.chillpavz.oredetectorreborn.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import com.chillpavz.oredetectorreborn.registry.ModDataComponents;

/**
 * How hard the goggles have been pushed lately.
 *
 * <p>Each visualisation adds {@link #PER_SCAN}; the total bleeds off at {@link #DECAY_PER_SECOND}
 * a second. Below {@link #MAX} it only degrades the highlight (dimmer, flickering, shorter);
 * at {@link #MAX} the goggles refuse to fire and give the wearer Nausea instead. So it is a
 * self-clearing pressure on rapid re-scanning rather than a flat cooldown.
 *
 * <p>Decay is NOT ticked. The value is stored with the game time it was written at and the decay
 * is worked out on read, so a stack sitting in a chest costs nothing and nothing has to run every
 * tick to keep it honest.
 *
 * @param value     strain as of {@link #updatedAt}, before any decay
 * @param updatedAt the game time that value was written at
 */
public record Strain(int value, long updatedAt) {

    /** At and above this the goggles refuse to fire. */
    public static final int MAX = 100;
    /** Added by one visualisation. Five back-to-back scans reach {@link #MAX}. */
    public static final int PER_SCAN = 20;
    /**
     * Bleed-off rate. This has to be read against the scan cooldown, not on its own: a scan can
     * only happen every {@code cooldownTicks}, so strain rises at all only while
     * {@code PER_SCAN > DECAY_PER_SECOND * cooldownSeconds}. At the default five second cooldown
     * that is 20 against 10, a net +10 a scan, so the goggles give out on the tenth back to back
     * scan and are comfortable at any normal pace. The design doc suggested 5 a second; at that
     * rate the decay (25) outruns the gain (20) and strain could never reach the ceiling at all.
     */
    public static final int DECAY_PER_SECOND = 2;
    /** How long the Nausea lasts on CROSSING {@link #MAX}. It is not re-applied while blocked. */
    public static final int NAUSEA_TICKS = 100;

    /**
     * Ceiling on the STORED number. Strain can only be added below {@link #MAX}, so it can never
     * exceed MAX + PER_SCAN; this is a belt-and-braces clamp against a hand-edited component.
     */
    private static final int STORED_CAP = MAX + PER_SCAN;

    public static final Strain NONE = new Strain(0, 0L);

    public static final Codec<Strain> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("value").forGetter(Strain::value),
            Codec.LONG.fieldOf("updated_at").forGetter(Strain::updatedAt)
    ).apply(instance, Strain::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Strain> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Strain::value,
            ByteBufCodecs.VAR_LONG, Strain::updatedAt,
            Strain::new);

    public Strain {
        value = Math.max(0, Math.min(STORED_CAP, value));
    }

    public static Strain of(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STRAIN, NONE);
    }

    public static void set(ItemStack stack, Strain strain) {
        if (strain.value() <= 0) {
            stack.remove(ModDataComponents.STRAIN);
        } else {
            stack.set(ModDataComponents.STRAIN, strain);
        }
    }

    /**
     * Strain right now, with the bleed-off since it was written applied.
     *
     * <p>Elapsed time is clamped at zero. A client reading this before it has seen a single tick
     * has a game time of 0, which would otherwise make the elapsed span negative and the strain
     * grow on its own.
     */
    public int currentAt(long gameTime) {
        long elapsed = Math.max(0L, gameTime - updatedAt);
        long decayed = value - elapsed * DECAY_PER_SECOND / 20L;
        return (int) Math.max(0L, decayed);
    }

    /** True when the goggles are too strained to visualise anything. */
    public boolean isBlockedAt(long gameTime) {
        return currentAt(gameTime) >= MAX;
    }

    /**
     * How clear the highlight should be, 1.0 when rested down to 0.0 at {@link #MAX}. The client
     * dims, shortens and flickers the highlight by this, which is the early warning that the
     * next scan or two will be refused.
     */
    public float fidelityAt(long gameTime) {
        return 1.0F - Math.min(1.0F, currentAt(gameTime) / (float) MAX);
    }

    /** Adds one visualisation's worth, rebased on the current game time. */
    public Strain plusScan(long gameTime) {
        return new Strain(currentAt(gameTime) + PER_SCAN, gameTime);
    }
}
