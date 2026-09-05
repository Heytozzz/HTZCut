package com.heytozzz.htzcut.core.trigger;

/**
 * All the trigger kinds HTZCut understands, independent of how each
 * loader/version actually detects them.
 */
public enum TriggerType {
    ADVANCEMENT,
    ITEM_PICKUP,
    DIMENSION_ENTER,
    FIRST_JOIN,
    CUSTOM
}
