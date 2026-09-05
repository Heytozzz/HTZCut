package com.heytozzz.htzcut.neoforge.audio;

import com.heytozzz.htzcut.neoforge.init.HTZLog;
import net.neoforged.fml.ModList;

/**
 * Detects whether Simple Voice Chat is present and usable, without
 * assuming any particular version.
 *
 * SVC's mod version string is prefixed with the Minecraft version it
 * targets (e.g. "1.21.1-2.6.21"), which makes numeric version ranges in
 * neoforge.mods.toml unreliable and version-specific - that's why no
 * versionRange is declared there. Instead, compatibility is verified
 * here at runtime: we only check that the mod is loaded and that the
 * specific API classes/methods we actually call are present, via
 * reflection wrapped in try/catch. If SVC ever changes its API in a
 * future release, this fails closed (falls back to HTTP delivery)
 * instead of crashing the server.
 */
public final class SimpleVoiceChatSupport {

    private static Boolean cachedResult;

    private SimpleVoiceChatSupport() {
    }

    /**
     * @return true if Simple Voice Chat is loaded and its plugin API
     *         entry point can be reached. Cached after first check.
     */
    public static boolean isAvailable() {
        if (cachedResult != null) {
            return cachedResult;
        }

        boolean available = detect();
        cachedResult = available;

        if (available) {
            HTZLog.info("Simple Voice Chat detected - dialogue audio will use it when possible.");
        } else {
            HTZLog.info("Simple Voice Chat not detected (or incompatible) - using HTTP fallback for all dialogue audio.");
        }

        return available;
    }

    private static boolean detect() {
        if (!isModLoaded()) {
            return false;
        }
        return isApiReachable();
    }

    private static boolean isModLoaded() {
        try {
            return ModList.get().isLoaded("voicechat");
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Only confirms the specific API surface HTZCut actually relies on
     * exists - not a full version check. Any future SVC API change that
     * removes/renames these will simply make this return false instead
     * of throwing at a random point later.
     */
    private static boolean isApiReachable() {
        try {
            Class.forName("de.maxhenkel.voicechat.api.BukkitVoicechatService");
            Class.forName("de.maxhenkel.voicechat.api.VoicechatServerApi");
            Class.forName("de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel");
            Class.forName("de.maxhenkel.voicechat.api.opus.OpusEncoder");
            return true;
        } catch (Throwable t) {
            HTZLog.error("Simple Voice Chat is loaded but its expected API classes are missing "
                    + "(likely an incompatible version). Falling back to HTTP delivery.", t);
            return false;
        }
    }
}
