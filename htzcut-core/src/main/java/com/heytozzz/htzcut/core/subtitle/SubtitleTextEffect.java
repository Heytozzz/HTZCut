package com.heytozzz.htzcut.core.subtitle;

/**
 * How the subtitle's text reveals itself once the box has finished
 * appearing. TYPEWRITER_LETTERS/TYPEWRITER_WORDS reveal progressively
 * over subtitleTextDurationSeconds; the other effects show the full
 * text immediately but animate its entrance (fade/bounce/slide) over
 * that same duration.
 */
public enum SubtitleTextEffect {
    INSTANT,
    FADE_IN,
    BOUNCE,
    SLIDE_TOP,
    SLIDE_BOTTOM,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    TYPEWRITER_LETTERS,
    TYPEWRITER_WORDS
}
