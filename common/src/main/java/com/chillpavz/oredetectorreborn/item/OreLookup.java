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

import java.util.HashMap;
import java.util.Map;

import com.chillpavz.oredetectorreborn.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which ore a block counts as, plus how that ore is named and coloured.
 *
 * <p>Vanilla blocks are resolved up front, because they always exist. Other mods' ore blocks come
 * from {@link ModdedOres} by registry id and are resolved LAZILY on first use, since those mods
 * have not registered anything yet while this class is initialising.
 *
 * <p>Deliberately a hardcoded table for now. The modded-ore stage replaces it with common tag
 * matching ({@code c:ores/<material>}), at which point this becomes the fallback for untagged
 * blocks rather than the whole answer. The NAMES and COLOURS stay useful either way.
 */
public final class OreLookup {

    private static final Map<Block, String> BY_BLOCK = new HashMap<>();
    private static final Map<String, Integer> COLOURS = new HashMap<>();

    static {
        ore("coal", 0x6E6E6E, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        ore("copper", 0xE77C56, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        ore("iron", 0xD8D8D8, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        ore("gold", 0xFCEE4B, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
        ore("lapis", 0x3F63D0, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        ore("redstone", 0xE23D2E, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        ore("diamond", 0x4AEDD9, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        ore("emerald", 0x3BE37A, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        ore("quartz", 0xEDE6DD, Blocks.NETHER_QUARTZ_ORE);
        // Amethyst has no ore block; the geode blocks are what the detector looks for.
        ore("amethyst", 0xB57EDC, Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST);
        ore("netherite", 0x9A6B54, Blocks.ANCIENT_DEBRIS);
        // Zinc has no vanilla block, so only its colour is set here; the blocks that count as
        // zinc come from ModdedOres.
        COLOURS.put("zinc", 0xB8CFCC);
        // Energized Power's tin: the pale grey-white of its own ingot texture.
        COLOURS.put("tin", 0xD5DCE0);
        // Powah's uraninite. Sampled from the dust texture rather than guessed: its
        // commonest opaque pixels are #00ED12 and #00C911, fully saturated. The first
        // value here was an eyeballed olive and read as washed out beside every other
        // ore. Distinct from emerald (0x3BE37A), which is a mint green with a lot of
        // blue in it.
        COLOURS.put("uraninite", 0x00E512);
    }

    private OreLookup() {
    }

    /**
     * Every modded ore block that turned out to be registered, resolved once on first use.
     *
     * <p>Volatile and replaced wholesale rather than mutated, so the scan thread either sees the
     * finished map or builds its own identical one. Resolving is idempotent, so a race costs a
     * duplicated walk of a short list and nothing else.
     */
    private static volatile Map<Block, String> modded;

    /** The ore this block counts as, or null if it is not an ore the mod knows. */
    public static String oreTypeOf(BlockState state) {
        Block block = state.getBlock();
        String vanilla = BY_BLOCK.get(block);
        if (vanilla != null) {
            return vanilla;
        }
        return moddedBlocks().get(block);
    }

    /**
     * Resolves {@link ModdedOres#BY_ID} against the real block registry, once.
     *
     * <p>This cannot happen in a static initialiser: the other mods' blocks are registered after
     * this class is first touched, so every lookup would miss and the whole table would silently
     * do nothing.
     */
    private static Map<Block, String> moddedBlocks() {
        Map<Block, String> resolved = modded;
        if (resolved != null) {
            return resolved;
        }
        resolved = new HashMap<>();
        for (Map.Entry<Identifier, String> entry : ModdedOres.BY_ID.entrySet()) {
            // containsKey first, deliberately. The block registry is DEFAULTED, so getValue on an
            // absent mod's id returns AIR rather than null, and every unregistered id in the table
            // would map AIR to some ore. Air is very common in a scan.
            if (BuiltInRegistries.BLOCK.containsKey(entry.getKey())) {
                resolved.put(BuiltInRegistries.BLOCK.getValue(entry.getKey()), entry.getValue());
            }
        }
        Constants.LOG.info("Matched {} modded ore blocks out of {} known ids.",
                resolved.size(), ModdedOres.BY_ID.size());
        modded = resolved;
        return resolved;
    }

    /** Falls back to a readable name so a modded ore without a translation still reads properly. */
    public static Component displayName(String oreType) {
        return Component.translatableWithFallback(
                "ore." + Constants.MOD_ID + "." + oreType,
                OreGrinding.prettyFallback(oreType).replace(" Dust", " Ore"));
    }

    public static int colorOf(String oreType) {
        return COLOURS.getOrDefault(oreType, 0xFFFFFF);
    }

    private static void ore(String oreType, int colour, Block... blocks) {
        COLOURS.put(oreType, colour);
        for (Block block : blocks) {
            BY_BLOCK.put(block, oreType);
        }
    }
}
