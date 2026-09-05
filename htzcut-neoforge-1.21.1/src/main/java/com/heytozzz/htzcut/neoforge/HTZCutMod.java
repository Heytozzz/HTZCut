package com.heytozzz.htzcut.neoforge;

import com.heytozzz.htzcut.core.audio.AudioDeliveryRouter;
import com.heytozzz.htzcut.core.config.EventDefinition;
import com.heytozzz.htzcut.core.event.EventDispatcher;
import com.heytozzz.htzcut.core.permission.PermissionChecker;
import com.heytozzz.htzcut.neoforge.audio.HttpCacheDeliveryChannel;
import com.heytozzz.htzcut.neoforge.audio.NoopAudioAssetResolver;
import com.heytozzz.htzcut.neoforge.audio.SimpleVoiceChatDeliveryChannel;
import com.heytozzz.htzcut.neoforge.config.EventFileManager;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.heytozzz.htzcut.neoforge.init.HTZRuntime;
import com.heytozzz.htzcut.neoforge.narration.ChatNarrationSink;
import com.heytozzz.htzcut.neoforge.permission.PermissionCheckerFactory;
import com.heytozzz.htzcut.neoforge.trigger.GameTriggerListeners;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.List;

@Mod(HTZCutMod.MOD_ID)
public class HTZCutMod {

    public static final String MOD_ID = "htzcut";

    public HTZCutMod(IEventBus modEventBus) {
        HTZLog.info("Initializing HTZCut...");

        // Game event listeners can be registered immediately - they just
        // no-op via HTZRuntime.get() == null until the dispatcher below
        // is built, which always happens before any player can connect.
        NeoForge.EVENT_BUS.register(new GameTriggerListeners());
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void onServerStarting(ServerStartingEvent event) {
        PermissionChecker permissionChecker = PermissionCheckerFactory.detect();
        HTZLog.info("Permission backend in use: " + permissionChecker.backendName());

        // NoopAudioAssetResolver is a placeholder until the audio
        // transcoding pipeline stage is implemented - dialogue audio is
        // silently skipped for now, narration text still fires normally.
        AudioDeliveryRouter audioRouter = new AudioDeliveryRouter(
                List.of(new SimpleVoiceChatDeliveryChannel(), new HttpCacheDeliveryChannel()),
                new NoopAudioAssetResolver()
        );

        ChatNarrationSink narrationSink = new ChatNarrationSink(event.getServer());

        List<EventDefinition> definitions = EventFileManager.loadOrInitialize();
        HTZLog.info("Loaded " + definitions.size() + " event definition(s).");

        EventDispatcher dispatcher = new EventDispatcher(definitions, permissionChecker, audioRouter, narrationSink);
        HTZRuntime.set(dispatcher);
    }
}
