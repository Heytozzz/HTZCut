package com.heytozzz.htzcut.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Serializes an EventDefinition back to the same snake_case,
 * lowercase-enum YAML style EventConfigLoader reads. Hand-written rather
 * than driven through SnakeYAML's bean Representer: our schema is small
 * and fixed, and this guarantees exact, predictable output - useful both
 * for round-tripping files saved from the web editor and for the
 * editor's live preview to reason about.
 */
public class EventConfigWriter {

    public void write(EventDefinition def, Path targetFile) throws IOException {
        Files.writeString(targetFile, toYaml(def));
    }

    public String toYaml(EventDefinition def) {
        validate(def);

        StringBuilder sb = new StringBuilder();

        sb.append("id: \"").append(escape(def.getId())).append("\"\n\n");

        sb.append("trigger:\n");
        sb.append("  type: ").append(lower(def.getTrigger().getType())).append("\n");
        sb.append("  value: \"").append(escape(def.getTrigger().getValue())).append("\"\n\n");

        List<EventDefinition.ActionConfig> actions = def.getActions();
        if (actions != null && !actions.isEmpty()) {
            sb.append("actions:\n");
            for (EventDefinition.ActionConfig action : actions) {
                if (action.getType() == null) {
                    continue;
                }
                sb.append("  - type: ").append(lower(action.getType())).append("\n");
                switch (action.getType()) {
                    case SOUND -> sb.append("    sound: \"").append(escape(action.getSound())).append("\"\n");
                    case NARRATION -> {
                        sb.append("    text_key: \"").append(escape(action.getTextKey())).append("\"\n");
                        if (action.getFallbackLocale() != null && !action.getFallbackLocale().isBlank()) {
                            sb.append("    fallback_locale: \"")
                                    .append(escape(action.getFallbackLocale())).append("\"\n");
                        }
                    }
                    case DIALOGUE -> sb.append("    audio: \"").append(escape(action.getAudio())).append("\"\n");
                }
                sb.append("\n");
            }
        }

        if (def.getConditions() != null) {
            EventDefinition.ConditionsConfig conditions = def.getConditions();
            sb.append("conditions:\n");
            if (conditions.getPermission() != null && !conditions.getPermission().isBlank()) {
                sb.append("  permission: \"").append(escape(conditions.getPermission())).append("\"\n");
            }
            sb.append("  once_per_player: ").append(conditions.isOncePerPlayer()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Fails loudly and specifically instead of silently writing a
     * half-populated, confusing YAML file if the editor sends an
     * incomplete event (e.g. a trigger with no value selected yet).
     */
    private void validate(EventDefinition def) {
        if (def.getId() == null || def.getId().isBlank()) {
            throw new IllegalArgumentException("Event is missing an id.");
        }
        if (def.getTrigger() == null || def.getTrigger().getType() == null) {
            throw new IllegalArgumentException("Event '" + def.getId() + "' is missing a trigger type.");
        }
        if (def.getTrigger().getValue() == null || def.getTrigger().getValue().isBlank()) {
            throw new IllegalArgumentException("Event '" + def.getId() + "' is missing a trigger value.");
        }
        if (def.getActions() != null) {
            for (EventDefinition.ActionConfig action : def.getActions()) {
                validateAction(def.getId(), action);
            }
        }
    }

    private void validateAction(String eventId, EventDefinition.ActionConfig action) {
        if (action.getType() == null) {
            throw new IllegalArgumentException("Event '" + eventId + "' has an action with no type selected.");
        }
        switch (action.getType()) {
            case SOUND -> {
                if (isBlank(action.getSound())) {
                    throw new IllegalArgumentException(
                            "Event '" + eventId + "' has a sound action with no sound selected.");
                }
            }
            case NARRATION -> {
                if (isBlank(action.getTextKey())) {
                    throw new IllegalArgumentException(
                            "Event '" + eventId + "' has a narration action with no text_key set.");
                }
            }
            case DIALOGUE -> {
                if (isBlank(action.getAudio())) {
                    throw new IllegalArgumentException(
                            "Event '" + eventId + "' has a dialogue action with no audio file set.");
                }
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String lower(Enum<?> value) {
        return value == null ? "" : value.name().toLowerCase();
    }

    /**
     * Escapes everything that would otherwise produce broken or
     * misleading YAML inside a double-quoted scalar: backslashes (must
     * go first), quotes, and literal newlines/tabs, which YAML's
     * double-quoted style requires as escape sequences rather than raw
     * control characters.
     */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
