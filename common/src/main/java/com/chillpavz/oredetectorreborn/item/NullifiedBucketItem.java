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

import java.util.function.Consumer;

import com.chillpavz.oredetectorreborn.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A bucket of spent Attunement Liquid, drawn out of a Resonance Cauldron.
 *
 * <p>It can only be emptied into water or lava. There is deliberately no way to pour it on the
 * ground: the liquid is a by-product, and letting it be dumped anywhere would turn every drain into
 * litter. Disposing of it has to cost the player a trip to something that can take it.
 */
public class NullifiedBucketItem extends Item {

    public NullifiedBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // SOURCE fluids only, so a thin flowing edge does not count as somewhere to pour.
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != BlockHitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        var fluid = level.getFluidState(pos);
        boolean disposable = fluid.isSource()
                && (fluid.getType() == Fluids.WATER || fluid.getType() == Fluids.LAVA);
        if (!disposable) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            stack.shrink(1);
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        level.playSound(null, pos, fluid.getType() == Fluids.LAVA
                        ? SoundEvents.FIRE_EXTINGUISH : SoundEvents.BUCKET_EMPTY,
                SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.translatable("tooltip." + Constants.MOD_ID + ".nullified")
                .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("tooltip." + Constants.MOD_ID + ".nullified_dispose")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
