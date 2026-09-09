package com.heytozzz.htzcut.neoforge.cinematic;

import com.heytozzz.htzcut.core.cinematic.CameraKeyframe;
import com.heytozzz.htzcut.core.cinematic.CinematicSink;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-tick-driven cinematic playback: puts a player in spectator
 * mode and moves their camera through a sequence of keyframes, smoothly
 * interpolating position and rotation between them, then restores their
 * original gamemode and position once the path finishes.
 *
 * Supports path types per segment (defined on the *destination* keyframe):
 *   - linear  : straight line (default)
 *   - ellipse : elliptical arc in the horizontal plane + linear height
 *   - bezier  : quadratic Bezier with an automatic outward control point
 *
 * Ticked once per server tick from HTZCutMod (same pattern as
 * TickActionScheduler) - constructed once at server start and reused
 * across /htzcut reload, so an in-progress cinematic isn't interrupted
 * by reloading event definitions.
 *
 * Starting a new cinematic for a player who already has one running
 * cancels and restores the old one first - two cinematics fighting over
 * the same player would otherwise produce nonsensical camera movement.
 *
 * Known limitation: the player's own movement input isn't blocked
 * during playback (spectators can still fly with WASD); since position
 * is forced every tick regardless, this mostly just means their input
 * has no visible effect rather than causing a conflict, but it's not as
 * clean as properly disabling input client-side. A refinement for a
 * later iteration.
 */
public final class CinematicRunner implements CinematicSink {

    private static final int TICKS_PER_SECOND = 20;
    private static final double MIN_SEGMENT_SECONDS = 0.05;
    /** How far the auto control point bulges sideways, as a fraction of segment length. */
    private static final double CURVE_BULGE = 0.35;

    private final MinecraftServer server;
    private final Map<UUID, ActiveCinematic> active = new HashMap<>();

    public CinematicRunner(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void playCinematic(UUID playerId, List<CameraKeyframe> keyframes, Double totalDurationSeconds) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null || keyframes == null || keyframes.isEmpty()) {
            return;
        }

        ActiveCinematic existing = active.remove(playerId);
        if (existing != null) {
            restore(player, existing);
        }

        GameType originalGameMode = player.gameMode.getGameModeForPlayer();
        double originalX = player.getX();
        double originalY = player.getY();
        double originalZ = player.getZ();
        float originalYaw = player.getYRot();
        float originalPitch = player.getXRot();

        player.setGameMode(GameType.SPECTATOR);

        // The jump to the first keyframe is an instant cut, not part of
        // the interpolated path - only the segments BETWEEN keyframes
        // are smoothly animated. Its own timeSeconds is therefore
        // unused (there's nothing to interpolate on the way to it).
        CameraKeyframe first = keyframes.get(0);
        player.teleportTo(player.serverLevel(), first.x(), first.y(), first.z(),
                Set.of(), first.yaw(), first.pitch());

        List<CameraKeyframe> remainingKeyframes = keyframes.subList(1, keyframes.size());
        List<Segment> segments = buildSegments(remainingKeyframes, totalDurationSeconds);

        ActiveCinematic cinematic = new ActiveCinematic(
                segments,
                first.x(), first.y(), first.z(), first.yaw(), first.pitch(),
                originalX, originalY, originalZ, originalYaw, originalPitch,
                originalGameMode
        );

