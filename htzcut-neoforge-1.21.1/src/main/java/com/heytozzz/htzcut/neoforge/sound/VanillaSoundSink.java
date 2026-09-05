package com.heytozzz.htzcut.neoforge.sound;

import com.heytozzz.htzcut.core.sound.SoundSink;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.UUID;

/**
 * Plays any already-registered SoundEvent - vanilla, another mod's, or
 * HTZCut's own bundled UI sounds once those are registered - identified
 * by its full namespaced id (e.g. "minecraft:ui.toast.challenge_complete",
 * "htzcut:ui.click").
 *
 * Uses Player#playNotifySound, which only the target player hears - the
 * same mechanism vanilla uses for UI feedback sounds (XP pickup, chest
 * close, etc.), which is the right behavior for this action type.
 */
public class VanillaSoundSink implements SoundSink {

    private final MinecraftServer server;

    public VanillaSoundSink(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void playSound(UUID playerId, String soundId) {
        if (soundId == null) {
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            HTZLog.warn("Tried to play sound '" + soundId + "' to an offline player: " + playerId);
            return;
        }

        ResourceLocation location = ResourceLocation.tryParse(soundId);
        if (location == null) {
            HTZLog.warn("Invalid sound id in event config: '" + soundId + "'");
            return;
        }

        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(location);
        if (soundEvent == null) {
            HTZLog.warn("Sound '" + soundId + "' is not registered (typo, or its mod/resource pack isn't loaded).");
            return;
        }

        player.playNotifySound(soundEvent, SoundSource.MASTER, 1.0F, 1.0F);
    }
}
