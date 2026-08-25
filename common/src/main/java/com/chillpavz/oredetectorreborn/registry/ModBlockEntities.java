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
import java.util.Set;

import com.chillpavz.oredetectorreborn.Constants;
import com.chillpavz.oredetectorreborn.block.NullifiedCauldronBlockEntity;
import com.chillpavz.oredetectorreborn.block.ResonanceChamberBlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The mod's block entity types. At 26.2 BlockEntityType has no Builder any more; the constructor
 * takes the factory and the set of blocks it is valid for directly.
 */
public final class ModBlockEntities {

    public static final Map<Identifier, BlockEntityType<?>> BLOCK_ENTITIES = new LinkedHashMap<>();

    public static final BlockEntityType<ResonanceChamberBlockEntity> RESONANCE_CHAMBER =
            register("resonance_chamber", new BlockEntityType<>(
                    ResonanceChamberBlockEntity::new, Set.of(ModBlocks.RESONANCE_CHAMBER)));

    /** Holds the cauldron's EXACT contents; the blockstate's level is only the picture of it. */
    public static final BlockEntityType<NullifiedCauldronBlockEntity> NULLIFIED_CAULDRON =
            register("nullified_cauldron", new BlockEntityType<>(
                    NullifiedCauldronBlockEntity::new, Set.of(ModBlocks.NULLIFIED_CAULDRON)));

    private ModBlockEntities() {
    }

    private static <T extends BlockEntityType<?>> T register(String name, T type) {
        BLOCK_ENTITIES.put(Identifier.fromNamespaceAndPath(Constants.MOD_ID, name), type);
        return type;
    }

    public static void bootstrap() {
    }
}
