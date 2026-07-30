package com.chillpavz.oredetector.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Conditional detector for Create's zinc ore. Common code can't compile against Create, so blocks
 * are matched by their registry id — this works whether or not Create (Fly) is installed; when it
 * isn't, no block will carry these ids so the detector simply never triggers. The item is only
 * registered at all when Create is present (see the loader entrypoints).
 */
public class ZincDetector extends OreDetectorItem {

    private static final Identifier ZINC_ORE = Identifier.fromNamespaceAndPath("create", "zinc_ore");
    private static final Identifier DEEPSLATE_ZINC_ORE = Identifier.fromNamespaceAndPath("create", "deepslate_zinc_ore");

    public ZincDetector(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.equals(ZINC_ORE) || id.equals(DEEPSLATE_ZINC_ORE);
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
