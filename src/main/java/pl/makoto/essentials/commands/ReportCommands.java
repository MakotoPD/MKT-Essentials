package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /helpop <message> — sends a message to all online staff.
 * /report <player> <reason> — reports a player to all online staff.
 * Staff = anyone with mktessentials.admin.helpop (permission level 2).
 */
public class ReportCommands {

    private static final long COOLDOWN_MS = 30_000;
    private static final Map<UUID, Long> lastUsed = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("helpop")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.helpop", 0))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> helpop(context.getSource(),
                                StringArgumentType.getString(context, "message")))));

        dispatcher.register(Commands.literal("report")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.report", 0))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> report(context.getSource(),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "reason"))))));
    }

    private static boolean onCooldown(CommandSourceStack source, ServerPlayer player) {
        // Staff bypass the cooldown
        if (Permissions.hasPermission(player, "mktessentials.admin.helpop", 2)) return false;
        long now = System.currentTimeMillis();
        Long last = lastUsed.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - (now - last)) / 1000;
            source.sendFailure(MessageUtils.prefixed("&cPlease wait &6" + (remaining + 1) + "s &cbefore sending another message."));
            return true;
        }
        lastUsed.put(player.getUUID(), now);
        return false;
    }

    private static int helpop(CommandSourceStack source, String message) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        if (onCooldown(source, player)) return 0;

        Component staffMsg = MessageUtils.format("&c&l[HelpOp] &6" + player.getScoreboardName() + "&8: &f" + message);
        int delivered = notifyStaff(player, staffMsg);

        MKTEssentials.LOGGER.info("[HelpOp] {}: {}", player.getScoreboardName(), message);
        if (delivered == 0) {
            source.sendSuccess(() -> MessageUtils.prefixed("&7No staff is online right now — your message was logged."), false);
        } else {
            source.sendSuccess(() -> MessageUtils.prefixed("&aYour message has been sent to the staff."), false);
        }
        return 1;
    }

    private static int report(CommandSourceStack source, String reportedName, String reason) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        if (onCooldown(source, player)) return 0;

        Component staffMsg = MessageUtils.format("&c&l[Report] &6" + player.getScoreboardName()
                + " &7reported &c" + reportedName + "&8: &f" + reason);
        notifyStaff(player, staffMsg);

        MKTEssentials.LOGGER.info("[Report] {} reported {}: {}", player.getScoreboardName(), reportedName, reason);
        source.sendSuccess(() -> MessageUtils.prefixed("&aThank you, your report has been sent to the staff."), false);
        return 1;
    }

    /** Sends the message to all online staff (excluding the sender). @return number of staff notified */
    private static int notifyStaff(ServerPlayer sender, Component message) {
        int count = 0;
        for (ServerPlayer p : sender.getServer().getPlayerList().getPlayers()) {
            if (p.getUUID().equals(sender.getUUID())) continue;
            if (Permissions.hasPermission(p, "mktessentials.admin.helpop", 2)) {
                p.sendSystemMessage(message);
                count++;
            }
        }
        return count;
    }

    public static void cleanupPlayer(UUID uuid) {
        lastUsed.remove(uuid);
    }
}
