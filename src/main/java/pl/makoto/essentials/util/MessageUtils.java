package pl.makoto.essentials.util;

import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.Config;

public class MessageUtils {
    public static MutableComponent format(String text) {
        if (text == null) return Component.literal("");
        return Component.literal(text.replace('&', '\u00a7'));
    }

    public static MutableComponent format(ServerPlayer player, String text) {
        if (player == null) return format(text);
        Component parsed = Placeholders.parseText(normalizeLegacyPlaceholders(text), ServerPlaceholderContext.of(player));
        return format(parsed.getString());
    }

    public static MutableComponent prefixed(String text) {
        String prefix = Config.MESSAGE_PREFIX.get();
        return format(prefix + text);
    }

    private static String normalizeLegacyPlaceholders(String text) {
        if (text == null || text.isEmpty()) return "";
        return text
                .replace("{player}", "%mktessentials:full_name/safe%")
                .replace("{dot}", "%mktessentials:dot%")
                .replace("{prefix}", "%mktessentials:prefix%")
                .replace("{name}", "%mktessentials:name%")
                .replace("{suffix}", "%mktessentials:suffix%");
    }
}
