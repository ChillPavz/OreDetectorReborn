package com.chillpavz.oredetector.fabric;

import com.chillpavz.oredetector.fabric.config.OreDetectorConfigData;
import com.chillpavz.oredetector.registry.ModCreativeTabs;
import com.chillpavz.oredetector.registry.ModItems;
import com.chillpavz.oredetector.registry.ModSounds;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class OreDetectorFabric implements ModInitializer {

    // Create Fly (ZurrTum) is a Fabric port of Create; it registers under the "create" namespace.
    private static final String[] CREATE_MOD_IDS = {"create", "create-fly", "createfly", "create_fly"};

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

        ModSounds.SOUND_EVENTS.forEach((id, sound) -> Registry.register(BuiltInRegistries.SOUND_EVENT, id, sound));
        ModItems.ITEMS.forEach((id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ModCreativeTabs.KEY, ModCreativeTabs.MAIN);

        boolean createLoaded = isCreateLoaded();
        if (createLoaded) {
            Registry.register(BuiltInRegistries.ITEM, ModItems.ZINC_ID, ModItems.createZinc());
        }

        CreativeModeTabEvents.modifyOutputEvent(ModCreativeTabs.KEY).register(output -> {
            ModItems.ITEMS.values().forEach(item ->
                    output.accept(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            if (createLoaded) {
                output.accept(new ItemStack(ModItems.ZINC_DETECTOR), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
    }

    private static boolean isCreateLoaded() {
        for (String id : CREATE_MOD_IDS) {
            if (FabricLoader.getInstance().isModLoaded(id)) {
                return true;
            }
        }
        return false;
    }
}
