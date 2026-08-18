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
package com.chillpavz.oredetectorreborn.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import com.chillpavz.oredetectorreborn.Constants;
import com.chillpavz.oredetectorreborn.block.ResonanceChamberBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The mod's blocks and their paired BlockItems. Each loader module registers these maps.
 */
public final class ModBlocks {

    public static final Map<Identifier, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<Identifier, Item> BLOCK_ITEMS = new LinkedHashMap<>();

    public static final Block RESONANCE_CHAMBER = register("resonance_chamber",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(0.5F)
                    .lightLevel(state -> 1)
                    .sound(SoundType.STONE)
                    .noOcclusion(),
            ResonanceChamberBlock::new);

    private ModBlocks() {
    }

    private static Block register(String name, BlockBehaviour.Properties properties,
                                  java.util.function.Function<BlockBehaviour.Properties, Block> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, name);
        // Required from 1.21.2: a block built without its id throws at construction, and it is a
        // RUNTIME check that compiles perfectly.
        properties.setId(ResourceKey.create(Registries.BLOCK, id));
        Block block = factory.apply(properties);
        BLOCKS.put(id, block);

        // A BlockItem needs useBlockDescriptionPrefix() or its name resolves to item.<ns>.<name>
        // while the lang file keys on block.<ns>.<name>, showing the raw key in the tooltip.
        Item.Properties itemProperties = new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .useBlockDescriptionPrefix();
        BLOCK_ITEMS.put(id, new BlockItem(block, itemProperties));
        return block;
    }

    /** Forces class initialization so the static fields populate the maps. */
    public static void bootstrap() {
    }
}
