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
package com.chillpavz.oredetectorreborn.loot;

import java.util.LinkedHashMap;
import java.util.Map;

import com.chillpavz.oredetectorreborn.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Which vanilla loot tables the mod adds to, and the table it adds in each case.
 *
 * <p>The loot itself is NOT defined here. Each value names one of our own loot tables under
 * {@code data/ore_detector_reborn/loot_table/inject/}, which carries the drop chance and the
 * stack size. That keeps the balance in one JSON per injection rather than split between two
 * loaders: Fabric adds a pool referencing the table, NeoForge rolls the same table through a
 * {@code neoforge:add_table} global loot modifier, and both therefore drop exactly the same thing.
 *
 * <p>The NeoForge side is pure JSON and cannot read this map, so the modifier files under
 * {@code neoforge/src/main/resources/data/ore_detector_reborn/loot_modifiers/} repeat these
 * targets. {@code tools/check_loot_injection.py} asserts the two agree.
 */
public final class LootInjections {

    /** Vanilla table to add to -> our table supplying the addition. */
    public static final Map<ResourceKey<LootTable>, ResourceKey<LootTable>> TABLES = new LinkedHashMap<>();

    static {
        // Occasional: supply chests are the common find.
        add("minecraft", "chests/trial_chambers/supply", "inject/trial_chambers_supply");
        // Occasional but thinner: the decorated pots that stand in chamber corridors. NOT
        // "blocks/decorated_pot", which is the block-break table and would make every pot in the
        // world drop shards, including ones the player placed.
        add("minecraft", "pots/trial_chambers/corridor", "inject/trial_chambers_pot");
        // Rare: standard vaults. Ominous vaults use "reward_ominous" and are left alone, because
        // the Heavy Core they already drop is this tier's reward.
        add("minecraft", "chests/trial_chambers/reward", "inject/trial_chambers_reward");
    }

    private LootInjections() {
    }

    private static void add(String targetNamespace, String targetPath, String ourPath) {
        TABLES.put(
                ResourceKey.create(Registries.LOOT_TABLE,
                        Identifier.fromNamespaceAndPath(targetNamespace, targetPath)),
                ResourceKey.create(Registries.LOOT_TABLE,
                        Identifier.fromNamespaceAndPath(Constants.MOD_ID, ourPath)));
    }
}
