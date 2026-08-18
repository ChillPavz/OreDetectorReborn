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
package com.chillpavz.oredetectorreborn.menu;

import com.chillpavz.oredetectorreborn.block.ResonanceChamberBlockEntity;
import com.chillpavz.oredetectorreborn.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Resonance Chamber's menu.
 *
 * <p>Slot coordinates are vanilla's brewing stand coordinates, because the interface texture is a
 * recolour of vanilla's and nothing moved.
 */
public class ResonanceChamberMenu extends AbstractContainerMenu {

    private static final int CONTAINER_SLOTS = ResonanceChamberBlockEntity.CONTAINER_SIZE;

    private final Container container;
    private final ContainerData data;

    /** Client-side constructor: the menu is opened from the network with an empty stand-in. */
    public ResonanceChamberMenu(int id, Inventory inventory) {
        this(id, inventory,
                new SimpleContainer(CONTAINER_SLOTS),
                new SimpleContainerData(ResonanceChamberBlockEntity.NUM_DATA_VALUES));
    }

    public ResonanceChamberMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.RESONANCE_CHAMBER, id);
        checkContainerSize(container, CONTAINER_SLOTS);
        checkContainerDataCount(data, ResonanceChamberBlockEntity.NUM_DATA_VALUES);
        this.container = container;
        this.data = data;

        addSlot(new BottleSlot(container, 0, 56, 51));
        addSlot(new BottleSlot(container, 1, 79, 58));
        addSlot(new BottleSlot(container, 2, 102, 51));
        addSlot(new IngredientSlot(container, ResonanceChamberBlockEntity.SLOT_INGREDIENT, 79, 17));
        addSlot(new FuelSlot(container, ResonanceChamberBlockEntity.SLOT_FUEL, 17, 17));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    /** Ticks REMAINING on the current batch, 0 when idle. Matches vanilla's getBrewingTicks. */
    public int brewingTicks() {
        return data.get(ResonanceChamberBlockEntity.DATA_BREW_TIME);
    }

    /** Remaining batches of fuel. Drives the fuel gauge. */
    public int fuel() {
        return data.get(ResonanceChamberBlockEntity.DATA_FUEL);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < CONTAINER_SLOTS) {
            // Chamber -> player inventory.
            if (!moveItemStackTo(stack, CONTAINER_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (ResonanceChamberBlockEntity.isFuel(stack)) {
            if (!moveItemStackTo(stack, ResonanceChamberBlockEntity.SLOT_FUEL,
                    ResonanceChamberBlockEntity.SLOT_FUEL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ResonanceChamberBlockEntity.ingredientOre(stack) != null) {
            if (!moveItemStackTo(stack, ResonanceChamberBlockEntity.SLOT_INGREDIENT,
                    ResonanceChamberBlockEntity.SLOT_INGREDIENT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ResonanceChamberBlockEntity.isWaterBottle(stack)) {
            if (!moveItemStackTo(stack, 0, 3, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return result;
    }

    private static class BottleSlot extends Slot {
        BottleSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class IngredientSlot extends Slot {
        IngredientSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ResonanceChamberBlockEntity.ingredientOre(stack) != null;
        }
    }

    private static class FuelSlot extends Slot {
        FuelSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ResonanceChamberBlockEntity.isFuel(stack);
        }
    }
}
