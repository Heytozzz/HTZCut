package com.heytozzz.htzcut.core.audio;

import java.util.Optional;

/**
 * Resolves a user-facing audio id (e.g. "dragon_defeated") into whatever
 * files actually exist for it. This is the single point of truth for
 * "where do assets live" - nothing else in the codebase should build
 * paths to audio files by hand.
 */
public interface AudioAssetResolver {

    /**
     * @param audioId the id declared in an event's audio.id field
     * @return the resolved asset, or empty if it doesn't exist / hasn't
     *         finished processing yet
     */
    Optional<ResolvedAudioAsset> resolve(String audioId);

    /**
     * Forces re-scanning of the audios/ folders and re-triggers
     * transcoding for anything new or changed. Used by the web editor's
     * "recalculate audios" button and by AudioWatcherService.
     */
    void rescan();
}
