package com.heytozzz.htzcut.neoforge.config;

import com.heytozzz.htzcut.neoforge.init.HTZLog;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Reads config/htzcut/http.properties, creating it with sane defaults on
 * first run. "public_host" has to be set manually by the server admin
 * for dedicated servers: HTZCut has no reliable way to detect its own
 * externally-reachable address/port (NAT, port forwarding, reverse
 * proxies), so it doesn't try to guess beyond "localhost", which only
 * works for singleplayer or direct LAN connections.
 */
public final class HttpServerConfig {

    private static final String DEFAULT_PUBLIC_HOST = "localhost";
    private static final int DEFAULT_PORT = 25599;

    private final String publicHost;
    private final int port;

    private HttpServerConfig(String publicHost, int port) {
        this.publicHost = publicHost;
        this.port = port;
    }

    public String publicHost() {
        return publicHost;
    }

    public int port() {
        return port;
    }

    public static HttpServerConfig loadOrCreate() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("htzcut").resolve("http.properties");
        Properties props = new Properties();

        try {
            Files.createDirectories(file.getParent());

            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    props.load(in);
                }
            } else {
                props.setProperty("public_host", DEFAULT_PUBLIC_HOST);
                props.setProperty("port", String.valueOf(DEFAULT_PORT));
                try (OutputStream out = Files.newOutputStream(file)) {
                    props.store(out,
                            "HTZCut embedded HTTP audio server config.\n"
                                    + "public_host MUST be set to this server's real, externally reachable\n"
                                    + "address for dedicated servers behind NAT/port forwarding - HTZCut\n"
                                    + "cannot detect this automatically. \"localhost\" only works for\n"
                                    + "singleplayer or direct LAN connections.");
                }
                HTZLog.info("Created default HTTP audio server config at " + file);
            }
        } catch (IOException e) {
            HTZLog.error("Failed to load/create http.properties, using defaults", e);
        }

        String host = props.getProperty("public_host", DEFAULT_PUBLIC_HOST);
        int port = DEFAULT_PORT;
        try {
            port = Integer.parseInt(props.getProperty("port", String.valueOf(DEFAULT_PORT)));
        } catch (NumberFormatException e) {
            HTZLog.warn("Invalid port in http.properties, using default " + DEFAULT_PORT);
        }

        return new HttpServerConfig(host, port);
    }
}
