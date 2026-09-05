package com.heytozzz.htzcut.neoforge.permission;

import com.heytozzz.htzcut.core.permission.PermissionChecker;
import com.heytozzz.htzcut.neoforge.init.HTZLog;

import java.util.UUID;

/**
 * LuckPerms accessed via Bukkit's RegisteredServiceProvider, for hybrid
 * servers (Mohist/Arclight/etc.) where LuckPerms runs as a Bukkit plugin
 * rather than a NeoForge mod. Only instantiated after
 * PermissionCheckerFactory has confirmed a Bukkit environment with the
 * service actually registered.
 */
public class LuckPermsPluginChecker implements PermissionChecker {

    @Override
    public boolean hasPermission(UUID playerId, String permission) {
        try {
            // TODO: real lookup via
            //   Bukkit.getServicesManager().getRegistration(LuckPerms.class)
            //     .getProvider().getUserManager().getUser(playerId)
            //     .getCachedData().getPermissionData().checkPermission(permission)
            return false;
        } catch (Throwable t) {
            HTZLog.error("LuckPerms (plugin) permission lookup failed, denying by default", t);
            return false;
        }
    }

    @Override
    public String backendName() {
        return "luckperms-plugin";
    }
}
