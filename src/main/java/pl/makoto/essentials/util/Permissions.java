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

    /**
     * Resolves a numeric limit from dynamic permission nodes of the form {@code <prefix>.<number>},
     * EssentialsX-style. Scans every permission granted to the player and returns the highest number
     * found. {@code <prefix>.*} or {@code <prefix>.unlimited} grant {@link Integer#MAX_VALUE}.
     * Falls back to {@code defaultValue} when LuckPerms is absent or no matching node is granted.
     *
     * <p>Example: granting {@code mktessentials.homes.10} to a rank gives that rank 10 homes.</p>
     */
    public static int getMaxNumberPermission(ServerPlayer player, String prefix, int defaultValue) {
        if (player == null) return defaultValue;

        if (luckPermsAvailable) {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUUID());
                if (user != null) {
                    CachedPermissionData permissionData = user.getCachedData().getPermissionData();
                    int max = Integer.MIN_VALUE;
                    boolean found = false;
                    String dotted = prefix + ".";

                    for (java.util.Map.Entry<String, Boolean> entry : permissionData.getPermissionMap().entrySet()) {
                        if (!entry.getValue()) continue; // only granted (true) nodes
                        String node = entry.getKey();
                        if (!node.startsWith(dotted)) continue;

                        String suffix = node.substring(dotted.length());
                        if (suffix.equals("*") || suffix.equalsIgnoreCase("unlimited")) {
                            return Integer.MAX_VALUE;
                        }
                        try {
                            int n = Integer.parseInt(suffix);
                            if (n > max) {
                                max = n;
                                found = true;
                            }
                        } catch (NumberFormatException ignored) {
                            // non-numeric suffix, skip
                        }
                    }

                    if (found) return max;
                }
            } catch (NoClassDefFoundError e) {
                luckPermsAvailable = false;
            }
        }

        return defaultValue;
    }
}
