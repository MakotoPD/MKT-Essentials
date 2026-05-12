package pl.makoto.essentials.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing and formatting human-readable duration strings.
 * <p>
 * Supports multi-unit duration strings combining: d (days), h (hours), m (minutes), s (seconds).
 * Examples: "1d6h30m", "2h", "30s", "1d12h30m15s"
 */
public class DurationParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([dhms])");
    private static final Pattern VALID_FORMAT = Pattern.compile("^(\\d+[dhms])+$");

    private static final long SECONDS_MS = 1000L;
    private static final long MINUTES_MS = 60L * SECONDS_MS;
    private static final long HOURS_MS = 60L * MINUTES_MS;
    private static final long DAYS_MS = 24L * HOURS_MS;

    /**
     * Parses a human-readable duration string like "1d6h30m" into milliseconds.
     * Supports: d (days), h (hours), m (minutes), s (seconds).
     *
     * @param input the duration string to parse
     * @return the total duration in milliseconds
     * @throws IllegalArgumentException if input is null, empty, or malformed
     */
    public static long parse(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Duration string cannot be null or empty");
        }

        String normalized = input.trim().toLowerCase();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Duration string cannot be blank");
        }

        if (!VALID_FORMAT.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid duration format: '" + input + "'. Expected format like '1d6h30m' using units: d, h, m, s");
        }

        long totalMillis = 0;
        Matcher matcher = DURATION_PATTERN.matcher(normalized);

        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            totalMillis += switch (unit) {
                case "d" -> value * DAYS_MS;
                case "h" -> value * HOURS_MS;
                case "m" -> value * MINUTES_MS;
                case "s" -> value * SECONDS_MS;
                default -> throw new IllegalArgumentException("Unknown duration unit: " + unit);
            };
        }

        return totalMillis;
    }

    /**
     * Formats a duration in milliseconds to a human-readable string like "1d 6h 30m".
     * Only includes non-zero units. Returns "0s" if millis is 0.
     *
     * @param millis the duration in milliseconds
     * @return a human-readable duration string
     */
    public static String format(long millis) {
        if (millis <= 0) {
            return "0s";
        }

        long remaining = millis;

        long days = remaining / DAYS_MS;
        remaining %= DAYS_MS;

        long hours = remaining / HOURS_MS;
        remaining %= HOURS_MS;

        long minutes = remaining / MINUTES_MS;
        remaining %= MINUTES_MS;

        long seconds = remaining / SECONDS_MS;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s ");

        return sb.toString().trim();
    }
}
