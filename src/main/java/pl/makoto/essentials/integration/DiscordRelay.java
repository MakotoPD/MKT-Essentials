package pl.makoto.essentials.integration;

import com.google.gson.Gson;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.auth.DiscordBot;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.util.MessageUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * 2-way chat bridge between a Discord channel and in-game chat, riding on the existing linking bot
 * ({@link DiscordBot}) — no extra library. Direction in: {@link DiscordBot#onMessageReceived} calls
 * {@link #fromDiscord}. Direction out: chat/join/quit hooks call {@link #mcChat}/{@link #join}/{@link #quit}.
 *
 * <p>Formats live in {@code integration.yml}; user-supplied text is sanitised (no colour-code or
 * mention injection) in both directions. Extending later (e.g. per-webhook avatars, embeds, Discord
 * slash commands relayed in) means adding to this one class.
 */
public final class DiscordRelay {

    private DiscordRelay() {}

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final int MAX_MC_LEN = 256;

    private static boolean active() {
        return Settings.isDiscordRelayEnabled() && DiscordBot.isReady()
                && !Settings.getDiscordRelayChannelId().isBlank();
    }

    // --- MC → Discord --------------------------------------------------------------------------

    public static void mcChat(ServerPlayer player, String message) {
        if (!active()) return;
        String text = Settings.getDiscordRelayToDiscord()
                .replace("<player>", player.getScoreboardName())
                .replace("<message>", message);
        // Avatar keyed by the real account name (not UUID): in offline mode the UUID is locally
        // generated so mc-heads returns Steve, but by name it resolves the real Mojang skin — same as
        // the greeting face. getName() is the login name even when the player is nicked.
        send(text, player.getScoreboardName(), player.getGameProfile().getName());
    }

    public static void join(ServerPlayer player) {
        announce(Settings.getDiscordRelayJoin(), player);
    }

    public static void quit(ServerPlayer player) {
        announce(Settings.getDiscordRelayQuit(), player);
    }

    private static void announce(String format, ServerPlayer player) {
        if (!active() || !Settings.isDiscordRelayJoinQuit()) return;
        // System-style lines always post as the bot, never through the player webhook.
        DiscordBot.sendToChannel(Settings.getDiscordRelayChannelId(),
                format.replace("<player>", player.getScoreboardName()));
    }

    /** Posts through the configured webhook (player name + avatar) or falls back to the bot. */
    private static void send(String content, String username, String avatarName) {
        String webhook = Settings.getDiscordRelayWebhookUrl();
        if (webhook == null || webhook.isBlank()) {
            DiscordBot.sendToChannel(Settings.getDiscordRelayChannelId(), content);
            return;
        }
        String body = GSON.toJson(Map.of(
                "content", content,
                "username", username,
                "avatar_url", "https://mc-heads.net/avatar/" + avatarName + "/64",
                "allowed_mentions", Map.of("parse", List.of()) // block @everyone/role pings
        ));
        HttpRequest request = HttpRequest.newBuilder(URI.create(webhook))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(e -> {
                    MKTEssentials.LOGGER.warn("Discord relay webhook failed: {}", e.getMessage());
                    return null;
                });
    }

    // --- Discord → MC --------------------------------------------------------------------------

    public static void fromDiscord(String author, String content) {
        if (!Settings.isDiscordRelayEnabled()) return;
        MinecraftServer server = MKTEssentials.getServer();
        if (server == null) return;

        // Strip colour codes from user-controlled parts so Discord users can't recolour chat, and cap length.
        String safeAuthor = strip(author);
        String safeContent = strip(content);
        if (safeContent.length() > MAX_MC_LEN) safeContent = safeContent.substring(0, MAX_MC_LEN) + "…";

        String line = Settings.getDiscordRelayFromDiscord()
                .replace("<author>", safeAuthor)
                .replace("<message>", safeContent);

        server.execute(() -> server.getPlayerList()
                .broadcastSystemMessage(MessageUtils.formatBypass(line), false));
    }

    /** Neutralises legacy colour codes (&a, §c) so Discord users can't recolour chat; keeps a bare '&'. */
    private static String strip(String s) {
        return s == null ? "" : s.replaceAll("(?i)[&§]([0-9a-fk-or])", "$1");
    }
}
