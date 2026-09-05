package com.heytozzz.htzcut.neoforge.config;

import com.heytozzz.htzcut.core.config.EventConfigLoader;
import com.heytozzz.htzcut.core.config.EventDefinition;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Manages the user-editable copy of event definitions under
 * config/htzcut/events/. On first run (empty directory), the bundled
 * defaults from src/main/resources/events/ are copied there so users
 * never have to edit anything inside the jar - and so the web editor
 * (once built) has a normal, writable folder to work with.
 */
public final class EventFileManager {

    // Add new bundled default file names here as they're added under
    // src/main/resources/events/.
    private static final String[] DEFAULT_EVENT_FILES = {
            "example_dragon_defeated.yaml"
    };

    private EventFileManager() {
    }

    public static List<EventDefinition> loadOrInitialize() {
        Path eventsDir = FMLPaths.CONFIGDIR.get().resolve("htzcut").resolve("events");

        try {
            Files.createDirectories(eventsDir);
            copyDefaultsIfEmpty(eventsDir);
            return new EventConfigLoader().loadAll(eventsDir);
        } catch (IOException e) {
            HTZLog.error("Failed to load event definitions from " + eventsDir, e);
            return List.of();
        }
    }

    private static void copyDefaultsIfEmpty(Path eventsDir) throws IOException {
        boolean isEmpty;
        try (var stream = Files.list(eventsDir)) {
            isEmpty = stream.findAny().isEmpty();
        }

        if (!isEmpty) {
            return;
        }

        for (String fileName : DEFAULT_EVENT_FILES) {
            Path target = eventsDir.resolve(fileName);
            try (InputStream in = EventFileManager.class.getResourceAsStream("/events/" + fileName)) {
                if (in == null) {
                    HTZLog.warn("Bundled default event resource not found: " + fileName);
                    continue;
                }
                Files.copy(in, target);
                HTZLog.info("Copied default event definition to " + target);
            }
        }
    }
}
