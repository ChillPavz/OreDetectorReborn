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
import com.chillpavz.oredetectorreborn.config.OreDetectorConfig;
import com.chillpavz.oredetectorreborn.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Bottled Attunement Liquid. Like {@link CrushedOreItem} this is one item whose ore lives in the
 * shared {@code ore_type} component, so a bottle of iron and a bottle of zinc are the same item
 * with different data, and adding a material costs a texture rather than a registry entry.
 *
 * <p>Pouring lives here rather than on the detector, so the grip matches grinding: the thing being
 * consumed goes in the MAIN hand and the tool it feeds goes in the OFF hand. Keeping it off the
 * detector also means the detector's own right-click is free to scan, which it was not while the
 * detector had to check for a bottle first.
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

    /**
     * What one bottle of this ore is worth, after the config scalar.
     *
     * <p>Tiered by how valuable the ore is: 165 for coal down to 12 for netherite. That is where
     * rarity lives now, rather than in the price of reporting a block, which is a flat 1 for
     * everything.
     */
    private static int bottleSize(String oreType) {
        return OreDetectorConfig.scaleBottleYield(OreGrinding.bottleSizeOf(oreType));
    }

    /** The detector this bottle could pour into, or null if the off hand cannot take it. */
    private static ItemStack pourTarget(Player player, ItemStack bottle) {
        String ore = bottle.get(ModDataComponents.ORE_TYPE);
        if (ore == null) {
            return null;
        }
        ItemStack detector = player.getOffhandItem();
        if (!(detector.getItem() instanceof AttunedDetectorItem)) {
            return null;
        }
        // A completely full tank, or a seventh ore type, refuses rather than swallowing the bottle.
        return AttunedDetectorItem.tankOf(detector).acceptable(ore, bottleSize(ore)) > 0
                ? detector : null;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack bottle = player.getItemInHand(hand);
        if (pourTarget(player, bottle) == null) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof Player player) || pourTarget(player, stack) == null) {
            return 0;
        }
        // Paced by what actually goes in, not a flat time per bottle. Bottles are no longer one
        // size, so a flat time would mean forty-odd holds of a second and a half to fill a tank
        // with netherite; this makes a small bottle quick and a big one the full pour.
        String ore = stack.get(ModDataComponents.ORE_TYPE);
        int moved = AttunedDetectorItem.tankOf(player.getOffhandItem())
                .acceptable(ore, bottleSize(ore));
        return AttunedDetectorItem.flowTicks(moved);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return stack;
        }
        ItemStack detector = pourTarget(player, stack);
        if (detector == null) {
            return stack;
        }
        String ore = stack.get(ModDataComponents.ORE_TYPE);
        OreTank tank = AttunedDetectorItem.tankOf(detector);
        int accepted = tank.acceptable(ore, bottleSize(ore));
        if (accepted <= 0) {
            return stack;
        }
        if (!level.isClientSide()) {
            // The whole bottle goes even when only part of it fits, so topping up a nearly full
            // tank costs a bottle rather than being refused.
            AttunedDetectorItem.setTank(detector, tank.pour(ore, accepted));
            stack.shrink(1);
            if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8F, 1.0F);
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
