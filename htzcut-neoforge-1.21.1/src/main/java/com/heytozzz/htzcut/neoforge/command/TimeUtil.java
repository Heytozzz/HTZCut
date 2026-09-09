package com.heytozzz.htzcut.neoforge.command;

/**
 * Parses short duration strings used by the cinematic keyframe commands:
 * a number followed by a unit - "t" (ticks, 1/20 second), "s" (seconds),
 * or "m" (minutes). E.g. "20t", "1.5s", "2m".
 */
public final class TimeUtil {

    private static final double TICKS_PER_SECOND = 20.0;
    private static final double SECONDS_PER_MINUTE = 60.0;

    private TimeUtil() {
    }

    public static double parseSeconds(String input) {
        String trimmed = input.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Empty duration.");
        }

        char unit = trimmed.charAt(trimmed.length() - 1);
        String numberPart = trimmed.substring(0, trimmed.length() - 1);

        double value;
        try {
            value = Double.parseDouble(numberPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid duration '" + input + "' - expected a number followed by t/s/m, e.g. '20t', '1.5s', '2m'.");
        }

        if (value <= 0) {
            throw new IllegalArgumentException("Duration must be positive: '" + input + "'.");
        }

        return switch (unit) {
            case 't' -> value / TICKS_PER_SECOND;
            case 's' -> value;
            case 'm' -> value * SECONDS_PER_MINUTE;
            default -> throw new IllegalArgumentException(
                    "Invalid duration unit in '" + input + "' - use t (ticks), s (seconds), or m (minutes).");
        };
    }
}
