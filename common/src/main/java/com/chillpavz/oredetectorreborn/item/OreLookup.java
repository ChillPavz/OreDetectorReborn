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
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which ore a block counts as, plus how that ore is named and coloured.
 *
 * <p>Deliberately a hardcoded vanilla table for now. The modded-ore stage replaces the block map
 * with common tag matching ({@code c:ores/<material>}), at which point this becomes the fallback
 * for untagged blocks rather than the whole answer. The NAMES and COLOURS stay useful either way.
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
        // Zinc and other modded ores arrive with tag matching in the modded-ore stage.
        COLOURS.put("zinc", 0xB8CFCC);
    }

    private OreLookup() {
    }

    /** The ore this block counts as, or null if it is not an ore the mod knows. */
    public static String oreTypeOf(BlockState state) {
        return BY_BLOCK.get(state.getBlock());
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
