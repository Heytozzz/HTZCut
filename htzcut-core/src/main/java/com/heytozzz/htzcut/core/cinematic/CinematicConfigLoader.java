package com.heytozzz.htzcut.core.cinematic;

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
 * Loads named CinematicDefinition YAML files from a directory (or one
 * at a time). Same snake_case-tolerant property matching as
 * EventConfigLoader, duplicated rather than shared since the two load
 * different root types and SnakeYAML's Constructor is tied to one root
 * class per instance.
 */
public class CinematicConfigLoader {

    private final Yaml yaml;

    public CinematicConfigLoader() {
        Constructor constructor = new Constructor(CinematicDefinition.class, new LoaderOptions());
        constructor.setPropertyUtils(new SnakeCasePropertyUtils());
        this.yaml = new Yaml(constructor);
    }

    public CinematicDefinition loadOne(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            CinematicDefinition def = yaml.load(in);
            if (def != null) {
                // Older files written with a bare "keyframes:" deserialize
                // as null. Force a real list so callers never NPE.
                def.setKeyframes(def.getKeyframes());
            }
            return def;
        }
    }

    public List<CinematicDefinition> loadAll(Path cinematicsDirectory) throws IOException {
        List<CinematicDefinition> definitions = new ArrayList<>();

        if (!Files.isDirectory(cinematicsDirectory)) {
            return definitions;
        }

        try (Stream<Path> files = Files.list(cinematicsDirectory)) {
            List<Path> yamlFiles = files
                    .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                    .toList();

            for (Path file : yamlFiles) {
                CinematicDefinition def = loadOne(file);
                if (def != null) {
                    definitions.add(def);
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
