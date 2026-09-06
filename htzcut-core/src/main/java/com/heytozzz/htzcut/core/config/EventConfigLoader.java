package com.heytozzz.htzcut.core.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.ScalarNode;

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
        this.yaml = new Yaml(new LenientEventConstructor(new LoaderOptions()));
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
                EventDefinition def = loadOne(file);
                if (def != null) {
                    definitions.add(def);
                }
            }
        }

        return definitions;
    }

    /**
     * Loads a single event YAML file. Used by the web editor's event API
     * to read one file at a time (get/edit), separately from the
     * server's own full-directory startup/reload load.
     */
    public EventDefinition loadOne(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return yaml.load(in);
        }
    }

    /**
     * SnakeYAML's defaults require exact, case-sensitive matches for both
     * property names and enum values, which doesn't fit how we want event
     * YAML to read: snake_case keys (text_key, once_per_player) and
     * lowercase enum values (type: sound) rather than shouting-case Java
     * constants (TYPE: SOUND). This constructor relaxes both:
     *   - property lookup: snake_case -> camelCase before resolving
     *     against EventDefinition's JavaBean properties
     *   - enum values: matched case-insensitively against the enum's
     *     constant names
     */
    private static class LenientEventConstructor extends Constructor {

        LenientEventConstructor(LoaderOptions loaderOptions) {
            super(EventDefinition.class, loaderOptions);
            setPropertyUtils(new SnakeCasePropertyUtils());
            yamlClassConstructors.put(NodeId.scalar, new CaseInsensitiveEnumConstruct());
        }

        private class CaseInsensitiveEnumConstruct extends ConstructScalar {
            @Override
            public Object construct(Node node) {
                if (!node.getType().isEnum()) {
                    return super.construct(node);
                }

                String value = ((ScalarNode) node).getValue();
                for (Object constant : node.getType().getEnumConstants()) {
                    if (((Enum<?>) constant).name().equalsIgnoreCase(value)) {
                        return constant;
                    }
                }

                throw new YAMLException("Unable to find enum value '" + value
                        + "' for enum class: " + node.getType().getName());
            }
        }
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
