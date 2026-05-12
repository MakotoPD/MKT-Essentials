package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.util.BanManager;
import pl.makoto.essentials.util.DurationParser;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.config.I18n;

import java.util.UUID;

public class BanCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /ban <player> [reason]
        dispatcher.register(Commands.literal("ban")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.moderation.ban", 3))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> ban(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "reason"))))
                        .executes(context -> ban(
                                context.getSource(),
                                StringArgumentType.getString(context, "player"),
                                "Banned by an operator"))));

        // /tempban <player> <duration> [reason]
        dispatcher.register(Commands.literal("tempban")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.moderation.tempban", 3))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> tempBan(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "player"),
                                                StringArgumentType.getString(context, "duration"),
                                                StringArgumentType.getString(context, "reason"))))
                                .executes(context -> tempBan(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "duration"),
                                        "Temporarily banned by an operator")))));

        // /unban <player>
        dispatcher.register(Commands.literal("unban")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.moderation.unban", 3))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> unban(
                                context.getSource(),
                                StringArgumentType.getString(context, "player")))));
    }

    private static int ban(CommandSourceStack source, String playerName, String reason) {
        // Try to resolve as online player first
        ServerPlayer onlineTarget = source.getServer().getPlayerList().getPlayerByName(playerName);

        UUID uuid;
        if (onlineTarget != null) {
            uuid = onlineTarget.getUUID();
        } else {
            uuid = DataManager.resolveOfflineUUID(playerName, source.getServer());
        }

        if (uuid == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", playerName)));
            return 0;
        }

        String issuerName = source.getTextName();
        BanManager.ban(uuid, playerName, reason, issuerName);

        // Disconnect if online
        if (onlineTarget != null) {
            onlineTarget.connection.disconnect(Component.literal("§cYou have been banned.\n§7Reason: " + reason));
        }

        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("moderation.banned", "player", playerName, "reason", reason)), true);
        return 1;
    }

    private static int tempBan(CommandSourceStack source, String playerName, String durationStr, String reason) {
        // Parse duration
        long duration;
        try {
            duration = DurationParser.parse(durationStr);
        } catch (IllegalArgumentException e) {
            source.sendFailure(MessageUtils.prefixed("&cInvalid duration format! Use e.g. 1d6h30m (d=days, h=hours, m=minutes, s=seconds)."));
            return 0;
        }

        // Try to resolve as online player first
        ServerPlayer onlineTarget = source.getServer().getPlayerList().getPlayerByName(playerName);

        UUID uuid;
        if (onlineTarget != null) {
            uuid = onlineTarget.getUUID();
        } else {
            uuid = DataManager.resolveOfflineUUID(playerName, source.getServer());
        }

        if (uuid == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", playerName)));
            return 0;
        }

        String issuerName = source.getTextName();
        long expiration = System.currentTimeMillis() + duration;
        BanManager.tempBan(uuid, playerName, reason, issuerName, expiration);

        // Disconnect if online
        if (onlineTarget != null) {
            String formattedDuration = DurationParser.format(duration);
            onlineTarget.connection.disconnect(Component.literal(
                    "§cYou have been temporarily banned.\n§7Reason: " + reason + "\n§7Duration: " + formattedDuration));
        }

        String formattedDuration = DurationParser.format(duration);
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("moderation.temp-banned", "player", playerName, "duration", formattedDuration, "reason", reason)), true);
        return 1;
    }

    private static int unban(CommandSourceStack source, String playerName) {
        UUID uuid = DataManager.resolveOfflineUUID(playerName, source.getServer());

        if (uuid == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", playerName)));
            return 0;
        }

        if (!BanManager.isBanned(uuid)) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("moderation.not-banned")));
            return 0;
        }

        BanManager.unban(uuid);
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("moderation.unbanned", "player", playerName)), true);
        return 1;
    }
}
