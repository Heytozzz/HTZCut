package com.heytozzz.htzcut.neoforge.network;

import com.heytozzz.htzcut.neoforge.HTZCutMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent server -> client to tell a player's client "show this subtitle".
 * Plain data only, same pattern as PlayDialoguePayload. Durations travel
 * as ticks rather than seconds since that's what the client-side
 * playback state (ClientSubtitleHandler) actually counts on. boxEffect,
 * textEffect and position travel as enum names (strings) so the client
 * can fall back gracefully on an unrecognized value instead of failing
 * to decode the whole payload.
 */
public record ShowSubtitlePayload(String text,
                                   String boxEffect,
                                   String textEffect,
                                   int textDurationTicks,
                                   int holdTicks,
                                   String position) implements CustomPacketPayload {

    public static final Type<ShowSubtitlePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HTZCutMod.MOD_ID, "show_subtitle"));

    public static final StreamCodec<ByteBuf, ShowSubtitlePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ShowSubtitlePayload::text,
            ByteBufCodecs.STRING_UTF8, ShowSubtitlePayload::boxEffect,
            ByteBufCodecs.STRING_UTF8, ShowSubtitlePayload::textEffect,
            ByteBufCodecs.VAR_INT, ShowSubtitlePayload::textDurationTicks,
            ByteBufCodecs.VAR_INT, ShowSubtitlePayload::holdTicks,
            ByteBufCodecs.STRING_UTF8, ShowSubtitlePayload::position,
            ShowSubtitlePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
