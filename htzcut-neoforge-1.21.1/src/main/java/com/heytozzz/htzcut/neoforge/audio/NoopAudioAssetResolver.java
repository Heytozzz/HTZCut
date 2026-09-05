package com.heytozzz.htzcut.neoforge.audio;

import com.heytozzz.htzcut.core.audio.AudioAssetResolver;
import com.heytozzz.htzcut.core.audio.ResolvedAudioAsset;

import java.util.Optional;

/**
 * Placeholder resolver used until the real audio transcoding pipeline
 * (AudioWatcherService + transcoder) is implemented. Always reports "not
 * found", which makes AudioDeliveryRouter silently skip audio playback -
 * narration text still fires normally, since that's independent of this.
 */
public class NoopAudioAssetResolver implements AudioAssetResolver {

    @Override
    public Optional<ResolvedAudioAsset> resolve(String audioId) {
        return Optional.empty();
    }

    @Override
    public void rescan() {
        // Nothing to do yet.
    }
}
