package pl.makoto.essentials.util;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;

/**
 * Maps MiniMessage tag names to permission nodes and handles wildcard resolution.
 * Permission hierarchy:
 * - mktessentials.format.* grants everything
 * - mktessentials.format.decorations.* grants all decorations
 * - mktessentials.format.color.* grants all named colors + hex colors
 * - Individual nodes: mktessentials.format.decorations.bold, mktessentials.format.color.red, etc.
 */
public class FormatPermissions {

    public static final String PREFIX = "mktessentials.format";

    private static final int FALLBACK_OP_LEVEL = 2;

    // Decoration tag aliases → canonical decoration name
    private static final Map<String, String> DECORATION_ALIASES = Map.ofEntries(
            Map.entry("bold", "bold"),
            Map.entry("b", "bold"),
            Map.entry("italic", "italic"),
            Map.entry("i", "italic"),
            Map.entry("em", "italic"),
            Map.entry("underlined", "underlined"),
            Map.entry("u", "underlined"),
            Map.entry("strikethrough", "strikethrough"),
            Map.entry("st", "strikethrough"),
            Map.entry("obfuscated", "obfuscated"),
            Map.entry("obf", "obfuscated")
    );

    // All 16 named Minecraft colors
    private static final Set<String> NAMED_COLORS = Set.of(
            "black", "dark_blue", "dark_green", "dark_aqua",
            "dark_red", "dark_purple", "gold", "gray",
            "dark_gray", "blue", "green", "aqua",
            "red", "light_purple", "yellow", "white"
    );

    // Advanced tags that map directly to mktessentials.format.<tagname>
    private static final Map<String, String> ADVANCED_TAG_ALIASES = Map.ofEntries(
            Map.entry("gradient", "gradient"),
            Map.entry("rainbow", "rainbow"),
            Map.entry("pride", "pride"),
            Map.entry("transition", "transition"),
            Map.entry("shadow", "shadow"),
            Map.entry("hover", "hover"),
            Map.entry("click", "click"),
            Map.entry("insertion", "insertion"),
            Map.entry("reset", "reset"),
            Map.entry("newline", "newline"),
            Map.entry("br", "newline"),
            Map.entry("score", "score"),
            Map.entry("selector", "selector"),
            Map.entry("font", "font"),
            Map.entry("translatable", "translatable"),
            Map.entry("lang", "translatable"),
            Map.entry("keybind", "keybind"),
            Map.entry("nbt", "nbt")
    );

    /**
     * Returns the permission node for a given tag name.
     * Tag names are matched case-insensitively.
     *
     * Examples:
     *   "bold" → "mktessentials.format.decorations.bold"
     *   "b" → "mktessentials.format.decorations.bold"
     *   "red" → "mktessentials.format.color.red"
     *   "color:#FF0000" → "mktessentials.format.color.*"
     *   "gradient" → "mktessentials.format.gradient"
     *
     * @param tagName the tag name (may include arguments after a colon for color tags)
     * @return the permission node, or null if the tag is not recognized
     */
    public static String getNodeForTag(String tagName) {
        if (tagName == null || tagName.isEmpty()) {
            return null;
        }

        // Normalize to lowercase for case-insensitive matching
        String normalized = tagName.toLowerCase();

        // Handle "color:#RRGGBB" or "color:named" syntax
        if (normalized.startsWith("color:")) {
            String colorArg = normalized.substring(6); // after "color:"
            if (colorArg.startsWith("#")) {
                // Hex color → wildcard permission
                return PREFIX + ".color.*";
            }
            // Named color via color:name syntax
            if (NAMED_COLORS.contains(colorArg)) {
                return PREFIX + ".color." + colorArg;
            }
            // Unknown color argument, treat as unrecognized
            return null;
        }

        // Check if it's a hex color pattern directly (e.g., "#FF0000" as a tag name)
        if (normalized.startsWith("#") && normalized.length() == 7) {
            return PREFIX + ".color.*";
        }

        // Check decoration aliases
        String decorationCanonical = DECORATION_ALIASES.get(normalized);
        if (decorationCanonical != null) {
            return PREFIX + ".decorations." + decorationCanonical;
        }

        // Check named colors
        if (NAMED_COLORS.contains(normalized)) {
            return PREFIX + ".color." + normalized;
        }

        // Check advanced tags
        String advancedCanonical = ADVANCED_TAG_ALIASES.get(normalized);
        if (advancedCanonical != null) {
            return PREFIX + "." + advancedCanonical;
        }

        // Unrecognized tag
        return null;
    }

    /**
     * Checks if the player has permission for the given tag, including wildcard checks.
     * Wildcard hierarchy:
     * - mktessentials.format.* grants everything
     * - mktessentials.format.decorations.* grants all decorations
     * - mktessentials.format.color.* grants all named colors + hex colors
     *
     * Falls back to vanilla OP level 2 check if LuckPerms is unavailable.
     *
     * @param player  the player to check permissions for
     * @param tagName the tag name to check (may include arguments)
     * @return true if the player has permission to use this tag
     */
    public static boolean hasPermission(ServerPlayer player, String tagName) {
        if (player == null) {
            return false;
        }

        String node = getNodeForTag(tagName);
        if (node == null) {
            // Unrecognized tag — no permission node exists, deny by default
            return false;
        }

        // Check top-level wildcard: mktessentials.format.*
        if (Permissions.hasPermission(player, PREFIX + ".*", FALLBACK_OP_LEVEL)) {
            return true;
        }

        // Determine category for category-level wildcard check
        if (node.startsWith(PREFIX + ".decorations.")) {
            // Check decorations wildcard: mktessentials.format.decorations.*
            if (Permissions.hasPermission(player, PREFIX + ".decorations.*", FALLBACK_OP_LEVEL)) {
                return true;
            }
        } else if (node.startsWith(PREFIX + ".color.")) {
            // Check color wildcard: mktessentials.format.color.*
            if (Permissions.hasPermission(player, PREFIX + ".color.*", FALLBACK_OP_LEVEL)) {
                return true;
            }
        }

        // For hex colors, the node IS the wildcard (color.*), so if we reach here
        // and the node is "mktessentials.format.color.*", check it directly
        if (node.equals(PREFIX + ".color.*")) {
            return Permissions.hasPermission(player, node, FALLBACK_OP_LEVEL);
        }

        // Check the specific permission node
        return Permissions.hasPermission(player, node, FALLBACK_OP_LEVEL);
    }
}
