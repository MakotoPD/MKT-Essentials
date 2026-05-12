package pl.makoto.essentials.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.data.ShadowBanEntry;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ShadowBanManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path shadowBansFile;
    private static Map<String, ShadowBanEntry> shadowBans = new HashMap<>(); // keyed by UUID string

    // Set of UUIDs currently in "phantom" mode (online and active)
    private static final Set<UUID> phantomPlayers = new HashSet<>();

    public static void init(MinecraftServer server) {
        Path dataDir = server.getWorldPath(LevelResource.ROOT).resolve("mktessentials");
        shadowBansFile = dataDir.resolve("shadowbans.json");
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to create mktessentials data directory", e);
        }
        loadShadowBans();
    }

    public static void shadowBan(UUID uuid, String playerName, String reason, String issuer) {
        ShadowBanEntry entry = new ShadowBanEntry(playerName, reason, issuer, System.currentTimeMillis());
        shadowBans.put(uuid.toString(), entry);
        saveShadowBans();
    }

    public static void unShadowBan(UUID uuid) {
        shadowBans.remove(uuid.toString());
        phantomPlayers.remove(uuid);
        saveShadowBans();
    }

    public static boolean isShadowBanned(UUID uuid) {
        return shadowBans.containsKey(uuid.toString());
    }

    public static ShadowBanEntry getShadowBan(UUID uuid) {
        return shadowBans.get(uuid.toString());
    }

    public static List<ShadowBanEntry> listAll() {
        return new ArrayList<>(shadowBans.values());
    }

    public static Map<String, ShadowBanEntry> getAll() {
        return Collections.unmodifiableMap(shadowBans);
    }

    // Phantom mode management
    public static void addPhantom(UUID uuid) {
        phantomPlayers.add(uuid);
    }

    public static void removePhantom(UUID uuid) {
        phantomPlayers.remove(uuid);
    }

    public static boolean isPhantom(UUID uuid) {
        return phantomPlayers.contains(uuid);
    }

    private static void loadShadowBans() {
        if (!Files.exists(shadowBansFile)) {
            shadowBans = new HashMap<>();
            return;
        }
        try (Reader reader = Files.newBufferedReader(shadowBansFile)) {
            Type type = new TypeToken<Map<String, ShadowBanEntry>>(){}.getType();
            Map<String, ShadowBanEntry> loaded = GSON.fromJson(reader, type);
            shadowBans = loaded != null ? loaded : new HashMap<>();
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to load shadowbans.json", e);
            shadowBans = new HashMap<>();
        }
    }

    private static void saveShadowBans() {
        try (Writer writer = Files.newBufferedWriter(shadowBansFile)) {
            GSON.toJson(shadowBans, writer);
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to save shadowbans.json", e);
        }
    }
}
