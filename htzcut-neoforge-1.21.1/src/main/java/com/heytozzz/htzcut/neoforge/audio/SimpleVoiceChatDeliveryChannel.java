package com.heytozzz.htzcut.neoforge.audio;

import com.heytozzz.htzcut.core.audio.AudioDeliveryChannel;
import com.heytozzz.htzcut.core.audio.ResolvedAudioAsset;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Delivers dialogue audio through Simple Voice Chat's audio channel API.
 *
 * Follows the exact pattern from SVC's own documented examples: a
 * per-delivery StaticAudioChannel (heard only by the target player, like
 * a group/whisper channel) fed through the high-level AudioPlayer, which
 * takes a plain 48kHz/16-bit/mono short[] and handles pacing/sending
 * packets itself - no manual Opus framing needed on our side.
 *
 * Only ever constructed and actually used after SimpleVoiceChatSupport
 * confirms the mod is loaded and HTZVoicechatState has a live API
 * reference - both checked in isAvailableFor() before any de.maxhenkel
 * class is touched, so this stays safe when SVC is absent.
 */
public class SimpleVoiceChatDeliveryChannel implements AudioDeliveryChannel {

    private final MinecraftServer server;

    public SimpleVoiceChatDeliveryChannel(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public boolean isAvailableFor(UUID playerId) {
        if (!SimpleVoiceChatSupport.isAvailable()) {
            return false;
        }

        VoicechatServerApi api = HTZVoicechatState.get();
        if (api == null) {
            return false;
        }

        try {
            return api.getConnectionOf(playerId) != null;
        } catch (Throwable t) {
            HTZLog.error("Simple Voice Chat availability check failed, falling back to HTTP delivery", t);
            return false;
        }
    }

    @Override
    public void deliver(UUID playerId, ResolvedAudioAsset asset) {
        try {
            VoicechatServerApi api = HTZVoicechatState.get();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (api == null || player == null) {
                return;
            }

            VoicechatConnection connection = api.getConnectionOf(playerId);
            if (connection == null) {
                return;
            }

            short[] pcm = AudioDecoder.decodeToMonoPcm48k(asset.sourceFile());

            UUID channelId = UUID.randomUUID();
            StaticAudioChannel channel = api.createStaticAudioChannel(
                    channelId, api.fromServerLevel(player.serverLevel()), connection);
            if (channel == null) {
                HTZLog.warn("Simple Voice Chat did not create an audio channel for " + playerId
                        + " (player may have disconnected). Falling back is not automatic here - "
                        + "the next dialogue event will retry via isAvailableFor().");
                return;
            }

            AudioPlayer audioPlayer = api.createAudioPlayer(channel, api.createEncoder(), pcm);
            audioPlayer.startPlaying();
        } catch (Throwable t) {
            HTZLog.error("Failed to deliver dialogue audio '" + asset.audioId()
                    + "' via Simple Voice Chat to " + playerId, t);
        }
    }
}
