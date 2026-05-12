package pl.makoto.essentials.data;

public class ShadowBanEntry {
    private String playerName;
    private String reason;
    private String issuer;
    private long bannedAt; // epoch millis

    // No-arg constructor for Gson deserialization
    private ShadowBanEntry() {}

    public ShadowBanEntry(String playerName, String reason, String issuer, long bannedAt) {
        this.playerName = playerName;
        this.reason = reason;
        this.issuer = issuer;
        this.bannedAt = bannedAt;
    }

    // Getters
    public String getPlayerName() { return playerName; }
    public String getReason() { return reason; }
    public String getIssuer() { return issuer; }
    public long getBannedAt() { return bannedAt; }
}