        active.put(playerId, cinematic);
    }

    /** Called once per server tick from HTZCutMod. */
    public void tick() {
        if (active.isEmpty()) {
            return;
        }

        List<UUID> finished = new ArrayList<>();

        for (Map.Entry<UUID, ActiveCinematic> entry : active.entrySet()) {
            UUID playerId = entry.getKey();
            ActiveCinematic cinematic = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);

            if (player == null) {
                // Player disconnected mid-cinematic - nothing left to
                // restore them into, just drop it.
                finished.add(playerId);
                continue;
            }

            cinematic.elapsedTicks++;
            boolean done = applyFrame(player, cinematic);
            if (done) {
                restore(player, cinematic);
                finished.add(playerId);
            }
        }

        finished.forEach(active::remove);
    }

    /** Cancels every active cinematic and restores each player - called on server stop. */
    public void clear() {
        for (Map.Entry<UUID, ActiveCinematic> entry : active.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                restore(player, entry.getValue());
            }
        }
        active.clear();
    }

    /**
     * Converts each keyframe's timeSeconds into an actual tick count for
     * its segment. If totalDurationSeconds is set, every keyframe's
     * timeSeconds is treated as a relative weight and scaled so the
     * whole path takes exactly that long; otherwise each is used as a
     * literal duration.
     */
    private List<Segment> buildSegments(List<CameraKeyframe> keyframes, Double totalDurationSeconds) {
        List<Segment> segments = new ArrayList<>();

        double totalWeight = keyframes.stream()
                .mapToDouble(k -> Math.max(MIN_SEGMENT_SECONDS, k.timeSeconds()))
                .sum();

        for (CameraKeyframe keyframe : keyframes) {
            double rawSeconds = Math.max(MIN_SEGMENT_SECONDS, keyframe.timeSeconds());
            double actualSeconds = totalDurationSeconds != null
                    ? totalDurationSeconds * (rawSeconds / totalWeight)
                    : rawSeconds;
            int ticks = Math.max(1, (int) Math.round(actualSeconds * TICKS_PER_SECOND));
            segments.add(new Segment(keyframe, ticks));
        }

        return segments;
    }

    /** @return true once every segment has finished playing. */
    private boolean applyFrame(ServerPlayer player, ActiveCinematic cinematic) {
        long remaining = cinematic.elapsedTicks;
        double fromX = cinematic.startX;
        double fromY = cinematic.startY;
        double fromZ = cinematic.startZ;
        float fromYaw = cinematic.startYaw;
        float fromPitch = cinematic.startPitch;

        for (Segment segment : cinematic.segments) {
            if (remaining < segment.ticks) {
                float t = segment.ticks == 0 ? 1f : (float) remaining / segment.ticks;
                CameraKeyframe target = segment.keyframe;

                double x, y, z;
                switch (target.resolvedPathType()) {
                    case "ellipse" -> {
                        double[] p = interpolateEllipse(fromX, fromY, fromZ, target.x(), target.y(), target.z(), t);
                        x = p[0];
                        y = p[1];
                        z = p[2];
                    }
                    case "bezier" -> {
                        double[] p = interpolateBezier(fromX, fromY, fromZ, target.x(), target.y(), target.z(), t);
                        x = p[0];
                        y = p[1];
                        z = p[2];
                    }
                    default -> { // linear
                        x = lerp(fromX, target.x(), t);
                        y = lerp(fromY, target.y(), t);
                        z = lerp(fromZ, target.z(), t);
                    }
                }

                float yaw = lerpAngle(fromYaw, target.yaw(), t);
                float pitch = lerp(fromPitch, target.pitch(), t);

                player.teleportTo(player.serverLevel(), x, y, z, Set.of(), yaw, pitch);
                return false;
            }

            remaining -= segment.ticks;
            fromX = segment.keyframe.x();
            fromY = segment.keyframe.y();
            fromZ = segment.keyframe.z();
            fromYaw = segment.keyframe.yaw();
            fromPitch = segment.keyframe.pitch();
        }

        return true; // ran past every segment - cinematic finished
    }

    /**
     * Elliptical arc in the horizontal plane + linear height.
     * The major axis runs from A→B on XZ; the minor axis is perpendicular
     * and sized to CURVE_BULGE * distance so the path bows outward.
     * Parameter t goes 0→1 along a half-ellipse (sin/cos).
     */
    private double[] interpolateEllipse(double x0, double y0, double z0,
                                        double x1, double y1, double z1, float t) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double dist = Math.sqrt(dx * dx + dz * dz);

        // Midpoint of the chord
        double mx = (x0 + x1) * 0.5;
        double mz = (z0 + z1) * 0.5;

        // Perpendicular unit vector in XZ (rotated 90°)
        double perpX = 0;
        double perpZ = 0;
        if (dist > 1e-6) {
            perpX = -dz / dist;
            perpZ = dx / dist;
        }

        double minor = dist * CURVE_BULGE;

        // Parametric half-ellipse: angle from π → 0 so we start at A and end at B
        double angle = Math.PI * (1.0 - t);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double x = mx + (dx * 0.5) * cos + perpX * minor * sin;
        double z = mz + (dz * 0.5) * cos + perpZ * minor * sin;
        double y = lerp(y0, y1, t);

        return new double[]{x, y, z};
    }

    /**
     * Quadratic Bezier with an automatic control point offset perpendicular
     * to the segment (same bulge factor as ellipse). Gives a smooth curve
     * that feels less "perfectly elliptical" and more organic.
     */
    private double[] interpolateBezier(double x0, double y0, double z0,
                                       double x1, double y1, double z1, float t) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;
        double dist = Math.sqrt(dx * dx + dz * dz);

        double mx = (x0 + x1) * 0.5;
        double my = (y0 + y1) * 0.5;
        double mz = (z0 + z1) * 0.5;

        double perpX = 0;
        double perpZ = 0;
        if (dist > 1e-6) {
            perpX = -dz / dist;
            perpZ = dx / dist;
        }

        double bulge = dist * CURVE_BULGE;
        // Control point: midpoint + sideways offset (also slightly lifted)
        double cx = mx + perpX * bulge;
        double cy = my + Math.abs(dy) * 0.15 + 0.5; // slight vertical lift
        double cz = mz + perpZ * bulge;

        // Quadratic Bezier: (1-t)²·P0 + 2(1-t)t·C + t²·P1
        double u = 1.0 - t;
        double x = u * u * x0 + 2 * u * t * cx + t * t * x1;
        double y = u * u * y0 + 2 * u * t * cy + t * t * y1;
        double z = u * u * z0 + 2 * u * t * cz + t * t * z1;

        return new double[]{x, y, z};
    }

    private void restore(ServerPlayer player, ActiveCinematic cinematic) {
        try {
            player.setGameMode(cinematic.originalGameMode);
            player.teleportTo(player.serverLevel(), cinematic.originalX, cinematic.originalY, cinematic.originalZ,
                    Set.of(), cinematic.originalYaw, cinematic.originalPitch);
        } catch (Exception e) {
            HTZLog.error("Failed to restore player after cinematic playback", e);
        }
    }

    private double lerp(double from, double to, float t) {
        return from + (to - from) * t;
    }

    private float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /** Interpolates yaw the short way around the circle instead of always the "increasing" direction. */
    private float lerpAngle(float from, float to, float t) {
        float delta = ((to - from + 540f) % 360f) - 180f;
        return from + delta * t;
    }

    private record Segment(CameraKeyframe keyframe, int ticks) {
    }

    private static final class ActiveCinematic {
        final List<Segment> segments;
        final double startX;
        final double startY;
        final double startZ;
        final float startYaw;
        final float startPitch;
        final double originalX;
        final double originalY;
        final double originalZ;
        final float originalYaw;
        final float originalPitch;
        final GameType originalGameMode;
        long elapsedTicks;

        ActiveCinematic(List<Segment> segments, double startX, double startY, double startZ,
                         float startYaw, float startPitch,
                         double originalX, double originalY, double originalZ,
                         float originalYaw, float originalPitch,
                         GameType originalGameMode) {
            this.segments = segments;
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
            this.startYaw = startYaw;
            this.startPitch = startPitch;
            this.originalX = originalX;
            this.originalY = originalY;
            this.originalZ = originalZ;
            this.originalYaw = originalYaw;
            this.originalPitch = originalPitch;
            this.originalGameMode = originalGameMode;
        }
    }
}
