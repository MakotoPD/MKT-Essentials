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
