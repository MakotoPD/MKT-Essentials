package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.AdminManager;
import pl.makoto.essentials.util.AFKManager;
import pl.makoto.essentials.util.DurationParser;
import pl.makoto.essentials.util.IpBanManager;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class InfoCommands {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("whois")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.whois", 2))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> whois(context.getSource(),
                                StringArgumentType.getString(context, "player")))));

        dispatcher.register(Commands.literal("playtime")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.playtime", 0))
                .then(Commands.argument("player", StringArgumentType.word())
                        .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.playtime", 2))
                        .executes(context -> playtimeOther(context.getSource(),
                                StringArgumentType.getString(context, "player"))))
                .executes(context -> playtimeSelf(context.getSource())));
    }

    private static int whois(CommandSourceStack source, String playerName) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(playerName);
        UUID uuid = online != null ? online.getUUID()
                : DataManager.resolveOfflineUUID(playerName, source.getServer());
        if (uuid == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", playerName)));
            return 0;
        }

        PlayerData data = DataManager.getPlayerData(uuid);

        send(source, "&7--- &6Whois: " + playerName + " &7---");
        send(source, "&7UUID: &f" + uuid);
        if (data.getNickname() != null && !data.getNickname().isBlank()) {
            send(source, "&7Nickname: &r" + data.getNickname());
        }

        if (online != null) {
            send(source, "&7Status: &aOnline" + (AFKManager.isAFK(uuid) ? " &7(AFK)" : ""));
            send(source, "&7IP: &f" + IpBanManager.getPlayerIp(online));
            send(source, "&7Ping: &f" + online.connection.latency() + "ms");
            send(source, "&7Gamemode: &f" + online.gameMode.getGameModeForPlayer().getName());
            send(source, "&7Location: &f" + online.level().dimension().location()
                    + " &7@ &f" + (int) online.getX() + ", " + (int) online.getY() + ", " + (int) online.getZ());
            send(source, "&7Health: &f" + String.format("%.1f", online.getHealth()) + "/20 &7Food: &f" + online.getFoodData().getFoodLevel() + "/20");
            send(source, "&7Fly: " + onOff(data.isFlyEnabled()) + " &7God: " + onOff(data.isGodMode())
                    + " &7Vanish: " + onOff(AdminManager.isVanished(uuid)));
        } else {
            send(source, "&7Status: &cOffline");
            if (data.getLastIp() != null) send(source, "&7Last IP: &f" + data.getLastIp());
            if (data.getLastSeenAt() > 0) send(source, "&7Last seen: &f" + DATE_FORMAT.format(Instant.ofEpochMilli(data.getLastSeenAt())));
        }

        if (data.getFirstJoinAt() > 0) send(source, "&7First join: &f" + DATE_FORMAT.format(Instant.ofEpochMilli(data.getFirstJoinAt())));
        send(source, "&7Play time: &f" + DurationParser.format(data.getTotalPlayTimeMillis()));

        long mute = data.getMuteExpiration();
        if (mute == -1) {
            send(source, "&7Muted: &cpermanently");
        } else if (mute > System.currentTimeMillis()) {
            send(source, "&7Muted: &cyes &7(expires in " + DurationParser.format(mute - System.currentTimeMillis()) + ")");
        }
        return 1;
    }

    private static String onOff(boolean value) {
        return value ? "&aon" : "&coff";
    }

    private static void send(CommandSourceStack source, String line) {
        source.sendSuccess(() -> MessageUtils.format(line), false);
    }

    private static int playtimeSelf(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-only")));
            return 0;
        }
        PlayerData data = DataManager.getPlayerData(player.getUUID());
        source.sendSuccess(() -> MessageUtils.prefixed("&7Your play time: &6"
                + DurationParser.format(data.getTotalPlayTimeMillis())), false);
        return 1;
    }

    private static int playtimeOther(CommandSourceStack source, String playerName) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(playerName);
        UUID uuid = online != null ? online.getUUID()
                : DataManager.resolveOfflineUUID(playerName, source.getServer());
        if (uuid == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", playerName)));
            return 0;
        }
        PlayerData data = DataManager.getPlayerData(uuid);
        source.sendSuccess(() -> MessageUtils.prefixed("&7Play time of &6" + playerName + "&7: &6"
                + DurationParser.format(data.getTotalPlayTimeMillis())), false);
        return 1;
    }
}
