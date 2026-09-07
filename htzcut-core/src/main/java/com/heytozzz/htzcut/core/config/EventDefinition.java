package com.heytozzz.htzcut.core.config;

import com.heytozzz.htzcut.core.action.ActionType;
import com.heytozzz.htzcut.core.subtitle.SubtitleBoxEffect;
import com.heytozzz.htzcut.core.subtitle.SubtitlePosition;
import com.heytozzz.htzcut.core.subtitle.SubtitleTextEffect;
import com.heytozzz.htzcut.core.trigger.TriggerType;

import java.util.List;

/**
 * Plain data representation of a single event YAML file under /events.
 * Intentionally kept as simple, user-facing fields only - no codec,
 * path or transport details belong here. Those are resolved internally
 * by AudioAssetResolver / AudioDeliveryRouter at runtime.
 */
public class EventDefinition {

    private String id;
    private TriggerConfig trigger;
    private List<ActionConfig> actions;
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

    public List<ActionConfig> getActions() {
        return actions;
    }

    public void setActions(List<ActionConfig> actions) {
        this.actions = actions;
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
     * A single action executed when the event fires. Only the fields
     * relevant to its "type" need to be set - which fields those are is
     * documented per ActionType:
     *   SOUND     -> sound
     *   NARRATION -> textKey, fallbackLocale
     *   DIALOGUE  -> audio, and optionally subtitle + subtitleBoxEffect,
     *                subtitleTextEffect, subtitleTextDurationSeconds,
     *                subtitleHoldSeconds, subtitlePosition (subtitle box
     *                is skipped entirely if subtitle is left unset)
     *
     * delaySeconds applies to every action type: how long to wait,
     * after the previous action in the list started, before this one
     * runs (not relative to the event trigger itself). Left unset or
     * zero, actions run back-to-back exactly as before this existed.
     */
    public static class ActionConfig {
        private ActionType type;
        private Double delaySeconds;
        private String sound;
        private String textKey;
        private String fallbackLocale;
        private String audio;
        private String subtitle;
        private SubtitleBoxEffect subtitleBoxEffect;
        private SubtitleTextEffect subtitleTextEffect;
        private Double subtitleTextDurationSeconds;
        private Double subtitleHoldSeconds;
        private SubtitlePosition subtitlePosition;

        public ActionType getType() {
            return type;
        }

        public void setType(ActionType type) {
            this.type = type;
        }

        public Double getDelaySeconds() {
            return delaySeconds;
        }

        public void setDelaySeconds(Double delaySeconds) {
            this.delaySeconds = delaySeconds;
        }

        public String getSound() {
            return sound;
        }

        public void setSound(String sound) {
            this.sound = sound;
        }

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

        public String getAudio() {
            return audio;
        }

        public void setAudio(String audio) {
            this.audio = audio;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public SubtitleBoxEffect getSubtitleBoxEffect() {
            return subtitleBoxEffect;
        }

        public void setSubtitleBoxEffect(SubtitleBoxEffect subtitleBoxEffect) {
            this.subtitleBoxEffect = subtitleBoxEffect;
        }

        public SubtitleTextEffect getSubtitleTextEffect() {
            return subtitleTextEffect;
        }

        public void setSubtitleTextEffect(SubtitleTextEffect subtitleTextEffect) {
            this.subtitleTextEffect = subtitleTextEffect;
        }

        public Double getSubtitleTextDurationSeconds() {
            return subtitleTextDurationSeconds;
        }

        public void setSubtitleTextDurationSeconds(Double subtitleTextDurationSeconds) {
            this.subtitleTextDurationSeconds = subtitleTextDurationSeconds;
        }

        public Double getSubtitleHoldSeconds() {
            return subtitleHoldSeconds;
        }

        public void setSubtitleHoldSeconds(Double subtitleHoldSeconds) {
            this.subtitleHoldSeconds = subtitleHoldSeconds;
        }

        public SubtitlePosition getSubtitlePosition() {
            return subtitlePosition;
        }

        public void setSubtitlePosition(SubtitlePosition subtitlePosition) {
            this.subtitlePosition = subtitlePosition;
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
