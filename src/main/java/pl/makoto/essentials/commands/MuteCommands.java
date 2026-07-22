package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.DurationParser;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.config.I18n;

import java.util.UUID;

public class MuteCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mute")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.mute", 2))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("until", StringArgumentType.string())
                                .executes(context -> mute(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "until"))))
                        .executes(context -> mute(context.getSource(), EntityArgument.getPlayer(context, "player"), "forever")))
                .then(Commands.argument("offlinePlayer", StringArgumentType.word())
                        .then(Commands.argument("until", StringArgumentType.string())
                                .executes(context -> muteOffline(context.getSource(), StringArgumentType.getString(context, "offlinePlayer"), StringArgumentType.getString(context, "until"))))
                        .executes(context -> muteOffline(context.getSource(), StringArgumentType.getString(context, "offlinePlayer"), "forever"))));

        dispatcher.register(Commands.literal("unmute")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.unmute", 2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> unmute(context.getSource(), EntityArgument.getPlayer(context, "player")))));

        // /mutelist — currently muted online players
        dispatcher.register(Commands.literal("mutelist")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.mutelist", 2))
                .executes(context -> mutelist(context.getSource())));

        // /tempmute <player> <duration> [reason] — duration is required
        dispatcher.register(Commands.literal("tempmute")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.mute", 2))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> tempMute(context.getSource(),
                                                StringArgumentType.getString(context, "player"),
                                                StringArgumentType.getString(context, "duration"),
                                                StringArgumentType.getString(context, "reason"))))
                                .executes(context -> tempMute(context.getSource(),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "duration"),
                                        "Muted by an operator")))));
    }

    private static int tempMute(CommandSourceStack source, String playerName, String durationStr, String reason) {
        long duration;
        try {
            duration = DurationParser.parse(durationStr);
        } catch (IllegalArgumentException e) {
            source.sendFailure(MessageUtils.prefixed("&cInvalid duration format! Use e.g. 1d6h30m (d=days, h=hours, m=minutes, s=seconds)."));
            return 0;
        }

        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(playerName);
        UUID uuid = online != null ? online.getUUID()
                : DataManager.resolveOfflineUUID(playerName, source.getServer());
        if (uuid == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", playerName)));
            return 0;
        }

        PlayerData data = DataManager.getPlayerData(uuid);
        data.setMuteExpiration(System.currentTimeMillis() + duration);
        DataManager.savePlayerData(uuid);
        pl.makoto.essentials.util.PunishmentManager.record(uuid, "tempmute", reason, source.getTextName(), duration);

        String formatted = DurationParser.format(duration);
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("moderation.muted", "player", playerName, "duration", "for " + formatted)), true);
        if (online != null) {
            online.sendSystemMessage(MessageUtils.prefixed(I18n.get("moderation.muted-notify", "duration", "for " + formatted)
                    + " &7Reason: &f" + reason));
        }
        return 1;
    }

    private static int mute(CommandSourceStack source, ServerPlayer target, String until) {
        if (target == null) return 0;

        long expiration = -1;
        if (!until.equalsIgnoreCase("forever")) {
            // Simple duration parser: 2h, 1d, etc.
            try {
                long duration = parseDuration(until);
                expiration = System.currentTimeMillis() + duration;
            } catch (Exception e) {
                source.sendFailure(MessageUtils.prefixed("&cInvalid duration format! Use e.g. 2h, 1d."));
                return 0;
            }
        }

        PlayerData data = DataManager.getPlayerData(target.getUUID());
        data.setMuteExpiration(expiration);
        DataManager.savePlayerData(target.getUUID());
        pl.makoto.essentials.util.PunishmentManager.record(target.getUUID(), "mute", "Muted by an operator",
                source.getTextName(), expiration > 0 ? expiration - System.currentTimeMillis() : 0);

        String timeStr = until.equalsIgnoreCase("forever") ? "forever" : "until " + until;
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("moderation.muted", "player", target.getScoreboardName(), "duration", timeStr)), true);
        target.sendSystemMessage(MessageUtils.prefixed(I18n.get("moderation.muted-notify", "duration", timeStr)));
        return 1;
    }

    private static int muteOffline(CommandSourceStack source, String playerName, String until) {
        UUID uuid = DataManager.resolveOfflineUUID(playerName, source.getServer());
        if (uuid == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", playerName)));
            return 0;
        }

        long expiration = -1;
        if (!until.equalsIgnoreCase("forever")) {
            try {
                long duration = parseDuration(until);
                expiration = System.currentTimeMillis() + duration;
            } catch (Exception e) {
                source.sendFailure(MessageUtils.prefixed("&cInvalid duration format! Use e.g. 2h, 1d."));
                return 0;
            }
        }

        PlayerData data = DataManager.getPlayerData(uuid);
        data.setMuteExpiration(expiration);
        DataManager.savePlayerData(uuid);

        String timeStr = until.equalsIgnoreCase("forever") ? "forever" : "until " + until;
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("moderation.muted", "player", playerName, "duration", timeStr + " (offline)")), true);
        return 1;
    }

    private static int unmute(CommandSourceStack source, ServerPlayer target) {
        if (target == null) return 0;

        PlayerData data = DataManager.getPlayerData(target.getUUID());
        data.setMuteExpiration(0);
        DataManager.savePlayerData(target.getUUID());

        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("moderation.unmuted", "player", target.getScoreboardName())), true);
        target.sendSystemMessage(MessageUtils.prefixed("&aYou have been unmuted."));
        return 1;
    }

    private static int mutelist(CommandSourceStack source) {
        long now = System.currentTimeMillis();
        java.util.List<ServerPlayer> muted = new java.util.ArrayList<>();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            long expiration = DataManager.getPlayerData(player.getUUID()).getMuteExpiration();
            if (expiration == -1 || expiration > now) {
                muted.add(player);
            }
        }

        if (muted.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed("&7No online players are muted."), false);
            return 1;
        }

        source.sendSuccess(() -> MessageUtils.prefixed("&cMuted online players (&f" + muted.size() + "&c):"), false);
        for (ServerPlayer player : muted) {
            long expiration = DataManager.getPlayerData(player.getUUID()).getMuteExpiration();
            String duration = expiration == -1 ? "&4permanent" : "&e" + DurationParser.format(expiration - now);
            source.sendSuccess(() -> MessageUtils.format("&8- &f" + player.getScoreboardName() + " &7(" + duration + "&7)"), false);
        }
        return 1;
    }

    private static long parseDuration(String s) {
        return DurationParser.parse(s);
    }
}
