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
import com.chillpavz.oredetectorreborn.item.OreTank;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

/**
 * The mod's data components. Each loader module registers {@link #COMPONENTS} into the game
 * registry; the instances themselves are loader-agnostic.
 */
public final class ModDataComponents {

    public static final Map<Identifier, DataComponentType<?>> COMPONENTS = new LinkedHashMap<>();

    /**
     * Which ore a Crushed Ore or Attunement Liquid stack carries, as a bare material name
     * ({@code "iron"}, {@code "zinc"}), NOT a block or item id. One name covers every mod's
     * version of that material, which is what lets a single dust item serve modded ores later.
     *
     * <p>It MUST be {@code persistent}: the client picks the texture with a
     * {@code minecraft:select} model keyed on this component, and that resolves the {@code when}
     * values through the component's own Codec. A network-only component is rejected outright
     * ("Component can't be serialized"), so a stream codec alone would break item rendering.
     */
    public static final DataComponentType<String> ORE_TYPE = register("ore_type",
            DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    /** What is loaded in the detector's tank. Persistent, like every component we render from. */
    public static final DataComponentType<OreTank> ORE_TANK = register("ore_tank",
            DataComponentType.<OreTank>builder()
                    .persistent(OreTank.CODEC)
                    .networkSynchronized(OreTank.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }

    private static <T> DataComponentType<T> register(String name, DataComponentType<T> type) {
        COMPONENTS.put(Identifier.fromNamespaceAndPath(Constants.MOD_ID, name), type);
        return type;
    }

    /** Forces class initialization so the static fields populate {@link #COMPONENTS}. */
    public static void bootstrap() {
    }
}
