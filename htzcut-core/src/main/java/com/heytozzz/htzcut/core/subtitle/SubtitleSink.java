package com.heytozzz.htzcut.core.subtitle;

import java.util.UUID;

/**
 * Shows a dialogue subtitle box to a specific player for a fixed amount
 * of time. Deliberately independent from AudioDeliveryChannel/Router:
 * the subtitle is a HUD concern that should appear regardless of which
 * audio channel (Simple Voice Chat, HTTP fallback, or none at all) ends
 * up delivering the dialogue's audio.
 */
public interface SubtitleSink {

    /**
     * @param playerId        player to show the subtitle to
     * @param text            raw text to display (already resolved -
     *                        no translation key lookup happens here)
     * @param durationSeconds how long the box should stay on screen
     * @param position        where to anchor the box on screen
     */
    void showSubtitle(UUID playerId, String text, double durationSeconds, SubtitlePosition position);
}
