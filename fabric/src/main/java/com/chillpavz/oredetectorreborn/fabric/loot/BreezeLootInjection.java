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
package com.chillpavz.oredetectorreborn.fabric.loot;

import com.chillpavz.oredetectorreborn.loot.LootInjections;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;

/**
 * Adds the mod's Trial Chamber materials to vanilla loot, the Fabric way.
 *
 * <p>Each injection is a single extra pool holding one reference to one of our own loot tables, so
 * the drop chance and the stack size live in that table's JSON rather than in this file. NeoForge
 * rolls the very same tables through {@code neoforge:add_table}, which is what keeps the two
 * loaders honest with each other. See {@link LootInjections}.
 */
public final class BreezeLootInjection {

    private BreezeLootInjection() {
    }

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            ResourceKey<LootTable> ours = LootInjections.TABLES.get(key);
            if (ours != null) {
                tableBuilder.withPool(LootPool.lootPool().add(NestedLootTable.lootTableReference(ours)));
            }
        });
    }
}
