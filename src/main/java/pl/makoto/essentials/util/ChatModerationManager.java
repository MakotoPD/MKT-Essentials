package pl.makoto.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.data.DataManager;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat moderation filters (anti-flood, excessive caps, swear filter), inspired by
 * FlectonePulse's {@code format/moderation} modules but implemented in MKT's static-manager style.
 * <p>
 * A single {@link #process} call decides whether a message is blocked and returns the possibly
 * transformed message (caps lowered, repeated characters collapsed, swears censored). State needed
 * for flood detection is kept per player and cleared on quit via {@link #cleanupPlayer}.
 */
public final class ChatModerationManager {

    /** Result of moderating a chat message. When {@code blocked}, {@code blockMessage} explains why. */
    public record Result(boolean blocked, String blockMessage, String message) {
        static Result allow(String message) { return new Result(false, null, message); }
        static Result block(String reason) { return new Result(true, reason, null); }
    }

    private record FloodEntry(long time, String lastMessage) {}

    private static final Map<UUID, FloodEntry> FLOOD = new ConcurrentHashMap<>();
    private static final Pattern URL = Pattern.compile("https?://\\S+");

    private ChatModerationManager() {}

    public static void cleanupPlayer(UUID uuid) {
        FLOOD.remove(uuid);
    }

    public static Result process(ServerPlayer sender, String message) {
        if (!Settings.isChatModEnabled()) return Result.allow(message);

        UUID uuid = sender.getUUID();
        long now = System.currentTimeMillis();

        // --- Newbie: block links from players below a playtime threshold ---
        if (Settings.isNewbieChatEnabled() && Settings.isNewbieBlockLinks()) {
            long playMinutes = DataManager.getPlayerData(uuid).getTotalPlayTimeMillis() / 60_000L;
            if (playMinutes < Settings.getNewbieMinPlaytimeMinutes() && URL.matcher(message).find()) {
                return Result.block("&cNew players can't post links yet.");
            }
        }

        // --- Flood / anti-spam ---
        if (Settings.isFloodEnabled()) {
            FloodEntry previous = FLOOD.get(uuid);
            if (previous != null) {
                long cooldownMs = (long) (Settings.getFloodCooldownSeconds() * 1000L);
                if (now - previous.time() < cooldownMs) {
                    return Result.block("&cPlease wait a moment before sending another message.");
                }
                if (Settings.isFloodBlockDuplicate() && message.equalsIgnoreCase(previous.lastMessage())) {
                    return Result.block("&cPlease don't repeat the same message.");
                }
            }
        }

        String working = message;

        // --- Excessive CAPS ---
        if (Settings.isCapsEnabled() && working.length() >= Settings.getCapsMinLength()) {
            int letters = 0;
            int upper = 0;
            for (int i = 0; i < working.length(); i++) {
                char c = working.charAt(i);
                if (Character.isLetter(c)) {
                    letters++;
                    if (Character.isUpperCase(c)) upper++;
                }
            }
            if (letters > 0 && (upper * 100) / letters > Settings.getCapsMaxPercent()) {
                if ("block".equalsIgnoreCase(Settings.getCapsMode())) {
                    return Result.block("&cPlease don't use excessive capital letters.");
                }
                working = working.toLowerCase(Locale.ROOT);
            }
        }

        // --- Collapse repeated characters ---
        int maxRepeat = Settings.getFloodMaxRepeatedChars();
        if (maxRepeat > 0) {
            working = collapseRepeats(working, maxRepeat);
        }

        // --- Swear filter ---
        if (Settings.isSwearEnabled() && !Settings.getSwearWords().isEmpty()) {
            boolean block = "block".equalsIgnoreCase(Settings.getSwearMode());
            String censorChar = Settings.getSwearCensorChar();
            if (censorChar == null || censorChar.isEmpty()) censorChar = "*";

            for (String word : Settings.getSwearWords()) {
                if (word == null || word.isBlank()) continue;
                Pattern pattern = Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(working);
                if (!matcher.find()) continue;
                if (block) {
                    return Result.block("&cYour message contains a blocked word.");
                }
                // Censor every occurrence with the censor char repeated to the match length
                matcher.reset();
                StringBuilder sb = new StringBuilder();
                while (matcher.find()) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(censorChar.repeat(matcher.group().length())));
                }
                matcher.appendTail(sb);
                working = sb.toString();
            }
        }

        // Message allowed — record it for flood detection.
        FLOOD.put(uuid, new FloodEntry(now, message));
        return Result.allow(working);
    }

    /** Collapses runs of the same character longer than {@code max} down to {@code max} copies. */
    private static String collapseRepeats(String input, int max) {
        StringBuilder sb = new StringBuilder(input.length());
        int runLength = 0;
        char previous = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == previous) {
                runLength++;
            } else {
                runLength = 1;
                previous = c;
            }
            if (runLength <= max) sb.append(c);
        }
        return sb.toString();
    }
}
