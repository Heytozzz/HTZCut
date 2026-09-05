package com.heytozzz.htzcut.neoforge.trigger;

import com.heytozzz.htzcut.neoforge.init.HTZLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players have connected before, to detect FIRST_JOIN
 * triggers. Persisted as a plain UUID-per-line file inside the world
 * save folder, rather than relying on vanilla stats - those aren't
 * reliably queryable as "never played before" at the exact moment
 * PlayerLoggedInEvent fires.
 */
public final class FirstJoinTracker {

    private static Path storageFile;
    private static Set<String> knownPlayers;

    private FirstJoinTracker() {
    }

    /**
     * @return true if this is the first time this UUID has connected to
     *         this world (and records it as known from now on)
     */
    public static boolean isFirstJoin(MinecraftServer server, UUID playerId) {
        ensureLoaded(server);

        String id = playerId.toString();
        if (knownPlayers.contains(id)) {
            return false;
        }

        knownPlayers.add(id);
        persist();
        return true;
    }

    private static void ensureLoaded(MinecraftServer server) {
        if (knownPlayers != null) {
            return;
        }

        storageFile = server.getWorldPath(LevelResource.ROOT).resolve("htzcut").resolve("known_players.txt");
        knownPlayers = new HashSet<>();

        if (Files.exists(storageFile)) {
            try {
                knownPlayers.addAll(Files.readAllLines(storageFile));
            } catch (IOException e) {
                HTZLog.error("Failed to read known players file, treating all players as first-join", e);
            }
        }
    }

    private static void persist() {
        try {
            Files.createDirectories(storageFile.getParent());
            Files.write(storageFile, knownPlayers);
        } catch (IOException e) {
            HTZLog.error("Failed to persist known players file", e);
        }
    }
}
