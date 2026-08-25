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
import com.chillpavz.oredetectorreborn.menu.GrindMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The grinder's interface: our own recolour of the grindstone panel, with one input rather than
 * two, centred and levelled with the arrow and the output.
 *
 * <p>Because this is our own screen rather than a mixin into vanilla's, the title is simply the
 * menu's own display name, so it reads "Grind" instead of "Repair and Disenchant" with nothing
 * overridden. Vanilla's grindstone keeps its own screen, its own slots and its own title.
 *
 * <p>26.2 draws screens through the extract-then-render split, so the drawing goes in
 * {@code extractBackground} against a {@link GuiGraphicsExtractor}; there is no {@code GuiGraphics}
 * and no {@code renderBg} any more.
 */
public class GrindScreen extends AbstractContainerScreen<GrindMenu> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            Constants.MOD_ID, "textures/gui/container/grinder.png");

    public GrindScreen(GrindMenu menu, Inventory inventory, Component title) {
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
    }
}
