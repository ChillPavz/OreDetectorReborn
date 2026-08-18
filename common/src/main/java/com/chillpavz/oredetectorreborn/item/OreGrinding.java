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
import java.util.Locale;
import java.util.Map;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The ore materials the mod knows how to grind, and what grinding one costs.
 *
 * <p>Yield is inverse to value, and shears wear is proportional to it, so bulk ores are cheap to
 * process and precious ones are not. Vanilla shears have 238 durability, which is 119 grinds of
 * coal but only 23 of netherite.
 *
 * <p><b>Redstone is deliberately absent.</b> Redstone ore already drops a dust, so that dust feeds
 * the Resonance Chamber directly and there is nothing to grind.
 */
public final class OreGrinding {

    /** One grindable material: what goes in, what comes out, and what it costs the shears. */
    public record Entry(String oreType, Item input, int dustYield, int shearsDamage) {
    }

    /** Input item -> what grinding it produces. Iteration order is the tier order below. */
    public static final Map<Item, Entry> BY_INPUT = new LinkedHashMap<>();

    static {
        // Mass-producer: abundant, big-vein ores.
        add("coal", Items.COAL, 4, 2);
        add("copper", Items.RAW_COPPER, 4, 2);
        // Standard.
        add("lapis", Items.LAPIS_LAZULI, 3, 3);
        // Baseline.
        add("iron", Items.RAW_IRON, 2, 4);
        add("gold", Items.RAW_GOLD, 2, 4);
        add("quartz", Items.QUARTZ, 2, 4);
        add("amethyst", Items.AMETHYST_SHARD, 2, 4);
        // Precious.
        add("diamond", Items.DIAMOND, 1, 6);
        add("emerald", Items.EMERALD, 1, 6);
        // Legendary.
        add("netherite", Items.NETHERITE_SCRAP, 1, 10);
        // Zinc and the other modded materials arrive in the modded-ore stage, where their item ids
        // can be checked against the real jars rather than guessed.
    }

    private OreGrinding() {
    }

    /** The grind for this input, or null if it is not a grindable material. */
    public static Entry forInput(Item input) {
        return BY_INPUT.get(input);
    }

    /** Fallback display name for an ore type with no translation, e.g. "zinc" -> "Zinc Dust". */
    public static String prettyFallback(String oreType) {
        if (oreType == null || oreType.isEmpty()) {
            return "Crushed Ore";
        }
        String head = oreType.substring(0, 1).toUpperCase(Locale.ROOT);
        return head + oreType.substring(1).toLowerCase(Locale.ROOT) + " Dust";
    }

    private static void add(String oreType, Item input, int dustYield, int shearsDamage) {
        BY_INPUT.put(input, new Entry(oreType, input, dustYield, shearsDamage));
    }
}
