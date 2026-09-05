package com.heytozzz.htzcut.core.audio;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Everything the delivery layer needs to know about an audio, already
 * resolved. Callers never build file paths themselves - they always go
 * through AudioAssetResolver to get one of these.
 *
 * @param audioId       user-facing id, matches EventDefinition.AudioConfig#getId()
 * @param category      dialogue or ui
 * @param sourceFile    the original .ogg uploaded/placed by the user (source of truth)
 * @param opusFramesFile cached SVC-ready representation, present only for
 *                       DIALOGUE audios once transcoding has completed
 * @param httpReadyFile cached HTTP-servable representation (fallback path),
 *                       present only for DIALOGUE audios
 */
public record ResolvedAudioAsset(
        String audioId,
        AudioCategory category,
        Path sourceFile,
        Optional<Path> opusFramesFile,
        Optional<Path> httpReadyFile
) {
}
