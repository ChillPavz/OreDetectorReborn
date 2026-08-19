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

import com.chillpavz.oredetectorreborn.config.OreDetectorConfig;
import com.chillpavz.oredetectorreborn.registry.ModDataComponents;

/**
 * How hard the goggles have been pushed lately.
 *
 * <p>Each visualisation adds {@link #perScan()}; the total bleeds off one point every
 * {@code strainDecayTicks}. Below {@link #MAX} nothing is felt at all: the highlight is drawn at
 * full strength whatever the strain, and the number in the tooltip is the only warning. Crossing
 * {@link #MAX} gives the wearer Nausea and refuses to visualise until it decays back under. So it
 * is a self-clearing pressure on rapid re-scanning rather than a flat cooldown.
 *
 * <p>All three of those numbers are config options; only the ceiling is fixed. See
 * {@link OreDetectorConfig#applyStrain}, which warns when a combination makes strain unreachable.
 *
 * <p>Decay is NOT ticked. The value is stored with the game time it was written at and the decay
 * is worked out on read, so a stack sitting in a chest costs nothing and nothing has to run every
 * tick to keep it honest.
 *
 * @param value     strain as of {@link #updatedAt}, before any decay
 * @param updatedAt the game time that value was written at
 */
public record Strain(int value, long updatedAt) {

    /**
     * At and above this the goggles refuse to fire.
     *
     * <p>Deliberately NOT configurable: it is the denominator the tooltip shows, so keeping it at
     * 100 means "Strain: 40 / 100" always reads as a percentage. Everything about how fast that
     * number moves is tunable instead.
     */
    public static final int MAX = 100;

    /**
     * Ceiling on the STORED number. Strain is only ever added below {@link #MAX}, so it cannot
     * exceed MAX plus one scan's worth; this is a clamp against a hand-edited component.
     */
    private static final int STORED_CAP = MAX + OreDetectorConfig.STRAIN_PER_SCAN_MAX;

    /** Added by one visualisation. */
    public static int perScan() {
        return OreDetectorConfig.strainPerScan;
    }

    /**
     * How long the Nausea lasts on CROSSING {@link #MAX}, in ticks. It is not re-applied while
     * blocked, and zero means the effect is switched off and only the refusal remains.
     */
    public static int nauseaTicks() {
        return OreDetectorConfig.nauseaSeconds * 20;
    }

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
        long decayed = value - elapsed / Math.max(1, OreDetectorConfig.strainDecayTicks);
        return (int) Math.max(0L, decayed);
    }

    /** True when the goggles are too strained to visualise anything. */
    public boolean isBlockedAt(long gameTime) {
        return currentAt(gameTime) >= MAX;
    }

    /** Adds one visualisation's worth, rebased on the current game time. */
    public Strain plusScan(long gameTime) {
        return new Strain(currentAt(gameTime) + perScan(), gameTime);
    }
}
