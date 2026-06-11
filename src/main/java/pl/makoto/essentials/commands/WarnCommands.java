package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.util.BanManager;
import pl.makoto.essentials.util.DurationParser;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.PunishmentManager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class WarnCommands {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("warn")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.moderation.warn", 2))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> warn(context.getSource(),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "reason"))))
                        .executes(context -> warn(context.getSource(),
                                StringArgumentType.getString(context, "player"),
                                "Warned by an operator"))));

        dispatcher.register(Commands.literal("unwarn")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.moderation.warn", 2))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> unwarn(context.getSource(),
                                StringArgumentType.getString(context, "player")))));

        dispatcher.register(Commands.literal("warns")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.moderation.warn", 2))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> warns(context.getSource(),
                                StringArgumentType.getString(context, "player")))));

        dispatcher.register(Commands.literal("history")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.moderation.history", 2))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> history(context.getSource(),
                                StringArgumentType.getString(context, "player")))));
    }

    private static UUID resolve(CommandSourceStack source, String playerName) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(playerName);
        UUID uuid = online != null ? online.getUUID()
                : DataManager.resolveOfflineUUID(playerName, source.getServer());
        if (uuid == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", playerName)));
        }
        return uuid;
    }

    private static int warn(CommandSourceStack source, String playerName, String reason) {
        UUID uuid = resolve(source, playerName);
        if (uuid == null) return 0;

        PunishmentManager.record(uuid, "warn", reason, source.getTextName(), 0);
        int warns = PunishmentManager.countActiveWarns(uuid);
        int max = Settings.getMaxWarns();

        source.sendSuccess(() -> MessageUtils.prefixed("&7Warned &6" + playerName + " &7(&e" + warns
                + (max > 0 ? "/" + max : "") + "&7). Reason: &f" + reason), true);

        ServerPlayer online = source.getServer().getPlayerList().getPlayer(uuid);
        if (online != null) {
            online.sendSystemMessage(MessageUtils.prefixed("&cYou have been warned (&e" + warns
                    + (max > 0 ? "/" + max : "") + "&c). Reason: &f" + reason));
        }

        // Escalation: too many active warns → automatic tempban
        if (max > 0 && warns >= max) {
            final long durationMs = parseWarnBanDuration();
            String banReason = "Too many warnings (" + warns + ")";
            BanManager.tempBan(uuid, playerName, banReason, "MKT Essentials", System.currentTimeMillis() + durationMs);
            PunishmentManager.record(uuid, "tempban", banReason, "MKT Essentials", durationMs);
            PunishmentManager.clearWarns(uuid);

            if (online != null) {
                online.connection.disconnect(Component.literal(
                        "§cYou have been temporarily banned.\n§7Reason: " + banReason
                        + "\n§7Duration: " + DurationParser.format(durationMs)));
            }
            source.sendSuccess(() -> MessageUtils.prefixed("&c" + playerName + " &7reached the warn limit and was banned for &e"
                    + DurationParser.format(durationMs) + "&7."), true);
        }
        return 1;
    }

    private static long parseWarnBanDuration() {
        try {
            return DurationParser.parse(Settings.getWarnBanDuration());
        } catch (IllegalArgumentException e) {
            return 24 * 60 * 60 * 1000L; // fall back to 1 day on a bad config value
        }
    }

    private static int unwarn(CommandSourceStack source, String playerName) {
        UUID uuid = resolve(source, playerName);
        if (uuid == null) return 0;

        if (PunishmentManager.revokeLatestWarn(uuid)) {
            int warns = PunishmentManager.countActiveWarns(uuid);
            source.sendSuccess(() -> MessageUtils.prefixed("&7Revoked the latest warn of &6" + playerName
                    + "&7. Active warns: &e" + warns), true);
            return 1;
        }
        source.sendFailure(MessageUtils.prefixed("&c" + playerName + " has no active warns."));
        return 0;
    }

    private static int warns(CommandSourceStack source, String playerName) {
        UUID uuid = resolve(source, playerName);
        if (uuid == null) return 0;

        List<PunishmentManager.Entry> active = PunishmentManager.getHistory(uuid).stream()
                .filter(e -> "warn".equals(e.type) && e.active)
                .toList();
        if (active.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed("&6" + playerName + " &7has no active warns."), false);
            return 1;
        }
        source.sendSuccess(() -> MessageUtils.prefixed("&7Active warns of &6" + playerName + " &7(&e" + active.size() + "&7):"), false);
        for (PunishmentManager.Entry e : active) {
            String line = "&8- &f" + e.reason + " &7(by &6" + e.issuer + "&7, " + DATE_FORMAT.format(Instant.ofEpochMilli(e.timestamp)) + ")";
            source.sendSuccess(() -> MessageUtils.format(line), false);
        }
        return 1;
    }

    private static int history(CommandSourceStack source, String playerName) {
        UUID uuid = resolve(source, playerName);
        if (uuid == null) return 0;

        List<PunishmentManager.Entry> history = PunishmentManager.getHistory(uuid);
        if (history.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed("&6" + playerName + " &7has a clean record."), false);
            return 1;
        }
        source.sendSuccess(() -> MessageUtils.prefixed("&7Punishment history of &6" + playerName + " &7(&e" + history.size() + "&7):"), false);
        for (PunishmentManager.Entry e : history) {
            String typeColor = switch (e.type) {
                case "warn" -> e.active ? "&e" : "&8";
                case "ban", "tempban" -> "&c";
                case "mute", "tempmute" -> "&6";
                case "kick" -> "&b";
                default -> "&7";
            };
            String duration = e.durationMs > 0 ? " &7[" + DurationParser.format(e.durationMs) + "]" : "";
            String line = "&8- " + typeColor + e.type.toUpperCase() + duration + " &f" + e.reason
                    + " &7(by &6" + e.issuer + "&7, " + DATE_FORMAT.format(Instant.ofEpochMilli(e.timestamp)) + ")";
            source.sendSuccess(() -> MessageUtils.format(line), false);
        }
        return 1;
    }
}
