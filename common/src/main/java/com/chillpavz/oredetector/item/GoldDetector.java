package com.chillpavz.oredetector.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class GoldDetector extends OreDetectorItem {

    public GoldDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE) || state.is(Blocks.NETHER_GOLD_ORE);
    }

    @Override
    protected Component getOreName() {
        return Component.translatable("ore.oredetector.gold");
    }

    @Override
    protected int getOreColor() {
        return 0xFCEE4B;
    }
}
