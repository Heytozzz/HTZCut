package com.heytozzz.htzcut.neoforge.permission;

import com.heytozzz.htzcut.core.permission.PermissionChecker;
import com.heytozzz.htzcut.neoforge.init.HTZLog;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;

/**
 * Decides, at runtime, which PermissionChecker implementation to use.
 *
 * On a hybrid server both a NeoForge-mod LuckPerms and a Bukkit-plugin
 * LuckPerms could theoretically be present; "preferred_backend" in the
 * global config (plugin | mod | auto) will let the user pick which one
 * wins. For the skeleton stage this always resolves in "auto" order:
 * plugin -> mod -> vanilla OP.
 *
 * Every check below is wrapped defensively: referencing LuckPerms or
 * Bukkit classes must never throw NoClassDefFoundError and crash startup
 * just because one of the two environments isn't present.
 */
public final class PermissionCheckerFactory {

    private PermissionCheckerFactory() {
    }

    public static PermissionChecker detect(MinecraftServer server) {
        if (isLuckPermsPluginAvailable()) {
            HTZLog.info("Detected LuckPerms as a Bukkit plugin (hybrid server).");
            return new LuckPermsPluginChecker();
        }

        if (isLuckPermsModAvailable()) {
            HTZLog.info("Detected LuckPerms as a NeoForge mod.");
            return new LuckPermsModChecker();
        }

        HTZLog.info("No LuckPerms backend found, falling back to vanilla OP permissions.");
        return new VanillaOpChecker(server);
    }

    private static boolean isLuckPermsModAvailable() {
        try {
            return ModList.get().isLoaded("luckperms");
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isLuckPermsPluginAvailable() {
        try {
            // Only reachable on hybrid servers where a Bukkit classloader
            // and org.bukkit.Bukkit are actually present. Class.forName is
            // used instead of a direct import so a pure NeoForge server
            // (no Bukkit at all) never even attempts to link this class.
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");

            Object servicesManager = bukkitClass.getMethod("getServicesManager").invoke(null);
            Object registration = servicesManager.getClass()
                    .getMethod("getRegistration", Class.class)
                    .invoke(servicesManager, luckPermsClass);

            return registration != null;
        } catch (Throwable t) {
            // Any failure here (class not found, no Bukkit environment,
            // service not registered) simply means "not available".
            return false;
        }
    }
}
