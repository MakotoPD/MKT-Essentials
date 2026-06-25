package pl.makoto.essentials.auth;

import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Opcjonalna integracja: wysyła zdarzenia (od)linkowania do zewnętrznego backendu
 * (np. panel www / wspólna baza). W pełni konfigurowalna (auth.web-sync.* w
 * settings.yml) i domyślnie WYŁĄCZONA — MKT-Essentials jest modem publicznym,
 * więc bez konfiguracji nic nie wysyła. Wywołania są asynchroniczne i best-effort:
 * błąd HTTP nie wpływa na lokalny stan konta (SQLite pozostaje źródłem dla auth).
 */
public final class WebSync {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MKT-WebSync");
        t.setDaemon(true);
        return t;
    });

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .executor(EXECUTOR)
            .build();

    private WebSync() {}

    public static boolean isConfigured() {
        return Settings.isWebSyncEnabled() && !Settings.getWebSyncUrl().isBlank();
    }

    /** Konto połączone. mcName/discordName/avatar mogą być null. */
    public static void linked(String mcUuid, String mcName, String discordId, String discordName, String avatarUrl) {
        if (!isConfigured() || mcUuid == null || discordId == null) return;
        StringBuilder json = new StringBuilder("{");
        field(json, "action", "link", true);
        field(json, "minecraftUuid", mcUuid, false);
        if (mcName != null) field(json, "minecraftUsername", mcName, false);
        field(json, "discordId", discordId, false);
        if (discordName != null) field(json, "discordUsername", discordName, false);
        if (avatarUrl != null) field(json, "discordAvatar", avatarUrl, false);
        json.append("}");
        send(json.toString());
    }

    /** Przy starcie: wypchnij wszystkie połączone konta (idempotentny upsert na backendzie). */
    public static void backfill() {
        if (!isConfigured()) return;
        EXECUTOR.execute(() -> {
            var accounts = AccountDatabase.getLinkedAccounts();
            for (AccountDatabase.LinkedAccount a : accounts) {
                linked(a.mcUuid(), a.mcName(), a.discordId(), null, null);
            }
            MKTEssentials.LOGGER.info("[WebSync] Backfill wysłany dla {} połączonych kont.", accounts.size());
        });
    }

    /** Konto rozłączone. */
    public static void unlinked(String mcUuid, String discordId) {
        if (!isConfigured() || (mcUuid == null && discordId == null)) return;
        StringBuilder json = new StringBuilder("{");
        field(json, "action", "unlink", true);
        if (mcUuid != null) field(json, "minecraftUuid", mcUuid, false);
        if (discordId != null) field(json, "discordId", discordId, false);
        json.append("}");
        send(json.toString());
    }

    private static void send(String body) {
        String url = Settings.getWebSyncUrl();
        String secret = Settings.getWebSyncSecret();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + secret)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HTTP.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((res, err) -> {
                        if (err != null) {
                            MKTEssentials.LOGGER.warn("[WebSync] Link sync failed: {}", err.getMessage());
                        } else if (res.statusCode() < 200 || res.statusCode() >= 300) {
                            MKTEssentials.LOGGER.warn("[WebSync] Link sync HTTP {}", res.statusCode());
                        }
                    });
        } catch (Exception e) {
            MKTEssentials.LOGGER.warn("[WebSync] Link sync error: {}", e.getMessage());
        }
    }

    private static void field(StringBuilder sb, String key, String value, boolean first) {
        if (!first) sb.append(',');
        sb.append('"').append(key).append("\":\"").append(escape(value)).append('"');
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }
}
