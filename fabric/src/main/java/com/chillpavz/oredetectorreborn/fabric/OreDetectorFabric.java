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
package com.chillpavz.oredetectorreborn.fabric;

import com.chillpavz.oredetectorreborn.fabric.config.OreDetectorConfigData;
import com.chillpavz.oredetectorreborn.fabric.loot.BreezeLootInjection;
import com.chillpavz.oredetectorreborn.item.OreGrindingInteraction;
import com.chillpavz.oredetectorreborn.network.ModNetworking;
import com.chillpavz.oredetectorreborn.network.ScanHighlightPayload;
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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.ItemEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class OreDetectorFabric implements ModInitializer {


    @Override
    public void onInitialize() {
        // Load config first so items pick up the durability multiplier when they are created.
        AutoConfig.register(OreDetectorConfigData.class, GsonConfigSerializer::new);
        ConfigHolder<OreDetectorConfigData> config = AutoConfig.getConfigHolder(OreDetectorConfigData.class);
        config.getConfig().applyToRuntime();
        config.registerSaveListener((holder, data) -> {
            data.applyToRuntime();
            return InteractionResult.SUCCESS;
        });

        ModDataComponents.COMPONENTS.forEach((id, type) ->
                Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, type));
        ModSounds.SOUND_EVENTS.forEach((id, sound) -> Registry.register(BuiltInRegistries.SOUND_EVENT, id, sound));
        ModBlocks.BLOCKS.forEach((id, block) -> Registry.register(BuiltInRegistries.BLOCK, id, block));
        ModItems.ITEMS.forEach((id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        ModBlocks.BLOCK_ITEMS.forEach((id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        ModBlockEntities.BLOCK_ENTITIES.forEach((id, type) ->
                Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type));
        ModMenus.MENUS.forEach((id, type) -> Registry.register(BuiltInRegistries.MENU, id, type));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ModCreativeTabs.KEY, ModCreativeTabs.MAIN);


        // The payload TYPE must be registered on both sides; only the client registers a handler.
        PayloadTypeRegistry.clientboundPlay().register(
                ScanHighlightPayload.TYPE, ScanHighlightPayload.STREAM_CODEC);
        ModNetworking.setSender(ServerPlayNetworking::send);

        BreezeLootInjection.register();
        ItemEvents.USE.register(OreGrindingInteraction::tryGrind);

        CreativeModeTabEvents.modifyOutputEvent(ModCreativeTabs.KEY).register(output -> {
            ModItems.ITEMS.values().forEach(item ->
                    ModItems.creativeStacks(item).forEach(stack ->
                            output.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS)));
            ModBlocks.BLOCK_ITEMS.values().forEach(item ->
                    output.accept(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
    }
}
