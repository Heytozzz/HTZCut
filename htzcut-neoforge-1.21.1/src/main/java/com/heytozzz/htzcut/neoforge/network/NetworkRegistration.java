package com.heytozzz.htzcut.neoforge.network;

import com.heytozzz.htzcut.neoforge.client.ClientDialogueHandler;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers PlayDialoguePayload on the mod's network channel. The
 * handler lambda is guarded by FMLEnvironment.dist.isClient(), so
 * ClientDialogueHandler (which references client-only classes like
 * Minecraft and javax.sound.sampled playback) is never classloaded on a
 * dedicated server - Java only resolves a class the first time it's
 * actually referenced at runtime, and that branch never executes there.
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
    }
}
