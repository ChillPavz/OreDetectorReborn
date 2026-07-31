package com.chillpavz.oredetector.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class QuartzDetector extends OreDetectorItem {

    public QuartzDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return state.is(Blocks.NETHER_QUARTZ_ORE);
    }

    @Override
    protected Component getOreName() {
        return Component.translatable("ore.oredetector.quartz");
    }

    @Override
    protected int getOreColor() {
        return 0xEDE6DD;
    }
}
