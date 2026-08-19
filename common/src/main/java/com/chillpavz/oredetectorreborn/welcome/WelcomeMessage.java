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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The notice a player gets the first time they join a world with this version installed.
 *
 * <p>2.0 replaced eleven detectors with one, and moved every material behind a Trial Chamber. A
 * player updating from 1.x would otherwise find their detectors gone and no obvious way to make
 * the new one, so this is not decoration: it is the only in-game explanation of what changed.
 */
public final class WelcomeMessage {

    /**
     * Bump this to show the notice again to players who have already seen an older one. It is
     * the message's own version, deliberately not the mod's: a patch release should not re-notify
     * everyone, and a rewritten notice should.
     */
    public static final int VERSION = 1;

    private WelcomeMessage() {
    }

    /** Shows the notice if this player has not seen this version of it in this world. */
    public static void showIfNew(ServerPlayer player) {
        ServerLevel overworld = player.level().getServer().overworld();
        WelcomeState state = overworld.getDataStorage().computeIfAbsent(WelcomeState.TYPE);
        if (!state.markSeen(player.getUUID(), VERSION)) {
            return;
        }
        for (Component line : lines()) {
            player.sendSystemMessage(line);
        }
    }

    /**
     * The notice, as chat lines.
     *
     * <p>Every line is a translation key so this can be localised, and the colours are applied
     * here rather than written into the strings, so a translator cannot break the formatting.
     */
    private static Component[] lines() {
        return new Component[]{
                Component.empty(),
                key("title").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                key("rework").withStyle(ChatFormatting.WHITE),
                bullet(key("chambers").withStyle(ChatFormatting.GOLD)),
                bullet(key("advancements").withStyle(ChatFormatting.GREEN)),
                bullet(key("guidebook").withStyle(ChatFormatting.GRAY)),
                bullet(key("legacy").withStyle(ChatFormatting.LIGHT_PURPLE)),
                Component.empty(),
        };
    }

    private static Component bullet(Component text) {
        return Component.literal(" \u2022 ").withStyle(ChatFormatting.DARK_GRAY).append(text);
    }

    private static net.minecraft.network.chat.MutableComponent key(String name) {
        return Component.translatable("message." + Constants.MOD_ID + ".welcome." + name);
    }
}
