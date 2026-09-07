package com.heytozzz.htzcut.neoforge.command;

import com.heytozzz.htzcut.neoforge.HTZCutMod;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * /htzcut reload - re-reads every event YAML from config/htzcut/events/
 * and rebuilds the dispatcher, without restarting the HTTP audio server
 * or re-detecting the permission backend.
 *
 * /htzcut editor - sends a clickable chat link that opens the web
 * editor, with the host/port already filled in.
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
                        .then(Commands.literal("editor")
                                .executes(context -> {
                                    int port = mod.webEditorPort();
                                    if (port <= 0) {
                                        context.getSource().sendFailure(
                                                Component.literal("[HTZCut] The web editor isn't running yet."));
                                        return 0;
                                    }

                                    String host = resolveHost(context.getSource().getServer());
                                    String url = "http://" + host + ":" + port;

                                    context.getSource().sendSuccess(() -> clickableLink(url), false);
                                    return 1;
                                })
                        )
        );
    }

    /**
     * Builds "[HTZCut] Click to open the web editor: <url>" with the URL
     * itself clickable (opens in the default browser) and hoverable.
     */
    private static Component clickableLink(String url) {
        Component link = Component.literal(url)
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT, Component.literal("Abrir " + url))));

        return Component.literal("[HTZCut] Click to open the web editor: ").append(link);
    }

    /**
     * Prefers the "server-ip" property from server.properties when it's
     * been explicitly set (a bound/public address the admin already
     * confirmed works); falls back to this machine's own local address,
     * and finally to "localhost" if even that lookup fails. Admins
     * running behind NAT/port-forwarding with no server-ip set may need
     * to swap the host in the link for their real public address.
     */
    private static String resolveHost(MinecraftServer server) {
        String configured = server.getServerIp();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }
}
