package com.heytozzz.htzcut.neoforge.webeditor;

import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server for the web editor. Separate from
 * HtzHttpAudioServer (different concern, different port) even though
 * both use the same lightweight JDK HttpServer approach.
 *
 * Endpoints:
 *   GET /                              -> static editor page (bundled)
 *   GET /api/registries/items          -> JSON array of all item ids
 *   GET /api/registries/sounds         -> JSON array of all sound ids
 *   GET /api/registries/advancements   -> JSON array of {id, icon}
 *   GET /assets/{namespace}/{path...}  -> raw model/texture bytes, read
 *                                         straight from the classpath
 *                                         (mod jars still contain their
 *                                         client assets even on a
 *                                         dedicated server - we just
 *                                         don't normally load them into
 *                                         a ResourceManager). Used by
 *                                         Deepslate in the browser to
 *                                         render real 3D item previews
 *                                         without needing a running
 *                                         Minecraft client anywhere.
 *
 * Event CRUD (creating/editing events and uploading dialogue audio from
 * the browser) is a follow-up iteration - this first pass only exposes
 * read-only registry data and static assets, enough to build the
 * item/sound/advancement picker with real 3D previews.
 */
public class WebEditorServer {

    private final MinecraftServer server;
    private final int port;
    private HttpServer httpServer;

    public WebEditorServer(MinecraftServer server, int port) {
        this.server = server;
        this.port = port;
    }

    public void start() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/api/registries/items", this::handleItems);
            httpServer.createContext("/api/registries/sounds", this::handleSounds);
            httpServer.createContext("/api/registries/advancements", this::handleAdvancements);
            httpServer.createContext("/assets/", this::handleAsset);
            httpServer.createContext("/", this::handleStatic);
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
            HTZLog.info("HTZCut web editor listening on port " + port
                    + " - open http://localhost:" + port + " in a browser.");
        } catch (IOException e) {
            HTZLog.error("Failed to start HTZCut web editor on port " + port, e);
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            HTZLog.info("HTZCut web editor stopped.");
        }
    }

    private void handleItems(HttpExchange exchange) throws IOException {
        writeJson(exchange, RegistryApi.items().toString());
    }

    private void handleSounds(HttpExchange exchange) throws IOException {
        writeJson(exchange, RegistryApi.sounds().toString());
    }

    private void handleAdvancements(HttpExchange exchange) throws IOException {
        writeJson(exchange, RegistryApi.advancements(server).toString());
    }

    /**
     * Serves a raw asset (model JSON or texture PNG) straight from
     * whichever mod jar declares it, by classpath lookup - e.g.
     * "/assets/minecraft/models/item/diamond.json" reads the classpath
     * resource "assets/minecraft/models/item/diamond.json".
     */
    private void handleAsset(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath().substring(1); // drop leading '/'

        try (InputStream in = WebEditorServer.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            byte[] data = in.readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", contentTypeFor(path));
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(data);
            }
        } finally {
            exchange.close();
        }
    }

    /**
     * Serves the bundled static editor page from
     * src/main/resources/webeditor/. "/" maps to index.html.
     */
    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }

        String resourcePath = "webeditor" + path;
        try (InputStream in = WebEditorServer.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            byte[] data = in.readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", contentTypeFor(resourcePath));
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(data);
            }
        } finally {
            exchange.close();
        }
    }

    private void writeJson(HttpExchange exchange, String json) throws IOException {
        try {
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(data);
            }
        } finally {
            exchange.close();
        }
    }

    private String contentTypeFor(String path) {
        if (path.endsWith(".json")) {
            return "application/json";
        }
        if (path.endsWith(".png")) {
            return "image/png";
        }
        if (path.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (path.endsWith(".js")) {
            return "application/javascript";
        }
        if (path.endsWith(".css")) {
            return "text/css";
        }
        return "application/octet-stream";
    }
}
