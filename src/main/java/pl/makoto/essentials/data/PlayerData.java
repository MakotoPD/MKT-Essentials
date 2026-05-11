package pl.makoto.essentials.data;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String nickname;
    private boolean recording;
    private boolean streaming;
    private final Map<String, SavedLocation> homes = new HashMap<>();
    private long muteExpiration = 0; // 0 = not muted, -1 = permanent
    private transient Stack<SavedLocation> backStack = new Stack<>();
    private final java.util.Map<String, Long> kitCooldowns = new java.util.HashMap<>();

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
    public Stack<SavedLocation> getBackStack() { 
        if (backStack == null) backStack = new Stack<>();
        return backStack; 
    }

    public static class SavedLocation {
        public final String dimension;
        public final double x, y, z;
        public final float yaw, pitch;

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
