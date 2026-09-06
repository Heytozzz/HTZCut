package com.heytozzz.htzcut.neoforge.client;

import com.heytozzz.htzcut.neoforge.HTZCutMod;
import com.heytozzz.htzcut.neoforge.client.subtitle.ClientSubtitleHandler;
import com.heytozzz.htzcut.neoforge.client.subtitle.SubtitleHudLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-only event hookups. Only ever registered from HTZCutMod behind
 * an FMLEnvironment.dist.isClient() check, so this class - and the
 * client-rendering classes it references - is never touched on a
 * dedicated server.
 */
public final class HTZClientEvents {

    private HTZClientEvents() {
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(HTZCutMod.MOD_ID, "subtitle_box"),
                SubtitleHudLayer::render
        );
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        ClientSubtitleHandler.tick();
    }
}
