package pl.makoto.essentials.auth;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.mindrot.jbcrypt.BCrypt;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.util.MessageUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class AuthManager {

    // Result enums
    public enum RegisterResult { SUCCESS, PASSWORDS_DONT_MATCH, ALREADY_REGISTERED, MUST_LINK_FIRST, PASSWORD_TOO_SHORT, PASSWORD_TOO_LONG }
    public enum LoginResult { SUCCESS, WRONG_PASSWORD, NOT_REGISTERED, MAX_ATTEMPTS_EXCEEDED }
    public enum ChangePasswordResult { SUCCESS, WRONG_OLD_PASSWORD, NEW_PASSWORDS_DONT_MATCH, PASSWORD_TOO_SHORT, PASSWORD_TOO_LONG }
    public enum LinkResult { SUCCESS, CODE_INVALID, CODE_EXPIRED, DISCORD_ALREADY_LINKED }

    private static final Set<UUID> authenticatedPlayers = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> loginAttempts = new ConcurrentHashMap<>();

    private static MinecraftServer server;

    private AuthManager() {}

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    public static void init(MinecraftServer srv) {
        server = srv;
        if (Settings.getAuthMode() == AuthMode.DISABLED) {
            MKTEssentials.LOGGER.info("Auth system is disabled.");
            return;
        }

        AccountDatabase.init();
        AccountDatabase.deleteExpiredSessions();
        MKTEssentials.LOGGER.info("Auth system initialized (mode: {}).", Settings.getAuthMode().name());

        if (Settings.isDiscordEnabled()) {
            DiscordBot.initAsync();
        }
    }

    public static void shutdown() {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        DiscordBot.shutdown();
        AccountDatabase.shutdown();
        authenticatedPlayers.clear();
        loginAttempts.clear();
        MKTEssentials.LOGGER.info("Auth system shut down.");
    }

    // ─── State Queries ─────────────────────────────────────────────────────────

    public static boolean isAuthenticated(UUID uuid) {
        return authenticatedPlayers.contains(uuid);
    }

    public static boolean needsLink(UUID uuid) {
        AuthMode mode = Settings.getAuthMode();
        if (!mode.requiresLink()) return false;
        AccountDatabase.AccountRecord account = AccountDatabase.getAccount(uuid);
        return account == null || account.discordId() == null || account.discordId().isEmpty();
    }

    public static boolean needsPassword(UUID uuid) {
        AuthMode mode = Settings.getAuthMode();
        if (!mode.requiresPassword()) return false;
        AccountDatabase.AccountRecord account = AccountDatabase.getAccount(uuid);
        return account == null || account.passwordHash() == null || account.passwordHash().isEmpty();
    }

    // ─── Join / Quit ───────────────────────────────────────────────────────────

    public static void handleJoin(ServerPlayer player) {
        AuthMode mode = Settings.getAuthMode();
        if (mode == AuthMode.DISABLED) return;

        UUID uuid = player.getUUID();
        String ip = getPlayerIp(player);

        // Check for valid session
        AccountDatabase.SessionRecord session = AccountDatabase.getSession(uuid);
        if (session != null && session.sessionIp().equals(ip)) {
            // Valid session — auto-authenticate
            markAuthenticated(player);
            AccountDatabase.updateLastLogin(uuid, ip);
            return;
        }

        // Determine what the player needs
        AccountDatabase.AccountRecord account = AccountDatabase.getAccount(uuid);

        if (mode == AuthMode.OPTIONAL) {
            // Optional mode — authenticate immediately
            markAuthenticated(player);
            if (account == null) {
                // First join in optional mode — activate newbie protection
                activateNewbieIfFirstJoin(player, true);
            }
            return;
        }

        // Player needs to authenticate — freeze them
        FreezeManager.freeze(player);
        NewbieProtection.pauseTimer(uuid);

        if (mode.requiresLink() && (account == null || account.discordId() == null || account.discordId().isEmpty())) {
            // Needs Discord link
            player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.welcome-link-required")));
            String code = generateLinkCode(uuid);
            player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.link-code", "code", code)));
            player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.link-instruction",
                    "command", Settings.getDiscordLinkCommandName(), "code", code)));
        } else if (mode.requiresPassword()) {
            if (account == null || account.passwordHash() == null || account.passwordHash().isEmpty()) {
                // Needs to register
                player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.welcome-register")));
            } else {
                // Needs to login
                player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.welcome-login")));
            }
        }
    }

    public static void handleQuit(UUID uuid) {
        authenticatedPlayers.remove(uuid);
        loginAttempts.remove(uuid);
        FreezeManager.cleanup(uuid);
        NewbieProtection.cleanup(uuid);
    }

    // ─── Registration ──────────────────────────────────────────────────────────

    public static RegisterResult register(ServerPlayer player, String password, String confirmPassword) {
        UUID uuid = player.getUUID();
        AuthMode mode = Settings.getAuthMode();

        // Check if must link first in FULL mode
        if (mode == AuthMode.FULL) {
            AccountDatabase.AccountRecord account = AccountDatabase.getAccount(uuid);
            if (account == null || account.discordId() == null || account.discordId().isEmpty()) {
                return RegisterResult.MUST_LINK_FIRST;
            }
        }

        // Check if already registered
        AccountDatabase.AccountRecord existing = AccountDatabase.getAccount(uuid);
        if (existing != null && existing.passwordHash() != null && !existing.passwordHash().isEmpty()) {
            return RegisterResult.ALREADY_REGISTERED;
        }

        // Validate password
        if (password.length() < 4) return RegisterResult.PASSWORD_TOO_SHORT;
        if (password.length() > 64) return RegisterResult.PASSWORD_TOO_LONG;
        if (!password.equals(confirmPassword)) return RegisterResult.PASSWORDS_DONT_MATCH;

        // Hash and store
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        String ip = getPlayerIp(player);

        if (existing != null) {
            // Account exists (from link) — update password
            AccountDatabase.updatePasswordHash(uuid, hash);
            AccountDatabase.updateLastLogin(uuid, ip);
        } else {
            // Create new account
            AccountDatabase.createAccount(uuid, hash, player.getScoreboardName(), ip);
        }

        // Create session
        createSession(uuid, ip);
        markAuthenticated(player);
        activateNewbieIfFirstJoin(player, existing == null);

        return RegisterResult.SUCCESS;
    }

    // ─── Login ─────────────────────────────────────────────────────────────────

    public static LoginResult login(ServerPlayer player, String password) {
        UUID uuid = player.getUUID();

        AccountDatabase.AccountRecord account = AccountDatabase.getAccount(uuid);
        if (account == null || account.passwordHash() == null || account.passwordHash().isEmpty()) {
            return LoginResult.NOT_REGISTERED;
        }

        // Check max attempts
        int attempts = loginAttempts.getOrDefault(uuid, 0);
        if (attempts >= Settings.getMaxLoginAttempts()) {
            return LoginResult.MAX_ATTEMPTS_EXCEEDED;
        }

        // Verify password
        if (!BCrypt.checkpw(password, account.passwordHash())) {
            attempts++;
            loginAttempts.put(uuid, attempts);
            if (attempts >= Settings.getMaxLoginAttempts()) {
                // Kick the player
                player.connection.disconnect(MessageUtils.format(
                        I18n.get("auth.kicked-timeout", "seconds", String.valueOf(Settings.getLoginTimeoutSeconds()))));
                return LoginResult.MAX_ATTEMPTS_EXCEEDED;
            }
            return LoginResult.WRONG_PASSWORD;
        }

        // Success
        String ip = getPlayerIp(player);
        AccountDatabase.updateLastLogin(uuid, ip);
        createSession(uuid, ip);
        loginAttempts.remove(uuid);
        markAuthenticated(player);

        return LoginResult.SUCCESS;
    }

    // ─── Change Password ───────────────────────────────────────────────────────

    public static ChangePasswordResult changePassword(ServerPlayer player, String oldPw, String newPw, String confirmPw) {
        UUID uuid = player.getUUID();

        AccountDatabase.AccountRecord account = AccountDatabase.getAccount(uuid);
        if (account == null || account.passwordHash() == null) {
            return ChangePasswordResult.WRONG_OLD_PASSWORD;
        }

        if (!BCrypt.checkpw(oldPw, account.passwordHash())) {
            return ChangePasswordResult.WRONG_OLD_PASSWORD;
        }

        if (newPw.length() < 4) return ChangePasswordResult.PASSWORD_TOO_SHORT;
        if (newPw.length() > 64) return ChangePasswordResult.PASSWORD_TOO_LONG;
        if (!newPw.equals(confirmPw)) return ChangePasswordResult.NEW_PASSWORDS_DONT_MATCH;

        String hash = BCrypt.hashpw(newPw, BCrypt.gensalt());
        AccountDatabase.updatePasswordHash(uuid, hash);

        return ChangePasswordResult.SUCCESS;
    }

    // ─── Link Code ─────────────────────────────────────────────────────────────

    public static String generateLinkCode(UUID uuid) {
        // Delete any existing codes for this player
        AccountDatabase.deleteCodesForPlayer(uuid);

        // Generate unique 6-digit code
        String code;
        int retries = 0;
        do {
            code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            retries++;
        } while (AccountDatabase.getLinkCode(code) != null && retries < 10);

        long expiresAt = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes
        AccountDatabase.createLinkCode(code, uuid, expiresAt);
        return code;
    }

    public static LinkResult completeLinking(String code, String discordId) {
        AccountDatabase.LinkCodeRecord linkCode = AccountDatabase.getLinkCode(code);
        if (linkCode == null) {
            return LinkResult.CODE_INVALID;
        }

        if (System.currentTimeMillis() > linkCode.expiresAt()) {
            AccountDatabase.deleteLinkCode(code);
            return LinkResult.CODE_EXPIRED;
        }

        // Check if Discord ID is already linked to another account
        AccountDatabase.AccountRecord existingLink = AccountDatabase.getAccountByDiscordId(discordId);
        if (existingLink != null && !existingLink.mcUuid().equals(linkCode.mcUuid())) {
            return LinkResult.DISCORD_ALREADY_LINKED;
        }

        UUID playerUuid = UUID.fromString(linkCode.mcUuid());

        // Ensure account exists
        AccountDatabase.AccountRecord account = AccountDatabase.getAccount(playerUuid);
        if (account == null) {
            // Create a minimal account for link-only mode
            AccountDatabase.createAccount(playerUuid, null, null, "");
        }

        // Update discord_id
        AccountDatabase.updateDiscordId(playerUuid, discordId);
        AccountDatabase.deleteLinkCode(code);
        AccountDatabase.deleteCodesForPlayer(playerUuid);

        // Assign Discord role if configured
        DiscordBot.assignLinkedRole(discordId);

        // If player is online and frozen, check if they can be authenticated now
        ServerPlayer player = findPlayer(playerUuid);
        if (player != null) {
            player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.link-success")));

            AuthMode mode = Settings.getAuthMode();
            if (mode == AuthMode.LINK_ONLY) {
                // Link-only mode — linking is sufficient
                createSession(playerUuid, getPlayerIp(player));
                markAuthenticated(player);
                activateNewbieIfFirstJoin(player, true);
            } else if (mode == AuthMode.FULL) {
                // Full mode — still needs password
                AccountDatabase.AccountRecord updatedAccount = AccountDatabase.getAccount(playerUuid);
                if (updatedAccount != null && updatedAccount.passwordHash() != null && !updatedAccount.passwordHash().isEmpty()) {
                    // Already has password — prompt login
                    player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.welcome-login")));
                } else {
                    // Needs to register
                    player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.welcome-register")));
                }
            }
        }

        return LinkResult.SUCCESS;
    }

    // ─── Unlink ────────────────────────────────────────────────────────────────

    public static void unlink(ServerPlayer player) {
        UUID uuid = player.getUUID();
        AccountDatabase.AccountRecord account = AccountDatabase.getAccount(uuid);
        if (account != null && account.discordId() != null) {
            DiscordBot.removeLinkedRole(account.discordId());
        }
        AccountDatabase.updateDiscordId(uuid, null);

        AuthMode mode = Settings.getAuthMode();
        if (mode.requiresLink()) {
            // Re-freeze the player since link is required
            authenticatedPlayers.remove(uuid);
            FreezeManager.freeze(player);
            player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.welcome-link-required")));
            String code = generateLinkCode(uuid);
            player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.link-code", "code", code)));
            player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.link-instruction",
                    "command", Settings.getDiscordLinkCommandName(), "code", code)));
        }
    }

    // ─── Mark Authenticated ────────────────────────────────────────────────────

    public static void markAuthenticated(ServerPlayer player) {
        UUID uuid = player.getUUID();
        authenticatedPlayers.add(uuid);
        loginAttempts.remove(uuid);
        FreezeManager.unfreeze(player);
        NewbieProtection.resumeTimer(uuid);
    }

    // ─── Admin Commands ────────────────────────────────────────────────────────

    public static void adminReset(UUID targetUuid) {
        AccountDatabase.deleteAccount(targetUuid);
        AccountDatabase.deleteSession(targetUuid);
        AccountDatabase.deleteCodesForPlayer(targetUuid);
        authenticatedPlayers.remove(targetUuid);

        ServerPlayer player = findPlayer(targetUuid);
        if (player != null) {
            FreezeManager.freeze(player);
        }
    }

    public static void adminUnlink(UUID targetUuid) {
        AccountDatabase.AccountRecord account = AccountDatabase.getAccount(targetUuid);
        if (account != null && account.discordId() != null) {
            DiscordBot.removeLinkedRole(account.discordId());
        }
        AccountDatabase.updateDiscordId(targetUuid, null);

        ServerPlayer player = findPlayer(targetUuid);
        if (player != null && Settings.getAuthMode().requiresLink()) {
            authenticatedPlayers.remove(targetUuid);
            FreezeManager.freeze(player);
        }
    }

    public static AccountDatabase.AccountRecord adminInfo(UUID targetUuid) {
        return AccountDatabase.getAccount(targetUuid);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private static void createSession(UUID uuid, String ip) {
        long expiresAt = System.currentTimeMillis() + (Settings.getSessionTimeoutHours() * 3600L * 1000L);
        AccountDatabase.createOrUpdateSession(uuid, ip, expiresAt);
    }

    private static void activateNewbieIfFirstJoin(ServerPlayer player, boolean isFirstJoin) {
        if (isFirstJoin && Settings.getNewbieProtectionMinutes() > 0) {
            NewbieProtection.activate(player, Settings.getNewbieProtectionMinutes());
        }
    }

    private static String getPlayerIp(ServerPlayer player) {
        var addr = player.connection.getRemoteAddress();
        if (addr == null) return "unknown";
        String full = addr.toString();
        // Format is typically /ip:port
        if (full.startsWith("/")) full = full.substring(1);
        int colonIdx = full.lastIndexOf(':');
        return colonIdx > 0 ? full.substring(0, colonIdx) : full;
    }

    private static ServerPlayer findPlayer(UUID uuid) {
        if (server == null) return null;
        return server.getPlayerList().getPlayer(uuid);
    }

    public static int getLoginAttempts(UUID uuid) {
        return loginAttempts.getOrDefault(uuid, 0);
    }
}
