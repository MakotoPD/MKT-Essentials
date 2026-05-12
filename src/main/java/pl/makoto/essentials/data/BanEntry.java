package pl.makoto.essentials.data;

public class BanEntry {
    private String playerName;
    private String reason;
    private String issuer;
    private long bannedAt;    // epoch millis
    private long expiresAt;   // epoch millis, -1 = permanent

    // No-arg constructor for Gson deserialization
    private BanEntry() {}

    public BanEntry(String playerName, String reason, String issuer, long bannedAt, long expiresAt) {
        this.playerName = playerName;
        this.reason = reason;
        this.issuer = issuer;
        this.bannedAt = bannedAt;
        this.expiresAt = expiresAt;
    }

    // Getters
    public String getPlayerName() { return playerName; }
    public String getReason() { return reason; }
    public String getIssuer() { return issuer; }
    public long getBannedAt() { return bannedAt; }
    public long getExpiresAt() { return expiresAt; }

    public boolean isPermanent() { return expiresAt == -1; }
    public boolean isExpired() { return !isPermanent() && System.currentTimeMillis() > expiresAt; }
}
