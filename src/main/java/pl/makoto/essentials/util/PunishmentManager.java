package pl.makoto.essentials.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import pl.makoto.essentials.MKTEssentials;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Persistent punishment history (warns, bans, mutes, kicks) keyed by player UUID.
 * Warns can be active or revoked; other entry types are informational records
 * shown by /history.
 */
public class PunishmentManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path punishmentsFile;
    private static Map<String, List<Entry>> punishments = new HashMap<>();

    public static class Entry {
        public String type;       // "warn", "ban", "tempban", "mute", "tempmute", "kick"
        public String reason;
        public String issuer;
        public long timestamp;
        public long durationMs;   // 0 = permanent or not applicable
        public boolean active;    // only meaningful for warns

        @SuppressWarnings("unused")
        private Entry() {} // Gson

        public Entry(String type, String reason, String issuer, long timestamp, long durationMs, boolean active) {
            this.type = type;
            this.reason = reason;
            this.issuer = issuer;
            this.timestamp = timestamp;
            this.durationMs = durationMs;
            this.active = active;
        }
    }

    public static void init(MinecraftServer server) {
        Path dataDir = server.getWorldPath(LevelResource.ROOT).resolve("mktessentials");
        punishmentsFile = dataDir.resolve("punishments.json");
        load();
    }

    public static void record(UUID uuid, String type, String reason, String issuer, long durationMs) {
        punishments.computeIfAbsent(uuid.toString(), k -> new ArrayList<>())
                .add(new Entry(type, reason, issuer, System.currentTimeMillis(), durationMs, "warn".equals(type)));
        save();
    }

    public static List<Entry> getHistory(UUID uuid) {
        return punishments.getOrDefault(uuid.toString(), List.of());
    }

    public static int countActiveWarns(UUID uuid) {
        return (int) getHistory(uuid).stream()
                .filter(e -> "warn".equals(e.type) && e.active)
                .count();
    }

    /** Revokes the most recent active warn. @return true if one was revoked */
    public static boolean revokeLatestWarn(UUID uuid) {
        List<Entry> list = punishments.get(uuid.toString());
        if (list == null) return false;
        for (int i = list.size() - 1; i >= 0; i--) {
            Entry e = list.get(i);
            if ("warn".equals(e.type) && e.active) {
                e.active = false;
                save();
                return true;
            }
        }
        return false;
    }

    /** Deactivates all warns (used after warn-threshold escalation). */
    public static void clearWarns(UUID uuid) {
        List<Entry> list = punishments.get(uuid.toString());
        if (list == null) return;
        boolean changed = false;
        for (Entry e : list) {
            if ("warn".equals(e.type) && e.active) {
                e.active = false;
                changed = true;
            }
        }
        if (changed) save();
    }

    private static void load() {
        if (!Files.exists(punishmentsFile)) {
            punishments = new HashMap<>();
            return;
        }
        try (Reader reader = Files.newBufferedReader(punishmentsFile)) {
            Type type = new TypeToken<Map<String, List<Entry>>>(){}.getType();
            Map<String, List<Entry>> loaded = GSON.fromJson(reader, type);
            punishments = loaded != null ? loaded : new HashMap<>();
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to load punishments.json", e);
            punishments = new HashMap<>();
        }
    }

    private static void save() {
        try (Writer writer = Files.newBufferedWriter(punishmentsFile)) {
            GSON.toJson(punishments, writer);
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to save punishments.json", e);
        }
    }
}
