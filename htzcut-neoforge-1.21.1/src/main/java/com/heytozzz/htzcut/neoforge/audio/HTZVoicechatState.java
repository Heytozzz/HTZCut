package com.heytozzz.htzcut.neoforge.audio;

import de.maxhenkel.voicechat.api.VoicechatServerApi;

/**
 * Holds the VoicechatServerApi instance once Simple Voice Chat hands it
 * to HTZVoicechatPlugin via the VoicechatServerStartedEvent. Anything
 * that needs to talk to SVC (SimpleVoiceChatDeliveryChannel) reads it
 * from here rather than trying to obtain it independently.
 *
 * Stays null for the entire server lifetime if SVC isn't installed -
 * every reader must treat that as "SVC unavailable", not an error.
 */
public final class HTZVoicechatState {

    private static volatile VoicechatServerApi api;

    private HTZVoicechatState() {
    }

    public static void set(VoicechatServerApi serverApi) {
        api = serverApi;
    }

    public static VoicechatServerApi get() {
        return api;
    }
}
