package pl.makoto.essentials.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Lekki serwer HTTP (JDK built-in) — kanał przychodzący panel www → mod.
 * Odwrotność {@link WebSync}: pozwala panelowi administracyjnemu rozłączyć konto
 * bezpośrednio w grze. Źródłem prawdy dla auth jest SQLite moda, więc samo
 * wyczyszczenie wiersza po stronie strony nie wystarcza (i tak cofnąłby je backfill).
 *
 * Domyślnie WYŁĄCZONY i bindowany na 127.0.0.1 — web i mod stoją zwykle na tym
 * samym VPS, więc port nie musi być wystawiony na świat. Autoryzacja: Bearer
 * (ten sam sekret co web-sync, o ile nie nadpisany w configu).
 *
 * Endpoint:
 *   POST /unlink  body: {"discordId":"..."} lub {"minecraftUuid":"..."}
 */
public final class WebApiServer {

    private static HttpServer server;

    private WebApiServer() {}

    public static void start() {
        if (!Settings.isWebApiEnabled()) return;
        if (server != null) return;
        if (Settings.getWebApiSecret().isBlank()) {
            MKTEssentials.LOGGER.warn("[WebApi] Włączone, ale sekret jest pusty — serwer nie wystartuje.");
            return;
        }
        try {
            server = HttpServer.create(
                    new InetSocketAddress(Settings.getWebApiBind(), Settings.getWebApiPort()), 0);
            server.createContext("/unlink", WebApiServer::handleUnlink);
            server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "MKT-WebApi");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            MKTEssentials.LOGGER.info("[WebApi] Nasłuchuje na {}:{}",
                    Settings.getWebApiBind(), Settings.getWebApiPort());
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("[WebApi] Nie udało się wystartować serwera HTTP", e);
            server = null;
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            MKTEssentials.LOGGER.info("[WebApi] Zatrzymany.");
        }
    }

    private static void handleUnlink(HttpExchange ex) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                respond(ex, 405, "{\"error\":\"method_not_allowed\"}");
                return;
            }
            if (!authorized(ex)) {
                respond(ex, 401, "{\"error\":\"unauthorized\"}");
                return;
            }

            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String discordId = extract(body, "discordId");
            String mcUuid = extract(body, "minecraftUuid");

            if (discordId == null && mcUuid == null) {
                respond(ex, 400, "{\"error\":\"missing_discordId_or_minecraftUuid\"}");
                return;
            }

            boolean ok = discordId != null
                    ? AuthManager.adminUnlinkByDiscordId(discordId)
                    : AuthManager.adminUnlinkByUuid(mcUuid);

            if (ok) respond(ex, 200, "{\"ok\":true}");
            else respond(ex, 404, "{\"error\":\"account_not_found\"}");
        } catch (Exception e) {
            MKTEssentials.LOGGER.warn("[WebApi] Błąd obsługi /unlink: {}", e.getMessage());
            respond(ex, 500, "{\"error\":\"internal\"}");
        } finally {
            ex.close();
        }
    }

    private static boolean authorized(HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        return auth != null && auth.equals("Bearer " + Settings.getWebApiSecret());
    }

    private static void respond(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Minimalny ekstraktor płaskich pól string — payload panelu jest trywialny
    // ({"discordId":"...","minecraftUuid":"..."}), więc nie wciągamy parsera JSON.
    private static String extract(String json, String key) {
        String needle = "\"" + key + "\"";
        int k = json.indexOf(needle);
        if (k < 0) return null;
        int colon = json.indexOf(':', k + needle.length());
        if (colon < 0) return null;
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length() || json.charAt(i) != '"') return null; // null / liczba / brak
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                sb.append(json.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        String val = sb.toString().trim();
        return val.isEmpty() ? null : val;
    }
}
