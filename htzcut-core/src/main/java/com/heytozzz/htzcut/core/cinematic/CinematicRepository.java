package com.heytozzz.htzcut.core.cinematic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * CRUD for named CinematicDefinitions backed by one YAML file per
 * cinematic in a directory. Deliberately stateless/uncached - every
 * call re-reads or re-writes disk directly, so /htzcut cinematic
 * commands always see the latest state without needing a separate
 * "reload" step, and an event referencing a cinematic by name always
 * picks up edits made after the event itself was last reloaded.
 */
public class CinematicRepository {

    private final Path cinematicsDir;
    private final CinematicConfigLoader loader = new CinematicConfigLoader();
    private final CinematicConfigWriter writer = new CinematicConfigWriter();

    public CinematicRepository(Path cinematicsDir) {
        this.cinematicsDir = cinematicsDir;
    }

    public List<String> listNames() throws IOException {
        if (!Files.isDirectory(cinematicsDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(cinematicsDir)) {
            return files
                    .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                    .map(p -> stripExtension(p.getFileName().toString()))
                    .sorted()
                    .toList();
        }
    }

    public boolean exists(String name) {
        return Files.isRegularFile(fileFor(name));
    }

    public Optional<CinematicDefinition> load(String name) throws IOException {
        Path file = fileFor(name);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.ofNullable(loader.loadOne(file));
    }

    public void create(String name) throws IOException {
        if (exists(name)) {
            throw new IllegalArgumentException("A cinematic named '" + name + "' already exists.");
        }
        CinematicDefinition def = new CinematicDefinition();
        def.setName(name);
        save(def);
    }

    public void save(CinematicDefinition def) throws IOException {
        Files.createDirectories(cinematicsDir);
        writer.write(def, fileFor(def.getName()));
    }

    public boolean delete(String name) throws IOException {
        return Files.deleteIfExists(fileFor(name));
    }

    private Path fileFor(String name) {
        return cinematicsDir.resolve(name + ".yaml");
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
