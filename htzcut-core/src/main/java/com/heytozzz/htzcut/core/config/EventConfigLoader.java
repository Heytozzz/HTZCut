package com.heytozzz.htzcut.core.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

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
        // Event YAML is intentionally written in snake_case (text_key,
        // once_per_player, ...) since that's the friendlier convention for
        // people hand-editing config files - it's not meant to mirror our
        // internal camelCase Java field names. SnakeYAML's default property
        // lookup requires an exact match, so this translates snake_case
        // keys to camelCase before resolving against EventDefinition's
        // JavaBean properties.
        constructor.setPropertyUtils(new SnakeCasePropertyUtils());
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

    private static class SnakeCasePropertyUtils extends PropertyUtils {
        @Override
        public Property getProperty(Class<?> type, String name) {
            return super.getProperty(type, toCamelCase(name));
        }

        private String toCamelCase(String snakeCase) {
            StringBuilder result = new StringBuilder();
            boolean upperNext = false;
            for (char c : snakeCase.toCharArray()) {
                if (c == '_') {
                    upperNext = true;
                    continue;
                }
                result.append(upperNext ? Character.toUpperCase(c) : c);
                upperNext = false;
            }
            return result.toString();
        }
    }
}
