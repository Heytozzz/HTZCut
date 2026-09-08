package com.heytozzz.htzcut.neoforge.persistence;

import com.heytozzz.htzcut.core.persistence.FiredOnceStore;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists once_per_player state as one "playerId:eventId" line per
 * entry, appended to a file inside the world save folder - mirrors
 * FirstJoinTracker's location convention (world-scoped, not
 * config-scoped, so progress resets appropriately per save) but appends
 * rather than rewriting the whole file on every write, since this can
 * accumulate far more entries over a server's lifetime than the known
 * players list does.
 *
 * Constructed once per server start (see HTZCutMod) and reused across
 * /htzcut reload calls - reload rebuilds the EventDispatcher and event
 * definitions, but must never lose track of who already completed a
 * once-only event.
 */
public final class FileFiredOnceStore implements FiredOnceStore {

    private final Path storageFile;
    private final Set<String> fired = ConcurrentHashMap.newKeySet();

    public FileFiredOnceStore(MinecraftServer server) {
        this.storageFile = server.getWorldPath(LevelResource.ROOT).resolve("htzcut").resolve("fired_once.txt");
        load();
    }

    private void load() {
        if (!Files.exists(storageFile)) {
            return;
        }
        try {
            fired.addAll(Files.readAllLines(storageFile));
            HTZLog.info("Loaded " + fired.size() + " once_per_player record(s) from " + storageFile);
        } catch (IOException e) {
            HTZLog.error("Failed to read " + storageFile + " - once_per_player state will restart empty", e);
        }
    }

    @Override
    public boolean hasFired(UUID playerId, String eventId) {
        return fired.contains(key(playerId, eventId));
    }

    @Override
    public synchronized void markFired(UUID playerId, String eventId) {
        String key = key(playerId, eventId);
        if (!fired.add(key)) {
            return; // already recorded - avoid duplicate lines on repeated calls
        }
        try {
            Files.createDirectories(storageFile.getParent());
            Files.writeString(storageFile, key + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            HTZLog.error("Failed to persist once_per_player record '" + key + "' - "
                    + "this completion may be forgotten if the server restarts before a later write succeeds", e);
        }
    }

    private String key(UUID playerId, String eventId) {
        return playerId + ":" + eventId;
    }
}
