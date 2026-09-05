package com.heytozzz.htzcut.core.trigger;

import java.util.UUID;

/**
 * Loader-agnostic representation of "something happened that might fire an
 * HTZCut event". The neoforge module translates real game events
 * (AdvancementEvent, PlayerChangedDimensionEvent, etc.) into instances of
 * this class and hands them to the core dispatcher.
 *
 * @param playerId the player the trigger happened to
 * @param type     the kind of trigger
 * @param value    the specific identifier for this trigger
 *                 (e.g. "minecraft:end/kill_dragon", "minecraft:the_end",
 *                 an item id, or a custom event id)
 */
public record HTZTriggerFired(UUID playerId, TriggerType type, String value) {
}
