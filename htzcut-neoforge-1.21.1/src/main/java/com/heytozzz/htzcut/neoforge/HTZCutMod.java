package com.heytozzz.htzcut.neoforge;

import com.heytozzz.htzcut.core.audio.AudioDeliveryRouter;
import com.heytozzz.htzcut.core.audio.FileAudioAssetResolver;
import com.heytozzz.htzcut.core.config.EventDefinition;
import com.heytozzz.htzcut.core.event.EventDispatcher;
import com.heytozzz.htzcut.core.permission.PermissionChecker;
import com.heytozzz.htzcut.neoforge.audio.HttpCacheDeliveryChannel;
import com.heytozzz.htzcut.neoforge.audio.HtzHttpAudioServer;
import com.heytozzz.htzcut.neoforge.audio.SimpleVoiceChatDeliveryChannel;
import com.heytozzz.htzcut.neoforge.config.EventFileManager;
import com.heytozzz.htzcut.neoforge.config.HttpServerConfig;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.heytozzz.htzcut.neoforge.init.HTZRuntime;
import com.heytozzz.htzcut.neoforge.narration.ChatNarrationSink;
import com.heytozzz.htzcut.neoforge.network.NetworkRegistration;
import com.heytozzz.htzcut.neoforge.permission.PermissionCheckerFactory;
import com.heytozzz.htzcut.neoforge.sound.VanillaSoundSink;
import com.heytozzz.htzcut.neoforge.trigger.GameTriggerListeners;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Mod(HTZCutMod.MOD_ID)
public class HTZCutMod {

    public static final String MOD_ID = "htzcut";

    private HtzHttpAudioServer httpAudioServer;

    public HTZCutMod(IEventBus modEventBus) {
        HTZLog.info("Initializing HTZCut...");

        modEventBus.addListener(NetworkRegistration::register);

        // Game event listeners can be registered immediately - they just
        // no-op via HTZRuntime.get() == null until the dispatcher below
        // is built, which always happens before any player can connect.
        NeoForge.EVENT_BUS.register(new GameTriggerListeners());
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void onServerStarting(ServerStartingEvent event) {
        PermissionChecker permissionChecker = PermissionCheckerFactory.detect();
        HTZLog.info("Permission backend in use: " + permissionChecker.backendName());

        Path dialoguesDir = FMLPaths.CONFIGDIR.get().resolve("htzcut").resolve("audios").resolve("dialogues");
        try {
            Files.createDirectories(dialoguesDir);
        } catch (IOException e) {
            HTZLog.error("Failed to create dialogues audio directory", e);
        }

        FileAudioAssetResolver assetResolver = new FileAudioAssetResolver(dialoguesDir);

        HttpServerConfig httpConfig = HttpServerConfig.loadOrCreate();
        httpAudioServer = new HtzHttpAudioServer(assetResolver, httpConfig.port());
        httpAudioServer.start();

        AudioDeliveryRouter audioRouter = new AudioDeliveryRouter(
                List.of(
                        new SimpleVoiceChatDeliveryChannel(),
                        new HttpCacheDeliveryChannel(event.getServer(), httpConfig)
                ),
                assetResolver
        );

        ChatNarrationSink narrationSink = new ChatNarrationSink(event.getServer());
        VanillaSoundSink soundSink = new VanillaSoundSink(event.getServer());

        List<EventDefinition> definitions = EventFileManager.loadOrInitialize();
        HTZLog.info("Loaded " + definitions.size() + " event definition(s).");
        HTZLog.info("Drop dialogue .ogg files into " + dialoguesDir + " to make them playable.");

        EventDispatcher dispatcher = new EventDispatcher(
                definitions, permissionChecker, audioRouter, narrationSink, soundSink);
        HTZRuntime.set(dispatcher);
    }

    private void onServerStopping(ServerStoppingEvent event) {
        if (httpAudioServer != null) {
            httpAudioServer.stop();
        }
    }
}
