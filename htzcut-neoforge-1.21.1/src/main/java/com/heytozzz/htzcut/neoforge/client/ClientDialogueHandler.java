package com.heytozzz.htzcut.neoforge.client;

import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.heytozzz.htzcut.neoforge.network.PlayDialoguePayload;
import net.minecraft.client.Minecraft;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client-only: downloads (and caches) the dialogue .ogg from HTZCut's
 * embedded HTTP server, then decodes and plays it via
 * javax.sound.sampled - through the vorbisspi Jar-in-Jar dependency,
 * which registers Ogg Vorbis support with the Java Sound API - rather
 * than through Minecraft's own registered-SoundEvent pipeline, since
 * this audio isn't known at compile time.
 *
 * This class must never be referenced outside of a client-only code
 * path (see NetworkRegistration) - it touches Minecraft's client
 * classes and would fail to classload on a dedicated server.
 *
 * Known limitation: playback bypasses Minecraft's volume sliders
 * (master/voice/etc.) for now. Routing dialogue audio through the
 * game's own sound categories is a refinement for a later iteration.
 */
public final class ClientDialogueHandler {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "HTZCut-Dialogue-Audio");
        thread.setDaemon(true);
        return thread;
    });

    private ClientDialogueHandler() {
    }

    public static void handle(PlayDialoguePayload payload) {
        EXECUTOR.submit(() -> {
            try {
                Path cacheFile = resolveCacheFile(payload.audioId());
                if (!Files.exists(cacheFile)) {
                    download(payload.url(), cacheFile);
                }
                play(cacheFile);
            } catch (Exception e) {
                HTZLog.error("Failed to download/play dialogue audio '" + payload.audioId() + "'", e);
            }
        });
    }

    private static Path resolveCacheFile(String audioId) throws IOException {
        Path cacheDir = Minecraft.getInstance().gameDirectory.toPath().resolve("htzcut_cache");
        Files.createDirectories(cacheDir);
        return cacheDir.resolve(audioId + ".ogg");
    }

    private static void download(String url, Path target) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(15000);

        Path tempFile = target.resolveSibling(target.getFileName() + ".part");
        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
        }
        Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void play(Path file) throws Exception {
        try (AudioInputStream rawStream = AudioSystem.getAudioInputStream(file.toFile())) {
            AudioFormat baseFormat = rawStream.getFormat();
            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );

            try (AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, rawStream)) {
                SourceDataLine line = AudioSystem.getSourceDataLine(decodedFormat);
                line.open(decodedFormat);
                line.start();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = decodedStream.read(buffer, 0, buffer.length)) != -1) {
                    line.write(buffer, 0, bytesRead);
                }

                line.drain();
                line.close();
            }
        }
    }
}
