package pl.makoto.essentials.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.MKTEssentials;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Named schedulers defined in {@code scheduler.yml}: each owns a list of commands, a selection
 * mode and an execution schedule, and runs them through the shared console executor in
 * {@link CommandQueueManager#execute}.
 * <p>
 * Definitions live in YAML, so they survive restarts and {@code /mkt reload} picks up edits.
 * Runtime state — the next run time, sequential position, shuffle bag and run history — is
 * deliberately in memory only; it is worth nothing after a restart.
 */
@EventBusSubscriber(modid = MKTEssentials.MODID)
public class SchedulerManager {

    /** Schedulers are minute-grained at best, so a check every second is generous. */
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int HISTORY_SIZE = 10;
    /**
     * How late a time-of-day slot may still fire. Without it a server booting at noon would
     * immediately run every slot configured earlier that day.
     */
    private static final long TIME_SLOT_GRACE_MS = 5 * 60 * 1000;

    private static final Random RANDOM = new Random();

    private static Map<String, Scheduler> schedulers = new LinkedHashMap<>();
    private static int tickCounter = 0;
    private static long serverStartedAt = 0;

    public enum Mode {
        /** Run every command, in order. */
        ALL,
        /** Run one command picked uniformly at random. */
        RANDOM,
        /** Run N commands picked at random. */
        RANDOM_N,
        /** Run one command picked by weight. */
        WEIGHTED,
        /** Run the next command in the list, advancing on every run. */
        SEQUENTIAL,
        /** Run one command at random, but exhaust the whole list before repeating any. */
        SHUFFLE
    }

    public static class Entry {
        public final String command;
        public final int weight;

        Entry(String command, int weight) {
            this.command = command;
            this.weight = weight;
        }
    }

    public static class Run {
        public final long at;
        public final String command;
        public final boolean success;
        public final String output;

        Run(String command, boolean success, String output) {
            this.at = System.currentTimeMillis();
            this.command = command;
            this.success = success;
            this.output = output;
        }
    }

    public static class Scheduler {
        public String name;
        public boolean enabled = true;
        public Mode mode = Mode.ALL;
        public int randomCount = 1;
        public List<Entry> commands = new ArrayList<>();

        // Schedule
        public long intervalMinMs;
        public long intervalMaxMs;
        public long startDelayMs;
        public boolean once;
        /** Times of day to fire at; when set, the interval is ignored. */
        public List<LocalTime> at = new ArrayList<>();
        public List<DayOfWeek> days = new ArrayList<>();
        /** Extra random delay spread across the commands of one run. */
        public long spreadMs;

        // Conditions
        public int minPlayers;
        public int maxPlayers = Integer.MAX_VALUE;
        public String requiredPlayer;
        public String requiredDimension;
        public long minUptimeMs;
        public LocalTime betweenFrom;
        public LocalTime betweenTo;
        public int gameTimeFrom = -1;
        public int gameTimeTo = -1;
        /** Whether an unmet condition skips this run or holds it until the condition is met. */
        public boolean waitForConditions;

        // Runtime state
        public long nextRun;
        public long lastRun;
        public boolean finished;
        public int sequentialIndex;
        public final Deque<Integer> shuffleBag = new ArrayDeque<>();
        public final Deque<Run> history = new ArrayDeque<>();
        public int lastPicked = -1;

        public Run lastResult() {
            return history.peekLast();
        }
    }

    // ── Loading ──────────────────────────────────────────────────────────────

    /** Replaces every scheduler with the definitions from {@code scheduler.yml}. */
    @SuppressWarnings("unchecked")
    public static void load(Map<String, Object> config) {
        Map<String, Scheduler> loaded = new LinkedHashMap<>();
        Object section = config.get("schedulers");
        if (section instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> raw : map.entrySet()) {
                if (!(raw.getValue() instanceof Map<?, ?> body)) continue;
                try {
                    Scheduler scheduler = parse(raw.getKey().toString(), (Map<String, Object>) body);
                    if (scheduler != null) loaded.put(scheduler.name.toLowerCase(Locale.ROOT), scheduler);
                } catch (Exception e) {
                    MKTEssentials.LOGGER.error("Scheduler '{}' is misconfigured and was skipped", raw.getKey(), e);
                }
            }
        }
        schedulers = loaded;
        long now = System.currentTimeMillis();
        for (Scheduler scheduler : schedulers.values()) scheduler.nextRun = now + scheduler.startDelayMs;
        if (!schedulers.isEmpty()) {
            MKTEssentials.LOGGER.info("Loaded {} scheduler(s).", schedulers.size());
        }
    }

    private static Scheduler parse(String name, Map<String, Object> body) {
        Scheduler scheduler = new Scheduler();
        scheduler.name = name;
        scheduler.enabled = bool(body.get("enabled"), true);
        scheduler.once = bool(body.get("once"), false);
        scheduler.startDelayMs = duration(body.get("delay"), 0);
        scheduler.spreadMs = duration(body.get("spread"), 0);

        // every: "5m" or "5m-10m" for a random interval
        String every = body.get("every") != null ? body.get("every").toString().trim() : null;
        if (every != null && !every.isEmpty()) {
            int dash = every.indexOf('-');
            if (dash > 0) {
                scheduler.intervalMinMs = DurationParser.parse(every.substring(0, dash).trim());
                scheduler.intervalMaxMs = DurationParser.parse(every.substring(dash + 1).trim());
            } else {
                scheduler.intervalMinMs = scheduler.intervalMaxMs = DurationParser.parse(every);
            }
        }

        for (String time : stringList(body.get("at"))) scheduler.at.add(LocalTime.parse(time.trim()));
        for (String day : stringList(body.get("days"))) {
            scheduler.days.add(DayOfWeek.valueOf(day.trim().toUpperCase(Locale.ROOT)));
        }

        String mode = body.get("mode") != null ? body.get("mode").toString().trim().toLowerCase(Locale.ROOT) : "all";
        if (mode.startsWith("random:")) {
            scheduler.mode = Mode.RANDOM_N;
            scheduler.randomCount = Integer.parseInt(mode.substring("random:".length()).trim());
        } else {
            scheduler.mode = switch (mode) {
                case "random" -> Mode.RANDOM;
                case "weighted" -> Mode.WEIGHTED;
                case "sequential" -> Mode.SEQUENTIAL;
                case "shuffle" -> Mode.SHUFFLE;
                default -> Mode.ALL;
            };
        }

        parseConditions(scheduler, body.get("conditions"));

        Object commands = body.get("commands");
        if (commands instanceof List<?> list) {
            for (Object raw : list) {
                if (raw instanceof Map<?, ?> entry) {
                    Object command = entry.get("command");
                    if (command == null) continue;
                    scheduler.commands.add(new Entry(command.toString(), integer(entry.get("weight"), 1)));
                } else if (raw != null) {
                    scheduler.commands.add(new Entry(raw.toString(), 1));
                }
            }
        }
        if (scheduler.commands.isEmpty()) {
            MKTEssentials.LOGGER.warn("Scheduler '{}' has no commands and was skipped.", name);
            return null;
        }
        if (scheduler.at.isEmpty() && scheduler.intervalMaxMs <= 0 && !scheduler.once) {
            MKTEssentials.LOGGER.warn("Scheduler '{}' has neither 'every', 'at' nor 'once' and was skipped.", name);
            return null;
        }
        return scheduler;
    }

    private static void parseConditions(Scheduler scheduler, Object raw) {
        if (!(raw instanceof Map<?, ?> conditions)) return;
        scheduler.minPlayers = integer(conditions.get("min-players"), 0);
        scheduler.maxPlayers = integer(conditions.get("max-players"), Integer.MAX_VALUE);
        scheduler.minUptimeMs = duration(conditions.get("uptime"), 0);
        if (conditions.get("player") != null) scheduler.requiredPlayer = conditions.get("player").toString();
        if (conditions.get("dimension") != null) scheduler.requiredDimension = conditions.get("dimension").toString();
        scheduler.waitForConditions = "wait".equalsIgnoreCase(String.valueOf(conditions.get("when-invalid")));

        Object between = conditions.get("between");
        if (between != null) {
            String[] parts = between.toString().split("-", 2);
            if (parts.length == 2) {
                scheduler.betweenFrom = LocalTime.parse(parts[0].trim());
                scheduler.betweenTo = LocalTime.parse(parts[1].trim());
            }
        }
        Object gameTime = conditions.get("game-time");
        if (gameTime != null) {
            String[] parts = gameTime.toString().split("-", 2);
            if (parts.length == 2) {
                scheduler.gameTimeFrom = Integer.parseInt(parts[0].trim());
                scheduler.gameTimeTo = Integer.parseInt(parts[1].trim());
            }
        }
    }

    // ── Access ───────────────────────────────────────────────────────────────

    public static Scheduler get(String name) {
        return schedulers.get(name.toLowerCase(Locale.ROOT));
    }

    public static List<Scheduler> getAll() {
        return new ArrayList<>(schedulers.values());
    }

    public static List<String> getNames() {
        return schedulers.values().stream().map(s -> s.name).toList();
    }

    // ── Ticking ──────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (schedulers.isEmpty()) return;
        if (serverStartedAt == 0) serverStartedAt = System.currentTimeMillis();
        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;

        long now = System.currentTimeMillis();
        for (Scheduler scheduler : schedulers.values()) {
            if (!scheduler.enabled || scheduler.finished) continue;
            if (!isDue(scheduler, now)) continue;

            if (!conditionsMet(scheduler, event.getServer(), now)) {
                // "wait" leaves nextRun in the past so the run fires the moment conditions allow it.
                if (!scheduler.waitForConditions) reschedule(scheduler, now);
                continue;
            }

            fire(scheduler, event.getServer());
            if (scheduler.once) {
                scheduler.finished = true;
            } else {
                reschedule(scheduler, now);
            }
        }
    }

    private static boolean isDue(Scheduler scheduler, long now) {
        if (!scheduler.at.isEmpty()) return isTimeOfDayDue(scheduler, now);
        return now >= scheduler.nextRun;
    }

    /**
     * Time-of-day schedules fire once per configured time per day: a run is due when the clock has
     * passed one of the times and the last run was before it.
     */
    private static boolean isTimeOfDayDue(Scheduler scheduler, long now) {
        LocalDateTime local = LocalDateTime.now();
        if (!scheduler.days.isEmpty() && !scheduler.days.contains(local.getDayOfWeek())) return false;
        if (now < scheduler.nextRun) return false; // start delay

        for (LocalTime time : scheduler.at) {
            LocalDateTime slot = local.toLocalDate().atTime(time);
            if (local.isBefore(slot)) continue;
            long slotMillis = slot.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (now - slotMillis > TIME_SLOT_GRACE_MS) continue; // long past — do not fire it late
            if (scheduler.lastRun < slotMillis) return true;
        }
        return false;
    }

    private static void reschedule(Scheduler scheduler, long now) {
        if (!scheduler.at.isEmpty()) return; // driven by the clock, not an interval
        long interval = scheduler.intervalMaxMs > scheduler.intervalMinMs
                ? scheduler.intervalMinMs + (long) (RANDOM.nextDouble() * (scheduler.intervalMaxMs - scheduler.intervalMinMs))
                : scheduler.intervalMinMs;
        scheduler.nextRun = now + Math.max(1000, interval);
    }

    private static boolean conditionsMet(Scheduler scheduler, MinecraftServer server, long now) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.size() < scheduler.minPlayers) return false;
        if (players.size() > scheduler.maxPlayers) return false;
        if (now - serverStartedAt < scheduler.minUptimeMs) return false;

        if (scheduler.requiredPlayer != null
                && server.getPlayerList().getPlayerByName(scheduler.requiredPlayer) == null) return false;

        if (scheduler.requiredDimension != null) {
            boolean anyone = players.stream()
                    .anyMatch(p -> p.level().dimension().location().toString().equals(scheduler.requiredDimension));
            if (!anyone) return false;
        }

        if (scheduler.betweenFrom != null && !inTimeWindow(LocalTime.now(), scheduler.betweenFrom, scheduler.betweenTo)) {
            return false;
        }

        if (scheduler.gameTimeFrom >= 0) {
            long dayTime = server.overworld().getDayTime() % 24000L;
            if (!inRange(dayTime, scheduler.gameTimeFrom, scheduler.gameTimeTo)) return false;
        }
        return true;
    }

    /** Windows that wrap past midnight (22:00-06:00) are treated as one continuous period. */
    private static boolean inTimeWindow(LocalTime now, LocalTime from, LocalTime to) {
        return from.isAfter(to)
                ? !now.isBefore(from) || !now.isAfter(to)
                : !now.isBefore(from) && !now.isAfter(to);
    }

    private static boolean inRange(long value, long from, long to) {
        return from > to ? value >= from || value <= to : value >= from && value <= to;
    }

    // ── Firing ───────────────────────────────────────────────────────────────

    /** Runs a scheduler now, ignoring its schedule but not its command-selection mode. */
    public static void fire(Scheduler scheduler, MinecraftServer server) {
        scheduler.lastRun = System.currentTimeMillis();
        List<Entry> selected = select(scheduler);

        for (int i = 0; i < selected.size(); i++) {
            Entry entry = selected.get(i);
            long delayTicks = scheduler.spreadMs > 0 ? (long) (RANDOM.nextDouble() * scheduler.spreadMs) / 50 : 0;
            if (delayTicks <= 0) {
                run(scheduler, server, entry);
            } else {
                // A spread delay is seconds long; losing it on shutdown does not matter, so a plain
                // tick task beats persisting it.
                server.tell(new TickTask(server.getTickCount() + (int) delayTicks, () -> run(scheduler, server, entry)));
            }
        }
    }

    private static void run(Scheduler scheduler, MinecraftServer server, Entry entry) {
        CommandQueueManager.ExecutionResult result = CommandQueueManager.execute(server, entry.command);
        record(scheduler, new Run(entry.command, result.success(), result.output()));
        if (!result.success()) {
            MKTEssentials.LOGGER.warn("[Scheduler:{}] /{} failed: {}", scheduler.name, entry.command, result.output());
        }
    }

    private static void record(Scheduler scheduler, Run run) {
        scheduler.history.addLast(run);
        while (scheduler.history.size() > HISTORY_SIZE) scheduler.history.removeFirst();
    }

    /** Picks the commands for one run according to the scheduler's mode. */
    private static List<Entry> select(Scheduler scheduler) {
        List<Entry> commands = scheduler.commands;
        return switch (scheduler.mode) {
            case ALL -> new ArrayList<>(commands);
            case RANDOM -> List.of(commands.get(pickDistinct(scheduler, commands.size())));
            case RANDOM_N -> {
                List<Entry> picked = new ArrayList<>();
                for (int i = 0; i < Math.min(scheduler.randomCount, commands.size()); i++) {
                    picked.add(commands.get(RANDOM.nextInt(commands.size())));
                }
                yield picked;
            }
            case WEIGHTED -> List.of(commands.get(pickWeighted(commands)));
            case SEQUENTIAL -> {
                if (scheduler.sequentialIndex >= commands.size()) scheduler.sequentialIndex = 0;
                yield List.of(commands.get(scheduler.sequentialIndex++));
            }
            case SHUFFLE -> {
                if (scheduler.shuffleBag.isEmpty()) refillBag(scheduler, commands.size());
                yield List.of(commands.get(scheduler.shuffleBag.poll()));
            }
        };
    }

    /** Random pick that avoids repeating the previous command back to back. */
    private static int pickDistinct(Scheduler scheduler, int size) {
        int index = RANDOM.nextInt(size);
        if (size > 1 && index == scheduler.lastPicked) index = (index + 1 + RANDOM.nextInt(size - 1)) % size;
        scheduler.lastPicked = index;
        return index;
    }

    private static int pickWeighted(List<Entry> commands) {
        int total = commands.stream().mapToInt(e -> Math.max(0, e.weight)).sum();
        if (total <= 0) return RANDOM.nextInt(commands.size());
        int roll = RANDOM.nextInt(total);
        for (int i = 0; i < commands.size(); i++) {
            roll -= Math.max(0, commands.get(i).weight);
            if (roll < 0) return i;
        }
        return commands.size() - 1;
    }

    private static void refillBag(Scheduler scheduler, int size) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < size; i++) indices.add(i);
        Collections.shuffle(indices, RANDOM);
        scheduler.shuffleBag.addAll(indices);
    }

    // ── YAML helpers ─────────────────────────────────────────────────────────

    private static boolean bool(Object value, boolean fallback) {
        return value instanceof Boolean b ? b : fallback;
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number n ? n.intValue() : fallback;
    }

    private static long duration(Object value, long fallback) {
        if (value == null) return fallback;
        try {
            return DurationParser.parse(value.toString().trim());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) return list.stream().map(Object::toString).toList();
        if (value != null) return List.of(value.toString());
        return List.of();
    }
}
