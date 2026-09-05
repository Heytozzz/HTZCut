package com.heytozzz.htzcut.core.audio;

/**
 * Whether an audio is a dynamic dialogue (server-provided, routed through
 * SVC/HTTP) or a static UI/HUD sound (bundled in the jar, played through
 * vanilla's sound system). This is inferred from which folder the file
 * lives in - never something the user declares in YAML.
 */
public enum AudioCategory {
    DIALOGUE,
    UI
}
