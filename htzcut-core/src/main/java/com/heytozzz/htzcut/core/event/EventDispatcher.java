package com.heytozzz.htzcut.core.event;

import com.heytozzz.htzcut.core.audio.AudioDeliveryRouter;
import com.heytozzz.htzcut.core.config.EventDefinition;
import com.heytozzz.htzcut.core.narration.NarrationSink;
import com.heytozzz.htzcut.core.permission.PermissionChecker;
import com.heytozzz.htzcut.core.sound.SoundSink;
import com.heytozzz.htzcut.core.subtitle.SubtitlePosition;
import com.heytozzz.htzcut.core.subtitle.SubtitleSink;
import com.heytozzz.htzcut.core.trigger.HTZTriggerFired;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Receives loader-agnostic trigger notifications and, for every matching
 * EventDefinition, checks conditions (permission, once-per-player) and
 * executes each of its actions in declaration order.
 *
 * This class has zero knowledge of NeoForge, advancements, dimensions,
 * etc. - it only reacts to HTZTriggerFired instances that the neoforge
 * module produces from real game events.
 */
public class EventDispatcher {

    // Used when a dialogue action sets a subtitle but no explicit
    // subtitle_duration_seconds - long enough to read a short line
    // without needing every event author to specify it.
    private static final double DEFAULT_SUBTITLE_DURATION_SECONDS = 4.0;
    private static final SubtitlePosition DEFAULT_SUBTITLE_POSITION = SubtitlePosition.BOTTOM;

    private final List<EventDefinition> definitions;
    private final PermissionChecker permissionChecker;
    private final AudioDeliveryRouter audioDeliveryRouter;
    private final NarrationSink narrationSink;
    private final SoundSink soundSink;
    private final SubtitleSink subtitleSink;

    // Tracks which (playerId, eventId) pairs have already fired, for
    // once_per_player conditions. A real implementation should persist
    // this to disk instead of keeping it purely in memory.
    private final Set<String> firedOnce = ConcurrentHashMap.newKeySet();

    public EventDispatcher(List<EventDefinition> definitions,
                            PermissionChecker permissionChecker,
                            AudioDeliveryRouter audioDeliveryRouter,
                            NarrationSink narrationSink,
                            SoundSink soundSink,
                            SubtitleSink subtitleSink) {
        this.definitions = definitions;
        this.permissionChecker = permissionChecker;
        this.audioDeliveryRouter = audioDeliveryRouter;
        this.narrationSink = narrationSink;
        this.soundSink = soundSink;
        this.subtitleSink = subtitleSink;
    }

    public void onTrigger(HTZTriggerFired trigger) {
        for (EventDefinition def : definitions) {
            if (matches(def, trigger) && conditionsPass(def, trigger.playerId())) {
                fire(def, trigger.playerId());
            }
        }
    }

    private boolean matches(EventDefinition def, HTZTriggerFired trigger) {
        EventDefinition.TriggerConfig cfg = def.getTrigger();
        if (cfg == null) {
            return false;
        }
        return cfg.getType() == trigger.type() && cfg.getValue().equals(trigger.value());
    }

    private boolean conditionsPass(EventDefinition def, UUID playerId) {
        EventDefinition.ConditionsConfig conditions = def.getConditions();
        if (conditions == null) {
            return true;
        }

        if (conditions.getPermission() != null
                && !permissionChecker.hasPermission(playerId, conditions.getPermission())) {
            return false;
        }

        if (conditions.isOncePerPlayer()) {
            String key = playerId + ":" + def.getId();
            return firedOnce.add(key); // returns false if it was already present
        }

        return true;
    }

    private void fire(EventDefinition def, UUID playerId) {
        if (def.getActions() == null) {
            return;
        }

        for (EventDefinition.ActionConfig action : def.getActions()) {
            if (action.getType() == null) {
                continue;
            }

            switch (action.getType()) {
                case SOUND -> soundSink.playSound(playerId, action.getSound());
                case NARRATION -> narrationSink.sendNarration(
                        playerId, action.getTextKey(), action.getFallbackLocale());
                case DIALOGUE -> {
                    audioDeliveryRouter.playDialogue(playerId, action.getAudio());
                    fireSubtitleIfPresent(action, playerId);
                }
            }
        }
    }

    private void fireSubtitleIfPresent(EventDefinition.ActionConfig action, UUID playerId) {
        String subtitle = action.getSubtitle();
        if (subtitle == null || subtitle.isBlank()) {
            return;
        }

        double duration = action.getSubtitleDurationSeconds() != null
                ? action.getSubtitleDurationSeconds()
                : DEFAULT_SUBTITLE_DURATION_SECONDS;
        SubtitlePosition position = action.getSubtitlePosition() != null
                ? action.getSubtitlePosition()
                : DEFAULT_SUBTITLE_POSITION;

        subtitleSink.showSubtitle(playerId, subtitle, duration, position);
    }
}
