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
 * there).
 *
 * Runs in two conversion hops rather than one, because vorbisspi's
 * decoder only supports Vorbis -> PCM at the file's own sample
 * rate/channels - it can't resample or downmix in that same call:
 *   1. Vorbis -> native-rate/native-channel PCM (vorbisspi)
 *   2. that PCM -> 48kHz mono PCM (the JDK's built-in PCM-to-PCM
 *      conversion provider, which does support changing rate/channels)
 * ClientDialogueHandler only needs step 1 for its client-side playback
 * (it plays audio at the file's own format instead of resampling), so
 * this is the one place that needs both steps.
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
            // Step 1: Vorbis -> PCM, at the file's own sample rate and
            // channel count. vorbisspi's conversion provider only
            // supports this one hop (encoded -> PCM); it can't also
            // resample or downmix in the same call, which is what the
            // "Unsupported conversion" error further down was about.
            AudioFormat nativePcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    rawStream.getFormat().getSampleRate(),
                    16,
                    rawStream.getFormat().getChannels(),
                    rawStream.getFormat().getChannels() * 2,
                    rawStream.getFormat().getSampleRate(),
                    false // little-endian
            );

            try (AudioInputStream nativePcmStream = AudioSystem.getAudioInputStream(nativePcmFormat, rawStream)) {
                // Step 2: PCM -> PCM, resampling to 48kHz and downmixing
                // to mono. This hop is handled by the JDK's own built-in
                // conversion provider (javax.sound.sampled's default
                // provider), which - unlike vorbisspi - does support
                // changing sample rate and channel count between two PCM
                // formats in a single call.
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        TARGET_SAMPLE_RATE,
                        16,
                        1, // mono - required by SVC's AudioPlayer
                        2,
                        TARGET_SAMPLE_RATE,
                        false // little-endian
                );

                try (AudioInputStream decodedStream = AudioSystem.getAudioInputStream(targetFormat, nativePcmStream)) {
                    byte[] bytes = decodedStream.readAllBytes();

                    ShortBuffer shortBuffer = ByteBuffer.wrap(bytes)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .asShortBuffer();

                    short[] samples = new short[shortBuffer.remaining()];
                    shortBuffer.get(samples);
                    return samples;
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to decode " + oggFile + " for Simple Voice Chat playback", e);
        }
    }
}
