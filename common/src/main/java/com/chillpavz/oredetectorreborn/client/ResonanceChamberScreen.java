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
package com.chillpavz.oredetectorreborn.client;

import com.chillpavz.oredetectorreborn.Constants;
import com.chillpavz.oredetectorreborn.block.ResonanceChamberBlockEntity;
import com.chillpavz.oredetectorreborn.menu.ResonanceChamberMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Resonance Chamber's interface.
 *
 * <p>26.2 draws screens through the extract-then-render split, so the drawing goes in
 * {@code extractBackground} against a {@link GuiGraphicsExtractor}; there is no {@code GuiGraphics}
 * and no {@code renderBg} any more. Layout and arithmetic are taken from vanilla's own
 * BrewingStandScreen, because the background is a recolour of vanilla's sheet and nothing moved.
 *
 * <p>The fuel gauge is our recoloured sprite. The progress arrow and the bubbles are still
 * vanilla's, drawn straight from the vanilla sprite paths, until recoloured ones exist.
 */
public class ResonanceChamberScreen extends AbstractContainerScreen<ResonanceChamberMenu> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            Constants.MOD_ID, "textures/gui/container/resonance_chamber.png");
    private static final Identifier FUEL_LENGTH = Identifier.fromNamespaceAndPath(
            Constants.MOD_ID, "container/resonance_chamber/fuel_length");
    private static final Identifier BREW_PROGRESS =
            Identifier.withDefaultNamespace("container/brewing_stand/brew_progress");
    private static final Identifier BUBBLES =
            Identifier.withDefaultNamespace("container/brewing_stand/bubbles");

    /** Vanilla's bubble animation lengths, indexed by {@code ticks / 2 % 7}. */
    private static final int[] BUBBLE_LENGTHS = {29, 24, 20, 16, 11, 6, 0};

    public ResonanceChamberScreen(ResonanceChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        extractor.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0.0F, 0.0F,
                imageWidth, imageHeight, 256, 256);

        int fuel = Mth.clamp((18 * menu.fuel() + ResonanceChamberBlockEntity.FUEL_USES - 1)
                / ResonanceChamberBlockEntity.FUEL_USES, 0, 18);
        if (fuel > 0) {
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, FUEL_LENGTH, 18, 4, 0, 0,
                    x + 60, y + 44, fuel, 4);
        }

        int ticks = menu.brewingTicks();
        if (ticks > 0) {
            int arrow = (int) (28.0F * (1.0F - ticks / (float) ResonanceChamberBlockEntity.BREW_TIME));
            if (arrow > 0) {
                extractor.blitSprite(RenderPipelines.GUI_TEXTURED, BREW_PROGRESS, 9, 28, 0, 0,
                        x + 97, y + 16, 9, arrow);
            }
            int bubble = BUBBLE_LENGTHS[ticks / 2 % 7];
            if (bubble > 0) {
                extractor.blitSprite(RenderPipelines.GUI_TEXTURED, BUBBLES, 12, 29, 0, 29 - bubble,
                        x + 63, y + 14 + 29 - bubble, 12, bubble);
            }
        }
    }
}
