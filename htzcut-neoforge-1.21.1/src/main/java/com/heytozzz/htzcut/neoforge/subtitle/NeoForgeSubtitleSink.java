package com.heytozzz.htzcut.neoforge.subtitle;

import com.heytozzz.htzcut.core.subtitle.SubtitleBoxEffect;
import com.heytozzz.htzcut.core.subtitle.SubtitlePosition;
import com.heytozzz.htzcut.core.subtitle.SubtitleSink;
import com.heytozzz.htzcut.core.subtitle.SubtitleTextEffect;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.heytozzz.htzcut.neoforge.network.ShowSubtitlePayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Server-side SubtitleSink: converts the core's player-agnostic request
 * into a network payload sent to that specific player's client. Kept
 * completely separate from the audio delivery channels - a subtitle box
 * shows up the same way whether the dialogue audio comes through Simple
 * Voice Chat, the HTTP fallback, or fails to deliver at all.
 */
public class NeoForgeSubtitleSink implements SubtitleSink {

    private static final int TICKS_PER_SECOND = 20;

    private final MinecraftServer server;

    public NeoForgeSubtitleSink(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void showSubtitle(UUID playerId,
                              String text,
                              SubtitleBoxEffect boxEffect,
                              SubtitleTextEffect textEffect,
                              double textDurationSeconds,
                              double holdSeconds,
                              SubtitlePosition position) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            HTZLog.warn("Tried to show a subtitle to an offline player: " + playerId);
            return;
        }

        int textDurationTicks = toTicks(textDurationSeconds);
        int holdTicks = toTicks(holdSeconds);

        PacketDistributor.sendToPlayer(player, new ShowSubtitlePayload(
                text, boxEffect.name(), textEffect.name(), textDurationTicks, holdTicks, position.name()));
    }

    private static int toTicks(double seconds) {
        return Math.max(0, (int) Math.round(seconds * TICKS_PER_SECOND));
    }
}
