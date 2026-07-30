package com.chillpavz.oredetector.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class NetheriteDetector extends OreDetectorItem {

    public NetheriteDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return state.is(Blocks.ANCIENT_DEBRIS);
    }

    @Override
    protected Component getOreName() {
        return Component.translatable("ore.oredetector.netherite");
    }

    @Override
    protected int getOreColor() {
        return 0x9A6B54;
    }
}
