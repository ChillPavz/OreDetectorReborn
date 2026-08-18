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
package com.chillpavz.oredetectorreborn.block;

import com.chillpavz.oredetectorreborn.item.AttunementLiquidItem;
import com.chillpavz.oredetectorreborn.menu.ResonanceChamberMenu;
import com.chillpavz.oredetectorreborn.registry.ModBlockEntities;
import com.chillpavz.oredetectorreborn.registry.ModDataComponents;
import com.chillpavz.oredetectorreborn.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Resonance Chamber's contents and processing.
 *
 * <p>Written from scratch rather than extending vanilla's BrewingStandBlockEntity, which is welded
 * to PotionBrewing: its ingredient test, its output and its slot rules all resolve through the
 * potion registry, and none of that can be redirected at an ore dust.
 *
 * <p>Slot order matches vanilla's brewing stand exactly (three bottles, then ingredient, then
 * fuel), so the menu and the recoloured interface texture line up with vanilla's coordinates.
 */
public class ResonanceChamberBlockEntity extends BaseContainerBlockEntity {

    public static final int SLOT_INGREDIENT = 3;
    public static final int SLOT_FUEL = 4;
    public static final int CONTAINER_SIZE = 5;

    public static final int DATA_BREW_TIME = 0;
    public static final int DATA_FUEL = 1;
    public static final int NUM_DATA_VALUES = 2;

    /** Ticks for one batch. Vanilla brewing is 400; the chamber matches it. */
    public static final int BREW_TIME = 400;
    /** Batches one Breeze Powder fuels, matching vanilla's blaze powder. */
    public static final int FUEL_USES = 20;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int brewTime;
    private int fuel;
    /** The ore the current batch is producing, captured when the batch starts. */
    private String brewingOre;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == DATA_BREW_TIME ? brewTime : fuel;
        }

        @Override
        public void set(int index, int value) {
            if (index == DATA_BREW_TIME) {
                brewTime = value;
            } else {
                fuel = value;
            }
        }

        @Override
        public int getCount() {
            return NUM_DATA_VALUES;
        }
    };

    public ResonanceChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESONANCE_CHAMBER, pos, state);
    }

    /** The ore a stack in the ingredient slot would produce, or null if it is not a valid input. */
    public static String ingredientOre(ItemStack stack) {
        if (stack.is(ModItems.CRUSHED_ORE)) {
            return stack.get(ModDataComponents.ORE_TYPE);
        }
        // Redstone ore already drops a dust, so vanilla redstone is the input for that one
        // material and there is no crushed redstone at all.
        if (stack.is(Items.REDSTONE)) {
            return "redstone";
        }
        return null;
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(ModItems.BREEZE_POWDER);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResonanceChamberBlockEntity be) {
        ItemStack fuelStack = be.items.get(SLOT_FUEL);
        if (be.fuel <= 0 && isFuel(fuelStack)) {
            be.fuel = FUEL_USES;
            fuelStack.shrink(1);
            be.setChanged();
        }

        String ore = ingredientOre(be.items.get(SLOT_INGREDIENT));
        boolean canBrew = ore != null && be.hasEmptyBottle() && be.fuel > 0;

        if (be.brewTime > 0) {
            if (!canBrew || !ore.equals(be.brewingOre)) {
                // The ingredient or the bottles changed part way through, so abandon the batch
                // rather than hand back liquid for an ore that is no longer in the slot.
                be.brewTime = 0;
                be.brewingOre = null;
            } else if (--be.brewTime == 0) {
                be.finishBrew();
            }
            be.setChanged();
        } else if (canBrew) {
            be.brewTime = BREW_TIME;
            be.brewingOre = ore;
            be.fuel--;
            be.setChanged();
        }

        BlockState updated = state;
        for (int i = 0; i < 3; i++) {
            updated = updated.setValue(ResonanceChamberBlock.HAS_BOTTLE[i], !be.items.get(i).isEmpty());
        }
        if (updated != state) {
            level.setBlock(pos, updated, 2);
        }
    }

    private boolean hasEmptyBottle() {
        for (int i = 0; i < 3; i++) {
            if (items.get(i).is(Items.GLASS_BOTTLE)) {
                return true;
            }
        }
        return false;
    }

    private void finishBrew() {
        if (brewingOre == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            if (items.get(i).is(Items.GLASS_BOTTLE)) {
                items.set(i, AttunementLiquidItem.of(ModItems.ATTUNEMENT_LIQUID, brewingOre, 1));
            }
        }
        items.get(SLOT_INGREDIENT).shrink(1);
        brewingOre = null;
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.ore_detector_reborn.resonance_chamber");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ResonanceChamberMenu(id, inventory, this, dataAccess);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> stacks) {
        items = stacks;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_FUEL) {
            return isFuel(stack);
        }
        if (slot == SLOT_INGREDIENT) {
            return ingredientOre(stack) != null;
        }
        return stack.is(Items.GLASS_BOTTLE) || stack.is(ModItems.ATTUNEMENT_LIQUID);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        brewTime = input.getIntOr("BrewTime", 0);
        fuel = input.getIntOr("Fuel", 0);
        String ore = input.getStringOr("BrewingOre", "");
        brewingOre = ore.isEmpty() ? null : ore;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("BrewTime", brewTime);
        output.putInt("Fuel", fuel);
        if (brewingOre != null) {
            output.putString("BrewingOre", brewingOre);
        }
    }
}
