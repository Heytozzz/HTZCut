package com.heytozzz.htzcut.core.subtitle;

/**
 * Where the dialogue subtitle box should be anchored on the player's
 * screen. Kept as a small, fixed set (rather than raw x/y) so it stays
 * simple to author in YAML and trivial to lay out on any screen size -
 * a loader module is free to map these onto whatever pixel offsets fit
 * its HUD.
 */
public enum SubtitlePosition {
    BOTTOM,
    TOP
}
