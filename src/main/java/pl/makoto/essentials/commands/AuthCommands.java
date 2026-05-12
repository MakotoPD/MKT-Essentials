package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.auth.AccountDatabase;
import pl.makoto.essentials.auth.AuthManager;
import pl.makoto.essentials.auth.AuthMode;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class AuthCommands {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private AuthCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        AuthMode mode = Settings.getAuthMode();

        // /register <password> <confirmPassword>
        if (mode.requiresPassword()) {
            dispatcher.register(Commands.literal("register")
                    .then(Commands.argument("password", StringArgumentType.word())
                            .then(Commands.argument("confirmPassword", StringArgumentType.word())
                                    .executes(ctx -> {
                                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                                        String password = StringArgumentType.getString(ctx, "password");
                                        String confirm = StringArgumentType.getString(ctx, "confirmPassword");
                                        return handleRegister(player, password, confirm);
                                    }))));
        }

        // /login <password>
        if (mode.requiresPassword()) {
            dispatcher.register(Commands.literal("login")
                    .then(Commands.argument("password", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                String password = StringArgumentType.getString(ctx, "password");
                                return handleLogin(player, password);
                            })));
        }

        // /changepassword <oldPassword> <newPassword> <confirmPassword>
        if (mode.requiresPassword()) {
            dispatcher.register(Commands.literal("changepassword")
                    .then(Commands.argument("oldPassword", StringArgumentType.word())
                            .then(Commands.argument("newPassword", StringArgumentType.word())
                                    .then(Commands.argument("confirmPassword", StringArgumentType.word())
                                            .executes(ctx -> {
                                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                String oldPw = StringArgumentType.getString(ctx, "oldPassword");
                                                String newPw = StringArgumentType.getString(ctx, "newPassword");
                                                String confirm = StringArgumentType.getString(ctx, "confirmPassword");
                                                return handleChangePassword(player, oldPw, newPw, confirm);
                                            })))));
        }

        // /link — generate a link code
        if (mode.requiresLink() || mode == AuthMode.OPTIONAL) {
            dispatcher.register(Commands.literal("link")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        return handleLink(player);
                    }));
        }

        // /unlink — remove Discord link
        if (mode.requiresLink() || mode == AuthMode.OPTIONAL) {
            dispatcher.register(Commands.literal("unlink")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        return handleUnlink(player);
                    }));
        }

        // /discord — show Discord link info
        dispatcher.register(Commands.literal("discord")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    return handleDiscordInfo(player);
                }));

        // /auth <admin subcommands>
        dispatcher.register(Commands.literal("auth")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.auth.admin", 3))
                .then(Commands.literal("reset")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    return handleAdminReset(ctx.getSource(), target);
                                })))
                .then(Commands.literal("unlink")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    return handleAdminUnlink(ctx.getSource(), target);
                                })))
                .then(Commands.literal("info")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    return handleAdminInfo(ctx.getSource(), target);
                                }))));
    }

    // ─── Handlers ──────────────────────────────────────────────────────────────

    private static int handleRegister(ServerPlayer player, String password, String confirm) {
        AuthManager.RegisterResult result = AuthManager.register(player, password, confirm);
        switch (result) {
            case SUCCESS -> player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.register-success")));
            case PASSWORDS_DONT_MATCH -> player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.passwords-dont-match")));
            case ALREADY_REGISTERED -> player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.already-registered")));
            case MUST_LINK_FIRST -> player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.must-link-first")));
            case PASSWORD_TOO_SHORT -> player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.password-too-short")));
            case PASSWORD_TOO_LONG -> player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.password-too-long")));
        }
        return result == AuthManager.RegisterResult.SUCCESS ? 1 : 0;
    }

    private static int handleLogin(ServerPlayer player, String password) {
        AuthManager.LoginResult result = AuthManager.login(player, password);
        switch (result) {
            case SUCCESS -> player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.login-success")));
            case WRONG_PASSWORD -> {
                int attempts = AuthManager.getLoginAttempts(player.getUUID());
                player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.login-failed",
                        "attempts", String.valueOf(attempts),
                        "max", String.valueOf(Settings.getMaxLoginAttempts()))));
            }
            case NOT_REGISTERED -> player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.not-registered")));
            case MAX_ATTEMPTS_EXCEEDED -> {} // Player is already being kicked
        }
        return result == AuthManager.LoginResult.SUCCESS ? 1 : 0;
    }

    private static int handleChangePassword(ServerPlayer player, String oldPw, String newPw, String confirm) {
        AuthManager.ChangePasswordResult result = AuthManager.changePassword(player, oldPw, newPw, confirm);
        switch (result) {
            case SUCCESS -> player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.password-changed")));
            case WRONG_OLD_PASSWORD -> player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.wrong-old-password")));
            case NEW_PASSWORDS_DONT_MATCH -> player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.passwords-dont-match")));
            case PASSWORD_TOO_SHORT -> player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.password-too-short")));
            case PASSWORD_TOO_LONG -> player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.password-too-long")));
        }
        return result == AuthManager.ChangePasswordResult.SUCCESS ? 1 : 0;
    }

    private static int handleLink(ServerPlayer player) {
        UUID uuid = player.getUUID();

        // Check if already linked
        AccountDatabase.AccountRecord account = AccountDatabase.getAccount(uuid);
        if (account != null && account.discordId() != null && !account.discordId().isEmpty()) {
            player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.already-linked")));
            return 0;
        }

        String code = AuthManager.generateLinkCode(uuid);
        player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.link-code", "code", code)));
        player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.link-instruction",
                "command", Settings.getDiscordLinkCommandName(), "code", code)));
        return 1;
    }

    private static int handleUnlink(ServerPlayer player) {
        UUID uuid = player.getUUID();
        AccountDatabase.AccountRecord account = AccountDatabase.getAccount(uuid);
        if (account == null || account.discordId() == null || account.discordId().isEmpty()) {
            player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.no-discord-linked")));
            return 0;
        }

        AuthManager.unlink(player);
        player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.unlink-success")));
        return 1;
    }

    private static int handleDiscordInfo(ServerPlayer player) {
        UUID uuid = player.getUUID();
        AccountDatabase.AccountRecord account = AccountDatabase.getAccount(uuid);
        if (account != null && account.discordId() != null && !account.discordId().isEmpty()) {
            player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.discord-info", "discord_id", account.discordId())));
        } else {
            player.sendSystemMessage(MessageUtils.prefixed(I18n.get("auth.no-discord-linked")));
        }
        return 1;
    }

    private static int handleAdminReset(CommandSourceStack source, ServerPlayer target) {
        AuthManager.adminReset(target.getUUID());
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("auth.admin-reset", "player", target.getScoreboardName())), true);
        return 1;
    }

    private static int handleAdminUnlink(CommandSourceStack source, ServerPlayer target) {
        AuthManager.adminUnlink(target.getUUID());
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("auth.admin-unlink", "player", target.getScoreboardName())), true);
        return 1;
    }

    private static int handleAdminInfo(CommandSourceStack source, ServerPlayer target) {
        AccountDatabase.AccountRecord account = AuthManager.adminInfo(target.getUUID());
        if (account == null) {
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("auth.admin-no-account")), false);
            return 0;
        }

        String name = target.getScoreboardName();
        String discord = account.discordId() != null ? account.discordId() : "None";
        String registered = DATE_FORMAT.format(Instant.ofEpochMilli(account.registeredAt()));
        String lastLogin = DATE_FORMAT.format(Instant.ofEpochMilli(account.lastLogin()));
        String lastIp = account.lastIp() != null ? account.lastIp() : "Unknown";

        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("auth.admin-info-header", "player", name)), false);
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("auth.admin-info-uuid", "uuid", target.getUUID().toString())), false);
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("auth.admin-info-discord", "discord", discord)), false);
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("auth.admin-info-registered", "date", registered)), false);
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("auth.admin-info-last-login", "date", lastLogin)), false);
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("auth.admin-info-last-ip", "ip", lastIp)), false);
        return 1;
    }
}
