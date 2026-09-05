package com.heytozzz.htzcut.neoforge.audio;

import com.heytozzz.htzcut.core.audio.AudioDeliveryChannel;
import com.heytozzz.htzcut.core.audio.ResolvedAudioAsset;
import com.heytozzz.htzcut.neoforge.config.HttpServerConfig;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.heytozzz.htzcut.neoforge.network.PlayDialoguePayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Fallback delivery channel: the embedded HTTP endpoint serves the .ogg
 * file (asset.httpReadyFile()) and the client downloads + caches it
 * locally before playing it via javax.sound.sampled (see
 * ClientDialogueHandler).
 *
 * Always reports itself as available - this is the guaranteed last
 * resort so every player receives dialogue audio regardless of whether
 * they have Simple Voice Chat installed.
 */
public class HttpCacheDeliveryChannel implements AudioDeliveryChannel {

    private final MinecraftServer server;
    private final HttpServerConfig httpConfig;

    public HttpCacheDeliveryChannel(MinecraftServer server, HttpServerConfig httpConfig) {
        this.server = server;
        this.httpConfig = httpConfig;
    }

    @Override
    public boolean isAvailableFor(UUID playerId) {
        return true;
    }

    @Override
    public void deliver(UUID playerId, ResolvedAudioAsset asset) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            HTZLog.warn("Tried to deliver dialogue audio to an offline player: " + playerId);
            return;
        }

        String url = "http://" + httpConfig.publicHost() + ":" + httpConfig.port() + "/audio/" + asset.audioId();
        PacketDistributor.sendToPlayer(player, new PlayDialoguePayload(asset.audioId(), url));
    }
}
