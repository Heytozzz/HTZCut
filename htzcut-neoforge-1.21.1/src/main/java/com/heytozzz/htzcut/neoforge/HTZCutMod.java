package com.heytozzz.htzcut.neoforge;

import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.heytozzz.htzcut.neoforge.permission.PermissionCheckerFactory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(HTZCutMod.MOD_ID)
public class HTZCutMod {

    public static final String MOD_ID = "htzcut";

    public HTZCutMod(IEventBus modEventBus) {
        HTZLog.info("Initializing HTZCut...");

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Permission backend detection happens lazily and defensively -
            // see PermissionCheckerFactory for why this can't be resolved
            // at class-load time.
            var permissionChecker = PermissionCheckerFactory.detect();
            HTZLog.info("Permission backend in use: " + permissionChecker.backendName());

            // AudioWatcherService, AudioAssetResolver, EventDispatcher and
            // the AudioDeliveryChannel implementations are wired up here in
            // the next implementation stage - kept out of the skeleton.
        });
    }
}
