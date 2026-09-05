package com.heytozzz.htzcut.neoforge.client;

import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.heytozzz.htzcut.neoforge.network.PlayDialoguePayload;
import com.mojang.blaze3d.audio.OggAudioStream;
import net.minecraft.client.Minecraft;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client-only: downloads (and caches) the dialogue .ogg from HTZCut's
 * embedded HTTP server, then decodes and plays it.
 *
 * Decoding uses Minecraft's own bundled com.mojang.blaze3d.audio.OggAudioStream
 * rather than an external Ogg Vorbis library. This is a deliberate choice:
 * an earlier version embedded the classic javazoom/jcraft vorbisspi+jorbis
 * libraries via Jar-in-Jar, which crashed the game with a Java module
 * system conflict ("reads more than one module named jorbis") whenever
 * another installed mod (e.g. Iris) happened to bundle a same-named
 * "jorbis" jar of its own - the module name collides regardless of which
 * Maven coordinates either mod used, since it's derived from the jar's
 * filename. Using a class already inside the game's own jar can never
 * collide with anything any other mod embeds.
 *
 * Field/method names on Mojang's internal AudioFormat class aren't public
 * API and could rename across versions, so its channel count and sample
 * rate are read via reflection instead of a hard compile-time reference -
 * this fails gracefully (logged, playback skipped) instead of crashing
 * the whole mod if Mojang ever changes that class's shape.
 *
 * Known limitation: playback bypasses Minecraft's volume sliders
 * (master/voice/etc.) for now. Routing dialogue audio through the game's
 * own sound categories is a refinement for a later iteration.
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
        try (InputStream fileIn = Files.newInputStream(file);
             OggAudioStream oggStream = new OggAudioStream(fileIn)) {

            Object mojangFormat = oggStream.getFormat();
            int channels = extractChannels(mojangFormat);
            int sampleRate = extractSampleRate(mojangFormat);

            ByteBuffer pcm = oggStream.readAll();
            byte[] data = new byte[pcm.remaining()];
            pcm.get(data);

            AudioFormat javaFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate,
                    16,
                    channels,
                    channels * 2,
                    sampleRate,
                    false
            );

            SourceDataLine line = AudioSystem.getSourceDataLine(javaFormat);
            line.open(javaFormat);
            line.start();
            line.write(data, 0, data.length);
            line.drain();
            line.close();
        }
    }

    private static int extractChannels(Object mojangFormat) throws Exception {
        try {
            Method method = mojangFormat.getClass().getMethod("getChannels");
            Object result = method.invoke(mojangFormat);
            if (result instanceof Integer count) {
                return count;
            }
            if (result instanceof Enum<?> channelEnum) {
                return channelEnum.name().toLowerCase().contains("mono") ? 1 : 2;
            }
        } catch (NoSuchMethodException ignored) {
            // Try the alternate accessor name below.
        }
        Method method = mojangFormat.getClass().getMethod("getChannelCount");
        return (int) method.invoke(mojangFormat);
    }

    private static int extractSampleRate(Object mojangFormat) throws Exception {
        Method method = mojangFormat.getClass().getMethod("getSampleRate");
        return (int) method.invoke(mojangFormat);
    }
}
