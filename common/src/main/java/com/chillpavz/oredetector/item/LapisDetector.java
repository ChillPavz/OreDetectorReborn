package com.chillpavz.oredetector.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class LapisDetector extends OreDetectorItem {

    private static final ModdedOres UNIVERSAL_LAPIS = ModdedOres.universalOres("lapis");

    public LapisDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)
                || UNIVERSAL_LAPIS.matches(state);
    }

    @Override
    protected Component getOreName() {
        return Component.translatable("ore.oredetector.lapis");
    }

    @Override
    protected int getOreColor() {
        return 0x3F63D0;
    }
}
