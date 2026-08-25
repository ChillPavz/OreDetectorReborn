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
import com.chillpavz.oredetectorreborn.network.ScanVolumePayload;
import com.chillpavz.oredetectorreborn.welcome.WelcomeMessage;
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
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class OreDetectorFabric implements ModInitializer {


    /**
     * Sends the crafting recipes to clients so the guidebook can draw them.
     *
     * <p>Fabric does NOT sync recipes to the client by default; a mod has to opt each serializer
     * in. Patchouli reads its crafting pages out of whatever arrived, and opts nothing in itself,
     * so on Fabric every recipe page in every book renders as an empty space with no title. On
     * NeoForge recipes are synced wholesale, which is why the same book was fine there.
     *
     * <p>The cost is honest: this syncs EVERY shaped and shapeless recipe in the game, not only
     * ours, because the opt-in is per serializer and not per recipe. That is a one-off cost at
     * join and it is the only lever the API offers.
     */
    private static void syncRecipesForTheGuidebook() {
        // Resolved by id: vanilla exposes no public constants for these, only the registry.
        for (String name : new String[]{"crafting_shaped", "crafting_shapeless"}) {
            Identifier id = Identifier.withDefaultNamespace(name);
            RecipeSerializer<?> serializer = BuiltInRegistries.RECIPE_SERIALIZER.getValue(id);
            if (serializer == null) {
                continue;
            }
            try {
                RecipeSynchronization.synchronizeRecipeSerializer(serializer);
            } catch (Throwable failure) {
                // Losing this costs the recipe pages in the book, nothing else.
                com.chillpavz.oredetectorreborn.Constants.LOG.error(
                        "Could not opt {} into recipe sync, so the guidebook's crafting pages "
                        + "will be blank on this loader.", id, failure);
            }
        }
    }

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
        PayloadTypeRegistry.clientboundPlay().register(
                ScanVolumePayload.TYPE, ScanVolumePayload.STREAM_CODEC);
        ModNetworking.setSender(ServerPlayNetworking::send);

        syncRecipesForTheGuidebook();

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> WelcomeMessage.showIfNew(handler.getPlayer()));

        BreezeLootInjection.register();
        // UseBlockCallback, NOT ItemEvents.USE, and the two do NOT share a contract. This one is
        // injected at the HEAD of ServerPlayerGameMode.useItemOn, so it fires even while sneaking
        // (vanilla's own sneak check comes later), and it treats InteractionResult.PASS as "not
        // handled" in the ordinary way. ItemEvents.USE, which grinding used to ride on, instead
        // treats NULL as "not handled" and returns any non-null result INSTEAD of running the
        // item's own use() - which is how returning PASS there once silently broke equipping the
        // goggles, pouring liquid, emptying the bucket and draining, on Fabric only. Both contracts
        // were read out of the mixin bytecode rather than assumed. Returning the wrong sentinel to
        // either one breaks unrelated interactions across the whole game with nothing logged.
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                OreGrindingInteraction.tryOpenGrinder(level, player, hand, hit.getBlockPos()));

        CreativeModeTabEvents.modifyOutputEvent(ModCreativeTabs.KEY).register(output -> {
            ModItems.ITEMS.values().forEach(item ->
                    ModItems.creativeStacks(item).forEach(stack ->
                            output.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS)));
            ModBlocks.BLOCK_ITEMS.values().forEach(item ->
                    output.accept(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
    }
}
