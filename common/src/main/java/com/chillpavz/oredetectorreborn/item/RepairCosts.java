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

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Keeps the anvil's prior-work penalty off this mod's repairable items.
 *
 * <p>Vanilla stamps {@code REPAIR_COST} on an anvil result and grows it as
 * {@code cost * 2 + 1} on every visit that is not a pure rename, so the sequence a player pays is
 * 0, 1, 3, 7, 15, 31 levels of penalty on top of the materials. At a displayed cost of 40 the
 * anvil voids the result outright.
 *
 * <p>That is fine for a tool you enchant once and keep. It is wrong for one built around a
 * recurring repair loop: the Ore Detector costs two Heavy Cores and would become permanently
 * unrepairable scrap on its seventh visit, and the Goggles on theirs, with nothing warning the
 * player on the way there.
 *
 * <p>Clearing the component keeps every repair at the flat cost of the Breeze Shards actually
 * consumed, one level each, forever. It touches only the stacks whose items call it.
 */
public final class RepairCosts {

    private RepairCosts() {
    }

    /**
     * Drops any prior-work penalty the anvil stamped on this stack.
     *
     * <p>Guarded on the read so the common case is a component lookup and no write at all; only
     * the tick after an anvil visit actually changes anything.
     */
    public static void clear(ItemStack stack) {
        if (stack.getOrDefault(DataComponents.REPAIR_COST, 0) != 0) {
            stack.remove(DataComponents.REPAIR_COST);
        }
    }
}
