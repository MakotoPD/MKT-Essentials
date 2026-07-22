package pl.makoto.essentials.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.util.IpBanManager;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * /geolocate &lt;player&gt; — best-effort IP geolocation via the free ip-api.com service,
 * inspired by FlectonePulse geolocate. Runs asynchronously; the result is delivered on the
 * server thread. Requires network access and only works for public IPs.
 */
public class GeoLocateCommands {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("geolocate")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.geolocate", 2))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> geolocate(context.getSource(), StringArgumentType.getString(context, "player")))));
    }

    private static int geolocate(CommandSourceStack source, String playerName) {
        MinecraftServer server = source.getServer();
        ServerPlayer online = server.getPlayerList().getPlayerByName(playerName);

        String ip;
        if (online != null) {
            ip = IpBanManager.getPlayerIp(online);
        } else {
            UUID uuid = DataManager.resolveOfflineUUID(playerName, server);
            ip = uuid != null ? DataManager.getPlayerData(uuid).getLastIp() : null;
        }

        if (ip == null || ip.isBlank()) {
            source.sendFailure(MessageUtils.prefixed("&cNo known IP address for &6" + playerName + "&c."));
            return 0;
        }

        source.sendSuccess(() -> MessageUtils.prefixed("&7Looking up location for &6" + playerName + "&7..."), false);

        ServerPlayer requester = source.getPlayer();
        UUID requesterId = requester != null ? requester.getUUID() : null;

        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "http://ip-api.com/json/" + ip + "?fields=status,message,country,regionName,city"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    String message = parse(response.body(), playerName);
                    server.execute(() -> deliver(server, requesterId, message));
                })
                .exceptionally(throwable -> {
                    server.execute(() -> deliver(server, requesterId,
                            "&cGeolocation lookup failed for &6" + playerName + "&c."));
                    return null;
                });
        return 1;
    }

    private static String parse(String body, String playerName) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!"success".equals(json.has("status") ? json.get("status").getAsString() : "")) {
                return "&cCould not locate &6" + playerName + "&c (private or unknown IP).";
            }
            String country = json.has("country") ? json.get("country").getAsString() : "?";
            String region = json.has("regionName") ? json.get("regionName").getAsString() : "";
            String city = json.has("city") ? json.get("city").getAsString() : "";
            StringBuilder location = new StringBuilder(country);
            if (!region.isBlank()) location.append(", ").append(region);
            if (!city.isBlank()) location.append(", ").append(city);
            return "&7Location of &6" + playerName + "&7: &f" + location;
        } catch (Exception e) {
            return "&cCould not parse geolocation for &6" + playerName + "&c.";
        }
    }

    private static void deliver(MinecraftServer server, UUID requesterId, String message) {
        if (requesterId == null) {
            MKTEssentials.LOGGER.info(message.replaceAll("&.", ""));
            return;
        }
        ServerPlayer requester = server.getPlayerList().getPlayer(requesterId);
        if (requester != null) {
            requester.sendSystemMessage(MessageUtils.prefixed(message));
        }
    }
}
