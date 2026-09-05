package com.heytozzz.htzcut.neoforge.narration;

import com.heytozzz.htzcut.core.narration.NarrationSink;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Sends narration as a translatable chat message. Minecraft's own
 * translation system already falls back to the client's default
 * language when a key is missing, which covers the common case;
 * honoring a per-event fallbackLocale explicitly (rather than the
 * client's own fallback) is a refinement for once the localization
 * pipeline exists.
 */
public class ChatNarrationSink implements NarrationSink {

    private final MinecraftServer server;

    public ChatNarrationSink(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void sendNarration(UUID playerId, String textKey, String fallbackLocale) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            HTZLog.warn("Tried to send narration to an offline player: " + playerId);
            return;
        }
        player.sendSystemMessage(Component.translatable(textKey));
    }
}
