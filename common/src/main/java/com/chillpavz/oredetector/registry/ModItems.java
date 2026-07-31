package com.chillpavz.oredetector.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import com.chillpavz.oredetector.Constants;
import com.chillpavz.oredetector.config.OreDetectorConfig;
import com.chillpavz.oredetector.item.DiamondDetector;
import com.chillpavz.oredetector.item.EmeraldDetector;
import com.chillpavz.oredetector.item.GoldDetector;
import com.chillpavz.oredetector.item.IronDetector;
import com.chillpavz.oredetector.item.LapisDetector;
import com.chillpavz.oredetector.item.NetheriteDetector;
import com.chillpavz.oredetector.item.RedstoneDetector;
import com.chillpavz.oredetector.item.ZincDetector;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Defines the mod's items. Instances are created here (loader-agnostic) with their registry id,
 * durability and repair material baked into the properties; each loader module registers the
 * {@link #ITEMS} entries. Durability is deliberately INVERSE to ore value (rarer ore -> fewer
 * scans) so the netherite detector can't be used to farm netherite cheaply.
 */
public final class ModItems {

    public static final Map<Identifier, Item> ITEMS = new LinkedHashMap<>();

    public static final Item IRON_DETECTOR = create("iron_detector", 200, Items.IRON_INGOT, IronDetector::new);
    public static final Item GOLD_DETECTOR = create("gold_detector", 160, Items.GOLD_INGOT, GoldDetector::new);
    public static final Item DIAMOND_DETECTOR = create("diamond_detector", 120, Items.DIAMOND, DiamondDetector::new);
    public static final Item NETHERITE_DETECTOR = create("netherite_detector", 80, Items.NETHERITE_INGOT, NetheriteDetector::new);
    public static final Item LAPIS_DETECTOR = create("lapis_detector", 180, Items.LAPIS_LAZULI, LapisDetector::new);
    public static final Item REDSTONE_DETECTOR = create("redstone_detector", 200, Items.REDSTONE, RedstoneDetector::new);
    public static final Item EMERALD_DETECTOR = create("emerald_detector", 120, Items.EMERALD, EmeraldDetector::new);

    // Optional Create integration. Created LAZILY and only when Create is installed — an item is
    // built with its id baked in (an "intrusive holder"), and an unregistered one crashes NeoForge
    // at load ("Some intrusive holders were not registered"). Kept OUT of ITEMS.
    public static final Identifier ZINC_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "zinc_detector");
    public static Item ZINC_DETECTOR = null;

    private ModItems() {
    }

    /** Builds the zinc detector on demand; call ONLY when Create is present, then register it. */
    public static Item createZinc() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ZINC_ID);
        TagKey<Item> zincIngots = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/zinc"));
        ZINC_DETECTOR = new ZincDetector(new Item.Properties().setId(key).durability(OreDetectorConfig.scaleDurability(200)).repairable(zincIngots));
        return ZINC_DETECTOR;
    }

    private static Item create(String name, int durability, Item repairMaterial, Function<Item.Properties, Item> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item.Properties properties = new Item.Properties()
                .setId(key)
                .durability(OreDetectorConfig.scaleDurability(durability))
                .repairable(repairMaterial);
        Item item = factory.apply(properties);
        ITEMS.put(id, item);
        return item;
    }

    /** Forces class initialization so the static fields populate {@link #ITEMS}. */
    public static void bootstrap() {
    }
}
