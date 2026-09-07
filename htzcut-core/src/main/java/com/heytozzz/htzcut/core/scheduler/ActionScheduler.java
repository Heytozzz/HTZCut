package com.heytozzz.htzcut.core.scheduler;

/**
 * Runs a task after a delay, in whatever time unit the loader module's
 * game loop actually ticks on (server ticks for NeoForge). Backs the
 * per-action "delay_seconds" field: EventDispatcher schedules each
 * action's execution through this instead of running the whole action
 * list synchronously, so events can stagger their actions over time.
 *
 * Implementations should run delaySeconds <= 0 (or very close to it)
 * immediately/synchronously rather than deferring by even one tick, so
 * events with no delays behave exactly as before this existed.
 */
public interface ActionScheduler {

    void schedule(double delaySeconds, Runnable task);
}
