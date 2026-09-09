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
 * keyframe, plus a particle trail along the straight segments between
 * them.
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

            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                double x = a.getX() + dx * t;
                double y = a.getY() + dy * t;
                double z = a.getZ() + dz * t;
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }
}
