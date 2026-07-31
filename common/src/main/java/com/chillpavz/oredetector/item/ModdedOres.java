package com.chillpavz.oredetector.item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ore blocks contributed by mods the common code can't compile against, matched by registry id so
 * nothing here needs a compile-time dependency on them.
 *
 * <p>The ids are resolved to real blocks once, on the first scan — by then the block registry is
 * populated and frozen. When the mod isn't installed nothing resolves, and {@link #matches} then
 * short-circuits on an empty set for the rest of the session: an absent mod costs a single lookup
 * pass and nothing after that. That is why these can be wired into every detector unconditionally,
 * on every loader, with no mod-loaded gate.
 */
public final class ModdedOres {

    private static final String UNIVERSAL_ORES = "universal_ores";

    /**
     * Host stones Universal Ores (Hugman) re-generates vanilla ores in. Not every stone carries
     * every ore (basalt and blackstone only host gold and quartz), but listing the full grid is
     * deliberate: the missing combinations simply aren't registered blocks, so they resolve to
     * nothing, and the mod filling more of the grid in later needs no change here.
     */
    private static final String[] UNIVERSAL_STONES =
            {"andesite", "diorite", "granite", "tuff", "calcite", "blackstone", "basalt"};

    private final List<Identifier> ids;
    private volatile Set<Block> blocks;

    private ModdedOres(List<Identifier> ids) {
        this.ids = ids;
    }

    /** Matches the given block paths in {@code namespace}. */
    public static ModdedOres of(String namespace, String... paths) {
        List<Identifier> ids = new ArrayList<>(paths.length);
        for (String path : paths) {
            ids.add(Identifier.fromNamespaceAndPath(namespace, path));
        }
        return new ModdedOres(ids);
    }

    /** Every Universal Ores variant of a vanilla ore, e.g. {@code "iron"} -> {@code universal_ores:andesite_iron_ore}. */
    public static ModdedOres universalOres(String ore) {
        List<Identifier> ids = new ArrayList<>(UNIVERSAL_STONES.length);
        for (String stone : UNIVERSAL_STONES) {
            ids.add(Identifier.fromNamespaceAndPath(UNIVERSAL_ORES, stone + "_" + ore + "_ore"));
        }
        return new ModdedOres(ids);
    }

    public boolean matches(BlockState state) {
        Set<Block> resolved = blocks;
        if (resolved == null) {
            resolved = resolve();
        }
        return !resolved.isEmpty() && resolved.contains(state.getBlock());
    }

    /** Looks the ids up in the (by now frozen) block registry; unknown ids are dropped. */
    private Set<Block> resolve() {
        Set<Block> found = new HashSet<>();
        for (Identifier id : ids) {
            // BLOCK is a defaulted registry, so getValue would hand back AIR for an unknown id.
            if (BuiltInRegistries.BLOCK.containsKey(id)) {
                found.add(BuiltInRegistries.BLOCK.getValue(id));
            }
        }
        Set<Block> resolved = Set.copyOf(found);
        blocks = resolved;
        return resolved;
    }
}
