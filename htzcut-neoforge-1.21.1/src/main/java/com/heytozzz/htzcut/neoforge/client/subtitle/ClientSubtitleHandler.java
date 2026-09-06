package com.heytozzz.htzcut.neoforge.client.subtitle;

import com.heytozzz.htzcut.core.subtitle.SubtitlePosition;
import com.heytozzz.htzcut.neoforge.network.ShowSubtitlePayload;

/**
 * Client-only: holds whatever subtitle is currently on screen, if any.
 * SubtitleHudLayer reads this every frame; ShowSubtitlePayload's handler
 * (see NetworkRegistration) writes to it whenever a new subtitle arrives.
 *
 * The countdown runs on client game ticks rather than wall-clock time so
 * it naturally pauses/resumes with the game (e.g. when the client is
 * lagging or the singleplayer world is paused).
 *
 * This class must never be referenced outside a client-only code path -
 * see NetworkRegistration and ClientDialogueHandler for the same rule.
 */
public final class ClientSubtitleHandler {

    private static volatile String text;
    private static volatile SubtitlePosition position = SubtitlePosition.BOTTOM;
    private static volatile int remainingTicks;

    private ClientSubtitleHandler() {
    }

    public static void handle(ShowSubtitlePayload payload) {
        text = payload.text();
        remainingTicks = payload.durationTicks();
        position = parsePosition(payload.position());
    }

    /** Called once per client tick to count the subtitle down. */
    public static void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    public static boolean isActive() {
        return text != null && remainingTicks > 0;
    }

    public static String text() {
        return text;
    }

    public static SubtitlePosition position() {
        return position;
    }

    private static SubtitlePosition parsePosition(String raw) {
        if (raw == null) {
            return SubtitlePosition.BOTTOM;
        }
        try {
            return SubtitlePosition.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SubtitlePosition.BOTTOM;
        }
    }
}
