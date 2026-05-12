package pl.makoto.essentials.auth;

public enum AuthMode {
    FULL, AUTH_ONLY, LINK_ONLY, OPTIONAL, DISABLED;

    public static AuthMode fromString(String s) {
        if (s == null) return DISABLED;
        return switch (s.toLowerCase().trim()) {
            case "full" -> FULL;
            case "auth-only" -> AUTH_ONLY;
            case "link-only" -> LINK_ONLY;
            case "optional" -> OPTIONAL;
            default -> DISABLED;
        };
    }

    public boolean requiresPassword() { return this == FULL || this == AUTH_ONLY; }
    public boolean requiresLink() { return this == FULL || this == LINK_ONLY; }
    public boolean requiresAnyAuth() { return this != OPTIONAL && this != DISABLED; }
}
