package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import pl.makoto.essentials.config.ConfigManager;
import pl.makoto.essentials.util.DurationParser;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.SchedulerManager;
import pl.makoto.essentials.util.SchedulerManager.Run;
import pl.makoto.essentials.util.SchedulerManager.Scheduler;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * /scheduler — inspection and manual control of the schedulers defined in {@code scheduler.yml}.
 * Level 4 like the command queue: a scheduler runs arbitrary console commands.
 */
public class SchedulerCommands {

    private static final String PERM_VIEW = "mktessentials.admin.scheduler.view";
    private static final String PERM_MANAGE = "mktessentials.admin.scheduler.manage";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("scheduler")
                .requires(source -> Permissions.hasPermission(source, PERM_VIEW, 4))
                .executes(context -> list(context.getSource()))
                .then(Commands.literal("list")
                        .executes(context -> list(context.getSource())))
                .then(Commands.literal("info")
                        .then(nameArg().executes(SchedulerCommands::info)))
                .then(Commands.literal("history")
                        .then(nameArg().executes(SchedulerCommands::history)))
                .then(Commands.literal("run")
                        .requires(source -> Permissions.hasPermission(source, PERM_MANAGE, 4))
                        .then(nameArg().executes(SchedulerCommands::run)))
                .then(Commands.literal("enable")
                        .requires(source -> Permissions.hasPermission(source, PERM_MANAGE, 4))
                        .then(nameArg().executes(context -> toggle(context, true))))
                .then(Commands.literal("disable")
                        .requires(source -> Permissions.hasPermission(source, PERM_MANAGE, 4))
                        .then(nameArg().executes(context -> toggle(context, false))))
                .then(Commands.literal("reload")
                        .requires(source -> Permissions.hasPermission(source, PERM_MANAGE, 4))
                        .executes(context -> reload(context.getSource()))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> nameArg() {
        return Commands.argument("name", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(SchedulerManager.getNames(), builder));
    }

    private static Scheduler resolve(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        Scheduler scheduler = SchedulerManager.get(name);
        if (scheduler == null) {
            context.getSource().sendFailure(MessageUtils.prefixed("&cNo scheduler named &f" + name + "&c."));
        }
        return scheduler;
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private static int list(CommandSourceStack source) {
        List<Scheduler> all = SchedulerManager.getAll();
        if (all.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed(
                    "&7No schedulers defined. Add them in &fconfig/mktessentials/scheduler.yml&7."), false);
            return 1;
        }

        source.sendSuccess(() -> MessageUtils.format("&8&m                &r &6&l Schedulers &r&8&m                "), false);
        for (Scheduler scheduler : all) {
            Component line = MessageUtils.format("&8• " + (scheduler.enabled ? "&a●" : "&8●") + " &6" + scheduler.name
                    + " &8· &7" + scheduler.commands.size() + " cmd, " + scheduler.mode.name().toLowerCase()
                    + " &8· &7next: &f" + describeNext(scheduler) + " ")
                    .copy()
                    .append(button("&8[&bi&8]", "/scheduler info " + scheduler.name))
                    .append(MessageUtils.format(" "))
                    .append(button("&8[&arun&8]", "/scheduler run " + scheduler.name));
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        Scheduler scheduler = resolve(context);
        if (scheduler == null) return 0;
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> MessageUtils.format("&8&m                &r &6&l " + scheduler.name + " &r&8&m                "), false);
        source.sendSuccess(() -> MessageUtils.format("&7Enabled: " + (scheduler.enabled ? "&ayes" : "&cno")
                + (scheduler.finished ? " &8(one-shot, already ran)" : "")), false);
        source.sendSuccess(() -> MessageUtils.format("&7Mode: &f" + scheduler.mode.name().toLowerCase()
                + (scheduler.mode == SchedulerManager.Mode.RANDOM_N ? " &7(&f" + scheduler.randomCount + "&7)" : "")), false);
        source.sendSuccess(() -> MessageUtils.format("&7Schedule: &f" + describeSchedule(scheduler)), false);
        source.sendSuccess(() -> MessageUtils.format("&7Next run: &f" + describeNext(scheduler)), false);
        source.sendSuccess(() -> MessageUtils.format("&7Last run: &f"
                + (scheduler.lastRun > 0 ? TIME_FORMAT.format(Instant.ofEpochMilli(scheduler.lastRun)) : "never")), false);

        String conditions = describeConditions(scheduler);
        if (!conditions.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.format("&7Conditions: &f" + conditions
                    + " &8(" + (scheduler.waitForConditions ? "wait" : "skip") + ")"), false);
        }

        Run last = scheduler.lastResult();
        if (last != null) {
            source.sendSuccess(() -> MessageUtils.format("&7Last command: &f/" + last.command), false);
            source.sendSuccess(() -> MessageUtils.format("&7Last result: "
                    + (last.success ? "&a" : "&c") + last.output), false);
        }

        source.sendSuccess(() -> MessageUtils.format("&7Commands (&f" + scheduler.commands.size() + "&7):"), false);
        for (SchedulerManager.Entry entry : scheduler.commands) {
            source.sendSuccess(() -> MessageUtils.format("  &8- &f/" + entry.command
                    + (scheduler.mode == SchedulerManager.Mode.WEIGHTED ? " &8(weight " + entry.weight + ")" : "")), false);
        }
        return 1;
    }

