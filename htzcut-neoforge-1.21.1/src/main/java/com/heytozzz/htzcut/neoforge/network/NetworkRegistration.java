package com.heytozzz.htzcut.neoforge.network;

import com.heytozzz.htzcut.neoforge.client.ClientDialogueHandler;
import com.heytozzz.htzcut.neoforge.client.subtitle.ClientSubtitleHandler;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers the mod's playToClient payloads. Handler lambdas are guarded
 * by FMLEnvironment.dist.isClient(), so the client-only handler classes
 * (which reference client-only classes like Minecraft, GuiGraphics, and
 * javax.sound.sampled playback) are never classloaded on a dedicated
 * server - Java only resolves a class the first time it's actually
 * referenced at runtime, and that branch never executes there.
 */
public final class NetworkRegistration {

    private NetworkRegistration() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                PlayDialoguePayload.TYPE,
                PlayDialoguePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        ClientDialogueHandler.handle(payload);
                    }
                })
        );
        registrar.playToClient(
                ShowSubtitlePayload.TYPE,
                ShowSubtitlePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        ClientSubtitleHandler.handle(payload);
                    }
                })
        );
    }
}
