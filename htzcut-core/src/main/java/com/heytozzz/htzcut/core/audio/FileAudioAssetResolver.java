package com.heytozzz.htzcut.core.audio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves dialogue audio ids against a directory of plain .ogg files.
 * No transcoding happens yet - the same .ogg file is used directly as
 * the HTTP-servable representation, which is all that's needed for the
 * HTTP delivery channel. Opus frames for Simple Voice Chat are left
 * empty until that pipeline stage is built; SVC delivery simply stays
 * unavailable until then, and playback falls through to HTTP
 * automatically via AudioDeliveryRouter.
 *
 * Kept in core (pure java.nio, no Minecraft imports) since resolving a
 * file by name has nothing loader-specific about it.
 */
public class FileAudioAssetResolver implements AudioAssetResolver {

    private final Path dialoguesDir;

    public FileAudioAssetResolver(Path dialoguesDir) {
        this.dialoguesDir = dialoguesDir;
    }

    @Override
    public Optional<ResolvedAudioAsset> resolve(String audioId) {
        if (audioId == null || audioId.isBlank()) {
            return Optional.empty();
        }

        String fileName = audioId.endsWith(".ogg") ? audioId : audioId + ".ogg";
        Path file = dialoguesDir.resolve(fileName);

        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }

        String canonicalId = fileName.substring(0, fileName.length() - ".ogg".length());

        return Optional.of(new ResolvedAudioAsset(
                canonicalId,
                AudioCategory.DIALOGUE,
                file,
                Optional.empty(), // opus frames: not implemented yet (SVC stage)
                Optional.of(file) // the raw .ogg is directly servable over HTTP as-is
        ));
    }

    @Override
    public void rescan() {
        // Lookup is already stateless/live (checked on every resolve()) -
        // nothing to refresh yet. Once a real AudioWatcherService with
        // caching/transcoding is built, this will trigger it explicitly.
    }
}
