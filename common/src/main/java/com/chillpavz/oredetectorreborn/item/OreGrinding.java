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
package com.chillpavz.oredetectorreborn.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The ore materials the mod knows how to grind, and what grinding one costs.
 *
 * <p>Inputs are the PROCESSED form of each material: the ingot where one exists, otherwise the
 * item the ore itself drops. Yield is inverse to value, so bulk ores are cheap to process and
 * precious ones are not, and that yield is now the only cost. The original shears gesture also
 * charged durability, which was never a real gate: one pair covered twenty-four netherite dusts.
 * Moving to the grindstone dropped it and nothing about the balance moved.
 *
 * <p><b>Redstone is deliberately absent.</b> Redstone ore already drops a dust, so that dust feeds
 * the Resonance Chamber directly and there is nothing to grind.
 */
public final class OreGrinding {

    /**
     * One grindable material: what goes in and what comes out.
     *
     * @param input the vanilla item, or null for a modded material, which is identified by its
     *              registry id instead and resolved through {@link #BY_MODDED_ID}
     */
    public record Entry(String oreType, Item input, int dustYield, int bottleSize,
                        Identifier output) {

        /**
         * True when grinding this produces ANOTHER MOD's dust rather than our own Crushed Ore.
         *
         * <p>Those entries are balanced by the other mod's economy, not by ours, so the yield is
         * not ours to choose: see {@link #addForeign}.
         */
        public boolean isForeign() {
            return output != null;
        }
    }

    /** Input item -> what grinding it produces. Iteration order is the tier order below. */
    public static final Map<Item, Entry> BY_INPUT = new LinkedHashMap<>();

    /**
     * The same thing for other mods' materials, keyed by registry id because naming their items
     * would mean a compile dependency on them.
     */
    public static final Map<Identifier, Entry> BY_MODDED_ID = new LinkedHashMap<>();

    /** Resolved from {@link #BY_MODDED_ID} on first use; see {@link #moddedInputs()}. */
    private static volatile Map<Item, Entry> modded;

    static {
        // Mass-producer: abundant, big-vein ores.
        add("coal", Items.COAL, 4, 165);
        add("copper", Items.COPPER_INGOT, 4, 165);
        // Standard.
        add("lapis", Items.LAPIS_LAZULI, 3, 90);
        // Baseline.
        add("iron", Items.IRON_INGOT, 2, 45);
        add("gold", Items.GOLD_INGOT, 2, 45);
        add("quartz", Items.QUARTZ, 2, 45);
        add("amethyst", Items.AMETHYST_SHARD, 2, 45);
        // Precious.
        add("diamond", Items.DIAMOND, 1, 25);
        add("emerald", Items.EMERALD, 1, 25);
        // Legendary. The INGOT, deliberately: 4 scrap plus 4 gold for a single dust, which is
        // several times what any other entry costs. Netherite is the most valuable thing the
        // detector looks for, so attuning to it is meant to hurt.
        add("netherite", Items.NETHERITE_INGOT, 1, 12);
        // Create's zinc, mirroring copper: it is an abundant, big-vein ore in exactly the same
        // way. Every Create variant uses the namespace `create`, so this one id covers Create,
        // Create Fabric and Create Fly alike.
        addModded("create", "zinc_ingot", "zinc", 4, 165);
        // Energized Power's tin. Its dust is THEIRS, not ours, because the mod already ships one
        // and duplicating it would leave two tin dusts sitting side by side in every recipe book.
        // Priced at the copper tier to detect: tin generates like copper and is no rarer.
        addForeign("energizedpower", "tin_ingot", "tin", 165, "energizedpower", "tin_dust");
        // Powah's uraninite. Its own dust does NOT exist (checked against the jar), so
        // this is the ordinary path and mints our Crushed Ore, not a foreign one. The
        // input is the smelted item, matching the rule everywhere else: uraninite_raw
        // smelts into uraninite, so uraninite is the processed form.
        addModded("powah", "uraninite", "uraninite", 2, 45);
    }

    private OreGrinding() {
    }

    /**
     * Every ore that can be ground into dust, vanilla first and then whichever modded materials
     * are actually present.
     *
     * <p>This exists because {@link #BY_INPUT} is the VANILLA half only. Enumerating that map
     * directly is how the creative tab quietly ended up with no zinc dust and no zinc liquid even
     * with Create installed: grinding zinc worked, the textures and names were all there, and the
     * items were simply never offered. Anything that wants "all the ores" must come through here.
     *
     * <p>Only safe to call once registries are populated, which covers the creative tab and
     * anything in gameplay, but NOT a loader's own init.
     */
    public static List<String> allOreTypes() {
        List<String> types = new ArrayList<>();
        for (Entry entry : BY_INPUT.values()) {
            types.add(entry.oreType());
        }
        for (Entry entry : moddedInputs().values()) {
            if (!types.contains(entry.oreType())) {
                types.add(entry.oreType());
            }
        }
        return types;
    }

    /** The grind for this input, or null if it is not a grindable material. */
    public static Entry forInput(Item input) {
        Entry vanilla = BY_INPUT.get(input);
        return vanilla != null ? vanilla : moddedInputs().get(input);
    }

