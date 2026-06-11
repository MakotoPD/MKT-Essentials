package pl.makoto.essentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.MKTEssentials;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = MKTEssentials.MODID)
public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path dataDir;
    private static Path playersDir;
    
    private static final Map<UUID, PlayerData> playerCache = new HashMap<>();
    private static Map<String, PlayerData.SavedLocation> warps = new HashMap<>();
    private static final Map<String, KitData> kitCache = new HashMap<>();
    private static MinecraftServer server;

    // Auto-save tick counter
    private static long lastSaveTick = 0;

    public static void init(MinecraftServer server) {
        DataManager.server = server;
        dataDir = server.getWorldPath(LevelResource.ROOT).resolve("mktessentials");
        playersDir = dataDir.resolve("players");
        
        try {
            Files.createDirectories(playersDir);
            loadWarps();
            loadKits();
            loadSpawn();
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to initialize MKT Essentials data directories", e);
        }
    }

    public static PlayerData getPlayerData(UUID uuid) {
        if (playerCache.containsKey(uuid)) return playerCache.get(uuid);
        
        Path playerFile = playersDir.resolve(uuid.toString() + ".json");
        if (Files.exists(playerFile)) {
            // Catch Exception, not just IOException — a corrupted file throws JsonSyntaxException,
            // and an empty file yields null; both must fall through to fresh data instead of
            // poisoning the cache or crashing the join handler.
            try (Reader reader = Files.newBufferedReader(playerFile)) {
                PlayerData data = GSON.fromJson(reader, PlayerData.class);
                if (data != null) {
                    playerCache.put(uuid, data);
                    return data;
                }
                MKTEssentials.LOGGER.warn("Player data file for {} is empty — creating fresh data.", uuid);
            } catch (Exception e) {
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

        // Data modified for offline players (e.g. /mute on someone offline) would otherwise
        // sit in the cache forever — there is no quit event to evict it.
        if (server != null && server.getPlayerList().getPlayer(uuid) == null) {
            playerCache.remove(uuid);
        }
    }

    public static void evictPlayer(UUID uuid) {
        savePlayerData(uuid);
        playerCache.remove(uuid);
    }

    public static void saveAll() {
        // Copy — savePlayerData may evict offline players' entries while we iterate
        for (UUID uuid : new java.util.ArrayList<>(playerCache.keySet())) {
            savePlayerData(uuid);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int interval = Settings.getAutoSaveInterval();
        if (interval <= 0) return;

        lastSaveTick++;
        if (lastSaveTick >= interval * 20L) {
            lastSaveTick = 0;
            saveAll();
        }
    }

    private static void loadWarps() {
        Path warpsFile = dataDir.resolve("warps.json");
        if (!Files.exists(warpsFile)) return;
        try (Reader reader = Files.newBufferedReader(warpsFile)) {
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, PlayerData.SavedLocation>>(){}.getType();
            Map<String, PlayerData.SavedLocation> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                warps = loaded;
            }
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to load warps", e);
            warps = new HashMap<>();
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

    // --- SPAWN ---
    private static PlayerData.SavedLocation spawnLocation;

    public static PlayerData.SavedLocation getSpawn() {
        return spawnLocation;
    }

    public static void setSpawn(PlayerData.SavedLocation loc) {
        spawnLocation = loc;
        Path file = dataDir.resolve("spawn.json");
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(loc, writer);
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to save spawn.json", e);
        }
    }

    private static void loadSpawn() {
        Path file = dataDir.resolve("spawn.json");
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            spawnLocation = GSON.fromJson(reader, PlayerData.SavedLocation.class);
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to load spawn.json", e);
        }
    }

    // --- OFFLINE UUID RESOLUTION ---
    public static UUID resolveOfflineUUID(String playerName, MinecraftServer server) {
        Optional<com.mojang.authlib.GameProfile> profile = server.getProfileCache().get(playerName);
        return profile.map(com.mojang.authlib.GameProfile::getId).orElse(null);
    }
}
