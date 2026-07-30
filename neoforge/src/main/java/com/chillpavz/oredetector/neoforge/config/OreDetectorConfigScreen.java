package com.chillpavz.oredetector.neoforge.config;

import me.shedaniel.autoconfig.AutoConfigClient;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only: wires the cloth-config screen into NeoForge's mod-list "Config" button. Kept in a
 * separate class so its client-only references are never loaded on a dedicated server.
 */
public final class OreDetectorConfigScreen {

    private OreDetectorConfigScreen() {
    }

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> AutoConfigClient.getConfigScreen(OreDetectorConfigData.class, parent).get());
    }
}
