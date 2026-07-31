package com.chillpavz.oredetector.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CoalDetector extends OreDetectorItem {

    private static final ModdedOres UNIVERSAL_COAL = ModdedOres.universalOres("coal");

    public CoalDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)
                || UNIVERSAL_COAL.matches(state);
    }

    @Override
    protected Component getOreName() {
        return Component.translatable("ore.oredetector.coal");
    }

    @Override
    protected int getOreColor() {
        return 0x6E6E6E;
    }
}
