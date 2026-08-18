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

import com.chillpavz.oredetectorreborn.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Resonance Chamber: a brewing stand in shape and interaction, burning Breeze Powder to turn
 * ore dust into Attunement Liquid.
 *
 * <p>The three {@code has_bottle_N} properties are the same ones vanilla uses, because the
 * blockstate and models were generated from vanilla's and swap only the texture.
 */
public class ResonanceChamberBlock extends BaseEntityBlock {

    public static final MapCodec<ResonanceChamberBlock> CODEC = simpleCodec(ResonanceChamberBlock::new);
    public static final BooleanProperty[] HAS_BOTTLE = new BooleanProperty[]{
            BooleanProperty.create("has_bottle_0"),
            BooleanProperty.create("has_bottle_1"),
            BooleanProperty.create("has_bottle_2"),
    };

    public ResonanceChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(HAS_BOTTLE[0], false)
                .setValue(HAS_BOTTLE[1], false)
                .setValue(HAS_BOTTLE[2], false));
    }

    @Override
    public MapCodec<ResonanceChamberBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResonanceChamberBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.RESONANCE_CHAMBER,
                        ResonanceChamberBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ResonanceChamberBlockEntity chamber) {
            player.openMenu(chamber);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // The same faint smoke vanilla puts over a brewing stand, so the block reads as working.
        double x = pos.getX() + 0.4 + random.nextFloat() * 0.2;
        double y = pos.getY() + 0.7 + random.nextFloat() * 0.3;
        double z = pos.getZ() + 0.4 + random.nextFloat() * 0.2;
        level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(HAS_BOTTLE[0], HAS_BOTTLE[1], HAS_BOTTLE[2]);
    }
}
