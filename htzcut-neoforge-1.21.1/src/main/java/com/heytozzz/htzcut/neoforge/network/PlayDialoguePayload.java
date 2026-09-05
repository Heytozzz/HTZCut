package com.heytozzz.htzcut.neoforge.network;

import com.heytozzz.htzcut.neoforge.HTZCutMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent server -> client to tell a player's client "go fetch and play this
 * dialogue audio". Contains only plain data (strings), safe to reference
 * on both sides - the actual download/playback logic lives client-side
 * only, in ClientDialogueHandler.
 */
public record PlayDialoguePayload(String audioId, String url) implements CustomPacketPayload {

    public static final Type<PlayDialoguePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HTZCutMod.MOD_ID, "play_dialogue"));

    public static final StreamCodec<ByteBuf, PlayDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PlayDialoguePayload::audioId,
            ByteBufCodecs.STRING_UTF8, PlayDialoguePayload::url,
            PlayDialoguePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
