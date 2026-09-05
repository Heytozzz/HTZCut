package com.heytozzz.htzcut.neoforge.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin wrapper around slf4j so every log line is consistently tagged,
 * regardless of which class logs it.
 */
public final class HTZLog {

    private static final Logger LOGGER = LoggerFactory.getLogger("HTZCut");

    private HTZLog() {
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message) {
        LOGGER.warn(message);
    }

    public static void error(String message, Throwable t) {
        LOGGER.error(message, t);
    }
}
