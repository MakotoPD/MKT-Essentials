package pl.makoto.essentials.util;

import pl.makoto.essentials.config.Settings;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interactive chat replacements, inspired by FlectonePulse's format/replacement module:
 * turns http(s) links into clickable links and {@code ||text||} into obfuscated spoilers.
 * <p>
 * Applied to the chat string AFTER permission filtering (server-controlled markup), so the
 * injected MiniMessage tags aren't stripped by the sender's formatting permissions.
 */
public final class ReplacementManager {

    private static final Pattern URL = Pattern.compile("https?://\\S+");
    private static final Pattern SPOILER = Pattern.compile("\\|\\|(.+?)\\|\\|");

    // Markdown-style formatting (bold must be matched before italic)
    private static final Pattern MD_BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern MD_ITALIC = Pattern.compile("\\*(.+?)\\*");
    private static final Pattern MD_UNDERLINE = Pattern.compile("__(.+?)__");
    private static final Pattern MD_STRIKE = Pattern.compile("~~(.+?)~~");
    private static final Pattern MD_MATRIX = Pattern.compile("\\?\\?(.+?)\\?\\?");

    private ReplacementManager() {}

    public static String apply(String input) {
        if (input == null || input.isEmpty()) return input;

        String result = input;
        if (Settings.isMarkdownEnabled()) result = markdown(result);
        if (Settings.isReplacementEnabled()) {
            result = emojis(result);
            if (Settings.isReplacementSpoiler()) result = spoilers(result);
            if (Settings.isReplacementUrl()) result = urls(result);
        }
        // Highlight questions (message ending with '?') by coloring the base text
        if (Settings.isQuestionEnabled() && input.stripTrailing().endsWith("?")) {
            result = "<color:" + Settings.getQuestionColor() + ">" + result + "</color>";
        }
        return result;
    }

    /** Converts Markdown markers to MiniMessage decoration tags (bold before italic). */
    public static String markdown(String input) {
        if (input == null || input.isEmpty()) return input;
        String result = input;
        result = wrap(result, MD_BOLD, "bold");
        result = wrap(result, MD_ITALIC, "italic");
        result = wrap(result, MD_UNDERLINE, "underlined");
        result = wrap(result, MD_STRIKE, "strikethrough");
        result = wrap(result, MD_MATRIX, "obfuscated");
        return result;
    }

    private static String wrap(String input, Pattern pattern, String tag) {
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) return input;
        matcher.reset();
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement("<" + tag + ">" + matcher.group(1) + "</" + tag + ">"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String emojis(String input) {
        if (input.indexOf(':') == -1) return input;
        String result = input;
        for (Map.Entry<String, String> e : Settings.getEmojis().entrySet()) {
            if (result.contains(e.getKey())) {
                result = result.replace(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    private static String spoilers(String input) {
        if (input.indexOf('|') == -1) return input;
        Matcher matcher = SPOILER.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            // Hidden text is shown as gray blocks and revealed on hover (FlectonePulse-style)
            String secret = matcher.group(1).replace("<", "").replace(">", "");
            int length = Math.min(Math.max(secret.length(), 1), 24);
            String blocks = "█".repeat(length);
            String replacement = "<hover:show_text:" + secret + "><dark_gray>" + blocks + "</dark_gray></hover>";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String urls(String input) {
        Matcher matcher = URL.matcher(input);
        StringBuilder sb = new StringBuilder();
        String color = Settings.getReplacementUrlColor();
        while (matcher.find()) {
            String url = matcher.group();
            String replacement = "<click:open_url:" + url + "><color:" + color + "><underlined>"
                    + url + "</underlined></color></click>";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
