package com.heytozzz.htzcut.core.cinematic;

import java.util.ArrayList;
import java.util.List;

/**
 * A named, reusable camera path managed through /htzcut cinematic
 * commands and stored as its own YAML file - as opposed to keyframes
 * authored inline inside an event's CINEMATIC action. An event
 * references one of these by name (EventDefinition.ActionConfig#getCinematic()).
 */
public class CinematicDefinition {

    private String name;
    private Double durationSeconds;
    private List<KeyframeConfig> keyframes = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * If set, every keyframe's own timeSeconds becomes a relative weight
     * and the whole path is scaled to last exactly this many seconds -
     * see CinematicSink for how this interacts with per-keyframe timing.
     */
    public Double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public List<KeyframeConfig> getKeyframes() {
        return keyframes;
    }

    public void setKeyframes(List<KeyframeConfig> keyframes) {
        this.keyframes = keyframes;
    }
}
