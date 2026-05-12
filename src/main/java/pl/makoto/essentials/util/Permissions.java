package pl.makoto.essentials.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.MKTEssentials;

public class Permissions {
    private static boolean luckPermsAvailable = false;
    private static LuckPerms luckPerms = null;

    public static void init() {
        try {
            luckPerms = LuckPermsProvider.get();
            luckPermsAvailable = true;
            MKTEssentials.LOGGER.info("Permissions: LuckPerms API hooked successfully.");
        } catch (NoClassDefFoundError | IllegalStateException e) {
            luckPermsAvailable = false;
        }
    }

    public static boolean hasPermission(ServerPlayer player, String node, int fallbackLevel) {
        if (player == null) return false;

        if (luckPermsAvailable) {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUUID());
                if (user != null) {
                    CachedPermissionData permissionData = user.getCachedData().getPermissionData();
                    Tristate result = permissionData.checkPermission(node);
                    if (result == Tristate.TRUE) return true;
                    if (result == Tristate.FALSE) return false;
                    // UNDEFINED falls through to vanilla check
                }
            } catch (NoClassDefFoundError e) {
                luckPermsAvailable = false;
            }
        }

        // Vanilla check (OP level)
        return player.hasPermissions(fallbackLevel);
    }

    public static boolean hasPermission(CommandSourceStack source, String node, int fallbackLevel) {
        if (source.getPlayer() != null) {
            return hasPermission(source.getPlayer(), node, fallbackLevel);
        }
        return source.hasPermission(fallbackLevel);
    }

    public static int getIntPermission(ServerPlayer player, String node, int defaultValue) {
        if (player == null) return defaultValue;

        if (luckPermsAvailable) {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUUID());
                if (user != null) {
                    CachedMetaData metaData = user.getCachedData().getMetaData();
                    String value = metaData.getMetaValue(node);
                    if (value != null) {
                        try {
                            return Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            // Not parseable as int, fall through to default
                        }
                    }
                }
            } catch (NoClassDefFoundError e) {
                luckPermsAvailable = false;
            }
        }

        return defaultValue;
    }
}
