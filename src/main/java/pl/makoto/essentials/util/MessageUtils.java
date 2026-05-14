package pl.makoto.essentials.util;

import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.config.Settings;

public class MessageUtils {

    /**
     * Maximum visible character length for player-authored content after formatting codes are stripped.
     */
    private static final int MAX_VISIBLE_LENGTH = 256;

    public static MutableComponent format(String text) {
        if (text == null) return Component.literal("");
        return Component.literal(text.replace('&', '\u00a7'));
    }

    public static MutableComponent format(ServerPlayer player, String text) {
        if (player == null) return format(text);
        try {
            Component parsed = Placeholders.parseText(normalizeLegacyPlaceholders(text), ServerPlaceholderContext.of(player));
            return format(parsed.getString());
        } catch (NoClassDefFoundError e) {
            return format(stripPlaceholders(text, player.getScoreboardName()));
        }
    }

    /**
     * Formats player-authored text with MiniMessage + legacy codes, applying permission filtering.
     * Pipeline: LegacyCodeConverter → PermissionFilter → MiniMessageParser
     * Enforces a 256-character visible text limit after formatting codes are stripped.
     *
     * @param player the player whose permissions are checked
     * @param text   raw player input text
     * @return styled MutableComponent
     */
    public static MutableComponent formatWithPermissions(ServerPlayer player, String text) {
        if (text == null || text.isEmpty()) return Component.literal("");

        // Step 1: Convert legacy & codes to MiniMessage equivalents
        String converted = LegacyCodeConverter.convert(text);

        // Step 2: Filter tags by player permissions
        String filtered = PermissionFilter.filter(player, converted);

        // Step 3: Enforce 256-character visible text limit
        filtered = enforceVisibleLimit(filtered);

        // Step 4: Parse MiniMessage into a styled Component
        return MiniMessageParser.parse(filtered);
    }

    /**
     * Formats text with MiniMessage + legacy codes, bypassing permission checks.
     * Pipeline: LegacyCodeConverter → MiniMessageParser (no permission filter, no truncation)
     * Used for admin/system messages (broadcasts, config-defined messages).
     *
     * @param text raw text input
     * @return styled MutableComponent
     */
    public static MutableComponent formatBypass(String text) {
        if (text == null || text.isEmpty()) return Component.literal("");

        // Step 1: Convert legacy & codes to MiniMessage equivalents
        String converted = LegacyCodeConverter.convert(text);

        // Step 2: Parse MiniMessage into a styled Component (no permission filtering, no truncation)
        return MiniMessageParser.parse(converted);
    }

    public static MutableComponent prefixed(String text) {
        String prefix = Settings.getMessagePrefix();
        return format(prefix + text);
    }

    /**
     * Enforces the 256-character visible text limit on a MiniMessage-formatted string.
     * Strips all MiniMessage tags to count visible characters, then truncates the input
     * at the point where visible characters exceed the limit while preserving tag structure.
     *
     * @param input MiniMessage-formatted string
     * @return truncated string if visible text exceeds 256 chars, otherwise unchanged
     */
    private static String enforceVisibleLimit(String input) {
        if (input == null || input.isEmpty()) return input;

        // Count visible characters (everything outside of < > tags)
        String visibleText = stripTags(input);
        if (visibleText.length() <= MAX_VISIBLE_LENGTH) {
            return input;
        }

        // Truncate: walk through the input, counting visible chars, and cut at the limit
        StringBuilder result = new StringBuilder();
        int visibleCount = 0;
        int i = 0;

        while (i < input.length() && visibleCount < MAX_VISIBLE_LENGTH) {
            if (input.charAt(i) == '<') {
                // Find the closing '>'
                int closeIdx = input.indexOf('>', i);
                if (closeIdx == -1) {
                    // No closing '>' — rest is visible text
                    int remaining = MAX_VISIBLE_LENGTH - visibleCount;
                    int end = Math.min(i + remaining, input.length());
                    result.append(input, i, end);
                    visibleCount += (end - i);
                    i = end;
                } else {
                    // Append the entire tag (doesn't count as visible)
                    result.append(input, i, closeIdx + 1);
                    i = closeIdx + 1;
                }
            } else {
                result.append(input.charAt(i));
                visibleCount++;
                i++;
            }
        }

        return result.toString();
    }

    /**
     * Strips all MiniMessage tags from the input, returning only visible text.
     */
    private static String stripTags(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            if (input.charAt(i) == '<') {
                int closeIdx = input.indexOf('>', i);
                if (closeIdx == -1) {
                    // No closing '>' — rest is literal text
                    sb.append(input, i, input.length());
                    break;
                } else {
                    i = closeIdx + 1;
                }
            } else {
                sb.append(input.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    private static String stripPlaceholders(String text, String playerName) {
        return text
                .replace("%mktessentials:full_name/safe%", playerName)
                .replace("%mktessentials:name%", playerName)
                .replace("%mktessentials:prefix%", "")
                .replace("%mktessentials:suffix%", "")
                .replace("%mktessentials:dot%", "")
                .replace("{player}", playerName)
                .replace("{name}", playerName)
                .replace("{prefix}", "")
                .replace("{suffix}", "")
                .replace("{dot}", "");
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
