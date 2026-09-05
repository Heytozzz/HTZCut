package com.heytozzz.htzcut.neoforge.command;

import com.heytozzz.htzcut.neoforge.HTZCutMod;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /htzcut reload - re-reads every event YAML from config/htzcut/events/
 * and rebuilds the dispatcher, without restarting the HTTP audio server
 * or re-detecting the permission backend.
 *
 * Gated behind vanilla OP level 2 for now. A LuckPerms-aware permission
 * node (e.g. "htzcut.command.reload") is a natural follow-up once the
 * real LuckPerms integration lands - PermissionCheckerFactory already
 * exists for that, this command just doesn't consult it yet.
 */
public final class HTZCommand {

    private HTZCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, HTZCutMod mod) {
        dispatcher.register(
                Commands.literal("htzcut")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    int count = mod.rebuildDispatcher();
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("[HTZCut] Reloaded " + count + " event definition(s)."),
                                            true
                                    );
                                    HTZLog.info("Event definitions reloaded via /htzcut reload (" + count + " event(s)).");
                                    return count;
                                })
                        )
        );
    }
}
