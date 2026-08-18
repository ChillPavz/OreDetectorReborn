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
 * Bottled Attunement Liquid. Like {@link CrushedOreItem} this is one item whose ore lives in the
 * shared {@code ore_type} component, so a bottle of iron and a bottle of zinc are the same item
 * with different data, and adding a material costs a texture rather than a registry entry.
 */
public class AttunementLiquidItem extends Item {

    public AttunementLiquidItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Item item, String oreType, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(ModDataComponents.ORE_TYPE, oreType);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        String oreType = stack.get(ModDataComponents.ORE_TYPE);
        if (oreType == null) {
            return super.getName(stack);
        }
        return Component.translatableWithFallback(
                "item." + Constants.MOD_ID + ".attunement_liquid." + oreType,
                OreGrinding.prettyFallback(oreType).replace(" Dust", "") + " Attunement Liquid");
    }
}
