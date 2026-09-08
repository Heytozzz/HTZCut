package com.heytozzz.htzcut.core.cinematic;

import java.util.List;
import java.util.UUID;

/**
 * Moves a player's camera through a sequence of keyframes - putting
 * them in spectator mode, smoothly interpolating position/rotation
 * between frames, and restoring their original gamemode/position once
 * done. All the actual player/level manipulation lives in the neoforge
 * implementation; core only describes what should happen.
 */
public interface CinematicSink {

    /**
     * @param keyframes            the camera path, in order
     * @param totalDurationSeconds if non-null, each keyframe's own
     *                             timeSeconds is treated as a relative
     *                             WEIGHT and the whole path is scaled to
     *                             last exactly this many seconds; if
     *                             null, each keyframe's timeSeconds is
     *                             used as its literal segment duration
     */
    void playCinematic(UUID playerId, List<CameraKeyframe> keyframes, Double totalDurationSeconds);
}
