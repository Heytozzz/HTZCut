package com.heytozzz.htzcut.neoforge;

import com.heytozzz.htzcut.core.audio.AudioDeliveryRouter;
import com.heytozzz.htzcut.core.audio.FileAudioAssetResolver;
import com.heytozzz.htzcut.core.config.EventDefinition;
import com.heytozzz.htzcut.core.event.EventDispatcher;
import com.heytozzz.htzcut.core.permission.PermissionChecker;
import com.heytozzz.htzcut.neoforge.audio.HttpCacheDeliveryChannel;
import com.heytozzz.htzcut.neoforge.audio.HtzHttpAudioServer;
import com.heytozzz.htzcut.neoforge.audio.SimpleVoiceChatDeliveryChannel;
import com.heytozzz.htzcut.neoforge.client.HTZClientEvents;
import com.heytozzz.htzcut.neoforge.command.HTZCommand;
import com.heytozzz.htzcut.neoforge.config.EventFileManager;
import com.heytozzz.htzcut.neoforge.config.HttpServerConfig;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.heytozzz.htzcut.neoforge.init.HTZRuntime;
import com.heytozzz.htzcut.neoforge.narration.ChatNarrationSink;
import com.heytozzz.htzcut.neoforge.network.NetworkRegistration;
import com.heytozzz.htzcut.neoforge.permission.PermissionCheckerFactory;
import com.heytozzz.htzcut.neoforge.sound.VanillaSoundSink;
import com.heytozzz.htzcut.neoforge.subtitle.NeoForgeSubtitleSink;
import com.heytozzz.htzcut.neoforge.trigger.GameTriggerListeners;
import com.heytozzz.htzcut.neoforge.webeditor.WebEditorConfig;
import com.heytozzz.htzcut.neoforge.webeditor.WebEditorServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
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
    private WebEditorServer webEditorServer;

    // Kept around so /htzcut reload can rebuild just the event list and
    // dispatcher without tearing down the HTTP server or re-detecting
    // the permission backend every time.
    private PermissionChecker permissionChecker;
    private AudioDeliveryRouter audioRouter;
    private ChatNarrationSink narrationSink;
    private VanillaSoundSink soundSink;
    private NeoForgeSubtitleSink subtitleSink;

    public HTZCutMod(IEventBus modEventBus) {
        HTZLog.info("Initializing HTZCut...");

        modEventBus.addListener(NetworkRegistration::register);

        // Game event listeners can be registered immediately - they just
        // no-op via HTZRuntime.get() == null until the dispatcher below
        // is built, which always happens before any player can connect.
        NeoForge.EVENT_BUS.register(new GameTriggerListeners());
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        // Guarded so the subtitle HUD layer (GuiGraphics et al.) is never
        // classloaded on a dedicated server - see NetworkRegistration for
        // the same rule applied to payload handlers.
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(HTZClientEvents::registerGuiLayers);
            NeoForge.EVENT_BUS.addListener(HTZClientEvents::onClientTick);
        }
    }

    private void onServerStarting(ServerStartingEvent event) {
        permissionChecker = PermissionCheckerFactory.detect();
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

        audioRouter = new AudioDeliveryRouter(
                List.of(
                        new SimpleVoiceChatDeliveryChannel(event.getServer()),
                        new HttpCacheDeliveryChannel(event.getServer(), httpConfig)
                ),
                assetResolver
        );

        narrationSink = new ChatNarrationSink(event.getServer());
        soundSink = new VanillaSoundSink(event.getServer());
        subtitleSink = new NeoForgeSubtitleSink(event.getServer());

        WebEditorConfig webEditorConfig = WebEditorConfig.loadOrCreate();
        Path eventsDir = FMLPaths.CONFIGDIR.get().resolve("htzcut").resolve("events");
        webEditorServer = new WebEditorServer(event.getServer(), webEditorConfig.port(), eventsDir);
        webEditorServer.start();

        HTZLog.info("Drop dialogue .ogg files into " + dialoguesDir + " to make them playable.");

        rebuildDispatcher();
    }

    private void onServerStopping(ServerStoppingEvent event) {
        if (httpAudioServer != null) {
            httpAudioServer.stop();
        }
        if (webEditorServer != null) {
            webEditorServer.stop();
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        HTZCommand.register(event.getDispatcher(), this);
    }

    /**
     * Re-reads every event YAML from config/htzcut/events/ and rebuilds
     * the EventDispatcher, without touching the HTTP server, permission
     * backend, or delivery channels. Called on server start and from
     * /htzcut reload.
     *
     * @return how many event definitions were loaded, for command feedback
     */
    public int rebuildDispatcher() {
        List<EventDefinition> definitions = EventFileManager.loadOrInitialize();
        HTZLog.info("Loaded " + definitions.size() + " event definition(s).");

        EventDispatcher dispatcher = new EventDispatcher(
                definitions, permissionChecker, audioRouter, narrationSink, soundSink, subtitleSink);
        HTZRuntime.set(dispatcher);

        return definitions.size();
    }
}
