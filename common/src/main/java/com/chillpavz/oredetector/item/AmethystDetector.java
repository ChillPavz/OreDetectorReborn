package com.chillpavz.oredetector.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class AmethystDetector extends OreDetectorItem {

    public AmethystDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        // Amethyst has no ore; detect the geode's amethyst blocks and budding amethyst.
        return state.is(Blocks.AMETHYST_BLOCK) || state.is(Blocks.BUDDING_AMETHYST);
    }

    @Override
    protected Component getOreName() {
        return Component.translatable("ore.oredetector.amethyst");
    }

    @Override
    protected int getOreColor() {
        return 0xB57EDC;
    }
}
