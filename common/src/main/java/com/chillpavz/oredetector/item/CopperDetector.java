package com.chillpavz.oredetector.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CopperDetector extends OreDetectorItem {

    public CopperDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE);
    }

    @Override
    protected Component getOreName() {
        return Component.translatable("ore.oredetector.copper");
    }

    @Override
    protected int getOreColor() {
        return 0xE77C56;
    }
}
