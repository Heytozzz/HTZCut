package com.heytozzz.htzcut.neoforge.trigger;

import com.heytozzz.htzcut.core.event.EventDispatcher;
import com.heytozzz.htzcut.core.trigger.HTZTriggerFired;
import com.heytozzz.htzcut.core.trigger.TriggerType;
import com.heytozzz.htzcut.neoforge.init.HTZRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

/**
 * The only place in the codebase that knows about both real NeoForge
 * game events and HTZCut's internal, loader-agnostic trigger model.
 * Registered on NeoForge.EVENT_BUS from the mod constructor; each
 * handler just translates the event into an HTZTriggerFired and hands
 * it to whatever EventDispatcher is currently active.
 */
public class GameTriggerListeners {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (FirstJoinTracker.isFirstJoin(player.getServer(), player.getUUID())) {
            // Event YAML for a FIRST_JOIN trigger should use this exact
            // sentinel as its trigger.value, since there's no natural
            // per-player identifier to match against otherwise.
            dispatch(player.getUUID(), TriggerType.FIRST_JOIN, "first_join");
        }
    }

    @SubscribeEvent
    public void onAdvancementEarned(AdvancementEvent.AdvancementEarnedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String advancementId = event.getAdvancement().id().toString();
        dispatch(player.getUUID(), TriggerType.ADVANCEMENT, advancementId);
    }

    @SubscribeEvent
    public void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String dimensionId = event.getTo().location().toString();
        dispatch(player.getUUID(), TriggerType.DIMENSION_ENTER, dimensionId);
    }

    @SubscribeEvent
    public void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(event.getStack().getItem()).toString();
        dispatch(player.getUUID(), TriggerType.ITEM_PICKUP, itemId);
    }

    private void dispatch(UUID playerId, TriggerType type, String value) {
        EventDispatcher dispatcher = HTZRuntime.get();
        if (dispatcher == null) {
            // Server hasn't finished starting yet - shouldn't normally
            // happen since players can't connect before ServerStartingEvent.
            return;
        }
        dispatcher.onTrigger(new HTZTriggerFired(playerId, type, value));
    }
}
