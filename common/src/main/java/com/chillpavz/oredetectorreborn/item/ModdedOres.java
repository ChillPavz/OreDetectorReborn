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

import net.minecraft.resources.Identifier;

/**
 * Ore blocks belonging to other mods, listed by registry id so nothing here needs a compile
 * dependency on any of them.
 *
 * <p>Listing a whole grid is deliberate and safe: a combination a mod does not actually register
 * is simply not a block, so it never matches anything and never errors. That also means the table
 * stays correct if one of these mods fills in more of its own grid later.
 *
 * <p>This is a stopgap with a known shape. The real answer is matching the ecosystem's common ore
 * BLOCK tags ({@code c:ores/<material>}), which would cover every ore mod ever written instead of
 * the handful named here. That is the modded-ore stage; until then these are the mods the mod is
 * actually shipped alongside.
 */
public final class ModdedOres {

    /** Every modded ore block this mod knows, as registry id -> the ore it counts as. */
    public static final Map<Identifier, String> BY_ID = new LinkedHashMap<>();

    /**
     * Host stones that Seamless Ores and Universal Ores generate OVERWORLD variants in.
     * Deliberately not `calcite`, which Seamless Ores does not ship.
     */
    private static final String[] OVERWORLD_STONES = {"andesite", "diorite", "granite", "tuff"};
    /** Host stones used for the NETHER ores (nether gold and quartz). */
    private static final String[] NETHER_STONES = {"basalt", "blackstone"};

    static {
        // Create. Its zinc is the one modded ore the mod ships real support for: dust texture,
        // liquid, lang and a grind entry. Note every Create variant (Create, Create Fabric and
        // Create Fly) uses the mod id and namespace `create`, so one spelling covers all of them.
        ore("create", "zinc", "zinc_ore", "deepslate_zinc_ore");

        // Energized Power's tin, the one supported mod that ships Fabric AND NeoForge on every
        // Minecraft version this mod does.
        ore("energizedpower", "tin", "tin_ore", "deepslate_tin_ore");

        // Powah's uraninite, which generates in three richness tiers rather than one,
        // so there are six blocks and not two.
        ore("powah", "uraninite", "uraninite_ore", "deepslate_uraninite_ore",
                "uraninite_ore_poor", "deepslate_uraninite_ore_poor",
                "uraninite_ore_dense", "deepslate_uraninite_ore_dense");

        // Seamless Ores restyles vanilla ores into the host stone they generate in, so each of
        // these counts as its plain vanilla counterpart. Only the VANILLA ores are listed: the
        // rest of its 295 blocks belong to other mods' materials, which this mod cannot name yet.
        for (String stone : OVERWORLD_STONES) {
            seamless(stone, "coal", "copper", "iron", "gold", "lapis", "redstone", "diamond",
                    "emerald", "zinc");
            // Seamless Ores names Energized Power's tin `energized_tin` to keep it apart from
            // the other mods' tin, which is a different texture and a different item.
            seamlessAs(stone, "energized_tin", "tin");
            // Note Seamless Ores orders the richness word differently from Powah:
            // <stone>_uraninite_poor_ore against Powah's uraninite_ore_poor.
            seamlessAs(stone, "uraninite", "uraninite");
            seamlessAs(stone, "uraninite_poor", "uraninite");
            seamlessAs(stone, "uraninite_dense", "uraninite");
        }
        // Its nether variants: basalt and blackstone gold, and basalt and blackstone quartz.
        for (String stone : NETHER_STONES) {
            seamless(stone, "gold", "quartz");
        }

        // Universal Ores does the same thing in its own namespace and over a wider stone list.
        // The full 7x9 grid is listed on purpose: checked against Universal Ores 1.8.0, it covers
        // all 44 blocks that mod actually registers, with 19 combinations it does not ship. Those
        // 19 are simply not blocks, so they match nothing and cost nothing, and the support stays
        // correct if it fills more of its own grid in later.
        for (String stone : new String[]{"andesite", "diorite", "granite", "tuff", "calcite",
                "blackstone", "basalt"}) {
            for (String ore : new String[]{"coal", "copper", "iron", "gold", "lapis", "redstone",
                    "diamond", "emerald", "quartz"}) {
                ore("universal_ores", ore, stone + "_" + ore + "_ore");
            }
        }
    }

    private ModdedOres() {
    }

    /**
     * A Seamless Ores variant whose BLOCK name differs from the ore it counts as.
     *
     * <p>Needed because that project disambiguates the several mods that each add a "tin": its
     * block is {@code <stone>_energized_tin_ore} while the material is plain {@code tin} to us.
     *
     * <p>It exists as a named helper rather than an inline concatenation for a second reason: the
     * cross-project audit rebuilds this table by parsing the source, and it can only read plain
     * string literals. An inline {@code stone + "_energized_tin_ore"} was read as the literal
     * {@code _energized_tin_ore} and reported as a missing block. Keep every call site literal.
     */
    private static void seamlessAs(String stone, String blockPrefix, String oreType) {
        ore("seamlessores", oreType, stone + "_" + blockPrefix + "_ore");
    }

    /** One Seamless Ores host stone's worth of vanilla ore variants. */
    private static void seamless(String stone, String... ores) {
        for (String ore : ores) {
            ore("seamlessores", ore, stone + "_" + ore + "_ore");
        }
    }

    private static void ore(String namespace, String oreType, String... paths) {
        for (String path : paths) {
            BY_ID.put(Identifier.fromNamespaceAndPath(namespace, path), oreType);
        }
    }
}
