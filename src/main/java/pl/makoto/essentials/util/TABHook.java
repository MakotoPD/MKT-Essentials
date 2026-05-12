package pl.makoto.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import pl.makoto.essentials.MKTEssentials;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.Function;

public class TABHook {
    private static boolean isPresent = false;

    public static boolean isPresent() { return isPresent; }

    public static void init() {
        isPresent = ModList.get().isLoaded("tab");
        if (isPresent) {
            registerPlaceholders();
            registerReloadListener();
        }
    }

    private static void registerPlaceholders() {
        try {
            Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
            Method getInstanceMethod = tabApiClass.getMethod("getInstance");
            Object tabApi = getInstanceMethod.invoke(null);
            
            Method getPlaceholderManagerMethod = tabApi.getClass().getMethod("getPlaceholderManager");
            Object placeholderManager = getPlaceholderManagerMethod.invoke(tabApi);
            
            // registerPlayerPlaceholder(String identifier, int refresh, Function<TabPlayer, String> function)
            Method registerMethod = placeholderManager.getClass().getMethod("registerPlayerPlaceholder", String.class, int.class, Function.class);
            
            registerTabPlaceholder(registerMethod, placeholderManager, "%mkt_full_name%", "%mktessentials:tab_full_name%");
            registerTabPlaceholder(registerMethod, placeholderManager, "%mkt_prefix%", "%mktessentials:prefix%");
            registerTabPlaceholder(registerMethod, placeholderManager, "%mkt_suffix%", "%mktessentials:suffix%");
            registerTabPlaceholder(registerMethod, placeholderManager, "%mktessentials:tab_full_name%", "%mktessentials:tab_full_name%");
            registerTabPlaceholder(registerMethod, placeholderManager, "%mktessentials:full_name%", "%mktessentials:full_name%");
            registerTabPlaceholder(registerMethod, placeholderManager, "%mktessentials:prefix%", "%mktessentials:prefix%");
            registerTabPlaceholder(registerMethod, placeholderManager, "%mktessentials:suffix%", "%mktessentials:suffix%");
            registerTabPlaceholder(registerMethod, placeholderManager, "%mktessentials:dot%", "%mktessentials:dot%");
            registerTabPlaceholder(registerMethod, placeholderManager, "%mktessentials:name%", "%mktessentials:name%");
            registerTabPlaceholder(registerMethod, placeholderManager, "%mktessentials:nick%", "%mktessentials:nick%");
            registerTabPlaceholder(registerMethod, placeholderManager, "%mktessentials:real_name%", "%mktessentials:real_name%");

            MKTEssentials.LOGGER.info("TAB Integration: Registered MKT placeholders.");
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("TAB Integration: Failed to register placeholders", e);
        }
    }

    private static void registerTabPlaceholder(Method registerMethod, Object placeholderManager, String tabIdentifier, String mktPlaceholder) throws Exception {
        registerMethod.invoke(placeholderManager, tabIdentifier, 500, (Function<Object, String>) playerObj -> {
            try {
                UUID uuid = (UUID) playerObj.getClass().getMethod("getUniqueId").invoke(playerObj);
                ServerPlayer player = MKTEssentials.getServer().getPlayerList().getPlayer(uuid);
                return player != null ? resolve(player, mktPlaceholder) : "";
            } catch (Exception e) {
                return "";
            }
        });
    }

    private static String resolve(ServerPlayer player, String placeholder) {
        return MessageUtils.format(player, placeholder).getString();
    }

    public static void refreshPlayer(ServerPlayer player) {
        if (!isPresent) return;
        try {
            Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
            Method getInstanceMethod = tabApiClass.getMethod("getInstance");
            Object tabApi = getInstanceMethod.invoke(null);

            Method getPlayerMethod = tabApi.getClass().getMethod("getPlayer", UUID.class);
            Object tabPlayer = getPlayerMethod.invoke(tabApi, player.getUUID());
            if (tabPlayer == null) return;

            Method getPlaceholderManagerMethod = tabApi.getClass().getMethod("getPlaceholderManager");
            Object placeholderManager = getPlaceholderManagerMethod.invoke(tabApi);

            refreshPlaceholders(placeholderManager, tabPlayer);
            applyTabListFormat(tabApi, tabPlayer, player);
            applyNameTagFormat(tabApi, tabPlayer, player);
        } catch (Exception e) {
            // Ignore if TAB is not fully loaded or other issues
        }
    }

