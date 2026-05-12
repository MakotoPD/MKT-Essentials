package pl.makoto.essentials.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.data.BanEntry;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BanManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path bansFile;
    private static Map<String, BanEntry> bans = new HashMap<>(); // keyed by UUID string

    public static void init(MinecraftServer server) {
        Path dataDir = server.getWorldPath(LevelResource.ROOT).resolve("mktessentials");
        bansFile = dataDir.resolve("bans.json");
        loadBans();
    }

    public static void ban(UUID uuid, String playerName, String reason, String issuer) {
        BanEntry entry = new BanEntry(playerName, reason, issuer, System.currentTimeMillis(), -1);
        bans.put(uuid.toString(), entry);
        saveBans();
    }

    public static void tempBan(UUID uuid, String playerName, String reason, String issuer, long expirationMs) {
        BanEntry entry = new BanEntry(playerName, reason, issuer, System.currentTimeMillis(), expirationMs);
        bans.put(uuid.toString(), entry);
        saveBans();
    }

    public static void unban(UUID uuid) {
        bans.remove(uuid.toString());
        saveBans();
    }

    public static boolean isBanned(UUID uuid) {
        BanEntry entry = bans.get(uuid.toString());
        if (entry == null) return false;
        if (entry.isExpired()) {
            bans.remove(uuid.toString());
            saveBans();
            return false;
        }
        return true;
    }

    public static BanEntry getBan(UUID uuid) {
        return bans.get(uuid.toString());
    }

    public static void checkExpired(UUID uuid) {
        BanEntry entry = bans.get(uuid.toString());
        if (entry != null && entry.isExpired()) {
            bans.remove(uuid.toString());
            saveBans();
        }
    }

    private static void loadBans() {
        if (!Files.exists(bansFile)) {
            bans = new HashMap<>();
            return;
        }
        try (Reader reader = Files.newBufferedReader(bansFile)) {
            Type type = new TypeToken<Map<String, BanEntry>>(){}.getType();
            Map<String, BanEntry> loaded = GSON.fromJson(reader, type);
            bans = loaded != null ? loaded : new HashMap<>();
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to load bans.json", e);
            bans = new HashMap<>();
        }
    }

    private static void saveBans() {
        try (Writer writer = Files.newBufferedWriter(bansFile)) {
            GSON.toJson(bans, writer);
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to save bans.json", e);
        }
    }
}
