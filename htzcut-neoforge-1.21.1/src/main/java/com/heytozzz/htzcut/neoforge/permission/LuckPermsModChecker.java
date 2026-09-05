package com.heytozzz.htzcut.neoforge.permission;

import com.heytozzz.htzcut.core.permission.PermissionChecker;
import com.heytozzz.htzcut.neoforge.init.HTZLog;

import java.util.UUID;

/**
 * LuckPerms accessed via its mod API (LuckPermsProvider, from
 * net.luckperms.api). Only ever instantiated after
 * PermissionCheckerFactory has confirmed the mod is actually loaded -
 * never reference net.luckperms.api classes anywhere else, or class
 * loading will explode when LuckPerms isn't present.
 */
public class LuckPermsModChecker implements PermissionChecker {

    @Override
    public boolean hasPermission(UUID playerId, String permission) {
        try {
            // TODO: real lookup via LuckPermsProvider.get()
            //   .getUserManager().getUser(playerId)
            //   .getCachedData().getPermissionData().checkPermission(permission)
            return false;
        } catch (Throwable t) {
            HTZLog.error("LuckPerms (mod) permission lookup failed, denying by default", t);
            return false;
        }
    }

    @Override
    public String backendName() {
        return "luckperms-mod";
    }
}
