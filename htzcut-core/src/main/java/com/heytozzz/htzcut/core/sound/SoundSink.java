package com.heytozzz.htzcut.core.sound;

import java.util.UUID;

/**
 * Plays an already-registered sound (vanilla, another mod's, or HTZCut's
 * own bundled UI sounds) to a single player, by its namespaced id
 * (e.g. "minecraft:ui.toast.challenge_complete", "htzcut:ui.click").
 *
 * This is deliberately separate from AudioDeliveryChannel: that one is
 * for dynamic dialogue .ogg files routed through SVC/HTTP, while this is
 * for instant, already-registered SoundEvents played through vanilla's
 * own sound system - no transcoding, no routing decision.
 */
public interface SoundSink {

    void playSound(UUID playerId, String soundId);
}
