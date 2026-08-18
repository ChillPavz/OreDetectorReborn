/*
 * This file is part of Ore Detector Reborn.
 * Copyright (c) 2026 chillpavz
 *
 * SPDX-License-Identifier: LicenseRef-PolyForm-Shield-1.0.0
 *
 * Licensed under the PolyForm Shield License 1.0.0. You may use, modify and
 * redistribute this file for any purpose EXCEPT providing a product that competes
 * with Ore Detector Reborn. See LICENSE, or
 * <https://polyformproject.org/licenses/shield/1.0.0>.
 *
 * Required Notice: Copyright chillpavz (https://github.com/ChillPavz/OreDetectorReborn)
 *
 * Portions descend from Ore Detector by restonic4, MIT licensed. See NOTICE.
 */
package com.chillpavz.oredetectorreborn.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.chillpavz.oredetectorreborn.Constants;
import com.chillpavz.oredetectorreborn.config.OreDetectorConfig;
import com.chillpavz.oredetectorreborn.item.AmethystDetector;
import com.chillpavz.oredetectorreborn.item.CoalDetector;
import com.chillpavz.oredetectorreborn.item.CrushedOreItem;
import com.chillpavz.oredetectorreborn.item.CopperDetector;
import com.chillpavz.oredetectorreborn.item.DiamondDetector;
import com.chillpavz.oredetectorreborn.item.EmeraldDetector;
import com.chillpavz.oredetectorreborn.item.GoldDetector;
import com.chillpavz.oredetectorreborn.item.IronDetector;
import com.chillpavz.oredetectorreborn.item.LapisDetector;
import com.chillpavz.oredetectorreborn.item.NetheriteDetector;
import com.chillpavz.oredetectorreborn.item.OreGrinding;
import com.chillpavz.oredetectorreborn.item.QuartzDetector;
import com.chillpavz.oredetectorreborn.item.RedstoneDetector;
import com.chillpavz.oredetectorreborn.item.ZincDetector;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/**
 * Defines the mod's items. Instances are created here (loader-agnostic) with their registry id,
 * durability and repair material baked into the properties; each loader module registers the
 * {@link #ITEMS} entries. Durability is deliberately INVERSE to ore value (rarer ore -> fewer
 * scans) so the netherite detector can't be used to farm netherite cheaply.
 */
public final class ModItems {

    public static final Map<Identifier, Item> ITEMS = new LinkedHashMap<>();

    // --- Trial Chamber materials (2.0) ---------------------------------------------------------
    // Plain crafting materials, no durability. Breeze Rod and Heavy Core are vanilla items and are
    // used as-is; these three are what the mod adds to the chamber's loot economy.
    public static final Item BREEZE_SHARD = material("breeze_shard");
    public static final Item BREEZE_CRYSTALS = material("breeze_crystals");
    public static final Item BREEZE_POWDER = material("breeze_powder");

    // One item for every ore's dust; the ore itself rides in the ore_type data component. Its
    // texture is chosen client-side by a select model, so this needs no per-ore registry entries.
    public static final Item CRUSHED_ORE = create("crushed_ore", CrushedOreItem::new);

    // Durability is tuned inverse to ore rarity/value: abundant, big-vein ores (coal/copper/iron) get
    // the most scans; rare, high-value ores (diamond/emerald/netherite) get the fewest so a detector
    // can't cheaply farm them. See CHANGELOG for the reasoning.
    public static final Item COAL_DETECTOR = create("coal_detector", 260, Items.COAL, CoalDetector::new);
    public static final Item COPPER_DETECTOR = create("copper_detector", 240, Items.COPPER_INGOT, CopperDetector::new);
    public static final Item IRON_DETECTOR = create("iron_detector", 220, Items.IRON_INGOT, IronDetector::new);
    public static final Item REDSTONE_DETECTOR = create("redstone_detector", 200, Items.REDSTONE, RedstoneDetector::new);
    public static final Item QUARTZ_DETECTOR = create("quartz_detector", 200, Items.QUARTZ, QuartzDetector::new);
    public static final Item LAPIS_DETECTOR = create("lapis_detector", 180, Items.LAPIS_LAZULI, LapisDetector::new);
    public static final Item AMETHYST_DETECTOR = create("amethyst_detector", 160, Items.AMETHYST_SHARD, AmethystDetector::new);
    public static final Item GOLD_DETECTOR = create("gold_detector", 150, Items.GOLD_INGOT, GoldDetector::new);
    public static final Item DIAMOND_DETECTOR = create("diamond_detector", 120, Items.DIAMOND, DiamondDetector::new);
    public static final Item EMERALD_DETECTOR = create("emerald_detector", 110, Items.EMERALD, EmeraldDetector::new);
    public static final Item NETHERITE_DETECTOR = create("netherite_detector", 80, Items.NETHERITE_INGOT, NetheriteDetector::new);

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

    /** A plain item built by a specific class. */
    private static Item create(String name, Function<Item.Properties, Item> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, name);
        Item item = factory.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)));
        ITEMS.put(id, item);
        return item;
    }

    /** A plain crafting material: no durability, no repair material, no custom class. */
    private static Item material(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, name);
        Item item = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)));
        ITEMS.put(id, item);
        return item;
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

    /**
     * The stacks the creative tab should show for one registered item.
     *
     * <p>Almost always a single stack. Crushed Ore is the exception: the item alone carries no ore,
     * so a bare stack renders as the fallback texture and is the only dust a player could ever take
     * from the tab. It expands to one properly attuned stack per grindable ore instead.
     */
    public static List<ItemStack> creativeStacks(Item item) {
        if (item == CRUSHED_ORE) {
            return OreGrinding.BY_INPUT.values().stream()
                    .map(entry -> CrushedOreItem.of(CRUSHED_ORE, entry.oreType(), 1))
                    .toList();
        }
        return List.of(new ItemStack(item));
    }

    /** Forces class initialization so the static fields populate {@link #ITEMS}. */
    public static void bootstrap() {
    }
}
