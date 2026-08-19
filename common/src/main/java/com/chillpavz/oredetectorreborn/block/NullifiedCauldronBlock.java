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

import com.chillpavz.oredetectorreborn.registry.ModBlocks;
import com.chillpavz.oredetectorreborn.registry.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A cauldron holding spent Attunement Liquid.
 *
 * <p>Draining the detector nullifies what comes out: the ore identity is gone, so this is one
 * fluid rather than one per ore, and one texture covers every combination a player might mix.
 *
 * <p>Extends the real cauldron class rather than a plain block. That is what makes anything asking
 * "is this a cauldron" say yes: the detector's own drain test, the vanilla interaction shape, and
 * third party readouts that special case cauldrons.
 *
 * <p>FOUR quarter levels rather than vanilla's three, so each is exactly 250 mB and a full one is
 * 1000, the same as a bucket.
 */
public class NullifiedCauldronBlock extends AbstractCauldronBlock {

    public static final MapCodec<NullifiedCauldronBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(propertiesCodec()).apply(instance, NullifiedCauldronBlock::new));

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 4);
    public static final int MAX_LEVEL = 4;
    /** One level. Four of them make a bucket. */
    public static final int PER_LEVEL = 250;
    public static final int CAPACITY = PER_LEVEL * MAX_LEVEL;

    public NullifiedCauldronBlock(Properties properties) {
        // No vanilla interactions: everything this cauldron does is handled below.
        super(properties, CauldronInteractions.EMPTY);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 1));
    }

    @Override
    public MapCodec<NullifiedCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(LEVEL) == MAX_LEVEL;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return (6.0 + state.getValue(LEVEL) * 3.0) / 16.0;
    }

    /** Middle-clicking gives a plain cauldron, which is also what readouts show as the icon. */
    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state,
                                       boolean includeData) {
        return new ItemStack(Items.CAULDRON);
    }

    public static int millibucketsIn(BlockState state) {
        return state.is(ModBlocks.NULLIFIED_CAULDRON) ? state.getValue(LEVEL) * PER_LEVEL : 0;
    }

    /** True if this position can take that much more liquid without going over a full bucket. */
    public static boolean canAccept(Level level, BlockPos pos, int millibuckets) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.NULLIFIED_CAULDRON) && !state.is(Blocks.CAULDRON)) {
            return false;
        }
        // Rounded UP to a whole level, because the cauldron only shows quarters. A dribble still
        // costs a quarter, and anything that would tip it past 1000 mB is refused outright: empty
        // it with a bucket first.
        int wanted = Math.max(1, (millibuckets + PER_LEVEL - 1) / PER_LEVEL);
        return millibucketsIn(state) / PER_LEVEL + wanted <= MAX_LEVEL;
    }

    /** Adds liquid, turning a plain cauldron into one of these. Call only after {@link #canAccept}. */
    public static void addTo(Level level, BlockPos pos, int millibuckets) {
        BlockState state = level.getBlockState(pos);
        int existing = millibucketsIn(state) / PER_LEVEL;
        int wanted = Math.max(1, (millibuckets + PER_LEVEL - 1) / PER_LEVEL);
        level.setBlock(pos, ModBlocks.NULLIFIED_CAULDRON.defaultBlockState()
                .setValue(LEVEL, Math.min(MAX_LEVEL, existing + wanted)), 3);
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.BUCKET)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        // Any level fills a bucket, not just a full one. Emptying it is how a player makes room
        // when a drain would otherwise be refused, so requiring it to be full would deadlock that.
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
