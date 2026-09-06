package com.heytozzz.htzcut.neoforge.webeditor;

import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server for the web editor. Separate from
 * HtzHttpAudioServer (different concern, different port) even though
 * both use the same lightweight JDK HttpServer approach.
 *
 * Endpoints:
 *   GET /                                    -> static editor page
 *   GET /api/registries/items?q=&page=&size= -> paginated {total, items}
 *   GET /api/registries/sounds?q=&page=&size=-> paginated {total, items}
 *   GET /api/registries/advancements?...     -> paginated {total, items}
 *   GET /api/icon/item/{namespace}/{path}    -> resolved item texture PNG,
 *                                                read via IconResolver
 *                                                (walks the real model
 *                                                JSON instead of guessing
 *                                                a texture path)
 *   GET /assets/{namespace}/{path...}        -> raw model/texture bytes
 *
 * Registry listings are paginated and filtered server-side on purpose:
 * some modpacks register several thousand items, and shipping that
 * whole list to the browser on every keystroke is exactly the kind of
 * unnecessary load this API is meant to avoid.
 *
 * Event CRUD (creating/editing events and uploading dialogue audio from
 * the browser) is a follow-up iteration - this first pass only exposes
 * read-only registry data and assets, enough to build the
 * item/sound/advancement picker with real icons.
 */
public class WebEditorServer {

    private static final int DEFAULT_PAGE_SIZE = 96;

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
            httpServer.createContext("/api/icon/item/", this::handleItemIcon);
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
        Map<String, String> query = parseQuery(exchange);
        writeJson(exchange, RegistryApi.items(
                query.get("q"), pageOf(query), sizeOf(query)).toString());
    }

    private void handleSounds(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange);
        writeJson(exchange, RegistryApi.sounds(
                query.get("q"), pageOf(query), sizeOf(query)).toString());
    }

    private void handleAdvancements(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange);
        writeJson(exchange, RegistryApi.advancements(
                server, query.get("q"), pageOf(query), sizeOf(query)).toString());
    }

    /**
     * "/api/icon/item/{namespace}/{path}" -> resolves the item's real
     * texture via its model JSON (see IconResolver) and streams it back,
     * rather than the browser guessing a texture path itself.
     */
    private void handleItemIcon(HttpExchange exchange) throws IOException {
        try {
            String remainder = exchange.getRequestURI().getPath().substring("/api/icon/item/".length());
            int slash = remainder.indexOf('/');
            if (slash < 0) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            String namespace = remainder.substring(0, slash);
            String itemPath = remainder.substring(slash + 1);

            Optional<String> texturePath = IconResolver.resolveTexturePath(namespace, itemPath);
            if (texturePath.isEmpty()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            streamClasspathResource(exchange, texturePath.get());
        } finally {
            exchange.close();
        }
    }

    /**
     * Serves a raw asset (model JSON or texture PNG) straight from
     * whichever mod jar declares it, by classpath lookup.
     */
    private void handleAsset(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath().substring(1); // drop leading '/'
        try {
            streamClasspathResource(exchange, path);
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
        try {
            streamClasspathResource(exchange, "webeditor" + path);
        } finally {
            exchange.close();
        }
    }

    private void streamClasspathResource(HttpExchange exchange, String classpathResource) throws IOException {
        try (InputStream in = WebEditorServer.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            byte[] data = in.readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", contentTypeFor(classpathResource));
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(data);
            }
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

    private Map<String, String> parseQuery(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            result.put(key, value);
        }
        return result;
    }

    private int pageOf(Map<String, String> query) {
        try {
            return Math.max(0, Integer.parseInt(query.getOrDefault("page", "0")));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int sizeOf(Map<String, String> query) {
        try {
            int size = Integer.parseInt(query.getOrDefault("size", String.valueOf(DEFAULT_PAGE_SIZE)));
            return Math.min(Math.max(size, 1), 500); // clamp to avoid abuse/huge responses
        } catch (NumberFormatException e) {
            return DEFAULT_PAGE_SIZE;
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
