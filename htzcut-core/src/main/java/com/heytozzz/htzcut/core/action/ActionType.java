package com.heytozzz.htzcut.core.action;

/**
 * The kinds of things an event can do when it fires. An event can have
 * multiple actions of different types, executed in the order they're
 * declared in the YAML.
 */
public enum ActionType {
    /**
     * Plays an already-registered SoundEvent by its namespaced id
     * (vanilla, another mod's, or HTZCut's own bundled UI sounds once
     * those are registered). Not for dynamic dialogue audio - see DIALOGUE.
     */
    SOUND,

    /**
     * Sends a translatable narration message to the player.
     */
    NARRATION,

    /**
     * Plays a dynamic dialogue audio file (server-provided .ogg), routed
     * through Simple Voice Chat or the HTTP fallback.
     */
    DIALOGUE,

    /**
     * Puts the player in spectator mode and moves their camera through
     * a sequence of keyframes, then restores their original gamemode
     * and position.
     */
    CINEMATIC
}
