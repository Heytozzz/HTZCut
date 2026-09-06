package com.heytozzz.htzcut.neoforge.webeditor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.heytozzz.htzcut.core.config.EventConfigLoader;
import com.heytozzz.htzcut.core.config.EventConfigWriter;
import com.heytozzz.htzcut.core.config.EventDefinition;
import com.heytozzz.htzcut.neoforge.init.HTZLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Backing implementation for the web editor's event CRUD endpoints.
 * Reads/writes the exact same files EventFileManager loads on server
 * start and /htzcut reload - the web editor and the config folder are
 * two views of the same source of truth, never a separate copy.
 */
public final class EventEditorApi {

    // Filenames come from the URL path - only allow safe characters and
    // require a .yaml/.yml extension, to rule out path traversal
    // ("../../something") or writing to an unexpected location.
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9_.-]+\\.ya?ml$");

    private final Path eventsDir;
    private final EventConfigLoader loader = new EventConfigLoader();
    private final EventConfigWriter writer = new EventConfigWriter();
    private final Gson gson = new Gson();

    public EventEditorApi(Path eventsDir) {
        this.eventsDir = eventsDir;
    }

    public boolean isValidFilename(String filename) {
        return SAFE_FILENAME.matcher(filename).matches();
    }

    /**
     * @return {filename, id, triggerType, actionCount} for every event
     *         file - enough for the editor's file list, without the
     *         cost of shipping every field of every event up front.
     */
    public JsonArray list() throws IOException {
        JsonArray array = new JsonArray();

        if (!Files.isDirectory(eventsDir)) {
            return array;
        }

        try (Stream<Path> files = Files.list(eventsDir)) {
            List<Path> yamlFiles = files
                    .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                    .sorted()
                    .toList();

            for (Path file : yamlFiles) {
                JsonObject entry = new JsonObject();
                entry.addProperty("filename", file.getFileName().toString());

                try {
                    EventDefinition def = loader.loadOne(file);
                    if (def != null) {
                        entry.addProperty("id", def.getId());
                        if (def.getTrigger() != null && def.getTrigger().getType() != null) {
                            entry.addProperty("triggerType", def.getTrigger().getType().name());
                        }
                        entry.addProperty("actionCount", def.getActions() != null ? def.getActions().size() : 0);
                    }
                } catch (Exception e) {
                    // A file with invalid YAML shouldn't hide every other
                    // file from the list - surface it as a broken entry
                    // the editor can flag, instead of failing the whole
                    // listing request.
                    entry.addProperty("error", e.getMessage());
                    HTZLog.warn("Event file '" + file.getFileName() + "' failed to parse: " + e.getMessage());
                }

                array.add(entry);
            }
        }

        return array;
    }

    /**
     * @return the full event as JSON, or null if the file doesn't exist.
     */
    public JsonObject get(String filename) throws IOException {
        Path file = eventsDir.resolve(filename);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        EventDefinition def = loader.loadOne(file);
        return gson.toJsonTree(def).getAsJsonObject();
    }

    /**
     * Parses the given JSON body as an EventDefinition and writes it to
     * disk, creating or overwriting {filename}. Validation (missing
     * id/trigger/action fields) happens inside EventConfigWriter and
     * surfaces as an IllegalArgumentException with a specific message,
     * which the caller turns into a 400 response.
     */
    public void save(String filename, String jsonBody) throws IOException {
        EventDefinition def = gson.fromJson(jsonBody, EventDefinition.class);
        if (def == null) {
            throw new IllegalArgumentException("Empty or invalid event JSON.");
        }
        writer.write(def, eventsDir.resolve(filename));
    }

    public boolean delete(String filename) throws IOException {
        return Files.deleteIfExists(eventsDir.resolve(filename));
    }

    /**
     * Copies the file as-is. The clone will share the original's "id"
     * field until edited - the editor UI should make this obvious (e.g.
     * pre-selecting the id field for editing right after cloning) rather
     * than us guessing a new unique id server-side.
     */
    public boolean clone(String sourceFilename, String targetFilename) throws IOException {
        Path source = eventsDir.resolve(sourceFilename);
        if (!Files.isRegularFile(source)) {
            return false;
        }
        Files.copy(source, eventsDir.resolve(targetFilename), StandardCopyOption.REPLACE_EXISTING);
        return true;
    }
}
