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

import com.chillpavz.oredetectorreborn.item.OreGrinding;
import com.chillpavz.oredetectorreborn.registry.ModItems;
import com.chillpavz.oredetectorreborn.registry.ModMenus;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Grinding an ore material down to dust, at a grindstone.
 *
 * <p>This replaces the original shears gesture. A block is more discoverable than a two-hand item
 * combination, the grindstone is the one vanilla block whose whole job is grinding, and it costs
 * nothing to reach. The shears' durability was never a real gate either: one pair covered
 * twenty-four netherite dusts, so about an ingot and a half of overhead across the most expensive
 * material in the mod. All of the balancing lives in the yield table, and that is unchanged.
 *
 * <p><b>Vanilla's grindstone is untouched.</b> This is our own menu on our own {@code MenuType},
 * opened by a SNEAK right-click; a plain right-click still gets Repair and Disenchant. Which sneak
 * clicks count, and why that displaces nothing, is spelled out on
 * {@link com.chillpavz.oredetectorreborn.item.OreGrindingInteraction}.
 *
 * <p>Transient, like vanilla's own grindstone: there is no block entity and nothing is stored in
 * the world, so whatever is left in the input slot is returned when the menu closes.
 */
public class GrindMenu extends AbstractContainerMenu {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_RESULT = 1;
    private static final int MENU_SLOTS = 2;

    /**
     * Slot positions, in panel-local coordinates and measured off the interface texture rather
     * than copied from vanilla's grindstone. Ours has one input rather than two, centred and
     * levelled with the arrow and the output.
     */
    private static final int INPUT_X = 49;
    private static final int INPUT_Y = 32;
    private static final int RESULT_X = 129;
    private static final int RESULT_Y = 32;

    private final Container input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            GrindMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();
    private final ContainerLevelAccess access;

    /** Client-side constructor: opened from the network, with no world access. */
    public GrindMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public GrindMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.GRIND, id);
        this.access = access;

        addSlot(new Slot(input, SLOT_INPUT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return OreGrinding.forInput(stack.getItem()) != null;
            }
        });

        addSlot(new Slot(result, SLOT_RESULT, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack taken) {
                // One material per take, however big the stack in the input slot is.
                input.getItem(SLOT_INPUT).shrink(1);
                input.setChanged();
                access.execute((level, pos) -> level.playSound(null, pos,
                        SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, 1.0F));
                super.onTake(player, taken);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    /** Recomputes the output whenever the input changes. */
    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == input) {
            OreGrinding.Entry entry = OreGrinding.forInput(input.getItem(SLOT_INPUT).getItem());
            // outputFor, not a Crushed Ore directly: a material whose dust belongs to another
            // mod hands back THEIR item instead, so there are never two dusts for one metal.
            result.setItem(SLOT_RESULT, entry == null
                    ? ItemStack.EMPTY
                    : OreGrinding.outputFor(entry, ModItems.CRUSHED_ORE));
            broadcastChanges();
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // The result is recomputed from the input, never stored, so only the input is given back.
        access.execute((level, pos) -> clearContainer(player, input));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Blocks.GRINDSTONE);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return moved;
        }
        ItemStack stack = slot.getItem();
        moved = stack.copy();

        if (index == SLOT_RESULT) {
            // Shift-clicking the output grinds repeatedly, the way any result slot does.
            if (!moveItemStackTo(stack, MENU_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, moved);
        } else if (index == SLOT_INPUT) {
            if (!moveItemStackTo(stack, MENU_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (OreGrinding.forInput(stack.getItem()) != null) {
            if (!moveItemStackTo(stack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
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
        if (stack.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return moved;
    }
}
