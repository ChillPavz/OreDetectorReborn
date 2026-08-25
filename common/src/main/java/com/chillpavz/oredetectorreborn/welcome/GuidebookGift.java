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
package com.chillpavz.oredetectorreborn.welcome;

import com.chillpavz.oredetectorreborn.Constants;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The Tome of Resonance, built without a compile dependency on the mod that renders it.
 *
 * <p>This branch uses <b>Modonomicon</b> where 26.1.x uses Patchouli, and the reason is coverage
 * rather than preference: Patchouli has no 26.2 build, while Modonomicon ships one for every
 * loader. It is not a candidate on 26.1.x in turn, because it publishes one jar per Minecraft
 * patch version and that branch is deliberately one jar for the whole 26.1 line.
 *
 * <p>Happily the two share the shape that makes this free. Modonomicon's book item carries the
 * book's id in a <b>persistent</b> {@code DataComponentType<Identifier>}, exactly as Patchouli's
 * does, so a stack is minted by setting one component and a recipe can do the same with no Java at
 * all. Persistence is what makes it work: a network-only component would be rejected on save.
 *
 * <p>Resolved through {@code containsKey} rather than {@code getValue}, because
 * {@code BuiltInRegistries.ITEM} is DEFAULTED and hands back AIR for an id whose mod is absent.
 * Without that check a missing Modonomicon would hand every new player a stack of air.
 */
public final class GuidebookGift {

    private static final Identifier BOOK_ITEM =
            Identifier.fromNamespaceAndPath("modonomicon", "modonomicon");
    private static final Identifier BOOK_COMPONENT =
            Identifier.fromNamespaceAndPath("modonomicon", "book_id");
    /** Must match the folder the book lives in, or the item opens nothing. */
    private static final Identifier OUR_BOOK =
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "resonance");

    private static boolean warned;

    private GuidebookGift() {
    }

    /** True when Modonomicon is present, i.e. when there is a book to give at all. */
    public static boolean available() {
        return BuiltInRegistries.ITEM.containsKey(BOOK_ITEM)
                && BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(BOOK_COMPONENT);
    }

    /**
     * The Tome as a stack, or an empty stack when Modonomicon is absent.
     *
     * <p>The unchecked cast is the price of having no compile dependency. It is safe as long as
     * the book component holds an Identifier, which is what it is declared as, and it is wrapped
     * so that a change there costs the book rather than the game.
     */
    @SuppressWarnings("unchecked")
    public static ItemStack create() {
        if (!available()) {
            return ItemStack.EMPTY;
        }
        try {
            Item item = BuiltInRegistries.ITEM.getValue(BOOK_ITEM);
            DataComponentType<Identifier> type = (DataComponentType<Identifier>)
                    BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(BOOK_COMPONENT);
            ItemStack stack = new ItemStack(item);
            stack.set(type, OUR_BOOK);
            return stack;
        } catch (Throwable failure) {
            if (!warned) {
                warned = true;
                Constants.LOG.error("Could not build the Tome of Resonance. Modonomicon's book "
                        + "component is not what this expects, so the book will not be given out.",
                        failure);
            }
            return ItemStack.EMPTY;
        }
    }

    /** Hands the Tome over, dropping it at the player's feet if the inventory is full. */
    public static void give(Player player) {
        ItemStack book = create();
        if (book.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
    }
}
