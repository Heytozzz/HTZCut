package com.heytozzz.htzcut.neoforge.webeditor;

import com.heytozzz.htzcut.neoforge.init.HTZLog;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Reads config/htzcut/webeditor.properties (port only for now), creating
 * it with a default on first run - same pattern as HttpServerConfig for
 * the audio server.
 */
public final class WebEditorConfig {

    private static final int DEFAULT_PORT = 25600;

    private final int port;

    private WebEditorConfig(int port) {
        this.port = port;
    }

    public int port() {
        return port;
    }

    public static WebEditorConfig loadOrCreate() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("htzcut").resolve("webeditor.properties");
        Properties props = new Properties();

        try {
            Files.createDirectories(file.getParent());

            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    props.load(in);
                }
            } else {
                props.setProperty("port", String.valueOf(DEFAULT_PORT));
                try (OutputStream out = Files.newOutputStream(file)) {
                    props.store(out, "HTZCut web editor port. Open http://<server-address>:<port> in a browser.");
                }
                HTZLog.info("Created default web editor config at " + file);
            }
        } catch (IOException e) {
            HTZLog.error("Failed to load/create webeditor.properties, using defaults", e);
        }

        int port = DEFAULT_PORT;
        try {
            port = Integer.parseInt(props.getProperty("port", String.valueOf(DEFAULT_PORT)));
        } catch (NumberFormatException e) {
            HTZLog.warn("Invalid port in webeditor.properties, using default " + DEFAULT_PORT);
        }

        return new WebEditorConfig(port);
    }
}
