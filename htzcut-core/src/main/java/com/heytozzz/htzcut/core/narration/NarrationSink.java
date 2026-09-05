package com.heytozzz.htzcut.core.narration;

import java.util.UUID;

/**
 * Delivers narration text to a player. The neoforge module implements
 * this to send a translatable chat message (and, in a later iteration,
 * subtitles or cinematic UI), keeping core free of any Minecraft
 * text/component classes.
 */
public interface NarrationSink {

    /**
     * @param playerId       who should receive the narration
     * @param textKey        translation key to display
     * @param fallbackLocale locale to fall back to if the player's client
     *                       has no translation for textKey (resolution
     *                       strategy is up to the implementation)
     */
    void sendNarration(UUID playerId, String textKey, String fallbackLocale);
}