    private static void refreshPlaceholders(Object placeholderManager, Object tabPlayer) throws Exception {
        Method getPlaceholderMethod = placeholderManager.getClass().getMethod("getPlaceholder", String.class);
        String[] placeholders = {
                "%mkt_full_name%", "%mkt_prefix%", "%mkt_suffix%",
                "%mktessentials:tab_full_name%", "%mktessentials:full_name%",
                "%mktessentials:prefix%", "%mktessentials:suffix%",
                "%mktessentials:dot%", "%mktessentials:name%",
                "%mktessentials:nick%", "%mktessentials:real_name%"
        };
        for (String ph : placeholders) {
            Object placeholder = getPlaceholderMethod.invoke(placeholderManager, ph);
            if (placeholder != null) {
                Method updateMethod = placeholder.getClass().getMethod("update", Class.forName("me.neznamy.tab.api.TabPlayer"));
                updateMethod.invoke(placeholder, tabPlayer);
            }
        }
    }

    private static void applyTabListFormat(Object tabApi, Object tabPlayer, ServerPlayer player) throws Exception {
        Object manager = tabApi.getClass().getMethod("getTabListFormatManager").invoke(tabApi);
        if (manager == null) return;

        invokeTabPlayerString(manager, "setPrefix", tabPlayer, "");
        invokeTabPlayerString(manager, "setName", tabPlayer, resolve(player, "%mktessentials:tab_full_name%"));
        invokeTabPlayerString(manager, "setSuffix", tabPlayer, "");
    }

    private static void applyNameTagFormat(Object tabApi, Object tabPlayer, ServerPlayer player) throws Exception {
        Object manager = tabApi.getClass().getMethod("getNameTagManager").invoke(tabApi);
        if (manager == null) return;

        invokeTabPlayerString(manager, "setPrefix", tabPlayer, resolve(player, "%mktessentials:prefix%"));
        invokeTabPlayerString(manager, "setSuffix", tabPlayer, resolve(player, "%mktessentials:suffix%"));
    }

    private static void invokeTabPlayerString(Object target, String methodName, Object tabPlayer, String value) throws Exception {
        Class<?> tabPlayerClass = Class.forName("me.neznamy.tab.api.TabPlayer");
        Method method = target.getClass().getMethod(methodName, tabPlayerClass, String.class);
        method.invoke(target, tabPlayer, value);
    }

    private static void registerReloadListener() {
        try {
            Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
            Method getInstanceMethod = tabApiClass.getMethod("getInstance");
            Object tabApi = getInstanceMethod.invoke(null);

            Method getEventBusMethod = tabApi.getClass().getMethod("getEventBus");
            Object eventBus = getEventBusMethod.invoke(tabApi);

            Class<?> tabLoadEventClass = Class.forName("me.neznamy.tab.api.event.plugin.TabLoadEvent");
            Class<?> eventHandlerClass = Class.forName("me.neznamy.tab.api.event.EventHandler");

            Object handlerProxy = Proxy.newProxyInstance(
                eventHandlerClass.getClassLoader(),
                new Class<?>[]{eventHandlerClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("handle")) {
                        MKTEssentials.LOGGER.info("TAB Integration: Reload detected, re-registering placeholders...");
                        registerPlaceholders();
                    }
                    return null;
                }
            );

            Method registerMethod = eventBus.getClass().getMethod("register", Class.class, eventHandlerClass);
            registerMethod.invoke(eventBus, tabLoadEventClass, handlerProxy);
            
            MKTEssentials.LOGGER.info("TAB Integration: Registered reload listener (v5 style).");
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("TAB Integration: Failed to register reload listener", e);
        }
    }
}
