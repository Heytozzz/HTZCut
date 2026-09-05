package com.heytozzz.htzcut.core.config;

import com.heytozzz.htzcut.core.trigger.TriggerType;

/**
 * Plain data representation of a single event YAML file under /events.
 * Intentionally kept as simple, user-facing fields only - no codec, path
 * or transport details belong here. Those are resolved internally by
 * AudioAssetResolver / AudioDeliveryRouter at runtime.
 */
public class EventDefinition {

    private String id;
    private TriggerConfig trigger;
    private AudioConfig audio;
    private NarrationConfig narration;
    private ConditionsConfig conditions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TriggerConfig getTrigger() {
        return trigger;
    }

    public void setTrigger(TriggerConfig trigger) {
        this.trigger = trigger;
    }

    public AudioConfig getAudio() {
        return audio;
    }

    public void setAudio(AudioConfig audio) {
        this.audio = audio;
    }

    public NarrationConfig getNarration() {
        return narration;
    }

    public void setNarration(NarrationConfig narration) {
        this.narration = narration;
    }

    public ConditionsConfig getConditions() {
        return conditions;
    }

    public void setConditions(ConditionsConfig conditions) {
        this.conditions = conditions;
    }

    public static class TriggerConfig {
        private TriggerType type;
        private String value;

        public TriggerType getType() {
            return type;
        }

        public void setType(TriggerType type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * User only ever provides an id + a file name. Whether this resolves
     * to SVC opus frames or an HTTP-servable file is decided internally
     * by AudioAssetResolver, never configured here.
     */
    public static class AudioConfig {
        private String id;
        private String file;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }
    }

    public static class NarrationConfig {
        private String textKey;
        private String fallbackLocale;

        public String getTextKey() {
            return textKey;
        }

        public void setTextKey(String textKey) {
            this.textKey = textKey;
        }

        public String getFallbackLocale() {
            return fallbackLocale;
        }

        public void setFallbackLocale(String fallbackLocale) {
            this.fallbackLocale = fallbackLocale;
        }
    }

    public static class ConditionsConfig {
        private String permission;
        private boolean oncePerPlayer;

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public boolean isOncePerPlayer() {
            return oncePerPlayer;
        }

        public void setOncePerPlayer(boolean oncePerPlayer) {
            this.oncePerPlayer = oncePerPlayer;
        }
    }
}
