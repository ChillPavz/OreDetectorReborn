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
package com.chillpavz.oredetectorreborn.neoforge;

import com.chillpavz.oredetectorreborn.Constants;
import com.chillpavz.oredetectorreborn.neoforge.config.OreDetectorConfigData;
import com.chillpavz.oredetectorreborn.neoforge.config.OreDetectorConfigScreen;
import com.chillpavz.oredetectorreborn.item.OreGrindingInteraction;
import com.chillpavz.oredetectorreborn.registry.ModBlockEntities;
import com.chillpavz.oredetectorreborn.registry.ModBlocks;
import com.chillpavz.oredetectorreborn.registry.ModCreativeTabs;
import com.chillpavz.oredetectorreborn.registry.ModMenus;
import com.chillpavz.oredetectorreborn.registry.ModDataComponents;
import com.chillpavz.oredetectorreborn.registry.ModItems;
import com.chillpavz.oredetectorreborn.registry.ModSounds;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class OreDetectorNeoForge {

    private static final String[] CREATE_MOD_IDS = {"create", "create-fly", "createfly", "create_fly"};

    public OreDetectorNeoForge(IEventBus eventBus, ModContainer container) {
        // Load config first so items pick up the durability multiplier when they are created.
        AutoConfig.register(OreDetectorConfigData.class, GsonConfigSerializer::new);
        ConfigHolder<OreDetectorConfigData> config = AutoConfig.getConfigHolder(OreDetectorConfigData.class);
        config.getConfig().applyToRuntime();
        config.registerSaveListener((holder, data) -> {
            data.applyToRuntime();
            return InteractionResult.SUCCESS;
        });

        eventBus.addListener(OreDetectorNeoForge::onRegister);
        // RightClickItem is a GAME bus event, not a mod bus one. Registering it on the
        // wrong bus fails silently.
        NeoForge.EVENT_BUS.addListener(OreDetectorNeoForge::onRightClickItem);
        eventBus.addListener(OreDetectorNeoForge::onBuildTabContents);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            OreDetectorConfigScreen.register(container);
            // RegisterMenuScreensEvent is a MOD bus event, unlike RightClickItem above.
            eventBus.addListener(
                    com.chillpavz.oredetectorreborn.neoforge.client.OreDetectorNeoForgeClient::onRegisterScreens);
        }
    }

    private static void onRegister(RegisterEvent event) {
        event.register(Registries.DATA_COMPONENT_TYPE,
                helper -> ModDataComponents.COMPONENTS.forEach(helper::register));
        event.register(Registries.SOUND_EVENT, helper -> ModSounds.SOUND_EVENTS.forEach(helper::register));
        event.register(Registries.BLOCK, helper -> ModBlocks.BLOCKS.forEach(helper::register));
        event.register(Registries.BLOCK_ENTITY_TYPE,
                helper -> ModBlockEntities.BLOCK_ENTITIES.forEach(helper::register));
        event.register(Registries.MENU, helper -> ModMenus.MENUS.forEach(helper::register));
        event.register(Registries.ITEM, helper -> {
            ModItems.ITEMS.forEach(helper::register);
            ModBlocks.BLOCK_ITEMS.forEach(helper::register);
            if (isCreateLoaded()) {
                helper.register(ModItems.ZINC_ID, ModItems.createZinc());
            }
        });
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModCreativeTabs.KEY, ModCreativeTabs.MAIN));
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        InteractionResult result = OreGrindingInteraction.tryGrind(
                event.getLevel(), event.getEntity(), event.getHand());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == ModCreativeTabs.KEY) {
            ModItems.ITEMS.values().forEach(item -> ModItems.creativeStacks(item).forEach(event::accept));
            ModBlocks.BLOCK_ITEMS.values().forEach(event::accept);
            if (isCreateLoaded()) {
                event.accept(ModItems.ZINC_DETECTOR);
            }
        }
    }

    private static boolean isCreateLoaded() {
        for (String id : CREATE_MOD_IDS) {
            if (net.neoforged.fml.ModList.get().isLoaded(id)) {
                return true;
            }
        }
        return false;
    }
}
