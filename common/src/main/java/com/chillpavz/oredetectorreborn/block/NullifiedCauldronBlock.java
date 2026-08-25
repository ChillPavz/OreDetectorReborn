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
public class NullifiedCauldronBlock extends AbstractCauldronBlock
        implements net.minecraft.world.level.block.EntityBlock {

    public static final MapCodec<NullifiedCauldronBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(propertiesCodec()).apply(instance, NullifiedCauldronBlock::new));

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 4);
    public static final int MIN_LEVEL = 1;
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

    /**
     * Exactly what is in the cauldron.
     *
     * <p>Reads the block entity, and falls back to the blockstate for a cauldron placed before
     * that existed: level times PER_LEVEL is precisely what the amount used to mean, so an old one
     * keeps the contents it had.
     */
    public static int millibucketsIn(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.NULLIFIED_CAULDRON)) {
            return 0;
        }
        return level.getBlockEntity(pos) instanceof NullifiedCauldronBlockEntity cauldron
                ? cauldron.millibuckets()
                : state.getValue(LEVEL) * PER_LEVEL;
    }

    /**
     * The quarter level drawn for an amount.
     *
     * <p>Rounds UP, so any liquid at all shows something and a nearly full cauldron looks full,
     * which matters because that is also when it starts refusing drains. Crossing a quarter moves
     * the picture up; below the next quarter it stays where it was.
     */
    private static int levelFor(int millibuckets) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL,
                (millibuckets + PER_LEVEL - 1) / PER_LEVEL));
    }

    /**
     * True if this position can take that much more liquid without going over a full bucket.
     *
     * <p>Exact now: a dribble costs a dribble. It used to round up to a whole quarter, which was
     * fine while every charge was a multiple of a bottle and wasteful once a bottle stopped being
     * one size. Anything that would tip it past a full bucket is still refused outright rather
     * than clamped, so nothing is silently swallowed.
     */
    public static boolean canAccept(Level level, BlockPos pos, int millibuckets) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.NULLIFIED_CAULDRON) && !state.is(Blocks.CAULDRON)) {
            return false;
        }
        return millibucketsIn(level, pos) + millibuckets <= CAPACITY;
    }

    /** Adds liquid, turning a plain cauldron into one of these. Call only after {@link #canAccept}. */
    public static void addTo(Level level, BlockPos pos, int millibuckets) {
        int total = Math.min(CAPACITY, millibucketsIn(level, pos) + millibuckets);
        // The block is replaced first so the block entity exists to write the exact amount into,
        // including for a plain vanilla cauldron or one placed before the entity existed.
        level.setBlock(pos, ModBlocks.NULLIFIED_CAULDRON.defaultBlockState()
                .setValue(LEVEL, levelFor(total)), 3);
        if (level.getBlockEntity(pos) instanceof NullifiedCauldronBlockEntity cauldron) {
            cauldron.setMillibuckets(total);
        }
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
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NullifiedCauldronBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }
}
