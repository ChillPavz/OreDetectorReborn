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
import com.chillpavz.oredetectorreborn.item.AttunedDetectorItem;
import com.chillpavz.oredetectorreborn.item.AttunementLiquidItem;
import com.chillpavz.oredetectorreborn.item.CrushedOreItem;
import com.chillpavz.oredetectorreborn.item.OreGrinding;
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

    // Bottled Attunement Liquid, keyed on the same ore_type component as the dust.
    public static final Item ATTUNEMENT_LIQUID = create("attunement_liquid", AttunementLiquidItem::new);

    // The flagship item. 1000 durability mirrors the Mace's 500, doubled because the recipe takes
    // two Heavy Cores. A Breeze Shard repairs 250, which is simply vanilla's 25% per repair
    // material, so no custom repair code is needed.
    public static final Item ORE_DETECTOR = createDetector();

    // Worn on the head, and worn DOWN by what the detector finds: 1 durability per ore block
    // reported while they are on. Half the detector's pool, since they only cost two crystals.
    public static final Item GOGGLES = createGoggles();

    // What you get by bucketing a full cauldron of drained liquid. Stacks to one, like any bucket.
    public static final Item NULLIFIED_BUCKET = createBucket();

    private ModItems() {
    }

    private static Item createBucket() {
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "nullified_bucket");
        Item item = new Item(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .stacksTo(1));
        ITEMS.put(id, item);
        return item;
    }

    private static Item createGoggles() {
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "goggles");
        Item item = new Item(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .durability(500)
                .repairable(BREEZE_SHARD)
                // No armour layer texture: a worn non-armour item renders through its model's
                // "head" display context, which is how the item definition drives it.
                .component(net.minecraft.core.component.DataComponents.EQUIPPABLE,
                        net.minecraft.world.item.equipment.Equippable
                                .builder(net.minecraft.world.entity.EquipmentSlot.HEAD)
                                .setEquipSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER)
                                .setSwappable(true)
                                .build()));
        ITEMS.put(id, item);
        return item;
    }

    private static Item createDetector() {
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "ore_detector");
        Item item = new AttunedDetectorItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .durability(1000)
                .repairable(BREEZE_SHARD));
        ITEMS.put(id, item);
        return item;
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
        if (item == ATTUNEMENT_LIQUID) {
            // Redstone has a liquid but no dust, because vanilla redstone is its input.
            return java.util.stream.Stream.concat(
                            OreGrinding.BY_INPUT.values().stream().map(OreGrinding.Entry::oreType),
                            java.util.stream.Stream.of("redstone"))
                    .map(ore -> AttunementLiquidItem.of(ATTUNEMENT_LIQUID, ore, 1))
                    .toList();
        }
        return List.of(new ItemStack(item));
    }

    /** Forces class initialization so the static fields populate {@link #ITEMS}. */
    public static void bootstrap() {
    }
}
