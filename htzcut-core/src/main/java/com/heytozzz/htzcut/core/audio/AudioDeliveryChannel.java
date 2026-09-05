package com.heytozzz.htzcut.core.audio;

import java.util.UUID;

/**
 * A way of getting a dialogue audio to a specific player. There are two
 * implementations expected in the neoforge module:
 *   - one backed by Simple Voice Chat (used when available and the
 *     player has an active voice connection)
 *   - one backed by an internal HTTP server + client-side cache (fallback)
 *
 * UI/HUD sounds never go through this - they use vanilla's sound system
 * directly since they're bundled in the jar.
 */
public interface AudioDeliveryChannel {

    /**
     * @return true if this channel can currently deliver audio to this
     *         specific player (e.g. false if the player has SVC installed
     *         but muted/disconnected)
     */
    boolean isAvailableFor(UUID playerId);

    /**
     * Sends the given resolved dialogue audio to the player. Implementations
     * are responsible for picking the right representation from the
     * ResolvedAudioAsset (opus frames vs http-ready file).
     */
    void deliver(UUID playerId, ResolvedAudioAsset asset);
}
