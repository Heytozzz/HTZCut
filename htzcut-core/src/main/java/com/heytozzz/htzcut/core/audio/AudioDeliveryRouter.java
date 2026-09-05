package com.heytozzz.htzcut.core.audio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Picks the best available AudioDeliveryChannel for a given player and
 * dispatches the audio through it. Channels are tried in order; the first
 * one that reports itself available for the player wins.
 *
 * In practice: [SimpleVoiceChatChannel, HttpCacheChannel] - SVC first,
 * HTTP as the always-available fallback.
 */
public class AudioDeliveryRouter {

    private final List<AudioDeliveryChannel> channelsInPriorityOrder;
    private final AudioAssetResolver assetResolver;

    public AudioDeliveryRouter(List<AudioDeliveryChannel> channelsInPriorityOrder,
                                AudioAssetResolver assetResolver) {
        this.channelsInPriorityOrder = channelsInPriorityOrder;
        this.assetResolver = assetResolver;
    }

    public void playDialogue(UUID playerId, String audioId) {
        Optional<ResolvedAudioAsset> asset = assetResolver.resolve(audioId);

        if (asset.isEmpty()) {
            // Asset not found or still being transcoded - caller should
            // have logged this already via the config validation step.
            return;
        }

        for (AudioDeliveryChannel channel : channelsInPriorityOrder) {
            if (channel.isAvailableFor(playerId)) {
                channel.deliver(playerId, asset.get());
                return;
            }
        }
        // No channel available at all (shouldn't happen once the HTTP
        // fallback is registered, since it should always report available).
    }
}
