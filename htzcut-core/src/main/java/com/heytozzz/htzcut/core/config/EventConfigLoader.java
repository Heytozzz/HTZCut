package com.heytozzz.htzcut.core.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads every *.yaml file under a given /events directory into
 * EventDefinition instances. Kept loader-agnostic on purpose: the
 * neoforge module only needs to point this at the right folder.
 */
public class EventConfigLoader {

    private final Yaml yaml;

    public EventConfigLoader() {
        Constructor constructor = new Constructor(EventDefinition.class, new LoaderOptions());
        this.yaml = new Yaml(constructor);
    }

    public List<EventDefinition> loadAll(Path eventsDirectory) throws IOException {
        List<EventDefinition> definitions = new ArrayList<>();

        if (!Files.isDirectory(eventsDirectory)) {
            return definitions;
        }

        try (Stream<Path> files = Files.list(eventsDirectory)) {
            List<Path> yamlFiles = files
                    .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                    .toList();

            for (Path file : yamlFiles) {
                try (InputStream in = Files.newInputStream(file)) {
                    EventDefinition def = yaml.load(in);
                    if (def != null) {
                        definitions.add(def);
                    }
                }
            }
        }

        return definitions;
    }
}