    /**
     * Resolves {@link #BY_MODDED_ID} against the real item registry, once.
     *
     * <p>Lazy for the same reason the ore blocks are: the other mod has not registered its items
     * yet while this class is initialising, so resolving here would silently find nothing.
     */
    private static Map<Item, Entry> moddedInputs() {
        Map<Item, Entry> resolved = modded;
        if (resolved != null) {
            return resolved;
        }
        resolved = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Entry> entry : BY_MODDED_ID.entrySet()) {
            // containsKey first: the item registry is defaulted too, so an absent mod's id would
            // otherwise resolve to AIR and make an empty hand grindable.
            if (BuiltInRegistries.ITEM.containsKey(entry.getKey())) {
                resolved.put(BuiltInRegistries.ITEM.getValue(entry.getKey()), entry.getValue());
            }
        }
        modded = resolved;
        return resolved;
    }

    private static void addModded(String namespace, String path, String oreType, int dustYield,
                                  int bottleSize) {
        BY_MODDED_ID.put(Identifier.fromNamespaceAndPath(namespace, path),
                new Entry(oreType, null, dustYield, bottleSize, null));
    }

    /**
     * A material whose dust belongs to ANOTHER MOD, so grinding it hands back their item rather
     * than our Crushed Ore.
     *
     * <p><b>The yield here is not a balance choice and must not be treated as one.</b> A modded
     * dust usually smelts back into its own ingot, so any yield above 1 is an ingot duplication
     * loop rather than a generous tier. Energized Power is exactly that shape: its
     * {@code #c:dusts/tin} blasts to a tin ingot 1:1. Its own pulverizer also turns one tin ingot
     * into exactly one tin dust at 100%, so matching that rate makes this a machine-free
     * alternative to a recipe they already ship, and neither a shortcut nor an exploit.
     *
     * <p>Read the other mod's own recipes before adding one of these. Both directions matter: what
     * their dust smelts into, and what rate they themselves grind at.
     */
    private static void addForeign(String namespace, String inputPath, String oreType,
                                   int bottleSize, String outputNamespace, String outputPath) {
        BY_MODDED_ID.put(Identifier.fromNamespaceAndPath(namespace, inputPath),
                new Entry(oreType, null, 1, bottleSize,
                        Identifier.fromNamespaceAndPath(outputNamespace, outputPath)));
    }

    /**
     * The stack grinding this entry should produce: another mod's dust when the entry names one,
     * otherwise our own Crushed Ore carrying the ore in its component.
     *
     * <p>Resolved through {@code containsKey} first, because BuiltInRegistries.ITEM is a DEFAULTED
     * registry: {@code getValue} on an id whose mod is absent returns AIR rather than null, and an
     * air result here would be a grinder that silently eats the material and hands back nothing.
     */
    public static ItemStack outputFor(Entry entry, Item ourDust) {
        if (entry.isForeign() && BuiltInRegistries.ITEM.containsKey(entry.output())) {
            return new ItemStack(BuiltInRegistries.ITEM.getValue(entry.output()), entry.dustYield());
        }
        if (entry.isForeign()) {
            // The mod that owns this dust is not installed, so nothing can be made of it. The
            // entry is only reachable through that mod's own ingot anyway, so this is unreachable
            // in practice and exists so a partial install degrades to "no recipe" not "no item".
            return ItemStack.EMPTY;
        }
        return CrushedOreItem.of(ourDust, entry.oreType(), entry.dustYield());
    }

    /** True when this ore's dust belongs to another mod, so we never mint one ourselves. */
    public static boolean isForeignDust(String oreType) {
        for (Entry entry : moddedInputs().values()) {
            if (entry.oreType().equals(oreType)) {
                return entry.isForeign();
            }
        }
        return false;
    }

    /**
     * The ore an ITEM counts as when it is another mod's dust, or null.
     *
     * <p>This is the reverse of {@link #outputFor}, and the Resonance Chamber needs it: a foreign
     * dust has to be a valid ingredient or the material could be ground and then never used.
     */
    public static String oreTypeOfForeignDust(ItemStack stack) {
        for (Entry entry : moddedInputs().values()) {
            if (entry.isForeign() && BuiltInRegistries.ITEM.containsKey(entry.output())
                    && stack.is(BuiltInRegistries.ITEM.getValue(entry.output()))) {
                return entry.oreType();
            }
        }
        return null;
    }

    /**
     * How many millibuckets one bottle of this ore's Attunement Liquid is worth.
     *
     * <p><b>This is where rarity lives now.</b> A millibucket is one ore reported, flatly, for
     * every material alike, so the tank is simply how many finds you are carrying. What differs is
     * how much brewing a millibucket costs: a bottle of coal is 165 of them and a bottle of
     * netherite is 12.
     *
     * <p>That is the inverse of the arrangement it replaced, where every bottle was 250 mB and a
     * netherite BLOCK cost 20 of them. Same brewing cost per dust, give or take, but the tank
     * stopped rationing an expensive ore to 37 finds however much you had brewed, and a charge can
     * no longer fall below the price of a single block and sit there unusable.
     *
     * <p>Falls back to the baseline tier rather than the cheapest, so an ore that somehow escapes
     * the table is not silently the most generous thing in the game.
     */
    public static int bottleSizeOf(String oreType) {
        for (Entry entry : BY_INPUT.values()) {
            if (entry.oreType().equals(oreType)) {
                return entry.bottleSize();
            }
        }
        for (Entry entry : moddedInputs().values()) {
            if (entry.oreType().equals(oreType)) {
                return entry.bottleSize();
            }
        }
        return 45;
    }

    /** Fallback display name for an ore type with no translation, e.g. "zinc" -> "Zinc Dust". */
    public static String prettyFallback(String oreType) {
        if (oreType == null || oreType.isEmpty()) {
            return "Crushed Ore";
        }
        String head = oreType.substring(0, 1).toUpperCase(Locale.ROOT);
        return head + oreType.substring(1).toLowerCase(Locale.ROOT) + " Dust";
    }

    private static void add(String oreType, Item input, int dustYield, int bottleSize) {
        BY_INPUT.put(input, new Entry(oreType, input, dustYield, bottleSize, null));
    }
}
