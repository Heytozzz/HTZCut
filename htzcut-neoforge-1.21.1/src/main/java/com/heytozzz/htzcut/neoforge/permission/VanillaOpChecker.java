package com.heytozzz.htzcut.neoforge.permission;

import com.heytozzz.htzcut.core.permission.PermissionChecker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Last-resort permission backend, used when neither LuckPerms mod nor
 * LuckPerms plugin is available.
 *
 * Vanilla has no concept of individual permission nodes - the closest
 * equivalent is simply "is this player a server operator". Every
 * htzcut.* node (including htzcut.ignore.&lt;event&gt; for the /htzcut
 * play bypass) resolves the same way here: true for ops, false
 * otherwise. LuckPerms backends give real per-node granularity instead.
 *
 * Only resolves online players - every current caller (event conditions,
 * /htzcut play's bypass check) only ever checks a player who is either
 * the one triggering an organic event or an online target selected via
 * a player selector, so offline-profile lookup isn't needed here.
 */
public class VanillaOpChecker implements PermissionChecker {

    private final MinecraftServer server;

    public VanillaOpChecker(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public boolean hasPermission(UUID playerId, String permission) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return false;
        }
        return server.getPlayerList().isOp(player.getGameProfile());
    }

    @Override
    public String backendName() {
        return "vanilla-op";
    }
}
