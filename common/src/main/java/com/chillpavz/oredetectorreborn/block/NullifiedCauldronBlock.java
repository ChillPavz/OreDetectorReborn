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
package com.chillpavz.oredetectorreborn.block;

import com.chillpavz.oredetectorreborn.item.OreTank;
import com.chillpavz.oredetectorreborn.registry.ModBlocks;
import com.chillpavz.oredetectorreborn.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cauldron holding spent Attunement Liquid.
 *
 * <p>Draining the detector nullifies what comes out: the ore identity is gone, so this is one
 * fluid rather than one per ore, and one texture covers every combination a player could pour in.
 *
 * <p>Three levels, matching a vanilla cauldron's own, and each is one bottle at
 * {@link OreTank#BOTTLE} mB. A full cauldron is therefore exactly one full detector tank.
 */
public class NullifiedCauldronBlock extends Block {

    public static final MapCodec<NullifiedCauldronBlock> CODEC = simpleCodec(NullifiedCauldronBlock::new);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 3);
    public static final int MAX_LEVEL = 3;

    /** One level is one bottle, so a full cauldron holds a whole tank. */
    public static final int PER_LEVEL = OreTank.BOTTLE;

    public NullifiedCauldronBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 1));
    }

    @Override
    public MapCodec<NullifiedCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, CollisionContext context) {
        return Blocks.CAULDRON.defaultBlockState().getShape(level, pos);
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                             BlockPos pos) {
        return Blocks.CAULDRON.defaultBlockState().getShape(level, pos);
    }

    /** How many levels a given amount of liquid is worth, rounded up so a dribble still shows. */
    public static int levelsFor(int millibuckets) {
        return Math.max(1, (millibuckets + PER_LEVEL - 1) / PER_LEVEL);
    }

    /**
     * Adds liquid to whatever is at this position, turning an empty vanilla cauldron into one of
     * these. Returns false when there is no room or nothing there to fill.
     */
    public static boolean addTo(Level level, BlockPos pos, int millibuckets) {
        BlockState state = level.getBlockState(pos);
        int existing = state.is(ModBlocks.NULLIFIED_CAULDRON) ? state.getValue(LEVEL) : 0;
        if (existing == 0 && !state.is(Blocks.CAULDRON)) {
            return false;
        }
        int wanted = existing + levelsFor(millibuckets);
        if (existing >= MAX_LEVEL) {
            return false;
        }
        level.setBlock(pos, ModBlocks.NULLIFIED_CAULDRON.defaultBlockState()
                .setValue(LEVEL, Math.min(MAX_LEVEL, wanted)), 3);
        return true;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        // Only a FULL cauldron fills a bucket, the same rule vanilla water follows.
        if (!stack.is(Items.BUCKET) || state.getValue(LEVEL) < MAX_LEVEL) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            stack.shrink(1);
            ItemStack filled = new ItemStack(ModItems.NULLIFIED_BUCKET);
            if (!player.getInventory().add(filled)) {
                player.drop(filled, false);
            }
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
        }
        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }
}
