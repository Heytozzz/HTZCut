package com.heytozzz.htzcut.neoforge.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Path;

/**
 * Decodes an .ogg file to raw 48kHz, 16-bit, mono PCM - the exact format
 * Simple Voice Chat's AudioPlayer API expects (see
 * SimpleVoiceChatDeliveryChannel). Uses javax.sound.sampled, backed by
 * vorbisspi (bundled via the htzcut-audio-libs subproject, with its
 * packages relocated to avoid the module-name collisions documented
 * there) - the same decoding technique ClientDialogueHandler uses for
 * client-side playback, just producing raw samples here instead of
 * writing them to a speaker.
 *
 * This runs server-side, once per delivery (no caching yet) - fine for
 * short dialogue lines; revisit if this ever shows up as a hotspot for
 * very long audio files or very frequent triggers.
 */
public final class AudioDecoder {

    private static final float TARGET_SAMPLE_RATE = 48000f;

    private AudioDecoder() {
    }

    public static short[] decodeToMonoPcm48k(Path oggFile) throws IOException {
        try (AudioInputStream rawStream = AudioSystem.getAudioInputStream(oggFile.toFile())) {
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    TARGET_SAMPLE_RATE,
                    16,
                    1, // mono - required by SVC's AudioPlayer
                    2,
                    TARGET_SAMPLE_RATE,
                    false // little-endian
            );

            try (AudioInputStream decodedStream = AudioSystem.getAudioInputStream(targetFormat, rawStream)) {
                byte[] bytes = decodedStream.readAllBytes();

                ShortBuffer shortBuffer = ByteBuffer.wrap(bytes)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer();

                short[] samples = new short[shortBuffer.remaining()];
                shortBuffer.get(samples);
                return samples;
            }
        } catch (Exception e) {
            throw new IOException("Failed to decode " + oggFile + " for Simple Voice Chat playback", e);
        }
    }
}
