package com.heytozzz.htzcut.neoforge.permission;

import com.heytozzz.htzcut.core.permission.PermissionChecker;

import java.util.UUID;

/**
 * Last-resort permission backend, used when neither LuckPerms mod nor
 * LuckPerms plugin is available. Real implementation (wiring to the
 * server's player list / OP level) is filled in during the permission
 * implementation stage.
 */
public class VanillaOpChecker implements PermissionChecker {

    @Override
    public boolean hasPermission(UUID playerId, String permission) {
        // TODO: resolve against MinecraftServer#getPlayerList().isOp(...)
        // once the server instance is threaded through here.
        return false;
    }

    @Override
    public String backendName() {
        return "vanilla-op";
    }
}
