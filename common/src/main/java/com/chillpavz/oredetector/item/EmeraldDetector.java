package com.chillpavz.oredetector.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class EmeraldDetector extends OreDetectorItem {

    private static final ModdedOres UNIVERSAL_EMERALD = ModdedOres.universalOres("emerald");

    public EmeraldDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)
                || UNIVERSAL_EMERALD.matches(state);
    }

    @Override
    protected Component getOreName() {
        return Component.translatable("ore.oredetector.emerald");
    }

    @Override
    protected int getOreColor() {
        return 0x3BE37A;
    }
}
