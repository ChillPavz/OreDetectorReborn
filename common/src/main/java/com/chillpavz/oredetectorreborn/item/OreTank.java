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

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * What is in the detector's tank: how many millibuckets of each ore's Attunement Liquid.
 *
 * <p>Immutable. Every operation returns a new tank, so a stack's component is replaced rather than
 * mutated in place, which is what data components require.
 *
 * <p>Capacity is exactly three bottles, so draining a part-used tank throws away as little as
 * possible. The type cap exists because range is driven by how many DISTINCT liquids are loaded,
 * and past six the penalty stops meaning anything.
 */
public record OreTank(Map<String, Integer> charges) {

    /** Three bottles at 100 mB each. */
    public static final int CAPACITY = 300;
    /** One bottle of Attunement Liquid. */
    public static final int BOTTLE = 100;
    /** Beyond this many distinct ores a pour is refused. */
    public static final int MAX_TYPES = 6;

    public static final OreTank EMPTY = new OreTank(Map.of());

    /**
     * Persistent codec. It MUST be persistent, not merely network synchronised, for the same reason
     * the ore_type component must be: anything the client renders or the world saves goes through
     * this, and a network-only component is rejected outright.
     */
    public static final Codec<OreTank> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(OreTank::new, OreTank::charges);

    public static final StreamCodec<RegistryFriendlyByteBuf, OreTank> STREAM_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, String, Integer, LinkedHashMap<String, Integer>>map(
                            LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT)
                    .map(OreTank::new, tank -> new LinkedHashMap<>(tank.charges()));

    public OreTank {
        charges = Map.copyOf(charges);
    }

    public int total() {
        int sum = 0;
        for (int amount : charges.values()) {
            sum += amount;
        }
        return sum;
    }

    public int freeSpace() {
        return CAPACITY - total();
    }

    public boolean isEmpty() {
        return total() <= 0;
    }

    /** How many distinct ores are loaded. This is what decides the detector's range. */
    public int typeCount() {
        return charges.size();
    }

    public int amountOf(String oreType) {
        return charges.getOrDefault(oreType, 0);
    }

    /** True if this tank can find the given ore at all. */
    public boolean holds(String oreType) {
        return amountOf(oreType) > 0;
    }

    /**
     * How much of a bottle this tank would actually accept.
     *
     * @return 0 when the pour must be refused: the tank is full, or the ore would be a seventh type
     */
    public int acceptable(String oreType) {
        if (freeSpace() <= 0) {
            return 0;
        }
        if (!charges.containsKey(oreType) && typeCount() >= MAX_TYPES) {
            return 0;
        }
        return Math.min(BOTTLE, freeSpace());
    }

    /**
     * Pours a bottle in. Fills the free space and leaves every other charge untouched; the caller
     * consumes the whole bottle even when only part of it fits, which is the only way to load more
     * than three types into a 300 mB tank.
     */
    public OreTank pour(String oreType, int amount) {
        if (amount <= 0) {
            return this;
        }
        Map<String, Integer> next = new LinkedHashMap<>(charges);
        next.merge(oreType, amount, Integer::sum);
        return new OreTank(next);
    }

    /** Spends liquid for ore that was actually found. Never goes below zero. */
    public OreTank drain(String oreType, int amount) {
        int current = amountOf(oreType);
        if (current <= 0 || amount <= 0) {
            return this;
        }
        Map<String, Integer> next = new LinkedHashMap<>(charges);
        int remaining = current - amount;
        if (remaining > 0) {
            next.put(oreType, remaining);
        } else {
            next.remove(oreType);
        }
        return new OreTank(next);
    }

    /** Ores in a stable display order: most liquid first, then alphabetically. */
    public java.util.List<Map.Entry<String, Integer>> sorted() {
        return charges.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
    }
}
