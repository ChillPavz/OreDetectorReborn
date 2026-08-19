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
 * Hands the player the Tome of Resonance, if Patchouli is installed.
 *
 * <p>Built entirely by registry lookup, so this mod still has no compile dependency on Patchouli.
 * The book item takes a single component holding the book's id, and that component's type is
 * fetched by name rather than imported.
 *
 * <p>Every step is guarded. Without Patchouli the item simply is not registered, and the player
 * gets the welcome message and no book, which is the correct outcome rather than a failure.
 */
public final class GuidebookGift {

    private static final Identifier BOOK_ITEM =
            Identifier.fromNamespaceAndPath("patchouli", "guide_book");
    private static final Identifier BOOK_COMPONENT =
            Identifier.fromNamespaceAndPath("patchouli", "book");
    /** Must match the folder the book lives in, or the item opens nothing. */
    private static final Identifier OUR_BOOK =
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "resonance");

    private static boolean warned;

    private GuidebookGift() {
    }

    /** True when Patchouli is present, i.e. when there is a book to give at all. */
    public static boolean available() {
        return BuiltInRegistries.ITEM.containsKey(BOOK_ITEM)
                && BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(BOOK_COMPONENT);
    }

    /**
     * The Tome as a stack, or an empty stack when Patchouli is absent.
     *
     * <p>The unchecked cast is the price of having no compile dependency. It is safe as long as
     * Patchouli keeps its book component holding an Identifier, which is what it has always been,
     * and it is wrapped so that a change there costs the book rather than the game.
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
                Constants.LOG.error("Could not build the Tome of Resonance. Patchouli's book "
                        + "component is not what this expects, so the book will not be given out.",
                        failure);
            }
            return ItemStack.EMPTY;
        }
    }

    /** Gives the player the Tome, dropping it at their feet if their inventory is full. */
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
