package com.heytozzz.htzcut.core.cinematic;

/**
 * A single camera position/rotation target. timeSeconds is how long it
 * takes to travel from the PREVIOUS keyframe (or the player's position
 * when the cinematic starts, for the first one) to this one - not an
 * absolute timestamp.
 */
public record CameraKeyframe(double x, double y, double z, float yaw, float pitch, double timeSeconds) {
}
