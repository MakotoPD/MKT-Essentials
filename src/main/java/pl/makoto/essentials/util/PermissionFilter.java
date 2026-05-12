package pl.makoto.essentials.util;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strips MiniMessage tags from the input string that the player lacks permission to use.
 * Inner text content is always preserved — only the tag markers themselves are removed.
 *
 * Behavior:
 * - If player is null (console, command block, RCON), all tags pass through (admin bypass).
 * - For each opening tag, resolve the permission node via FormatPermissions.
 * - If denied, remove the opening tag and its matching closing tag. Inner content remains.
 * - Nested tags are evaluated independently — a denied outer tag does not affect permitted inner tags.
 * - Unrecognized tags (getNodeForTag returns null) are left as-is (literal text for the parser).
 */
public class PermissionFilter {

    // Matches MiniMessage opening tags: <tagname> or <tagname:args>
    // and closing tags: </tagname>
    private static final Pattern TAG_PATTERN = Pattern.compile("<(/?)([^<>]+)>");

    /**
     * Scans the input for MiniMessage tags and removes any the player
     * does not have permission to use. Inner text content is preserved.
     *
     * @param player the player whose permissions are checked (null = bypass all)
     * @param input  MiniMessage-formatted string
     * @return filtered string with unauthorized tags removed
     */
    public static String filter(@Nullable ServerPlayer player, String input) {
        if (player == null) {
            // Admin bypass: console, command block, RCON — all tags pass through
            return input;
        }

        if (input == null || input.isEmpty()) {
            return input;
        }

        // First pass: identify which tag names are denied
        // We collect all denied tag names (canonical forms) so we can strip both
        // opening and closing tags in a single pass.
        List<String> deniedTagNames = collectDeniedTagNames(player, input);

        if (deniedTagNames.isEmpty()) {
            return input;
        }

        // Second pass: remove denied opening and closing tags
        return stripDeniedTags(input, deniedTagNames, player);
    }

    /**
     * Collects the canonical tag names that the player is denied from using.
     */
    private static List<String> collectDeniedTagNames(ServerPlayer player, String input) {
        List<String> denied = new ArrayList<>();
        Matcher matcher = TAG_PATTERN.matcher(input);

        while (matcher.find()) {
            boolean isClosing = !matcher.group(1).isEmpty();
            String tagContent = matcher.group(2);

            // Skip closing tags in the collection pass — we handle them via their opening tag
            if (isClosing) {
                continue;
            }

            String tagName = extractTagName(tagContent);
            if (tagName.isEmpty()) {
                continue;
            }

            // Check if this tag is recognized by the permission system
            String node = FormatPermissions.getNodeForTag(tagName);
            if (node == null) {
                // Unrecognized tag — leave as-is (will be rendered as literal text)
                continue;
            }

            // Check permission
            if (!FormatPermissions.hasPermission(player, tagName)) {
                if (!denied.contains(tagName.toLowerCase())) {
                    denied.add(tagName.toLowerCase());
                }
            }
        }

        return denied;
    }

    /**
     * Strips opening and closing tags for denied tag names from the input.
     * Preserves all inner text content.
     */
    private static String stripDeniedTags(String input, List<String> deniedTagNames, ServerPlayer player) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = TAG_PATTERN.matcher(input);
        int lastEnd = 0;

        while (matcher.find()) {
            boolean isClosing = !matcher.group(1).isEmpty();
            String tagContent = matcher.group(2);

            String tagName = extractTagName(tagContent);
            if (tagName.isEmpty()) {
                // Not a valid tag structure, leave as-is
                continue;
            }

            // Determine if this tag should be stripped
            boolean shouldStrip = isTagDenied(tagName, deniedTagNames, player);

            if (shouldStrip) {
                // Append text before this tag
                result.append(input, lastEnd, matcher.start());
                // Skip the tag itself (don't append it)
                lastEnd = matcher.end();
            }
        }

        // Append remaining text after the last match
        result.append(input, lastEnd, input.length());
        return result.toString();
    }

    /**
     * Checks if a tag name (from either opening or closing tag) is denied.
     * Handles aliases by checking the permission system directly.
     */
    private static boolean isTagDenied(String tagName, List<String> deniedTagNames, ServerPlayer player) {
        String normalized = tagName.toLowerCase();

        // Direct match in denied list
        if (deniedTagNames.contains(normalized)) {
            return true;
        }

        // For closing tags or aliases, check if the permission system recognizes this tag
        // and if the player lacks permission for it
        String node = FormatPermissions.getNodeForTag(tagName);
        if (node == null) {
            // Unrecognized tag — leave as-is
            return false;
        }

        return !FormatPermissions.hasPermission(player, tagName);
    }

    /**
     * Extracts the tag name from tag content.
     * For "bold" → "bold"
     * For "gradient:red:blue" → "gradient"
     * For "color:#FF0000" → "color:#FF0000" (color with hex needs full form for permission check)
     * For "hover:show_text:hello" → "hover"
     */
    private static String extractTagName(String tagContent) {
        if (tagContent == null || tagContent.isEmpty()) {
            return "";
        }

        String content = tagContent.trim().toLowerCase();

        // Special handling for "color:" prefix — keep the color argument for permission resolution
        if (content.startsWith("color:")) {
            // For "color:#RRGGBB" or "color:red", keep the full form
            // because FormatPermissions.getNodeForTag handles "color:#RRGGBB" specially
            int secondColon = content.indexOf(':', 6);
            if (secondColon == -1) {
                // "color:#FF0000" or "color:red" — return as-is
                return content;
            }
            // "color:#FF0000:extra" — just take "color:#FF0000"
            return content.substring(0, secondColon);
        }

        // For all other tags, the tag name is everything before the first colon
        int colonIndex = content.indexOf(':');
        if (colonIndex == -1) {
            return content;
        }
        return content.substring(0, colonIndex);
    }
}
