package pl.makoto.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import java.lang.reflect.Method;
import java.util.UUID;

public class LuckPermsHook {
    private static Boolean isPresent = null;
    private static Object luckPermsApi = null;
    private static Method getUserMethod = null;

    private static void ensureApi() {
        if (isPresent != null) return;
        
        isPresent = ModList.get().isLoaded("luckperms");
        if (isPresent) {
            try {
                Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                Method getMethod = providerClass.getMethod("get");
                luckPermsApi = getMethod.invoke(null);
                
                getUserMethod = luckPermsApi.getClass().getMethod("getUserManager");
            } catch (Exception e) {
                isPresent = false;
            }
        }
    }

    public static String getPrefix(ServerPlayer player) {
        ensureApi();
        if (!isPresent || player == null) return "";
        return getMetaData(player, "getPrefix");
    }

    public static String getSuffix(ServerPlayer player) {
        ensureApi();
        if (!isPresent || player == null) return "";
        return getMetaData(player, "getSuffix");
    }

    private static String getMetaData(ServerPlayer player, String methodName) {
        try {
            // Get UserManager
            Object userManager = getUserMethod.invoke(luckPermsApi);
            
            // Get User
            Method getUser = userManager.getClass().getMethod("getUser", UUID.class);
            Object user = getUser.invoke(userManager, player.getUUID());
            
            if (user == null) return "";
            
            // Get CachedData
            Method getCachedData = user.getClass().getMethod("getCachedData");
            Object cachedData = getCachedData.invoke(user);
            
            // Get MetaData
            Method getMetaData = cachedData.getClass().getMethod("getMetaData");
            Object metaData = getMetaData.invoke(cachedData);
            
            // Get value (getPrefix or getSuffix)
            Method getValue = metaData.getClass().getMethod(methodName);
            Object value = getValue.invoke(metaData);
            
            return value != null ? (String) value : "";
        } catch (Exception e) {
            return "";
        }
    }
}
