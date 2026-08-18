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

import com.chillpavz.oredetectorreborn.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Grinding an ore material into dust: shears in the off hand, the material in the main hand,
 * right-click.
 *
 * <p>This cannot be an {@code Item} override, because the item being used is a VANILLA one (coal,
 * raw iron). Each loader routes its own use-item hook here so the rule itself is written once:
 * Fabric through {@code ItemEvents.USE}, NeoForge through {@code PlayerInteractEvent.RightClickItem}.
 */
public final class OreGrindingInteraction {

    private OreGrindingInteraction() {
    }

    /**
     * Runs the grind if the player is holding the right things.
     *
     * @return {@link InteractionResult#PASS} when this is not a grind, so every other right-click
     *         behaves exactly as it did before. Only a real grind consumes the interaction.
     */
    public static InteractionResult tryGrind(Level level, Player player, InteractionHand hand) {
        // Only ever triggered by the main hand, or the off-hand pass would fire it a second time.
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.is(Items.SHEARS)) {
            return InteractionResult.PASS;
        }
        ItemStack material = player.getMainHandItem();
        OreGrinding.Entry entry = OreGrinding.forInput(material.getItem());
        if (entry == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            material.shrink(1);
            offhand.hurtAndBreak(entry.shearsDamage(), player, EquipmentSlot.OFFHAND);
            player.drop(CrushedOreItem.of(ModItems.CRUSHED_ORE, entry.oreType(), entry.dustYield()), false);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.8F, 1.2F);
        return InteractionResult.SUCCESS;
    }
}
