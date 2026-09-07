package com.heytozzz.htzcut.neoforge.audio;

import com.heytozzz.htzcut.neoforge.HTZCutMod;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

/**
 * HTZCut's Simple Voice Chat plugin entry point. Discovered automatically
 * by SVC on NeoForge/Forge via the @ForgeVoicechatPlugin annotation - no
 * manual registration call needed on our side.
 *
 * This class is only ever touched by SVC's own plugin-loading code, which
 * only runs if SVC itself is installed and loaded - if it's absent,
 * nothing references this class and it's never classloaded, same
 * lazy-loading safety principle used everywhere else this project talks
 * to a soft dependency.
 */
@ForgeVoicechatPlugin
public class HTZVoicechatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return HTZCutMod.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        HTZLog.info("Simple Voice Chat plugin initialized.");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        HTZVoicechatState.set(event.getVoicechat());
        HTZLog.info("Simple Voice Chat server API ready - dialogue audio can now use it.");
    }
}
