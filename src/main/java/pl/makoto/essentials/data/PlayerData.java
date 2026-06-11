package pl.makoto.essentials.data;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String nickname;
    private boolean recording;
    private boolean streaming;
    private final Map<String, SavedLocation> homes = new HashMap<>();
    private long muteExpiration = 0; // 0 = not muted, -1 = permanent
    private boolean godMode = false;
    private boolean flyEnabled = false;
    private boolean vanished = false;
    private float flySpeed = 0f;
    private float walkSpeed = 0f;
    private transient Deque<SavedLocation> backStack = new ArrayDeque<>();
    private final java.util.Map<String, Long> kitCooldowns = new java.util.HashMap<>();
    // Gson skips field initializers when deserializing (no no-arg constructor), so new
    // booleans are stored inverted — a missing field defaults to false = feature enabled.
    private boolean msgDisabled = false;
    private boolean tpaDisabled = false;
    private java.util.Set<String> ignoredPlayers = new java.util.HashSet<>();
    private long playTimeMillis = 0;
    private long firstJoinAt = 0;
    private long lastSeenAt = 0;
    private String lastIp;
    private transient long sessionStartMs = 0;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public java.util.Map<String, Long> getKitCooldowns() {
        return kitCooldowns;
    }

    public void setKitCooldown(String kitName, long expirationTimestamp) {
        kitCooldowns.put(kitName, expirationTimestamp);
    }

    public UUID getUuid() { return uuid; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public boolean isRecording() { return recording; }
    public void setRecording(boolean recording) { this.recording = recording; }
    
    public boolean isStreaming() { return streaming; }
    public void setStreaming(boolean streaming) { this.streaming = streaming; }

    public Map<String, SavedLocation> getHomes() { return homes; }
    public long getMuteExpiration() { return muteExpiration; }
    public void setMuteExpiration(long expiration) { this.muteExpiration = expiration; }

    public boolean isGodMode() { return godMode; }
    public void setGodMode(boolean godMode) { this.godMode = godMode; }

    public boolean isFlyEnabled() { return flyEnabled; }
    public void setFlyEnabled(boolean flyEnabled) { this.flyEnabled = flyEnabled; }

    public boolean isVanished() { return vanished; }
    public void setVanished(boolean vanished) { this.vanished = vanished; }

    public float getFlySpeed() { return flySpeed; }
    public void setFlySpeed(float flySpeed) { this.flySpeed = flySpeed; }

    public float getWalkSpeed() { return walkSpeed; }
    public void setWalkSpeed(float walkSpeed) { this.walkSpeed = walkSpeed; }
    public Deque<SavedLocation> getBackStack() {
        if (backStack == null) backStack = new ArrayDeque<>();
        return backStack;
    }

    public void pushBackLocation(SavedLocation loc) {
        if (backStack == null) backStack = new ArrayDeque<>();
        if (backStack.size() >= 10) {
            backStack.removeFirst(); // Remove oldest entry
        }
        backStack.addLast(loc);
    }

    public SavedLocation popBackLocation() {
        if (backStack == null || backStack.isEmpty()) return null;
        return backStack.removeLast();
    }

    public boolean isMsgDisabled() { return msgDisabled; }
    public void setMsgDisabled(boolean msgDisabled) { this.msgDisabled = msgDisabled; }

    public boolean isTpaDisabled() { return tpaDisabled; }
    public void setTpaDisabled(boolean tpaDisabled) { this.tpaDisabled = tpaDisabled; }

    private java.util.Set<String> ignored() {
        if (ignoredPlayers == null) ignoredPlayers = new java.util.HashSet<>();
        return ignoredPlayers;
    }

    public boolean isIgnoring(UUID other) {
        return ignored().contains(other.toString());
    }

    /** @return true if the player is now ignored, false if un-ignored */
    public boolean toggleIgnore(UUID other) {
        String key = other.toString();
        if (!ignored().remove(key)) {
            ignored().add(key);
            return true;
        }
        return false;
    }

    public long getFirstJoinAt() { return firstJoinAt; }
    public long getLastSeenAt() { return lastSeenAt; }
    public String getLastIp() { return lastIp; }

    public void startSession(long now, String ip) {
        sessionStartMs = now;
        lastSeenAt = now;
        lastIp = ip;
        if (firstJoinAt == 0) firstJoinAt = now;
    }

    public void endSession(long now) {
        if (sessionStartMs > 0) {
            playTimeMillis += Math.max(0, now - sessionStartMs);
            sessionStartMs = 0;
        }
        lastSeenAt = now;
    }

    /** Total play time including the current session (if online). */
    public long getTotalPlayTimeMillis() {
        long total = playTimeMillis;
        if (sessionStartMs > 0) total += Math.max(0, System.currentTimeMillis() - sessionStartMs);
        return total;
    }

    public static class SavedLocation {
        public String dimension;
        public double x, y, z;
        public float yaw, pitch;

        private SavedLocation() {} // No-arg constructor for Gson deserialization

        public SavedLocation(String dimension, Vec3 pos, float yaw, float pitch) {
            this.dimension = dimension;
            this.x = pos.x;
            this.y = pos.y;
            this.z = pos.z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
