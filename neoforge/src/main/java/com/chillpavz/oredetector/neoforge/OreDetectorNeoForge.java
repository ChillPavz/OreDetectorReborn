package com.chillpavz.oredetector.neoforge;

import com.chillpavz.oredetector.Constants;
import com.chillpavz.oredetector.neoforge.config.OreDetectorConfigData;
import com.chillpavz.oredetector.neoforge.config.OreDetectorConfigScreen;
import com.chillpavz.oredetector.registry.ModCreativeTabs;
import com.chillpavz.oredetector.registry.ModItems;
import com.chillpavz.oredetector.registry.ModSounds;
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
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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
        eventBus.addListener(OreDetectorNeoForge::onBuildTabContents);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            OreDetectorConfigScreen.register(container);
        }
    }

    private static void onRegister(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> ModSounds.SOUND_EVENTS.forEach(helper::register));
        event.register(Registries.ITEM, helper -> {
            ModItems.ITEMS.forEach(helper::register);
            if (isCreateLoaded()) {
                helper.register(ModItems.ZINC_ID, ModItems.createZinc());
            }
        });
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModCreativeTabs.KEY, ModCreativeTabs.MAIN));
    }

    private static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == ModCreativeTabs.KEY) {
            ModItems.ITEMS.values().forEach(event::accept);
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
