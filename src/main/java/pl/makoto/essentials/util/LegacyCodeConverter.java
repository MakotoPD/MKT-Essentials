package pl.makoto.essentials.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts legacy ampersand (&) color/decoration codes into MiniMessage tag equivalents.
 * <p>
 * Supports:
 * <ul>
 *   <li>&0–&9, &a–&f (color codes, case-insensitive)</li>
 *   <li>&l, &o, &n, &m, &k (decoration codes, case-insensitive)</li>
 *   <li>&r (reset, case-insensitive)</li>
 *   <li>&#RRGGBB (hex color codes)</li>
 * </ul>
 */
public class LegacyCodeConverter {

    private static final Map<Character, String> COLOR_MAP = Map.ofEntries(
            Map.entry('0', "<black>"),
            Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),
            Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),
            Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),
            Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),
            Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>")
    );

    private static final Map<Character, String> DECORATION_MAP = Map.of(
            'l', "<bold>",
            'o', "<italic>",
            'n', "<underlined>",
            'm', "<strikethrough>",
            'k', "<obfuscated>"
    );

    /**
     * Pattern to match hex color codes: &#RRGGBB (6 hex digits).
     * The ampersand followed by # and exactly 6 hex characters.
     */
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    /**
     * Converts all legacy &X codes and &#RRGGBB hex codes in the input
     * to their MiniMessage tag equivalents.
     *
     * @param input raw player text with potential & codes
     * @return text with & codes replaced by MiniMessage tags
     */
    public static String convert(String input) {
        if (input == null || input.isEmpty()) {
            return input == null ? "" : input;
        }

        // First pass: convert hex codes &#RRGGBB → <color:#RRGGBB>
        String result = convertHexCodes(input);

        // Second pass: convert standard &X codes
        result = convertStandardCodes(result);

        return result;
    }

    /** Reverse of the color/decoration maps: MiniMessage tag name → legacy code character. */
    private static final Map<String, Character> REVERSE_MAP = buildReverseMap();

    private static Map<String, Character> buildReverseMap() {
        Map<String, Character> map = new java.util.HashMap<>();
        COLOR_MAP.forEach((code, tag) -> map.put(tag.substring(1, tag.length() - 1), code));
        DECORATION_MAP.forEach((code, tag) -> map.put(tag.substring(1, tag.length() - 1), code));
        map.put("reset", 'r');
        return Map.copyOf(map);
    }

    // <tag>, </tag>, <color:name> or <color:#RRGGBB>
    private static final Pattern MINI_TAG = Pattern.compile("(?i)<(/?)([a-z_]+)(?::#?([0-9a-f]{6}|[a-z_]+))?>");

    /**
     * Converts MiniMessage color/decoration tags back into legacy {@code &} codes so a nickname
     * stored as MiniMessage renders correctly through MKT's legacy ({@code &}→{@code §}) display
     * pipeline and TAB integration. Unrecognized tags are left untouched; recognized closing tags
     * are dropped. Idempotent for strings that already contain only legacy codes.
     */
    public static String fromMiniMessage(String input) {
        if (input == null || input.isEmpty()) return input == null ? "" : input;
        if (input.indexOf('<') == -1) return input; // fast path: no tags

        Matcher matcher = MINI_TAG.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            boolean closing = !matcher.group(1).isEmpty();
            String name = matcher.group(2).toLowerCase();
            String arg = matcher.group(3);
            String replacement;

            if (closing) {
                replacement = (name.equals("color") || REVERSE_MAP.containsKey(name)) ? "" : matcher.group();
            } else if (name.equals("color") && arg != null) {
                if (arg.matches("(?i)[0-9a-f]{6}")) {
                    replacement = "&#" + arg.toUpperCase();
                } else {
                    Character code = REVERSE_MAP.get(arg.toLowerCase());
                    replacement = code != null ? "&" + code : matcher.group();
                }
            } else if (REVERSE_MAP.containsKey(name)) {
                replacement = "&" + REVERSE_MAP.get(name);
            } else {
                replacement = matcher.group(); // unknown tag — keep literal
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertHexCodes(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1).toUpperCase();
            matcher.appendReplacement(sb, "<color:#" + hex + ">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertStandardCodes(String input) {
        StringBuilder result = new StringBuilder(input.length());
        int i = 0;

        while (i < input.length()) {
            if (input.charAt(i) == '&' && i + 1 < input.length()) {
                char code = Character.toLowerCase(input.charAt(i + 1));

                if (code == 'r') {
                    result.append("<reset>");
                    i += 2;
                } else if (COLOR_MAP.containsKey(code)) {
                    result.append(COLOR_MAP.get(code));
                    i += 2;
                } else if (DECORATION_MAP.containsKey(code)) {
                    result.append(DECORATION_MAP.get(code));
                    i += 2;
                } else {
                    // Not a recognized code, keep the ampersand as-is
                    result.append(input.charAt(i));
                    i++;
                }
            } else {
                result.append(input.charAt(i));
                i++;
            }
        }

        return result.toString();
    }
}
