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

        List<Segment> segments = buildSegments(keyframes, totalDurationSeconds);

        ActiveCinematic cinematic = new ActiveCinematic(
                segments,
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                player.gameMode.getGameModeForPlayer()
        );

        player.setGameMode(GameType.SPECTATOR);
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

                double x = lerp(fromX, target.x(), t);
                double y = lerp(fromY, target.y(), t);
                double z = lerp(fromZ, target.z(), t);
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

    private void restore(ServerPlayer player, ActiveCinematic cinematic) {
        try {
            player.setGameMode(cinematic.originalGameMode);
            player.teleportTo(player.serverLevel(), cinematic.startX, cinematic.startY, cinematic.startZ,
                    Set.of(), cinematic.startYaw, cinematic.startPitch);
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
        final GameType originalGameMode;
        long elapsedTicks;

        ActiveCinematic(List<Segment> segments, double startX, double startY, double startZ,
                         float startYaw, float startPitch, GameType originalGameMode) {
            this.segments = segments;
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
            this.startYaw = startYaw;
            this.startPitch = startPitch;
            this.originalGameMode = originalGameMode;
        }
    }
}
