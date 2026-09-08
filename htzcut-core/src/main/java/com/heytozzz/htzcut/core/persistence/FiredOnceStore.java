package com.heytozzz.htzcut.core.persistence;

import java.util.UUID;

/**
 * Tracks which (player, event) pairs have already fired for
 * once_per_player conditions, persisted across server restarts and
 * /htzcut reload - reload only rebuilds the event definitions and
 * dispatcher wiring, it must never reset which players already
 * completed a once-only event.
 */
public interface FiredOnceStore {

    boolean hasFired(UUID playerId, String eventId);

    /** No-ops if this pair was already marked - callers don't need to check first. */
    void markFired(UUID playerId, String eventId);
}
