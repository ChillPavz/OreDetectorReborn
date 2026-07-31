package com.chillpavz.oredetector.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Conditional detector for Create's zinc ore. Common code can't compile against Create, so blocks
 * are matched by their registry id — this works whether or not Create (Fly) is installed; when it
 * isn't, no block will carry these ids so the detector simply never triggers. The item is only
 * registered at all when Create is present (see the loader entrypoints).
 */
public class ZincDetector extends OreDetectorItem {

    private static final ModdedOres ZINC = ModdedOres.of("create", "zinc_ore", "deepslate_zinc_ore");

    public ZincDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return ZINC.matches(state);
    }

    @Override
    protected Component getOreName() {
        return Component.translatable("ore.oredetector.zinc");
    }

    @Override
    protected int getOreColor() {
        return 0xB8CFCC;
    }
}
