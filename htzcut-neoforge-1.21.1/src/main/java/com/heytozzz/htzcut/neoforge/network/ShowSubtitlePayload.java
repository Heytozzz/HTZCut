package com.heytozzz.htzcut.neoforge.network;

import com.heytozzz.htzcut.neoforge.HTZCutMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent server -> client to tell a player's client "show this subtitle
 * box for this many ticks, anchored at this position". Plain data only,
 * same pattern as PlayDialoguePayload - duration travels as ticks
 * rather than seconds since that's what the client-side countdown
 * (ClientSubtitleHandler) actually runs on.
 */
public record ShowSubtitlePayload(String text, int durationTicks, String position) implements CustomPacketPayload {

    public static final Type<ShowSubtitlePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HTZCutMod.MOD_ID, "show_subtitle"));

    public static final StreamCodec<ByteBuf, ShowSubtitlePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ShowSubtitlePayload::text,
            ByteBufCodecs.VAR_INT, ShowSubtitlePayload::durationTicks,
            ByteBufCodecs.STRING_UTF8, ShowSubtitlePayload::position,
            ShowSubtitlePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
