package com.heytozzz.htzcut.neoforge.cinematic;

import com.heytozzz.htzcut.core.cinematic.KeyframeConfig;
import com.heytozzz.htzcut.core.scheduler.ActionScheduler;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.heytozzz.htzcut.neoforge.item.HTZItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Temporary in-world visualization for /htzcut cinematic keyframe list
 * and preview: a floating icon (Item Display entity showing
 * HTZItems.KEYFRAME_MARKER, always facing the viewer) over every
 * keyframe, plus a particle trail along the path between them
 * (respecting linear / ellipse / bezier path types).
 *
 * Deliberately NOT a permanent visual - everything spawned here is
 * scheduled for automatic removal after VISIBLE_SECONDS, and the
 * particle trail is periodically refreshed for that same window since
 * particles fade after roughly a second on their own.
 *
 * Uses a plain particle trail rather than a real rendered line: a
 * custom client-side line renderer would need render-event/shader code
 * that's far more version-fragile than anything else in this mod, for
 * a purely cosmetic editing aid.
 */
public final class CinematicVisualizer {

    private static final double VISIBLE_SECONDS = 30.0;
    private static final double PARTICLE_REFRESH_INTERVAL_SECONDS = 1.0;
    private static final double PARTICLES_PER_BLOCK = 2.0;
    private static final double CURVE_BULGE = 0.35;

    private final ActionScheduler scheduler;

    public CinematicVisualizer(ActionScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void show(ServerLevel level, List<KeyframeConfig> keyframes) {
        if (keyframes == null || keyframes.isEmpty()) {
            return;
        }

        spawnMarkers(level, keyframes);
        schedulePathRefreshes(level, keyframes);
    }

    private void spawnMarkers(ServerLevel level, List<KeyframeConfig> keyframes) {
        List<Display.ItemDisplay> spawned = new ArrayList<>();

        for (KeyframeConfig keyframe : keyframes) {
            try {
                Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
                display.setPos(keyframe.getX(), keyframe.getY(), keyframe.getZ());

                // Display.ItemDisplay has no public setters for its item
                // or billboard mode (both are private synced-data fields,
                // only ever populated internally from saved NBT) - so we
                // configure them the same way the vanilla /summon command
                // does: build the NBT it would have been saved/loaded
                // with, and load() it onto the freshly created entity.
                CompoundTag tag = new CompoundTag();
                ItemStack markerStack = new ItemStack(HTZItems.KEYFRAME_MARKER.get());
                tag.put("item", markerStack.save(level.registryAccess()));
                tag.putString("billboard", "center");
                display.load(tag);

                level.addFreshEntity(display);
                spawned.add(display);
            } catch (Exception e) {
                HTZLog.error("Failed to spawn a keyframe marker entity for cinematic visualization", e);
            }
        }

        scheduler.schedule(VISIBLE_SECONDS, () -> {
            for (Display.ItemDisplay display : spawned) {
                if (!display.isRemoved()) {
                    display.discard();
                }
            }
        });
    }

    private void schedulePathRefreshes(ServerLevel level, List<KeyframeConfig> keyframes) {
        int refreshes = (int) Math.ceil(VISIBLE_SECONDS / PARTICLE_REFRESH_INTERVAL_SECONDS);
        for (int i = 0; i < refreshes; i++) {
            scheduler.schedule(i * PARTICLE_REFRESH_INTERVAL_SECONDS, () -> drawPathOnce(level, keyframes));
        }
    }

    private void drawPathOnce(ServerLevel level, List<KeyframeConfig> keyframes) {
        for (int i = 0; i + 1 < keyframes.size(); i++) {
            KeyframeConfig a = keyframes.get(i);
            KeyframeConfig b = keyframes.get(i + 1);

            double dx = b.getX() - a.getX();
            double dy = b.getY() - a.getY();
            double dz = b.getZ() - a.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int steps = Math.max(1, (int) (distance * PARTICLES_PER_BLOCK));

            String pathType = b.resolvedPathType();

            for (int s = 0; s <= steps; s++) {
                float t = (float) s / steps;
                double x, y, z;

                switch (pathType) {
                    case "ellipse" -> {
                        double[] p = interpolateEllipse(
                                a.getX(), a.getY(), a.getZ(),
                                b.getX(), b.getY(), b.getZ(), t);
                        x = p[0];
                        y = p[1];
                        z = p[2];
                    }
                    case "bezier" -> {
                        double[] p = interpolateBezier(
                                a.getX(), a.getY(), a.getZ(),
                                b.getX(), b.getY(), b.getZ(), t);
                        x = p[0];
                        y = p[1];
                        z = p[2];
                    }
                    default -> {
                        x = a.getX() + dx * t;
                        y = a.getY() + dy * t;
                        z = a.getZ() + dz * t;
                    }
                }

                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    private double[] interpolateEllipse(double x0, double y0, double z0,
                                        double x1, double y1, double z1, float t) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double dist = Math.sqrt(dx * dx + dz * dz);

        double mx = (x0 + x1) * 0.5;
        double mz = (z0 + z1) * 0.5;

        double perpX = 0;
        double perpZ = 0;
        if (dist > 1e-6) {
            perpX = -dz / dist;
            perpZ = dx / dist;
        }

        double minor = dist * CURVE_BULGE;
        double angle = Math.PI * (1.0 - t);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double x = mx + (dx * 0.5) * cos + perpX * minor * sin;
        double z = mz + (dz * 0.5) * cos + perpZ * minor * sin;
        double y = y0 + (y1 - y0) * t;

        return new double[]{x, y, z};
    }

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
        double cx = mx + perpX * bulge;
        double cy = my + Math.abs(dy) * 0.15 + 0.5;
        double cz = mz + perpZ * bulge;

        double u = 1.0 - t;
        double x = u * u * x0 + 2 * u * t * cx + t * t * x1;
        double y = u * u * y0 + 2 * u * t * cy + t * t * y1;
        double z = u * u * z0 + 2 * u * t * cz + t * t * z1;

        return new double[]{x, y, z};
    }
}
