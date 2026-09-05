package com.heytozzz.htzcut.neoforge.audio;

import com.heytozzz.htzcut.core.audio.AudioAssetResolver;
import com.heytozzz.htzcut.core.audio.ResolvedAudioAsset;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Minimal embedded HTTP server that serves dialogue audio files to
 * clients - the fallback delivery path when Simple Voice Chat isn't
 * available. Uses the JDK's built-in com.sun.net.httpserver instead of
 * an external library, avoiding another Jar-in-Jar dependency for
 * something this simple.
 *
 * Endpoint: GET /audio/{audioId} -> raw .ogg bytes, or 404 if unknown.
 */
public class HtzHttpAudioServer {

    private final AudioAssetResolver assetResolver;
    private final int port;
    private HttpServer server;

    public HtzHttpAudioServer(AudioAssetResolver assetResolver, int port) {
        this.assetResolver = assetResolver;
        this.port = port;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/audio/", this::handle);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            HTZLog.info("HTZCut HTTP audio server listening on port " + port);
        } catch (IOException e) {
            HTZLog.error("Failed to start HTZCut HTTP audio server on port " + port, e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            HTZLog.info("HTZCut HTTP audio server stopped.");
        }
    }

    private void handle(HttpExchange exchange) {
        try {
            String path = exchange.getRequestURI().getPath();
            String audioId = path.substring(path.lastIndexOf('/') + 1);

            Optional<ResolvedAudioAsset> asset = assetResolver.resolve(audioId);
            if (asset.isEmpty() || asset.get().httpReadyFile().isEmpty()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            byte[] data = Files.readAllBytes(asset.get().httpReadyFile().get());
            exchange.getResponseHeaders().add("Content-Type", "audio/ogg");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(data);
            }
        } catch (IOException e) {
            HTZLog.error("Error serving audio request", e);
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (IOException ignored) {
                // Best-effort error response; nothing more we can do here.
            }
        } finally {
            exchange.close();
        }
    }
}