    private static int history(CommandContext<CommandSourceStack> context) {
        Scheduler scheduler = resolve(context);
        if (scheduler == null) return 0;
        CommandSourceStack source = context.getSource();

        if (scheduler.history.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed("&6" + scheduler.name + " &7has not run yet."), false);
            return 1;
        }
        source.sendSuccess(() -> MessageUtils.format("&8&m            &r &6&l " + scheduler.name + " history &r&8&m            "), false);
        // Newest first; the history is capped in memory and is not kept across a restart.
        List<Run> runs = new java.util.ArrayList<>(scheduler.history);
        for (int i = runs.size() - 1; i >= 0; i--) {
            Run run = runs.get(i);
            source.sendSuccess(() -> MessageUtils.format("&8[" + TIME_FORMAT.format(Instant.ofEpochMilli(run.at)) + "] "
                    + (run.success ? "&a✔ " : "&c✘ ") + "&f/" + run.command
                    + (run.success ? "" : " &8— &c" + run.output)), false);
        }
        return 1;
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        Scheduler scheduler = resolve(context);
        if (scheduler == null) return 0;
        SchedulerManager.fire(scheduler, context.getSource().getServer());
        context.getSource().sendSuccess(() -> MessageUtils.prefixed("&aRan scheduler &6" + scheduler.name
                + "&a. Check &f/scheduler history " + scheduler.name + "&a."), true);
        return 1;
    }

    private static int toggle(CommandContext<CommandSourceStack> context, boolean enabled) {
        Scheduler scheduler = resolve(context);
        if (scheduler == null) return 0;
        scheduler.enabled = enabled;
        if (enabled) scheduler.finished = false;
        context.getSource().sendSuccess(() -> MessageUtils.prefixed("&6" + scheduler.name + " &7is now "
                + (enabled ? "&aenabled" : "&cdisabled") + "&7."), true);
        // ponytail: runtime toggle only — the YAML is never rewritten, so say so rather than lie about it
        context.getSource().sendSuccess(() -> MessageUtils.prefixed(
                "&8Until the next reload — set &fenabled: " + enabled + " &8in scheduler.yml to persist it."), false);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        if (!ConfigManager.reload()) {
            source.sendFailure(MessageUtils.prefixed("&cReload failed — see the server console."));
            return 0;
        }
        source.sendSuccess(() -> MessageUtils.prefixed("&aReloaded &f" + SchedulerManager.getAll().size()
                + "&a scheduler(s) from scheduler.yml."), true);
        return 1;
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    private static String describeSchedule(Scheduler scheduler) {
        if (!scheduler.at.isEmpty()) {
            String times = String.join(", ", scheduler.at.stream().map(Object::toString).toList());
            String days = scheduler.days.isEmpty() ? "every day"
                    : String.join(", ", scheduler.days.stream().map(d -> d.name().toLowerCase()).toList());
            return "at " + times + " (" + days + ")";
        }
        if (scheduler.once) return "once, " + DurationParser.format(scheduler.startDelayMs) + " after start";
        return scheduler.intervalMaxMs > scheduler.intervalMinMs
                ? "every " + DurationParser.format(scheduler.intervalMinMs) + "-" + DurationParser.format(scheduler.intervalMaxMs)
                : "every " + DurationParser.format(scheduler.intervalMinMs);
    }

    private static String describeNext(Scheduler scheduler) {
        if (!scheduler.enabled) return "disabled";
        if (scheduler.finished) return "done";
        if (!scheduler.at.isEmpty()) return "next configured time";
        long remaining = scheduler.nextRun - System.currentTimeMillis();
        return remaining <= 0 ? "due now" : "in " + DurationParser.format(remaining);
    }

    private static String describeConditions(Scheduler scheduler) {
        List<String> parts = new java.util.ArrayList<>();
        if (scheduler.minPlayers > 0) parts.add("min " + scheduler.minPlayers + " players");
        if (scheduler.maxPlayers != Integer.MAX_VALUE) parts.add("max " + scheduler.maxPlayers + " players");
        if (scheduler.requiredPlayer != null) parts.add(scheduler.requiredPlayer + " online");
        if (scheduler.requiredDimension != null) parts.add("someone in " + scheduler.requiredDimension);
        if (scheduler.minUptimeMs > 0) parts.add("uptime " + DurationParser.format(scheduler.minUptimeMs));
        if (scheduler.betweenFrom != null) parts.add("between " + scheduler.betweenFrom + "-" + scheduler.betweenTo);
        if (scheduler.gameTimeFrom >= 0) parts.add("game time " + scheduler.gameTimeFrom + "-" + scheduler.gameTimeTo);
        return String.join(", ", parts);
    }

    private static Component button(String label, String command) {
        return MessageUtils.format(label)
                .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }
}
