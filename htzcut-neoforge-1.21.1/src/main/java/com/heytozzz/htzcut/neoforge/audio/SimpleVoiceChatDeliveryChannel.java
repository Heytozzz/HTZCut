package com.heytozzz.htzcut.neoforge.audio;

import com.heytozzz.htzcut.core.audio.AudioDeliveryChannel;
import com.heytozzz.htzcut.core.audio.ResolvedAudioAsset;
import com.heytozzz.htzcut.neoforge.init.HTZLog;

import java.util.UUID;

/**
 * Delivers dialogue audio through Simple Voice Chat's audio channel API,
 * using pre-transcoded opus frames (see ResolvedAudioAsset#opusFramesFile).
 *
 * Only ever constructed after SimpleVoiceChatSupport confirms the mod is
 * loaded - references to de.maxhenkel.voicechat.api classes must stay
 * confined to this package.
 *
 * Full implementation (StaticAudioChannel / EntityAudioChannel creation,
 * per-player connection lookup, frame pacing) is scheduled for the audio
 * delivery implementation stage.
 */
public class SimpleVoiceChatDeliveryChannel implements AudioDeliveryChannel {

    @Override
    public boolean isAvailableFor(UUID playerId) {
        if (!SimpleVoiceChatSupport.isAvailable()) {
            return false;
        }
        // TODO: VoicechatServerApi#getConnectionOf(playerId) != null
        return false;
    }

    @Override
    public void deliver(UUID playerId, ResolvedAudioAsset asset) {
        if (asset.opusFramesFile().isEmpty()) {
            HTZLog.warn("Asset " + asset.audioId() + " has no opus frames cached yet, skipping SVC delivery.");
            return;
        }
        // TODO: stream asset.opusFramesFile() frame-by-frame through
        // AudioChannel#send(byte[]).
    }
}
