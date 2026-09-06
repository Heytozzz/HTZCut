package com.heytozzz.htzcut.core.subtitle;

/**
 * How the subtitle box itself animates onto the screen before its text
 * starts appearing. The box always finishes this animation (a fixed,
 * short duration owned by the renderer) before the text effect begins -
 * see SubtitleTextEffect for the text's own reveal.
 */
public enum SubtitleBoxEffect {
    INSTANT,
    FADE_IN,
    BOUNCE,
    SLIDE_TOP,
    SLIDE_BOTTOM,
    SLIDE_LEFT,
    SLIDE_RIGHT
}
