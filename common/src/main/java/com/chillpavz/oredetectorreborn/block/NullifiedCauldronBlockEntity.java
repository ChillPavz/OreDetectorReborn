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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Exactly how much spent liquid a Resonance Cauldron is holding.
 *
 * <p><b>Why this exists at all.</b> The cauldron shows four quarter levels, and for a while that
 * WAS the storage: a drain rounded up to a whole level, so emptying a 36 mB charge cost a full
 * 250 mB of cauldron space. That was tolerable while every charge was a multiple of a 250 mB
 * bottle and became silly the moment bottles stopped being one size, because a netherite charge is
 * a dozen millibuckets and would still have swallowed a quarter of the cauldron.
 *
 * <p>So the amount is stored here, exactly, and the blockstate's LEVEL is only the picture drawn
 * of it. Crossing a quarter is what moves the picture up; anything under the next quarter leaves
 * it where it was.
 *
 * <p><b>A cauldron placed before this existed has no block entity</b>, and rather than migrating
 * anything the block simply reads its level and multiplies. That is exactly what the amount used
 * to mean, so an old cauldron carries on with the right contents and gains an exact one the first
 * time anybody pours into it.
 */
public class NullifiedCauldronBlockEntity extends BlockEntity {

    private static final String KEY = "Millibuckets";

    private int millibuckets;

    public NullifiedCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NULLIFIED_CAULDRON, pos, state);
    }

    public int millibuckets() {
        return millibuckets;
    }

    public void setMillibuckets(int amount) {
        millibuckets = Math.max(0, Math.min(NullifiedCauldronBlock.CAPACITY, amount));
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        millibuckets = input.getIntOr(KEY, 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(KEY, millibuckets);
    }
}
