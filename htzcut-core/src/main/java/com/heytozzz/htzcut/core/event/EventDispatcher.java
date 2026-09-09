package com.heytozzz.htzcut.core.event;

import com.heytozzz.htzcut.core.audio.AudioDeliveryRouter;
import com.heytozzz.htzcut.core.cinematic.CameraKeyframe;
import com.heytozzz.htzcut.core.cinematic.CinematicDefinition;
import com.heytozzz.htzcut.core.cinematic.CinematicRepository;
import com.heytozzz.htzcut.core.cinematic.CinematicSink;
import com.heytozzz.htzcut.core.cinematic.KeyframeConfig;
import com.heytozzz.htzcut.core.config.EventDefinition;
import com.heytozzz.htzcut.core.narration.NarrationSink;
import com.heytozzz.htzcut.core.permission.PermissionChecker;
import com.heytozzz.htzcut.core.persistence.FiredOnceStore;
import com.heytozzz.htzcut.core.scheduler.ActionScheduler;
import com.heytozzz.htzcut.core.sound.SoundSink;
import com.heytozzz.htzcut.core.subtitle.SubtitleBoxEffect;
import com.heytozzz.htzcut.core.subtitle.SubtitlePosition;
import com.heytozzz.htzcut.core.subtitle.SubtitleSink;
import com.heytozzz.htzcut.core.subtitle.SubtitleTextEffect;
import com.heytozzz.htzcut.core.trigger.HTZTriggerFired;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    // Used when a dialogue action sets a subtitle but leaves these
    // fields unset - long enough to read a short line without needing
    // every event author to specify all of them.
    private static final SubtitleBoxEffect DEFAULT_BOX_EFFECT = SubtitleBoxEffect.INSTANT;
    private static final SubtitleTextEffect DEFAULT_TEXT_EFFECT = SubtitleTextEffect.INSTANT;
    private static final double DEFAULT_TEXT_DURATION_SECONDS = 1.5;
    private static final double DEFAULT_HOLD_SECONDS = 4.0;
    private static final SubtitlePosition DEFAULT_SUBTITLE_POSITION = SubtitlePosition.BOTTOM;

    private final List<EventDefinition> definitions;
    private final PermissionChecker permissionChecker;
    private final AudioDeliveryRouter audioDeliveryRouter;
    private final NarrationSink narrationSink;
    private final SoundSink soundSink;
    private final SubtitleSink subtitleSink;
    private final ActionScheduler scheduler;
    private final FiredOnceStore firedOnceStore;
    private final CinematicSink cinematicSink;
    private final CinematicRepository cinematicRepository;

    public EventDispatcher(List<EventDefinition> definitions,
                            PermissionChecker permissionChecker,
                            AudioDeliveryRouter audioDeliveryRouter,
                            NarrationSink narrationSink,
                            SoundSink soundSink,
                            SubtitleSink subtitleSink,
                            ActionScheduler scheduler,
                            FiredOnceStore firedOnceStore,
                            CinematicSink cinematicSink,
                            CinematicRepository cinematicRepository) {
        this.definitions = definitions;
        this.permissionChecker = permissionChecker;
        this.audioDeliveryRouter = audioDeliveryRouter;
        this.narrationSink = narrationSink;
        this.soundSink = soundSink;
        this.subtitleSink = subtitleSink;
        this.scheduler = scheduler;
        this.firedOnceStore = firedOnceStore;
        this.cinematicSink = cinematicSink;
        this.cinematicRepository = cinematicRepository;
    }

    public void onTrigger(HTZTriggerFired trigger) {
        for (EventDefinition def : definitions) {
            if (matches(def, trigger) && conditionsPass(def, trigger.playerId())) {
                fire(def, trigger.playerId());
            }
        }
    }

    /** Every currently loaded event id, for command tab-completion. */
    public List<String> getEventIds() {
        return definitions.stream().map(EventDefinition::getId).toList();
    }

    public Optional<EventDefinition> findById(String eventId) {
        return definitions.stream().filter(def -> def.getId().equals(eventId)).findFirst();
    }

    /**
     * Manually fires an event by id, e.g. from /htzcut play.
     *
     * @param bypassConditions when true, skips the event's permission
     *                         and once_per_player conditions entirely
     *                         (and never touches the once_per_player
     *                         store for this call) - used when the
     *                         target player holds
     *                         "htzcut.ignore.&lt;eventId&gt;". When false,
     *                         the event's normal conditions apply exactly
     *                         as they would for an organic trigger.
     * @return true if the event existed and its conditions allowed it
     *         to fire (or were bypassed); false if the event id is
     *         unknown or its conditions blocked it.
     */
    public boolean fireManually(String eventId, UUID playerId, boolean bypassConditions) {
        Optional<EventDefinition> def = findById(eventId);
        if (def.isEmpty()) {
            return false;
        }

        if (!bypassConditions && !conditionsPass(def.get(), playerId)) {
            return false;
        }

        fire(def.get(), playerId);
        return true;
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
            if (firedOnceStore.hasFired(playerId, def.getId())) {
                return false;
            }
            firedOnceStore.markFired(playerId, def.getId());
            return true;
        }

        return true;
    }

    private void fire(EventDefinition def, UUID playerId) {
        if (def.getActions() == null) {
            return;
        }

        // Each action's delaySeconds is relative to the previous action
        // in the list (not the trigger itself), so they stack up into a
        // running offset from "now" that we hand to the scheduler.
        double cumulativeDelaySeconds = 0;
        for (EventDefinition.ActionConfig action : def.getActions()) {
            if (action.getType() == null) {
                continue;
            }

            cumulativeDelaySeconds += action.getDelaySeconds() != null
                    ? Math.max(0, action.getDelaySeconds())
                    : 0;

            double scheduledDelay = cumulativeDelaySeconds;
            scheduler.schedule(scheduledDelay, () -> executeAction(action, playerId));
        }
    }

    private void executeAction(EventDefinition.ActionConfig action, UUID playerId) {
        switch (action.getType()) {
            case SOUND -> soundSink.playSound(playerId, action.getSound());
            case NARRATION -> narrationSink.sendNarration(
                    playerId, action.getTextKey(), action.getFallbackLocale());
            case DIALOGUE -> {
                audioDeliveryRouter.playDialogue(playerId, action.getAudio());
                fireSubtitleIfPresent(action, playerId);
            }
            case CINEMATIC -> fireCinematic(action, playerId);
        }
    }

    private void fireCinematic(EventDefinition.ActionConfig action, UUID playerId) {
        String cinematicName = action.getCinematic();

        List<KeyframeConfig> rawKeyframes;
        Double durationSeconds;

        if (cinematicName != null && !cinematicName.isBlank()) {
            Optional<CinematicDefinition> named;
            try {
                named = cinematicRepository.load(cinematicName);
            } catch (IOException e) {
                return; // logged by the neoforge layer's own error handling around this call
            }
            if (named.isEmpty()) {
                return; // named cinematic doesn't exist (deleted after the event was authored?)
            }
            rawKeyframes = named.get().getKeyframes();
            durationSeconds = named.get().getDurationSeconds();
        } else {
            rawKeyframes = action.getKeyframes();
            durationSeconds = action.getDurationSeconds();
        }

        if (rawKeyframes == null || rawKeyframes.isEmpty()) {
            return;
        }

        List<CameraKeyframe> keyframes = rawKeyframes.stream()
                .map(k -> new CameraKeyframe(
                        k.getX(), k.getY(), k.getZ(), k.getYaw(), k.getPitch(),
                        k.getTimeSeconds() != null ? k.getTimeSeconds() : 1.0))
                .toList();

        cinematicSink.playCinematic(playerId, keyframes, durationSeconds);
    }

    private void fireSubtitleIfPresent(EventDefinition.ActionConfig action, UUID playerId) {
        String subtitle = action.getSubtitle();
        if (subtitle == null || subtitle.isBlank()) {
            return;
        }

        double duration = action.getSubtitleTextDurationSeconds() != null
                ? action.getSubtitleTextDurationSeconds()
                : DEFAULT_TEXT_DURATION_SECONDS;
        double hold = action.getSubtitleHoldSeconds() != null
                ? action.getSubtitleHoldSeconds()
                : DEFAULT_HOLD_SECONDS;
        SubtitleBoxEffect boxEffect = action.getSubtitleBoxEffect() != null
                ? action.getSubtitleBoxEffect()
                : DEFAULT_BOX_EFFECT;
        SubtitleTextEffect textEffect = action.getSubtitleTextEffect() != null
                ? action.getSubtitleTextEffect()
                : DEFAULT_TEXT_EFFECT;
        SubtitlePosition position = action.getSubtitlePosition() != null
                ? action.getSubtitlePosition()
                : DEFAULT_SUBTITLE_POSITION;

        subtitleSink.showSubtitle(playerId, subtitle, boxEffect, textEffect, duration, hold, position);
    }
}
