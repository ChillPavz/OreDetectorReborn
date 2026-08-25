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

import com.chillpavz.oredetectorreborn.Constants;
import com.chillpavz.oredetectorreborn.menu.GrindMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Opening the grinder: sneak plus right-click on a grindstone, holding a grindable material.
 *
 * <p>This cannot be an {@code Item} override, because the item being held is a VANILLA one (coal,
 * an iron ingot). Each loader routes its own block-interaction hook here so the rule is written
 * once: Fabric through {@code UseBlockCallback}, NeoForge through
 * {@code PlayerInteractEvent.RightClickBlock}.
 *
 * <p><b>Why sneak, and why it costs nothing.</b> A plain right-click on a grindstone is already
 * taken: {@code GrindstoneBlock} overrides {@code useWithoutItem}, and the default
 * {@code useItemOn} falls through to it, so the menu opens before any item's own {@code useOn}
 * runs. Sneaking with a non-empty hand makes vanilla skip the block interaction entirely, which
 * leaves the item to act; every grindable material here is a plain item with no {@code useOn}, so
 * that gesture currently does nothing at all. Taking it displaces no vanilla behaviour, and
 * Repair and Disenchant stays exactly where players expect it.
 *
 * <p><b>An empty main hand opens it too</b>, because requiring the material in hand to reach the
 * menu is the kind of thing nobody guesses and everybody reports.
 *
 * <p><b>The MAIN hand decides, and the off hand is ignored entirely.</b> Owner's call, and it is
 * the rule players can actually state: sneak plus an empty main hand opens the grinder no matter
 * what the other hand is carrying. A shield is the obvious case, and gating on the off hand would
 * have made the gesture work or not work for reasons nobody could see.
 *
 * <p>Mechanically this is possible because our hook runs at the HEAD of
 * {@code ServerPlayerGameMode.useItemOn}, before vanilla decides anything. Vanilla's own rule is
 * {@code isSecondaryUseActive() && !(mainHand.isEmpty() && offHand.isEmpty())} to skip the block
 * and let the items act; the main-hand pass runs first, so consuming it stops the off-hand pass.
 *
 * <p><b>The cost, stated rather than hidden: you cannot sneak-place a block from the OFF hand
 * against a grindstone while your main hand is empty.</b> That is the one behaviour this takes,
 * it is a rare thing to want, and main-hand placing is untouched because any non-grindable item
 * held in the main hand falls straight through.
 *
 * <p>With both hands empty, all vanilla did was open the grindstone, which a plain right-click
 * already does, so that half costs nothing at all.
 *
 * <p><b>Redstone is the one item that could have collided</b>, because it is a BlockItem and would
 * otherwise be placed by the same gesture. It is deliberately absent from the grind table, since
 * redstone ore already drops its own dust, so the conflict cannot arise.
 *
 * <p><b>The two loaders' hooks do NOT share a contract.</b> Fabric's {@code UseBlockCallback}
 * treats {@link InteractionResult#PASS} as "not handled" and only cancels on something else, which
 * is the ordinary reading. That is the opposite of {@code ItemEvents.USE}, which this mod used
 * before and which treats <em>null</em> as "not handled" and returns any non-null result instead of
 * calling the item's own {@code use()}. Both were verified by disassembling the mixin rather than
 * assumed, and returning the wrong sentinel to either one breaks unrelated interactions across the
 * whole game with nothing logged.
 */
public final class OreGrindingInteraction {

    private OreGrindingInteraction() {
    }

    /**
     * Opens the grinder if the player is sneak-clicking a grindstone with something grindable.
     *
     * @return {@link InteractionResult#PASS} when this is not a grind, so every other block
     *         click behaves exactly as it did before. Only a real grind consumes the interaction.
     */
    public static InteractionResult tryOpenGrinder(Level level, Player player, InteractionHand hand,
                                                   BlockPos pos) {
        // Main hand only, or the off-hand pass fires the whole thing a second time.
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        // Without the sneak check this would swallow every plain right-click on a grindstone and
        // take Repair and Disenchant away from anyone holding an ingot.
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level.getBlockState(pos).is(Blocks.GRINDSTONE)) {
            return InteractionResult.PASS;
        }
        // The main hand decides: empty, or holding something we can actually grind. The off hand
        // is deliberately not consulted, so a shield never changes whether this works. See the
        // class notes for what that costs.
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty() && OreGrinding.forInput(main.getItem()) == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, opener) -> new GrindMenu(id, inventory,
                            ContainerLevelAccess.create(level, pos)),
                    Component.translatable("container." + Constants.MOD_ID + ".grind")));
        }
        return InteractionResult.SUCCESS;
    }
}
