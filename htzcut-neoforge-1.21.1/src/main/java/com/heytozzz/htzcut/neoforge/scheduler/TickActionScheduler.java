package com.heytozzz.htzcut.neoforge.scheduler;

import com.heytozzz.htzcut.core.scheduler.ActionScheduler;
import com.heytozzz.htzcut.neoforge.init.HTZLog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Server-side ActionScheduler backed by the server tick loop rather than
 * a separate thread - delayed actions end up running on the server
 * thread exactly like an immediate one would, so sinks (SubtitleSink,
 * AudioDeliveryRouter, etc.) don't need to worry about cross-thread
 * access to server/player state.
 *
 * A zero-or-negative delay runs the task immediately, synchronously,
 * inside schedule() itself - so events with no delays behave exactly as
 * they did before this existed, with no extra tick of latency.
 *
 * Not thread-safe beyond what a single-threaded server tick loop
 * already guarantees - schedule() and tick() are only ever called from
 * the server thread (game event handlers and ServerTickEvent both run
 * there).
 */
public final class TickActionScheduler implements ActionScheduler {

    private static final int TICKS_PER_SECOND = 20;

    private final PriorityQueue<ScheduledTask> queue =
            new PriorityQueue<>(Comparator.comparingLong(t -> t.executeAtTick));
    private long currentTick;

    @Override
    public void schedule(double delaySeconds, Runnable task) {
        int delayTicks = (int) Math.round(Math.max(0, delaySeconds) * TICKS_PER_SECOND);
        if (delayTicks <= 0) {
            task.run();
            return;
        }
        queue.add(new ScheduledTask(currentTick + delayTicks, task));
    }

    /** Called once per server tick to run whatever is due. */
    public void tick() {
        currentTick++;

        List<Runnable> due = new ArrayList<>();
        while (!queue.isEmpty() && queue.peek().executeAtTick <= currentTick) {
            due.add(queue.poll().task);
        }

        for (Runnable task : due) {
            try {
                task.run();
            } catch (Exception e) {
                HTZLog.error("A delayed HTZCut action failed to run", e);
            }
        }
    }

    /** Drops every pending task - called on server stop so nothing fires into a stale/next server instance. */
    public void clear() {
        queue.clear();
    }

    private record ScheduledTask(long executeAtTick, Runnable task) {
    }
}
