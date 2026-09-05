package com.heytozzz.htzcut.neoforge.init;

import com.heytozzz.htzcut.core.event.EventDispatcher;

/**
 * Holds the currently active EventDispatcher so game event listeners
 * (registered early, in the mod constructor) can reach it once it's
 * built during ServerStartingEvent.
 */
public final class HTZRuntime {

    private static volatile EventDispatcher dispatcher;

    private HTZRuntime() {
    }

    public static void set(EventDispatcher newDispatcher) {
        dispatcher = newDispatcher;
    }

    public static EventDispatcher get() {
        return dispatcher;
    }
}
