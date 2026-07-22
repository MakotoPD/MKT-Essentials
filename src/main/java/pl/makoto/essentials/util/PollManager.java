package pl.makoto.essentials.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A single active yes/no poll, inspired by FlectonePulse's poll module.
 * Kept intentionally simple: one poll at a time, one vote per player (changeable).
 */
public final class PollManager {

    private static final Map<UUID, Boolean> VOTES = new ConcurrentHashMap<>();
    private static volatile String question;
    private static volatile boolean active;

    private PollManager() {}

    public static boolean isActive() {
        return active;
    }

    public static String getQuestion() {
        return question;
    }

    public static void start(String q) {
        question = q;
        VOTES.clear();
        active = true;
    }

    /** @return true if the vote was recorded (a poll is active). */
    public static boolean vote(UUID uuid, boolean yes) {
        if (!active) return false;
        VOTES.put(uuid, yes);
        return true;
    }

    public static int yes() {
        return (int) VOTES.values().stream().filter(Boolean::booleanValue).count();
    }

    public static int no() {
        return (int) VOTES.values().stream().filter(b -> !b).count();
    }

    public static int total() {
        return VOTES.size();
    }

    public static void end() {
        active = false;
    }

    public static void cleanupPlayer(UUID uuid) {
        VOTES.remove(uuid);
    }
}
