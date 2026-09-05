package com.heytozzz.htzcut.neoforge.audio;

import com.heytozzz.htzcut.core.audio.AudioCategory;
import com.heytozzz.htzcut.neoforge.init.HTZLog;

import java.nio.file.Path;

/**
 * Watches audios/dialogues/ and audios/ui/ for new or changed .ogg files
 * and automatically triggers the appropriate processing:
 *   - DIALOGUE files -> transcode to opus frames (SVC) + copy to the
 *     HTTP-servable cache
 *   - UI files -> nothing to transcode, they're bundled and played via
 *     vanilla's sound system, but we still validate the file is a
 *     well-formed .ogg
 *
 * The user only ever interacts with the source .ogg files (via the web
 * editor or by dropping them in the folder); everything below this class
 * is invisible cache management.
 *
 * Full watch-service + transcoding pipeline implementation is scheduled
 * for the audio-delivery implementation stage - this is the skeleton
 * wiring only.
 */
public class AudioWatcherService {

    private final Path dialoguesDir;
    private final Path uiDir;
    private final Path cacheDir;

    public AudioWatcherService(Path dialoguesDir, Path uiDir, Path cacheDir) {
        this.dialoguesDir = dialoguesDir;
        this.uiDir = uiDir;
        this.cacheDir = cacheDir;
    }

    public void start() {
        HTZLog.info("AudioWatcherService starting (dialogues=" + dialoguesDir
                + ", ui=" + uiDir + ", cache=" + cacheDir + ")");
        // TODO: java.nio.file.WatchService on both directories, calling
        // onFileChanged(...) below for every create/modify event.
    }

    private void onFileChanged(Path file, AudioCategory category) {
        switch (category) {
            case DIALOGUE -> HTZLog.info("Dialogue audio changed, scheduling transcode: " + file);
            case UI -> HTZLog.info("UI audio changed, validating: " + file);
        }
        // TODO: dispatch to AudioTranscoder (not yet implemented).
    }
}
