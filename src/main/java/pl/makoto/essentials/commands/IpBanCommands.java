package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.IpBanManager;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.PunishmentManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class IpBanCommands {

    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("banip")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.moderation.banip", 3))
                .then(Commands.argument("playerOrIp", StringArgumentType.word())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> banIp(context.getSource(),
                                        StringArgumentType.getString(context, "playerOrIp"),
                                        StringArgumentType.getString(context, "reason"))))
                        .executes(context -> banIp(context.getSource(),
                                StringArgumentType.getString(context, "playerOrIp"),
                                "IP banned by an operator"))));

        dispatcher.register(Commands.literal("unbanip")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.moderation.banip", 3))
                .then(Commands.argument("ip", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            IpBanManager.getAll().keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> unbanIp(context.getSource(),
                                StringArgumentType.getString(context, "ip")))));
    }

    private static int banIp(CommandSourceStack source, String playerOrIp, String reason) {
        String ip;
        String associatedName = null;

        if (IPV4.matcher(playerOrIp).matches() || playerOrIp.contains(":")) {
            // Literal IP (IPv4 or bracket-less IPv6)
            ip = playerOrIp;
        } else {
            // Player name — online connection IP first, then last known IP from player data
            ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(playerOrIp);
            if (online != null) {
                ip = IpBanManager.getPlayerIp(online);
            } else {
                UUID uuid = DataManager.resolveOfflineUUID(playerOrIp, source.getServer());
                PlayerData data = uuid != null ? DataManager.getPlayerData(uuid) : null;
                ip = data != null ? data.getLastIp() : null;
            }
            associatedName = playerOrIp;
            if (ip == null || ip.isBlank()) {
                source.sendFailure(MessageUtils.prefixed("&cCould not determine the IP of '" + playerOrIp + "'."));
                return 0;
            }
        }

        IpBanManager.ban(ip, associatedName, reason, source.getTextName());

        // Kick everyone currently connected from that IP
        List<ServerPlayer> toKick = new ArrayList<>();
        for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            if (ip.equals(IpBanManager.getPlayerIp(p))) toKick.add(p);
        }
        for (ServerPlayer p : toKick) {
            PunishmentManager.record(p.getUUID(), "ban", reason + " (IP ban)", source.getTextName(), 0);
            p.connection.disconnect(Component.literal("§cYou have been IP banned.\n§7Reason: " + reason));
        }

        final String shownTarget = associatedName != null ? associatedName + " &7(&f" + ip + "&7)" : ip;
        source.sendSuccess(() -> MessageUtils.prefixed("&7IP banned &6" + shownTarget + "&7. Reason: &f" + reason), true);
        return 1;
    }

    private static int unbanIp(CommandSourceStack source, String ip) {
        if (IpBanManager.unban(ip)) {
            source.sendSuccess(() -> MessageUtils.prefixed("&aIP &6" + ip + " &ahas been unbanned."), true);
            return 1;
        }
        source.sendFailure(MessageUtils.prefixed("&cIP '" + ip + "' is not banned."));
        return 0;
    }
}
