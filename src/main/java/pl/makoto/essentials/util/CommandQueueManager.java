package pl.makoto.essentials.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.auth.AuthManager;
import pl.makoto.essentials.auth.AuthMode;
import pl.makoto.essentials.config.Settings;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistent queue of console commands to run for a player once a condition is met.
 * <p>
 * The four delivery modes from the feature request collapse into two fields:
 * {@code executeAt} (0 = as soon as possible) and {@code requireOnline}. An entry runs
 * when the deadline has passed and — if gated — the target is online and authenticated.
 * <p>
 * A single poll on the server tick drives everything, so a player joining is picked up
 * within {@link #CHECK_INTERVAL_TICKS} ticks without a separate join hook, and entries
 * whose deadline passed while the server was down simply run on the next boot.
 */
@EventBusSubscriber(modid = MKTEssentials.MODID)
public class CommandQueueManager {

    /** How often the queue is scanned. Commands are never time-critical, so 2s is plenty. */
    private static final int CHECK_INTERVAL_TICKS = 40;
    /** Attempts before an entry is parked as FAILED so a broken command can't loop forever. */
    private static final int MAX_ATTEMPTS = 3;
    /** Finished entries older than this are pruned on load. */
    // ponytail: constants, not config — promote to commands.yml if anyone actually wants to tune them
    private static final long RETENTION_MS = 30L * 24 * 60 * 60 * 1000;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path queueFile;
    private static List<Entry> queue = new ArrayList<>();
    private static int tickCounter = 0;

    public enum Status { PENDING, COMPLETED, FAILED, CANCELLED }

    public static class Entry {
        public int id;
        /** Null until the target is seen for the first time — a player may be bought a rank before ever joining. */
        public String targetUuid;
        public String targetName;
        public String command;
        /** Epoch millis; 0 = as soon as possible. */
        public long executeAt;
        /** Whether the target must be online (and authenticated) for the command to run. */
        public boolean requireOnline;
        public Status status;
        public String createdBy;
        public long createdAt;
        public long executedAt;
        /** Console output or error text from the run attempt. */
        public String result;
        public int attempts;
        /** Optional caller-supplied idempotency key — a webstore retry must not pay out twice. */
        public String dedupeKey;

        @SuppressWarnings("unused")
        private Entry() {} // Gson

        Entry(int id, UUID targetUuid, String targetName, String command, long executeAt,
              boolean requireOnline, String createdBy, String dedupeKey) {
            this.id = id;
            this.targetUuid = targetUuid != null ? targetUuid.toString() : null;
            this.targetName = targetName;
            this.command = command;
            this.executeAt = executeAt;
            this.requireOnline = requireOnline;
            this.status = Status.PENDING;
            this.createdBy = createdBy;
            this.createdAt = System.currentTimeMillis();
            this.dedupeKey = dedupeKey;
        }

        /** @return the target's UUID, or null if it is not known yet */
        public UUID uuid() {
            if (targetUuid == null) return null;
            try {
                return UUID.fromString(targetUuid);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        public boolean isFinished() {
            return status != Status.PENDING;
        }
    }

    public static void init(MinecraftServer server) {
        queueFile = server.getWorldPath(LevelResource.ROOT).resolve("mktessentials").resolve("command-queue.json");
        load();
    }

    // ── Queue management ─────────────────────────────────────────────────────

    /**
     * Queues a command.
     *
     * @param target       the target's UUID, or null if they have never joined — then the name is
     *                     matched instead and the UUID is filled in the first time they show up
     * @param delayMs      milliseconds to wait before running; 0 = as soon as possible
     * @param dedupeKey    optional idempotency key; null to skip the duplicate check
     * @return the new entry, or null if an entry with the same dedupeKey is already pending or completed
     */
    public static Entry add(UUID target, String targetName, String command, long delayMs,
                            boolean requireOnline, String createdBy, String dedupeKey) {
        if (dedupeKey != null && findByDedupeKey(dedupeKey) != null) return null;
        Entry entry = new Entry(nextId(), target, targetName, command,
                delayMs > 0 ? System.currentTimeMillis() + delayMs : 0,
                requireOnline, createdBy, dedupeKey);
        queue.add(entry);
        save();
        return entry;
    }

    /** An existing entry with this key that still counts as "already handled" (pending or completed). */
    public static Entry findByDedupeKey(String dedupeKey) {
        for (Entry e : queue) {
            if (dedupeKey.equals(e.dedupeKey)
                    && (e.status == Status.PENDING || e.status == Status.COMPLETED)) return e;
        }
        return null;
    }

    public static Entry get(int id) {
        for (Entry e : queue) {
            if (e.id == id) return e;
        }
        return null;
    }

    /** Newest first — the order every listing uses. */
    public static List<Entry> getAll() {
        List<Entry> copy = new ArrayList<>(queue);
        copy.sort((a, b) -> Integer.compare(b.id, a.id));
        return copy;
    }

    public static List<Entry> getByStatus(Status status) {
        return getAll().stream().filter(e -> e.status == status).toList();
    }

    /** Entries for a target, matched by UUID or — for targets never seen before — by name. */
    public static List<Entry> getByPlayer(UUID uuid, String name) {
        String key = uuid != null ? uuid.toString() : null;
        return getAll().stream()
                .filter(e -> (key != null && key.equals(e.targetUuid)) || name.equalsIgnoreCase(e.targetName))
                .toList();
    }

    /** @return true if the entry was pending and is now cancelled */
    public static boolean cancel(int id) {
        Entry e = get(id);
        if (e == null || e.status != Status.PENDING) return false;
        e.status = Status.CANCELLED;
        save();
        return true;
    }

    /** Puts a failed or cancelled entry back in line, clearing its attempt counter. */
    public static boolean retry(int id) {
        Entry e = get(id);
        if (e == null || e.status == Status.PENDING || e.status == Status.COMPLETED) return false;
        e.status = Status.PENDING;
        e.attempts = 0;
        e.result = null;
        e.executedAt = 0;
        save();
        return true;
    }

    /** Hard-deletes one entry regardless of status. */
    public static boolean remove(int id) {
        boolean removed = queue.removeIf(e -> e.id == id);
        if (removed) save();
        return removed;
    }

    /** Hard-deletes every finished entry. @return how many were removed */
    public static int clearFinished() {
        int before = queue.size();
        queue.removeIf(Entry::isFinished);
        int removed = before - queue.size();
        if (removed > 0) save();
        return removed;
    }

    public static int countPending() {
        return (int) queue.stream().filter(e -> e.status == Status.PENDING).count();
    }

    private static int nextId() {
        int max = 0;
        for (Entry e : queue) max = Math.max(max, e.id);
        return max + 1;
    }

    // ── Execution ────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (queueFile == null) return;
        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;
        process(event.getServer());
    }

    private static void process(MinecraftServer server) {
        long now = System.currentTimeMillis();
        boolean changed = false;
        // Snapshot: a queued command may itself queue more commands.
        for (Entry entry : new ArrayList<>(queue)) {
            if (entry.status != Status.PENDING) continue;
            if (entry.executeAt > now) continue;

            ServerPlayer target = null;
            if (entry.requireOnline) {
                UUID uuid = entry.uuid();
                target = uuid != null ? server.getPlayerList().getPlayer(uuid)
                        : server.getPlayerList().getPlayerByName(entry.targetName);
                // Not around yet (or still stuck on the login screen) — try again next cycle.
                if (target == null || !isReady(target)) continue;
                // First sighting of a target queued by name only: remember who they actually are.
                if (entry.targetUuid == null) entry.targetUuid = target.getUUID().toString();
            }

            run(server, entry, target);
            changed = true;
        }
        if (changed) save();
    }

    /**
     * Whether a reward may be handed to this player yet. With the auth system off nobody is ever
     * marked authenticated, so the login gate only applies when auth is actually enabled.
     */
    private static boolean isReady(ServerPlayer player) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return true;
        return AuthManager.isAuthenticated(player.getUUID());
    }

    private static void run(MinecraftServer server, Entry entry, ServerPlayer target) {
        entry.attempts++;
        String command = resolvePlaceholders(entry, target);
        ExecutionResult result = execute(server, command);

        if (result.success()) {
            entry.status = Status.COMPLETED;
            entry.executedAt = System.currentTimeMillis();
            entry.result = result.output();
            MKTEssentials.LOGGER.info("[CommandQueue] #{} ran for {}: /{}", entry.id, entry.targetName, command);
        } else {
            recordFailure(entry, result.output(), command);
        }
    }

    /** What running one command produced: whether it parsed and ran, and whatever it printed. */
    public record ExecutionResult(boolean success, String output) {}

    /**
     * Runs a command as the server console (permission level 4), capturing its output instead of
     * spilling it into chat. Shared with the scheduler so both report failures the same way.
     */
    public static ExecutionResult execute(MinecraftServer server, String command) {
        StringBuilder output = new StringBuilder();
        CommandSourceStack source = server.createCommandSourceStack().withSource(new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                if (output.length() < 240) output.append(message.getString()).append(' ');
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        });

        try {
            server.getCommands().getDispatcher().execute(command.startsWith("/") ? command.substring(1) : command, source);
            return new ExecutionResult(true, output.isEmpty() ? "OK" : output.toString().trim());
        } catch (CommandSyntaxException e) {
            return new ExecutionResult(false, e.getMessage());
        } catch (Exception e) {
            return new ExecutionResult(false, e.toString());
        }
    }

    private static void recordFailure(Entry entry, String message, String command) {
        if (entry.attempts >= MAX_ATTEMPTS) {
            fail(entry, message);
            MKTEssentials.LOGGER.warn("[CommandQueue] #{} failed permanently after {} attempts (/{}): {}",
                    entry.id, entry.attempts, command, message);
        } else {
            entry.result = message; // stays PENDING for the next cycle
            MKTEssentials.LOGGER.warn("[CommandQueue] #{} attempt {}/{} failed (/{}): {}",
                    entry.id, entry.attempts, MAX_ATTEMPTS, command, message);
        }
    }

    private static void fail(Entry entry, String message) {
        entry.status = Status.FAILED;
        entry.executedAt = System.currentTimeMillis();
        entry.result = message;
    }

    /** Substitutes the target into the command. */
    private static String resolvePlaceholders(Entry entry, ServerPlayer target) {
        String name = target != null ? target.getScoreboardName() : entry.targetName;
        return entry.command
                .replace("%player%", name)
                .replace("{player}", name)
                .replace("%uuid%", entry.targetUuid != null ? entry.targetUuid : "");
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private static void load() {
        if (!Files.exists(queueFile)) {
            queue = new ArrayList<>();
            return;
        }
        try (Reader reader = Files.newBufferedReader(queueFile)) {
            Type type = new TypeToken<List<Entry>>(){}.getType();
            List<Entry> loaded = GSON.fromJson(reader, type);
            queue = loaded != null ? new ArrayList<>(loaded) : new ArrayList<>();
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to load command-queue.json", e);
            queue = new ArrayList<>();
            return;
        }

        long cutoff = System.currentTimeMillis() - RETENTION_MS;
        int before = queue.size();
        // A null status means a hand-edited or corrupted file — treat it as still pending.
        queue.forEach(e -> {
            if (e.status == null) e.status = Status.PENDING;
        });
        queue.removeIf(e -> e.isFinished() && e.executedAt > 0 && e.executedAt < cutoff);
        if (queue.size() != before) save();

        int pending = countPending();
        if (pending > 0) {
            MKTEssentials.LOGGER.info("Command queue loaded — {} command(s) still pending.", pending);
        }
    }

    private static void save() {
        if (queueFile == null) return;
        try {
            Files.createDirectories(queueFile.getParent());
            try (Writer writer = Files.newBufferedWriter(queueFile)) {
                GSON.toJson(queue, writer);
            }
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to save command-queue.json", e);
        }
    }
}
