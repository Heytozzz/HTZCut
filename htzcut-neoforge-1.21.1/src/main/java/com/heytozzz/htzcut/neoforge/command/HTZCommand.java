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
import net.minecraft.server.dedicated.DedicatedServer;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
     * confirmed works); otherwise scans this machine's own network
     * interfaces for a usable address. Admins running behind
     * NAT/port-forwarding with no server-ip set may need to swap the
     * host in the link for their real public address.
     *
     * getServerIp() only exists on DedicatedServer (it comes from the
     * ServerInterface that DedicatedServer implements, not from
     * MinecraftServer itself) - so the integrated/singleplayer server
     * just always falls through to the interface scan below.
     */
    private static String resolveHost(MinecraftServer server) {
        if (server instanceof DedicatedServer dedicated) {
            String configured = dedicated.getServerIp();
            if (configured != null && !configured.isBlank()) {
                return formatHost(configured);
            }
        }
        String detected = detectLocalAddress();
        return detected != null ? detected : "localhost";
    }

    /**
     * Walks live, non-loopback, non-virtual network interfaces looking
     * for a usable address, preferring IPv4 - InetAddress.getLocalHost()
     * is what this replaces, since on machines with multiple interfaces
     * (common on VPS/dedicated hosts) it can just as easily hand back a
     * link-local IPv6 address (fe80::...%scope) that's meaningless
     * outside that one host, instead of the actual reachable IP.
     *
     * Link-local, loopback, and multicast addresses are skipped
     * entirely on both passes - they're never a useful "open this in
     * your browser" target.
     */
    private static String detectLocalAddress() {
        List<InetAddress> candidates = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!isUsable(iface)) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isMulticastAddress()) {
                        continue;
                    }
                    candidates.add(address);
                }
            }
        } catch (SocketException e) {
            HTZLog.warn("Failed to enumerate network interfaces for /htzcut editor: " + e.getMessage());
            return null;
        }

        for (InetAddress address : candidates) {
            if (address instanceof Inet4Address) {
                return address.getHostAddress();
            }
        }
        for (InetAddress address : candidates) {
            if (address instanceof Inet6Address) {
                return formatHost(address.getHostAddress());
            }
        }
        return null;
    }

    private static boolean isUsable(NetworkInterface iface) {
        try {
            return iface.isUp() && !iface.isLoopback() && !iface.isVirtual();
        } catch (SocketException e) {
            return false;
        }
    }

    /**
     * Wraps a raw IPv6 literal in brackets (required inside a URL) and
     * drops any zone index (the "%eth0"/"%11" suffix some platforms
     * attach) - that suffix is only meaningful on the machine that
     * printed it and would just break the link for anyone else.
     */
    private static String formatHost(String host) {
        String withoutZone = host.split("%", 2)[0];
        return withoutZone.contains(":") ? "[" + withoutZone + "]" : withoutZone;
    }
}
