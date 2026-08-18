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

import com.chillpavz.oredetectorreborn.Constants;
import com.chillpavz.oredetectorreborn.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One item for every ore's dust. Which ore a stack holds lives in the {@code ore_type} data
 * component, and both the texture and the name follow from it: the texture through a
 * {@code minecraft:select} model in {@code assets/<ns>/items/crushed_ore.json}, the name here.
 *
 * <p>Registering one item rather than one per ore is what makes modded ores free later: a new
 * material needs a texture and a translation, not a registry entry.
 */
public class CrushedOreItem extends Item {

    public CrushedOreItem(Properties properties) {
        super(properties);
    }

    /** Builds a dust stack for the given ore type. */
    public static ItemStack of(Item dustItem, String oreType, int count) {
        ItemStack stack = new ItemStack(dustItem, count);
        stack.set(ModDataComponents.ORE_TYPE, oreType);
        return stack;
    }

    /** The ore this stack holds, or null if the component was never set. */
    public static String oreTypeOf(ItemStack stack) {
        return stack.get(ModDataComponents.ORE_TYPE);
    }

    @Override
    public Component getName(ItemStack stack) {
        String oreType = oreTypeOf(stack);
        if (oreType == null) {
            return super.getName(stack);
        }
        // A modded ore may have no translation of ours, so fall back to a readable name rather
        // than showing the raw key.
        return Component.translatableWithFallback(
                "item." + Constants.MOD_ID + ".crushed_ore." + oreType,
                OreGrinding.prettyFallback(oreType));
    }
}
