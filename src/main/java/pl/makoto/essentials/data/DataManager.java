package pl.makoto.essentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import pl.makoto.essentials.MKTEssentials;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path dataDir;
    private static Path playersDir;
    
    private static final Map<UUID, PlayerData> playerCache = new HashMap<>();
    private static Map<String, PlayerData.SavedLocation> warps = new HashMap<>();
    private static final Map<String, KitData> kitCache = new HashMap<>();
    private static MinecraftServer server;

    public static void init(MinecraftServer server) {
        DataManager.server = server;
        dataDir = server.getWorldPath(LevelResource.ROOT).resolve("mktessentials");
        playersDir = dataDir.resolve("players");
        
        try {
            Files.createDirectories(playersDir);
            loadWarps();
            loadKits();
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to initialize MKT Essentials data directories", e);
        }
    }

    public static PlayerData getPlayerData(UUID uuid) {
        if (playerCache.containsKey(uuid)) return playerCache.get(uuid);
        
        Path playerFile = playersDir.resolve(uuid.toString() + ".json");
        if (Files.exists(playerFile)) {
            try (Reader reader = Files.newBufferedReader(playerFile)) {
                PlayerData data = GSON.fromJson(reader, PlayerData.class);
                playerCache.put(uuid, data);
                return data;
            } catch (IOException e) {
                MKTEssentials.LOGGER.error("Failed to load player data for " + uuid, e);
            }
        }
        
        PlayerData newData = new PlayerData(uuid);
        playerCache.put(uuid, newData);
        return newData;
    }

    public static void savePlayerData(UUID uuid) {
        PlayerData data = playerCache.get(uuid);
        if (data == null) return;
        
        Path playerFile = playersDir.resolve(uuid.toString() + ".json");
        try (Writer writer = Files.newBufferedWriter(playerFile)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to save player data for " + uuid, e);
        }
    }

    private static void loadWarps() {
        Path warpsFile = dataDir.resolve("warps.json");
        if (Files.exists(warpsFile)) {
            try (Reader reader = Files.newBufferedReader(warpsFile)) {
                // Simplified warp loading
                Map<String, PlayerData.SavedLocation> loaded = GSON.fromJson(reader, Map.class);
                if (loaded != null) {
                    // Need to handle type conversion properly for warps
                }
            } catch (IOException e) {
                MKTEssentials.LOGGER.error("Failed to load warps", e);
            }
        }
    }
    
    public static Map<String, PlayerData.SavedLocation> getWarps() { return warps; }
    
    public static void saveWarps() {
        Path warpsFile = dataDir.resolve("warps.json");
        try (Writer writer = Files.newBufferedWriter(warpsFile)) {
            GSON.toJson(warps, writer);
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to save warps", e);
        }
    }

    // --- KITS ---
    public static void saveKits() {
        Path file = dataDir.resolve("kits.json");
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(kitCache, writer);
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to save kits", e);
        }
    }

    private static void loadKits() {
        Path file = dataDir.resolve("kits.json");
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, KitData>>(){}.getType();
            Map<String, KitData> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                kitCache.clear();
                kitCache.putAll(loaded);
            }
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to load kits", e);
        }
    }

    public static void addKit(KitData kit) {
        kitCache.put(kit.getName().toLowerCase(), kit);
        saveKits();
    }

    public static void deleteKit(String name) {
        kitCache.remove(name.toLowerCase());
        saveKits();
    }

    public static KitData getKit(String name) {
        return kitCache.get(name.toLowerCase());
    }

    public static java.util.Collection<KitData> getAllKits() {
        return kitCache.values();
    }
}
