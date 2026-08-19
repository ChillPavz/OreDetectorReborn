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

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The ore materials the mod knows how to grind, and what grinding one costs.
 *
 * <p>Inputs are the PROCESSED form of each material: the ingot where one exists, otherwise the
 * item the ore itself drops. Yield is inverse to value, and shears wear is proportional to it,
 * so bulk ores are cheap to process and precious ones are not. Vanilla shears have 238 durability, which is 119 grinds of
 * coal but only 23 of netherite.
 *
 * <p><b>Redstone is deliberately absent.</b> Redstone ore already drops a dust, so that dust feeds
 * the Resonance Chamber directly and there is nothing to grind.
 */
public final class OreGrinding {

    /**
     * One grindable material: what goes in, what comes out, and what it costs the shears.
     *
     * @param input the vanilla item, or null for a modded material, which is identified by its
     *              registry id instead and resolved through {@link #BY_MODDED_ID}
     */
    public record Entry(String oreType, Item input, int dustYield, int shearsDamage) {
    }

    /** Input item -> what grinding it produces. Iteration order is the tier order below. */
    public static final Map<Item, Entry> BY_INPUT = new LinkedHashMap<>();

    /**
     * The same thing for other mods' materials, keyed by registry id because naming their items
     * would mean a compile dependency on them.
     */
    public static final Map<Identifier, Entry> BY_MODDED_ID = new LinkedHashMap<>();

    /** Resolved from {@link #BY_MODDED_ID} on first use; see {@link #moddedInputs()}. */
    private static volatile Map<Item, Entry> modded;

    static {
        // Mass-producer: abundant, big-vein ores.
        add("coal", Items.COAL, 4, 2);
        add("copper", Items.COPPER_INGOT, 4, 2);
        // Standard.
        add("lapis", Items.LAPIS_LAZULI, 3, 3);
        // Baseline.
        add("iron", Items.IRON_INGOT, 2, 4);
        add("gold", Items.GOLD_INGOT, 2, 4);
        add("quartz", Items.QUARTZ, 2, 4);
        add("amethyst", Items.AMETHYST_SHARD, 2, 4);
        // Precious.
        add("diamond", Items.DIAMOND, 1, 6);
        add("emerald", Items.EMERALD, 1, 6);
        // Legendary. The INGOT, deliberately: 4 scrap plus 4 gold for a single dust, which is
        // several times what any other entry costs. Netherite is the most valuable thing the
        // detector looks for, so attuning to it is meant to hurt.
        add("netherite", Items.NETHERITE_INGOT, 1, 10);
        // Create's zinc, mirroring copper: it is an abundant, big-vein ore in exactly the same
        // way. Every Create variant uses the namespace `create`, so this one id covers Create,
        // Create Fabric and Create Fly alike.
        addModded("create", "zinc_ingot", "zinc", 4, 2);
    }

    private OreGrinding() {
    }

    /** The grind for this input, or null if it is not a grindable material. */
    public static Entry forInput(Item input) {
        Entry vanilla = BY_INPUT.get(input);
        return vanilla != null ? vanilla : moddedInputs().get(input);
    }

    /**
     * Resolves {@link #BY_MODDED_ID} against the real item registry, once.
     *
     * <p>Lazy for the same reason the ore blocks are: the other mod has not registered its items
     * yet while this class is initialising, so resolving here would silently find nothing.
     */
    private static Map<Item, Entry> moddedInputs() {
        Map<Item, Entry> resolved = modded;
        if (resolved != null) {
            return resolved;
        }
        resolved = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Entry> entry : BY_MODDED_ID.entrySet()) {
            // containsKey first: the item registry is defaulted too, so an absent mod's id would
            // otherwise resolve to AIR and make an empty hand grindable.
            if (BuiltInRegistries.ITEM.containsKey(entry.getKey())) {
                resolved.put(BuiltInRegistries.ITEM.getValue(entry.getKey()), entry.getValue());
            }
        }
        modded = resolved;
        return resolved;
    }

    private static void addModded(String namespace, String path, String oreType, int dustYield,
                                  int shearsDamage) {
        BY_MODDED_ID.put(Identifier.fromNamespaceAndPath(namespace, path),
                new Entry(oreType, null, dustYield, shearsDamage));
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
