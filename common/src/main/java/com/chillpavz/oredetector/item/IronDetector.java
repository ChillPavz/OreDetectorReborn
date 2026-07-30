package com.chillpavz.oredetector.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class IronDetector extends OreDetectorItem {

    public IronDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE);
    }

    @Override
    protected Component getOreName() {
        return Component.translatable("ore.oredetector.iron");
    }

    @Override
    protected int getOreColor() {
        return 0xD8D8D8;
    }
}
