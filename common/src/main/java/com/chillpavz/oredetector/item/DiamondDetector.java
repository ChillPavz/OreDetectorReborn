package com.chillpavz.oredetector.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DiamondDetector extends OreDetectorItem {

    public DiamondDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE);
    }

    @Override
    protected Component getOreName() {
        return Component.translatable("ore.oredetector.diamond");
    }

    @Override
    protected int getOreColor() {
        return 0x4AEDD9;
    }
}
