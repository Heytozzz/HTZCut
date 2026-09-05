package com.heytozzz.htzcut.neoforge.audio;

import com.heytozzz.htzcut.core.audio.AudioDeliveryChannel;
import com.heytozzz.htzcut.core.audio.ResolvedAudioAsset;

import java.util.UUID;

/**
 * Fallback delivery channel: an embedded HTTP endpoint serves the .ogg
 * file (asset.httpReadyFile()) and the client downloads + caches it
 * locally before playing it through a custom SoundInstance.
 *
 * Always reports itself as available - this is the guaranteed last
 * resort so every player receives dialogue audio regardless of whether
 * they have Simple Voice Chat installed.
 *
 * Full implementation (embedded HTTP server, client-side download +
 * local cache, custom SoundInstance playback) is scheduled for the audio
 * delivery implementation stage.
 */
public class HttpCacheDeliveryChannel implements AudioDeliveryChannel {

    @Override
    public boolean isAvailableFor(UUID playerId) {
        return true;
    }

    @Override
    public void deliver(UUID playerId, ResolvedAudioAsset asset) {
        // TODO: notify client (via a network packet) that audioId is ready
        // to fetch from the embedded HTTP endpoint, client downloads to
        // its local cache if not already present, then plays it.
    }
}
