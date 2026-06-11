package pl.makoto.essentials.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import pl.makoto.essentials.MKTEssentials;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * IP ban storage and lookup. Bans are keyed by plain IP string (no port).
 */
public class IpBanManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path ipBansFile;
    private static Map<String, IpBanEntry> ipBans = new HashMap<>();

    public static class IpBanEntry {
        public String playerName; // last known name associated with this IP (may be null)
        public String reason;
        public String issuer;
        public long bannedAt;

        @SuppressWarnings("unused")
        private IpBanEntry() {} // Gson

        public IpBanEntry(String playerName, String reason, String issuer, long bannedAt) {
            this.playerName = playerName;
            this.reason = reason;
            this.issuer = issuer;
            this.bannedAt = bannedAt;
        }
    }

    public static void init(MinecraftServer server) {
        Path dataDir = server.getWorldPath(LevelResource.ROOT).resolve("mktessentials");
        ipBansFile = dataDir.resolve("ipbans.json");
        load();
    }

    public static void ban(String ip, String playerName, String reason, String issuer) {
        ipBans.put(ip, new IpBanEntry(playerName, reason, issuer, System.currentTimeMillis()));
        save();
    }

    public static boolean unban(String ip) {
        boolean removed = ipBans.remove(ip) != null;
        if (removed) save();
        return removed;
    }

    public static boolean isBanned(String ip) {
        return ip != null && ipBans.containsKey(ip);
    }

    public static IpBanEntry getBan(String ip) {
        return ipBans.get(ip);
    }

    public static Map<String, IpBanEntry> getAll() {
        return ipBans;
    }

    /** Extracts the plain IP (no port, no leading slash) from a player's connection. */
    public static String getPlayerIp(ServerPlayer player) {
        var addr = player.connection.getRemoteAddress();
        if (addr == null) return null;
        String full = addr.toString();
        if (full.startsWith("/")) full = full.substring(1);
        int colonIdx = full.lastIndexOf(':');
        return colonIdx > 0 ? full.substring(0, colonIdx) : full;
    }

    private static void load() {
        if (!Files.exists(ipBansFile)) {
            ipBans = new HashMap<>();
            return;
        }
        try (Reader reader = Files.newBufferedReader(ipBansFile)) {
            Type type = new TypeToken<Map<String, IpBanEntry>>(){}.getType();
            Map<String, IpBanEntry> loaded = GSON.fromJson(reader, type);
            ipBans = loaded != null ? loaded : new HashMap<>();
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to load ipbans.json", e);
            ipBans = new HashMap<>();
        }
    }

    private static void save() {
        try (Writer writer = Files.newBufferedWriter(ipBansFile)) {
            GSON.toJson(ipBans, writer);
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to save ipbans.json", e);
        }
    }
}
