package com.heytozzz.htzcut.neoforge.client.subtitle;

import com.heytozzz.htzcut.core.subtitle.SubtitleBoxEffect;
import com.heytozzz.htzcut.core.subtitle.SubtitlePosition;
import com.heytozzz.htzcut.core.subtitle.SubtitleTextEffect;
import com.heytozzz.htzcut.neoforge.network.ShowSubtitlePayload;

/**
 * Client-only: holds whatever subtitle is currently on screen, if any,
 * and tracks how many ticks have elapsed since it started. SubtitleHudLayer
 * reads this every frame to know which of the three phases (box appearing,
 * text appearing, holding) it's in and how far along that phase is.
 * ShowSubtitlePayload's handler (see NetworkRegistration) writes to it
 * whenever a new subtitle arrives.
 *
 * Age counts up on client game ticks rather than wall-clock time so it
 * naturally pauses/resumes with the game (e.g. when the client is
 * lagging or the singleplayer world is paused).
 *
 * This class must never be referenced outside a client-only code path -
 * see NetworkRegistration and ClientDialogueHandler for the same rule.
 */
public final class ClientSubtitleHandler {

    /**
     * How long the box's own entrance animation takes, in ticks - fixed
     * for every subtitle rather than configurable per event, since it's
     * meant to read as part of the HUD's "feel" rather than the
     * dialogue's content.
     */
    public static final int BOX_ANIM_TICKS = 6;

    private static volatile String text;
    private static volatile SubtitleBoxEffect boxEffect = SubtitleBoxEffect.INSTANT;
    private static volatile SubtitleTextEffect textEffect = SubtitleTextEffect.INSTANT;
    private static volatile int textDurationTicks;
    private static volatile int holdTicks;
    private static volatile SubtitlePosition position = SubtitlePosition.BOTTOM;
    private static volatile int ageTicks;

    private ClientSubtitleHandler() {
    }

    public static void handle(ShowSubtitlePayload payload) {
        text = payload.text();
        boxEffect = parse(SubtitleBoxEffect.class, payload.boxEffect(), SubtitleBoxEffect.INSTANT);
        textEffect = parse(SubtitleTextEffect.class, payload.textEffect(), SubtitleTextEffect.INSTANT);
        textDurationTicks = Math.max(0, payload.textDurationTicks());
        holdTicks = Math.max(0, payload.holdTicks());
        position = parse(SubtitlePosition.class, payload.position(), SubtitlePosition.BOTTOM);
        ageTicks = 0;
    }

    /** Called once per client tick to advance the subtitle's age. */
    public static void tick() {
        if (isActive()) {
            ageTicks++;
        }
    }

    public static boolean isActive() {
        return text != null && ageTicks < totalTicks();
    }

    public static String text() {
        return text;
    }

    public static SubtitleBoxEffect boxEffect() {
        return boxEffect;
    }

    public static SubtitleTextEffect textEffect() {
        return textEffect;
    }

    public static SubtitlePosition position() {
        return position;
    }

    private static int totalTicks() {
        return BOX_ANIM_TICKS + textDurationTicks + holdTicks;
    }

    /** 0..1 progress of the box's own entrance animation. */
    public static float boxProgress() {
        if (BOX_ANIM_TICKS <= 0) {
            return 1f;
        }
        return clamp01(ageTicks / (float) BOX_ANIM_TICKS);
    }

    /** True once the box has finished appearing and the text phase has begun. */
    public static boolean isTextPhaseStarted() {
        return ageTicks >= BOX_ANIM_TICKS;
    }

    /** 0..1 progress of the text reveal, once the text phase has started. */
    public static float textProgress() {
        if (textDurationTicks <= 0) {
            return 1f;
        }
        int textAge = ageTicks - BOX_ANIM_TICKS;
        return clamp01(textAge / (float) textDurationTicks);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static <T extends Enum<T>> T parse(Class<T> type, String raw, T fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
