package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /helpop <message> — sends a message to all online staff.
 * /report <player> <reason> — reports a player to all online staff.
 * /reports [page] — staff-only history of everything the two commands above logged.
 * Staff = anyone with mktessentials.admin.helpop (permission level 2).
 */
public class ReportCommands {

    private static final long COOLDOWN_MS = 30_000;
    private static final Map<UUID, Long> lastUsed = new ConcurrentHashMap<>();

    private static final int PER_PAGE = 10;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

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

        dispatcher.register(Commands.literal("reports")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.helpop", 2))
                .executes(context -> reports(context.getSource(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> reports(context.getSource(),
                                IntegerArgumentType.getInteger(context, "page")))));
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
        append(player.getServer(), "HelpOp", player.getScoreboardName(), "-", message);
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
        append(player.getServer(), "Report", player.getScoreboardName(), reportedName, reason);
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

    // ── Storage ──────────────────────────────────────────────────────────
    // One tab-separated line per entry: timestamp, type, sender, target, message.
    // Lives next to the other persisted data, in <world>/mktessentials/reports.log

    private static Path logFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("mktessentials").resolve("reports.log");
    }

    private static void append(MinecraftServer server, String type, String sender, String target, String message) {
        try {
            Path file = logFile(server);
            Files.createDirectories(file.getParent());
            Files.writeString(file, System.currentTimeMillis() + "\t" + type + "\t" + sender + "\t" + target
                            + "\t" + message.replace('\t', ' ') + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            MKTEssentials.LOGGER.warn("Failed to write reports.log", e);
        }
    }

    /** Page 1 is the 10 newest entries; higher pages go further back. */
    private static int reports(CommandSourceStack source, int page) {
        List<String> lines;
        try {
            Path file = logFile(source.getServer());
            // ponytail: reads the whole file per lookup — fine for a staff log, rotate it if it ever grows big
            lines = Files.exists(file) ? Files.readAllLines(file, StandardCharsets.UTF_8) : List.of();
        } catch (IOException e) {
            MKTEssentials.LOGGER.warn("Failed to read reports.log", e);
            source.sendFailure(MessageUtils.prefixed("&cCould not read reports.log — see the server console."));
            return 0;
        }

        if (lines.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed("&7No reports have been filed yet."), false);
            return 1;
        }

        int pages = (lines.size() + PER_PAGE - 1) / PER_PAGE;
        final int current = Math.min(page, pages);
        int end = lines.size() - (current - 1) * PER_PAGE; // exclusive; newest entry on this page
        int start = Math.max(0, end - PER_PAGE);

        source.sendSuccess(() -> MessageUtils.format("&8&m                  &r &c&l Reports &r&8&m                  "), false);
        for (int i = end - 1; i >= start; i--) {
            Component line = renderEntry(lines.get(i));
            if (line != null) source.sendSuccess(() -> line, false);
        }
        source.sendSuccess(() -> footer(current, pages), false);
        return 1;
    }

    /** @return the formatted line, or null if the stored line is malformed */
    private static Component renderEntry(String raw) {
        String[] parts = raw.split("\t", 5);
        if (parts.length < 5) return null;
        String when;
        try {
            when = TIME_FMT.format(Instant.ofEpochMilli(Long.parseLong(parts[0])).atZone(ZoneId.systemDefault()));
        } catch (NumberFormatException e) {
            return null;
        }
        String who = "Report".equals(parts[1])
                ? "&6" + parts[2] + " &7-> &c" + parts[3]
                : "&6" + parts[2];
        return MessageUtils.format("&8[" + when + "] &c[" + parts[1] + "] " + who + "&8: &f" + parts[4]);
    }

    private static Component footer(int page, int pages) {
        MutableComponent line = Component.literal("");
        line.append(page < pages
                ? button("&a&l[<< Older]", "/reports " + (page + 1))
                : MessageUtils.format("&8&l[<< Older]"));
        line.append(MessageUtils.format("  &7Page &f" + page + "&7/&f" + pages + "  "));
        line.append(page > 1
                ? button("&a&l[Newer >>]", "/reports " + (page - 1))
                : MessageUtils.format("&8&l[Newer >>]"));
        return line;
    }

    private static Component button(String label, String command) {
        return MessageUtils.format(label)
                .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    public static void cleanupPlayer(UUID uuid) {
        lastUsed.remove(uuid);
    }
}
