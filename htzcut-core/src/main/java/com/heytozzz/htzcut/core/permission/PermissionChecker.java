package com.heytozzz.htzcut.core.permission;

import java.util.UUID;

/**
 * Abstraction over "does this player have this permission". Implementations
 * live in the neoforge module:
 *   - LuckPermsModChecker    (LuckPerms loaded as a NeoForge mod)
 *   - LuckPermsPluginChecker (LuckPerms loaded as a Bukkit plugin, hybrid servers)
 *   - VanillaOpChecker       (fallback, uses vanilla OP level)
 *
 * Which implementation is actually used is decided at runtime by
 * PermissionCheckerFactory, never assumed at compile time.
 */
public interface PermissionChecker {

    boolean hasPermission(UUID playerId, String permission);

    /**
     * A short identifier for logging/debugging, e.g. "luckperms-mod",
     * "luckperms-plugin", "vanilla-op".
     */
    String backendName();
}
