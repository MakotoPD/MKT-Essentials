package pl.makoto.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import pl.makoto.essentials.MKTEssentials;

import java.lang.reflect.Method;
import java.util.UUID;

public class Permissions {
    private static Boolean luckPermsPresent = null;
    private static Object luckPermsApi = null;

    private static void ensureApi() {
        if (luckPermsPresent != null) return;
        
        luckPermsPresent = ModList.get().isLoaded("luckperms");
        if (luckPermsPresent) {
            try {
                Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                Method getMethod = providerClass.getMethod("get");
                luckPermsApi = getMethod.invoke(null);
                MKTEssentials.LOGGER.info("Permissions: LuckPerms API hooked successfully.");
            } catch (Exception e) {
                luckPermsPresent = false;
                MKTEssentials.LOGGER.error("Permissions: Failed to hook LuckPerms API", e);
            }
        }
    }

    public static void init() {
    }

    public static boolean hasPermission(ServerPlayer player, String node, int fallbackLevel) {
        if (player == null) return false;
        ensureApi();
        
        if (luckPermsPresent && luckPermsApi != null) {
            try {
                Method getUserManager = luckPermsApi.getClass().getMethod("getUserManager");
                Object userManager = getUserManager.invoke(luckPermsApi);
                
                Method getUser = userManager.getClass().getMethod("getUser", UUID.class);
                Object user = getUser.invoke(userManager, player.getUUID());
                
                if (user != null) {
                    Method getCachedData = user.getClass().getMethod("getCachedData");
                    Object cachedData = getCachedData.invoke(user);
                    
                    Method getPermissionData = cachedData.getClass().getMethod("getPermissionData");
                    Object permissionData = getPermissionData.invoke(cachedData);
                    
                    Method checkPermission = permissionData.getClass().getMethod("checkPermission", String.class);
                    Object tristate = checkPermission.invoke(permissionData, node);
                    
                    // Tristate enum handling
                    String name = tristate.toString(); // "TRUE", "FALSE", or "UNDEFINED"
                    
                    if (name.equals("TRUE")) return true;
                    if (name.equals("FALSE")) return false;
                    
                    // If UNDEFINED, fall through to vanilla check
                }
            } catch (Exception e) {
                // Fallback on error
            }
        }
        
        // Vanilla check (OP level)
        return player.hasPermissions(fallbackLevel);
    }

    public static boolean hasPermission(net.minecraft.commands.CommandSourceStack source, String node, int fallbackLevel) {
        if (source.getPlayer() != null) {
            return hasPermission(source.getPlayer(), node, fallbackLevel);
        }
        return source.hasPermission(fallbackLevel);
    }
    
    public static int getIntPermission(ServerPlayer player, String node, int defaultValue) {
        if (player == null) return defaultValue;
        ensureApi();
        
        if (luckPermsPresent && luckPermsApi != null) {
            try {
                Method getUserManager = luckPermsApi.getClass().getMethod("getUserManager");
                Object userManager = getUserManager.invoke(luckPermsApi);
                
                Method getUser = userManager.getClass().getMethod("getUser", UUID.class);
                Object user = getUser.invoke(userManager, player.getUUID());
                
                if (user != null) {
                    Method getCachedData = user.getClass().getMethod("getCachedData");
                    Object cachedData = getCachedData.invoke(user);
                    
                    Method getMetaData = cachedData.getClass().getMethod("getMetaData");
                    Object metaData = getMetaData.invoke(cachedData);
                    
                    // Use getMetaValue(String) which returns a String (or null)
                    Method getMetaValue = metaData.getClass().getMethod("getMetaValue", String.class);
                    String value = (String) getMetaValue.invoke(metaData, node);
                    
                    if (value != null) {
                        try {
                            return Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            // Not parseable as int, fall through to default
                        }
                    }
                }
            } catch (Exception e) {
                // LuckPerms error, fall through to default
            }
        }
        return defaultValue;
    }
}
