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

import java.util.function.Consumer;

import com.chillpavz.oredetectorreborn.Constants;
import com.chillpavz.oredetectorreborn.client.ClientClock;
import com.chillpavz.oredetectorreborn.client.ShaderCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The goggles. Worn on the head, and the thing that turns a scan's numbers into something you can
 * look at.
 *
 * <p>The item itself holds no behaviour beyond its tooltip: the detector wears them down and
 * stamps their strain, and the client draws the highlight. This class exists so the wearer can
 * see the strain bleeding off rather than having to guess when the goggles will work again.
 */
public class GogglesItem extends Item {

    public GogglesItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.translatable("tooltip." + Constants.MOD_ID + ".goggles")
                .withStyle(ChatFormatting.GRAY));

        // A shaderpack stops the highlight drawing through terrain, and there is no way to tell
        // from in game that this is the reason rather than the goggles being broken.
        if (ShaderCompat.shaderPackInUse()) {
            adder.accept(Component.translatable("tooltip." + Constants.MOD_ID + ".goggles_shaders")
                    .withStyle(ChatFormatting.GOLD));
        }

        long now = ClientClock.gameTime();
        Strain strain = Strain.of(stack);
        int current = strain.currentAt(now);
        if (current <= 0) {
            adder.accept(Component.translatable("tooltip." + Constants.MOD_ID + ".strain_rested")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        // Amber while it only degrades the picture, red once the goggles will refuse to fire.
        ChatFormatting colour = current >= Strain.MAX ? ChatFormatting.RED : ChatFormatting.GOLD;
        adder.accept(Component.translatable("tooltip." + Constants.MOD_ID + ".strain",
                current, Strain.MAX).withStyle(colour));
    }
}
