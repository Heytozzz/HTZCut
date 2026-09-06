package com.heytozzz.htzcut.core.subtitle;

import java.util.UUID;

/**
 * Shows a dialogue subtitle box to a specific player. Deliberately
 * independent from AudioDeliveryChannel/Router: the subtitle is a HUD
 * concern that should appear regardless of which audio channel (Simple
 * Voice Chat, HTTP fallback, or none at all) ends up delivering the
 * dialogue's audio.
 *
 * Playback has three phases, always in this order:
 *   1. the box appears (boxEffect) - fixed, short duration owned by the
 *      renderer, not configurable per event
 *   2. the text appears (textEffect) over textDurationSeconds - either
 *      an entrance animation of the full line, or a progressive
 *      typewriter reveal
 *   3. everything stays fully visible for holdSeconds, then disappears
 */
public interface SubtitleSink {

    void showSubtitle(UUID playerId,
                       String text,
                       SubtitleBoxEffect boxEffect,
                       SubtitleTextEffect textEffect,
                       double textDurationSeconds,
                       double holdSeconds,
                       SubtitlePosition position);
}
