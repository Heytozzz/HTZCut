package com.heytozzz.htzcut.neoforge.webeditor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import java.nio.file.Path;
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
 *   GET /api/icon/item/{namespace}/{path}    -> resolved item texture PNG
 *   GET /assets/{namespace}/{path...}        -> raw model/texture bytes
 *   GET    /api/events                       -> list of event summaries
 *   GET    /api/events/{filename}            -> full event JSON
 *   POST   /api/events/{filename}            -> create/overwrite event
 *                                                (body: event JSON)
 *   DELETE /api/events/{filename}            -> delete event
 *   POST   /api/events/{filename}/clone      -> body: {"target":"new.yaml"}
 *
 * Registry listings are paginated and filtered server-side on purpose:
 * some modpacks register several thousand items, and shipping that
 * whole list to the browser on every keystroke is exactly the kind of
 * unnecessary load this API is meant to avoid.
 *
 * Note: saving/deleting/cloning events here does NOT automatically
 * reload the live dispatcher - that still requires /htzcut reload (or a
 * "Reload" button in the editor wired to the same command) so a
 * half-finished edit in the browser can never affect the running game
 * before the user explicitly applies it.
 */
public class WebEditorServer {

    private static final int DEFAULT_PAGE_SIZE = 96;

    private final MinecraftServer server;
    private final int port;
    private final EventEditorApi eventEditorApi;
    private HttpServer httpServer;

    public WebEditorServer(MinecraftServer server, int port, Path eventsDir) {
        this.server = server;
        this.port = port;
        this.eventEditorApi = new EventEditorApi(eventsDir);
    }

    public void start() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/api/registries/items", this::handleItems);
            httpServer.createContext("/api/registries/sounds", this::handleSounds);
            httpServer.createContext("/api/registries/advancements", this::handleAdvancements);
            httpServer.createContext("/api/icon/item/", this::handleItemIcon);
            httpServer.createContext("/api/events", this::handleEvents);
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
        writeJson(exchange, 200, RegistryApi.items(
                query.get("q"), pageOf(query), sizeOf(query)).toString());
    }

    private void handleSounds(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange);
        writeJson(exchange, 200, RegistryApi.sounds(
                query.get("q"), pageOf(query), sizeOf(query)).toString());
    }

    private void handleAdvancements(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange);
        writeJson(exchange, 200, RegistryApi.advancements(
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
     * Routes every /api/events... request by method + remaining path
     * segments, since the JDK's HttpServer has no built-in per-verb
     * routing:
     *   GET  /api/events                  -> list()
     *   GET  /api/events/{file}           -> get(file)
     *   POST /api/events/{file}           -> save(file, body)
     *   POST /api/events/{file}/clone     -> clone(file, body.target)
     *   DELETE /api/events/{file}         -> delete(file)
     */
    private void handleEvents(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String remainder = path.length() > "/api/events".length()
                    ? path.substring("/api/events".length() + 1)
                    : "";
            String method = exchange.getRequestMethod();

            if (remainder.isEmpty()) {
                if ("GET".equals(method)) {
                    writeJson(exchange, 200, eventEditorApi.list().toString());
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
                return;
            }

            if (remainder.endsWith("/clone")) {
                String filename = remainder.substring(0, remainder.length() - "/clone".length());
                if (!"POST".equals(method)) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                handleCloneEvent(exchange, filename);
                return;
            }

            String filename = remainder;
            if (!eventEditorApi.isValidFilename(filename)) {
                writeJson(exchange, 400, errorJson("Invalid filename."));
                return;
            }

            switch (method) {
                case "GET" -> {
                    JsonObject result = eventEditorApi.get(filename);
                    if (result == null) {
                        exchange.sendResponseHeaders(404, -1);
                    } else {
                        writeJson(exchange, 200, result.toString());
                    }
                }
                case "POST" -> {
                    String body = readBody(exchange);
                    try {
                        eventEditorApi.save(filename, body);
                        writeJson(exchange, 200, "{\"ok\":true}");
                    } catch (IllegalArgumentException e) {
                        writeJson(exchange, 400, errorJson(e.getMessage()));
                    }
                }
                case "DELETE" -> {
                    boolean deleted = eventEditorApi.delete(filename);
                    if (deleted) {
                        writeJson(exchange, 200, "{\"ok\":true}");
                    } else {
                        exchange.sendResponseHeaders(404, -1);
                    }
                }
                default -> exchange.sendResponseHeaders(405, -1);
            }
        } finally {
            exchange.close();
        }
    }

    private void handleCloneEvent(HttpExchange exchange, String sourceFilename) throws IOException {
        if (!eventEditorApi.isValidFilename(sourceFilename)) {
            writeJson(exchange, 400, errorJson("Invalid source filename."));
            return;
        }

        String body = readBody(exchange);
        JsonObject requestJson;
        try {
            requestJson = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            writeJson(exchange, 400, errorJson("Expected JSON body with a \"target\" filename."));
            return;
        }

        String target = requestJson.has("target") ? requestJson.get("target").getAsString() : null;
        if (target == null || !eventEditorApi.isValidFilename(target)) {
            writeJson(exchange, 400, errorJson("Invalid or missing target filename."));
            return;
        }

        boolean cloned = eventEditorApi.clone(sourceFilename, target);
        if (cloned) {
            writeJson(exchange, 200, "{\"ok\":true}");
        } else {
            exchange.sendResponseHeaders(404, -1);
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

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String errorJson(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", message);
        return obj.toString();
    }

    private void writeJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
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
