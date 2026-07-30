package com.chillpavz.oredetector.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneDetector extends OreDetectorItem {

    public RedstoneDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE);
    }

    @Override
    protected Component getOreName() {
        return Component.translatable("ore.oredetector.redstone");
    }

    @Override
    protected int getOreColor() {
        return 0xE23D2E;
    }
}
