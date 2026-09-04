package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.util.CommandQueueManager;
import pl.makoto.essentials.util.CommandQueueManager.Entry;
import pl.makoto.essentials.util.CommandQueueManager.Status;
import pl.makoto.essentials.util.DurationParser;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * /cmdqueue — administration of the persistent command queue.
 * <p>
 * Every node is permission level 4 by default: queueing a command is equivalent to running
 * it from the console, so anyone who can add to the queue can give themselves operator.
 */
public class QueueCommands {

    private static final String PERM_VIEW = "mktessentials.admin.queue.view";
    private static final String PERM_ADD = "mktessentials.admin.queue.add";
    private static final String PERM_MANAGE = "mktessentials.admin.queue.manage";

    private static final int PER_PAGE = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cmdqueue")
                .requires(source -> Permissions.hasPermission(source, PERM_VIEW, 4))
                .executes(context -> list(context.getSource(), CommandQueueManager.getAll(), 1, "/cmdqueue list"))
                .then(listNode())
                .then(playerNode())
                .then(infoNode())
                .then(addNode("add", true))
                .then(addNode("addnowait", false))
                .then(idNode("cancel"))
                .then(idNode("retry"))
                .then(idNode("remove"))
                .then(Commands.literal("clear")
                        .requires(source -> Permissions.hasPermission(source, PERM_MANAGE, 4))
                        .executes(context -> clear(context.getSource())))
                .then(Commands.literal("help")
                        .executes(context -> help(context.getSource()))));
    }

    // ── Tree building ────────────────────────────────────────────────────────

    private static LiteralArgumentBuilder<CommandSourceStack> listNode() {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("list")
                .executes(context -> list(context.getSource(), CommandQueueManager.getAll(), 1, "/cmdqueue list"))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> list(context.getSource(), CommandQueueManager.getAll(),
                                IntegerArgumentType.getInteger(context, "page"), "/cmdqueue list")));

        for (Status status : Status.values()) {
            String name = status.name().toLowerCase(Locale.ROOT);
            String base = "/cmdqueue list " + name;
            node.then(Commands.literal(name)
                    .executes(context -> list(context.getSource(),
                            CommandQueueManager.getByStatus(status), 1, base))
                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                            .executes(context -> list(context.getSource(),
                                    CommandQueueManager.getByStatus(status),
                                    IntegerArgumentType.getInteger(context, "page"), base))));
        }
        return node;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> playerNode() {
        return Commands.literal("player")
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> listPlayer(context, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> listPlayer(context,
                                        IntegerArgumentType.getInteger(context, "page")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> infoNode() {
        return Commands.literal("info")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .executes(context -> info(context.getSource(),
                                IntegerArgumentType.getInteger(context, "id"))));
    }

    /**
     * Builds one of the two add subtrees. Both accept an optional idempotency key and an
     * optional delay, which together cover all four delivery modes.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> addNode(String literal, boolean requireOnline) {
        return Commands.literal(literal)
                .requires(source -> Permissions.hasPermission(source, PERM_ADD, 4))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(commandArg(requireOnline, false, false))
                        .then(Commands.literal("in")
                                .then(Commands.argument("duration", StringArgumentType.word())
                                        .then(commandArg(requireOnline, true, false))))
                        .then(Commands.literal("key")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .then(commandArg(requireOnline, false, true))
                                        .then(Commands.literal("in")
                                                .then(Commands.argument("duration", StringArgumentType.word())
                                                        .then(commandArg(requireOnline, true, true)))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> commandArg(boolean requireOnline,
                                                                     boolean hasDuration, boolean hasKey) {
        return Commands.argument("command", StringArgumentType.greedyString())
                .executes(context -> add(context, requireOnline, hasDuration, hasKey));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> idNode(String literal) {
        return Commands.literal(literal)
                .requires(source -> Permissions.hasPermission(source, PERM_MANAGE, 4))
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                        .executes(context -> manage(context.getSource(), literal,
                                IntegerArgumentType.getInteger(context, "id"))));
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private static int add(CommandContext<CommandSourceStack> context, boolean requireOnline,
                           boolean hasDuration, boolean hasKey) {
        CommandSourceStack source = context.getSource();
        String playerName = StringArgumentType.getString(context, "player");
        String command = StringArgumentType.getString(context, "command").trim();
        String key = hasKey ? StringArgumentType.getString(context, "key") : null;

        if (command.isEmpty()) {
            source.sendFailure(MessageUtils.prefixed("&cThe command cannot be empty."));
            return 0;
        }
        // A queued /cmdqueue could queue itself — refuse before someone builds a fork bomb.
        if (command.replaceFirst("^/", "").toLowerCase(Locale.ROOT).startsWith("cmdqueue")) {
            source.sendFailure(MessageUtils.prefixed("&cThe queue cannot queue itself."));
            return 0;
        }

        long delayMs = 0;
        if (hasDuration) {
            try {
                delayMs = DurationParser.parse(StringArgumentType.getString(context, "duration"));
            } catch (IllegalArgumentException e) {
                source.sendFailure(MessageUtils.prefixed("&cInvalid duration. Use e.g. &f30s&c, &f10m&c, &f2h&c, &f1d12h&c."));
                return 0;
            }
        }

        // The target may never have joined (a webstore purchase before first login) — then the
        // entry is stored by name and bound to a UUID the first time they show up.
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(playerName);
        UUID uuid = online != null ? online.getUUID()
                : DataManager.resolveOfflineUUID(playerName, source.getServer());

        Entry entry = CommandQueueManager.add(uuid, playerName, command, delayMs,
                requireOnline, source.getTextName(), key);
        if (entry == null) {
            source.sendFailure(MessageUtils.prefixed("&cA command with key &f" + key
                    + "&c is already queued or completed — nothing was added."));
            return 0;
        }

        String when = delayMs > 0 ? "in &f" + DurationParser.format(delayMs) : "&fas soon as possible";
        String gate = requireOnline ? " &7once &f" + playerName + " &7is online" : "";
        source.sendSuccess(() -> MessageUtils.prefixed("&aQueued &7#&f" + entry.id + " &7for &6" + playerName
                + "&7, running " + when + gate + "&7."), true);
        if (uuid == null) {
            source.sendSuccess(() -> MessageUtils.prefixed("&e" + playerName
                    + " &7has never joined — the entry is matched by name until they do."), false);
        }
        return 1;
    }

    private static int listPlayer(CommandContext<CommandSourceStack> context, int page) {
        CommandSourceStack source = context.getSource();
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(playerName);
        UUID uuid = online != null ? online.getUUID()
                : DataManager.resolveOfflineUUID(playerName, source.getServer());
        return list(source, CommandQueueManager.getByPlayer(uuid, playerName), page,
                "/cmdqueue player " + playerName);
    }

    private static int list(CommandSourceStack source, List<Entry> entries, int page, String baseCommand) {
        if (entries.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed("&7The queue is empty."), false);
            return 1;
        }

        int pages = (entries.size() + PER_PAGE - 1) / PER_PAGE;
        final int current = Math.min(page, pages);
        int start = (current - 1) * PER_PAGE;
        int end = Math.min(entries.size(), start + PER_PAGE);

        int pending = CommandQueueManager.countPending();
        source.sendSuccess(() -> MessageUtils.format("&8&m                &r &6&l Command Queue &r&8&m                "), false);
        source.sendSuccess(() -> MessageUtils.format("&7Total: &f" + entries.size() + "  &7Pending: &e" + pending), false);
        for (int i = start; i < end; i++) {
            Component line = renderEntry(entries.get(i));
            source.sendSuccess(() -> line, false);
        }
        source.sendSuccess(() -> footer(current, pages, baseCommand), false);
        return 1;
    }

    private static int info(CommandSourceStack source, int id) {
        Entry entry = CommandQueueManager.get(id);
        if (entry == null) {
            source.sendFailure(MessageUtils.prefixed("&cNo queue entry with id &f#" + id + "&c."));
            return 0;
        }

        source.sendSuccess(() -> MessageUtils.format("&8&m                &r &6&l Queue #" + entry.id + " &r&8&m                "), false);
        source.sendSuccess(() -> MessageUtils.format("&7Status: " + statusColor(entry.status) + entry.status.name()), false);
        source.sendSuccess(() -> MessageUtils.format("&7Player: &6" + entry.targetName
                + (entry.targetUuid != null ? " &8(" + entry.targetUuid + ")" : " &8(never joined)")), false);
        source.sendSuccess(() -> MessageUtils.format("&7Command: &f/" + entry.command), false);
        source.sendSuccess(() -> MessageUtils.format("&7Added by: &f" + entry.createdBy
                + " &8· " + DATE_FORMAT.format(Instant.ofEpochMilli(entry.createdAt))), false);
        source.sendSuccess(() -> MessageUtils.format("&7Runs: &f" + describeSchedule(entry)), false);
        if (entry.executedAt > 0) {
            source.sendSuccess(() -> MessageUtils.format("&7Executed: &f"
                    + DATE_FORMAT.format(Instant.ofEpochMilli(entry.executedAt))), false);
        }
        if (entry.attempts > 0) {
            source.sendSuccess(() -> MessageUtils.format("&7Attempts: &f" + entry.attempts), false);
        }
        if (entry.dedupeKey != null) {
            source.sendSuccess(() -> MessageUtils.format("&7Key: &f" + entry.dedupeKey), false);
        }
        if (entry.result != null) {
            source.sendSuccess(() -> MessageUtils.format("&7Result: &f" + entry.result), false);
        }

        MutableComponent actions = Component.literal("");
        if (entry.status == Status.PENDING) {
            actions.append(suggestButton("&c&l[Cancel]", "/cmdqueue cancel " + entry.id)).append(MessageUtils.format(" "));
        } else if (entry.status != Status.COMPLETED) {
            actions.append(suggestButton("&a&l[Retry]", "/cmdqueue retry " + entry.id)).append(MessageUtils.format(" "));
        }
        actions.append(suggestButton("&4&l[Remove]", "/cmdqueue remove " + entry.id));
        source.sendSuccess(() -> actions, false);
        return 1;
    }

    private static int manage(CommandSourceStack source, String action, int id) {
        boolean ok = switch (action) {
            case "cancel" -> CommandQueueManager.cancel(id);
            case "retry" -> CommandQueueManager.retry(id);
            case "remove" -> CommandQueueManager.remove(id);
            default -> false;
        };
        if (!ok) {
            String why = switch (action) {
                case "cancel" -> "&cOnly a pending entry can be cancelled.";
                case "retry" -> "&cOnly a failed or cancelled entry can be retried.";
                default -> "&cNo queue entry with id &f#" + id + "&c.";
            };
            source.sendFailure(MessageUtils.prefixed(why));
            return 0;
        }
        String verb = switch (action) {
            case "cancel" -> "Cancelled";
            case "retry" -> "Re-queued";
            default -> "Removed";
        };
        source.sendSuccess(() -> MessageUtils.prefixed("&a" + verb + " queue entry &f#" + id + "&a."), true);
        return 1;
    }

    private static int clear(CommandSourceStack source) {
        int removed = CommandQueueManager.clearFinished();
        source.sendSuccess(() -> MessageUtils.prefixed("&aCleared &f" + removed
                + "&a finished entr" + (removed == 1 ? "y" : "ies") + " from the queue."), true);
        return 1;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> MessageUtils.format("&8&m                &r &6&l Command Queue &r&8&m                "), false);
        source.sendSuccess(() -> MessageUtils.format("&7Runs console commands for a player, reliably, even after a restart."), false);
        source.sendSuccess(() -> MessageUtils.format("&e/cmdqueue add <player> [key <k>] [in <time>] <command>"), false);
        source.sendSuccess(() -> MessageUtils.format("  &7Waits until the player is online."), false);
        source.sendSuccess(() -> MessageUtils.format("&e/cmdqueue addnowait <player> [key <k>] [in <time>] <command>"), false);
        source.sendSuccess(() -> MessageUtils.format("  &7Runs whether or not they are online."), false);
        source.sendSuccess(() -> MessageUtils.format("&7Use &f%player%&7 in the command for the target's name."), false);
        source.sendSuccess(() -> MessageUtils.format("&7&o  /cmdqueue add Steve in 1d lp user %player% parent remove vip"), false);
        source.sendSuccess(() -> MessageUtils.format("&e/cmdqueue list [pending|completed|failed|cancelled] [page]"), false);
        source.sendSuccess(() -> MessageUtils.format("&e/cmdqueue player <name> [page]&7, &e/cmdqueue info <id>"), false);
        source.sendSuccess(() -> MessageUtils.format("&e/cmdqueue cancel|retry|remove <id>&7, &e/cmdqueue clear"), false);
        return 1;
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    private static Component renderEntry(Entry entry) {
        String command = entry.command.length() > 32 ? entry.command.substring(0, 31) + "…" : entry.command;
        MutableComponent line = MessageUtils.format("&8#&7" + entry.id + " " + statusColor(entry.status) + "● &6"
                + entry.targetName + " &8· &f/" + command + " ");

        Component hover = MessageUtils.format("&f/" + entry.command
                + "\n&7Status: " + statusColor(entry.status) + entry.status.name()
                + "\n&7Runs: &f" + describeSchedule(entry)
                + "\n&7Added by: &f" + entry.createdBy
                + (entry.result != null ? "\n&7Result: &f" + entry.result : ""));
        line.setStyle(line.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));

        line.append(runButton("&8[&bi&8]", "/cmdqueue info " + entry.id));
        if (entry.status == Status.PENDING) {
            line.append(MessageUtils.format(" ")).append(suggestButton("&8[&cx&8]", "/cmdqueue cancel " + entry.id));
        }
        return line;
    }

    /** Human-readable answer to "when does this run?", tailored to the entry's status. */
    private static String describeSchedule(Entry entry) {
        if (entry.status == Status.COMPLETED) return "already executed";
        if (entry.status == Status.CANCELLED) return "cancelled";
        if (entry.status == Status.FAILED) return "gave up after " + entry.attempts + " attempt(s)";

        long remaining = entry.executeAt - System.currentTimeMillis();
        String when = remaining > 0
                ? "in " + DurationParser.format(remaining)
                : "as soon as possible";
        return entry.requireOnline ? when + ", once " + entry.targetName + " is online" : when;
    }

    private static String statusColor(Status status) {
        return switch (status) {
            case PENDING -> "&e";
            case COMPLETED -> "&a";
            case FAILED -> "&c";
            case CANCELLED -> "&8";
        };
    }

    private static Component footer(int page, int pages, String baseCommand) {
        MutableComponent line = Component.literal("");
        line.append(page < pages
                ? runButton("&a&l[<< Prev]", baseCommand + " " + (page + 1))
                : MessageUtils.format("&8&l[<< Prev]"));
        line.append(MessageUtils.format("  &7Page &f" + page + "&7/&f" + pages + "  "));
        line.append(page > 1
                ? runButton("&a&l[Next >>]", baseCommand + " " + (page - 1))
                : MessageUtils.format("&8&l[Next >>]"));
        return line;
    }

    private static Component runButton(String label, String command) {
        return MessageUtils.format(label)
                .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    /** Destructive actions only pre-fill the chat box, so a misclick cannot cancel an entry. */
    private static Component suggestButton(String label, String command) {
        return MessageUtils.format(label)
                .withStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                MessageUtils.format("&7Click to fill in &f" + command))));
    }
}
