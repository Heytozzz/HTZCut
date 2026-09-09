package com.heytozzz.htzcut.core.cinematic;

/**
 * A single camera position/rotation target. timeSeconds is how long it
 * takes to travel from the PREVIOUS keyframe (or the player's position
 * when the cinematic starts, for the first one) to this one - not an
 * absolute timestamp.
 *
 * pathType: "linear" (default), "ellipse", or "bezier".
 */
public record CameraKeyframe(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        double timeSeconds,
        String pathType
) {
    public CameraKeyframe(double x, double y, double z, float yaw, float pitch, double timeSeconds) {
        this(x, y, z, yaw, pitch, timeSeconds, "linear");
    }

    public String resolvedPathType() {
        if (pathType == null || pathType.isBlank()) {
            return "linear";
        }
        return pathType.trim().toLowerCase();
    }
}
