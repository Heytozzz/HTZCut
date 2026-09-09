package com.heytozzz.htzcut.core.cinematic;

/**
 * One camera position/rotation target, as authored either inline in an
 * event's CINEMATIC action or in a named CinematicDefinition managed via
 * /htzcut cinematic commands. timeSeconds is how long it takes to travel
 * from the previous keyframe (or the player's position when the
 * cinematic starts, for the first one) to this one - defaults to 1.0
 * second if left unset.
 *
 * Shared between EventDefinition.ActionConfig (inline keyframes) and
 * CinematicDefinition (named, reusable cinematics) so both read/write
 * the exact same shape.
 */
public class KeyframeConfig {

    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private Double timeSeconds;

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public Double getTimeSeconds() {
        return timeSeconds;
    }

    public void setTimeSeconds(Double timeSeconds) {
        this.timeSeconds = timeSeconds;
    }
}
