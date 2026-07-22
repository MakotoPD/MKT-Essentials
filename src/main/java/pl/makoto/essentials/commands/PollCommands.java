package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.PollManager;

/**
 * Simple yes/no polls, inspired by FlectonePulse: /poll start|end and /vote yes|no.
 * The poll broadcast carries clickable [Yes]/[No] buttons.
 */
public class PollCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("poll")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.poll", 2))
                .then(Commands.literal("start")
                        .then(Commands.argument("question", StringArgumentType.greedyString())
                                .executes(context -> start(context.getSource(), StringArgumentType.getString(context, "question")))))
                .then(Commands.literal("end").executes(context -> end(context.getSource()))));

        dispatcher.register(Commands.literal("vote")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.vote", 0))
                .then(Commands.literal("yes").executes(context -> vote(context.getSource(), true)))
                .then(Commands.literal("no").executes(context -> vote(context.getSource(), false))));
    }

    private static int start(CommandSourceStack source, String question) {
        if (PollManager.isActive()) {
            source.sendFailure(MessageUtils.prefixed("&cA poll is already running. End it first with &6/poll end&c."));
            return 0;
        }
        PollManager.start(question);

        var server = source.getServer();
        server.getPlayerList().broadcastSystemMessage(MessageUtils.formatBypass("&8&m                    "), false);
        server.getPlayerList().broadcastSystemMessage(MessageUtils.formatBypass("&e&lPoll: &f" + question), false);
        server.getPlayerList().broadcastSystemMessage(MessageUtils.formatBypass(
                "&7Vote: <click:run_command:/vote yes><green>[Yes]</green></click> "
                        + "<click:run_command:/vote no><red>[No]</red></click>"), false);
        server.getPlayerList().broadcastSystemMessage(MessageUtils.formatBypass("&8&m                    "), false);
        return 1;
    }

    private static int vote(CommandSourceStack source, boolean yes) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        if (!PollManager.isActive()) {
            source.sendFailure(MessageUtils.prefixed("&cThere is no active poll."));
            return 0;
        }
        PollManager.vote(player.getUUID(), yes);
        source.sendSuccess(() -> MessageUtils.prefixed("&aYou voted &6" + (yes ? "Yes" : "No") + "&a."), false);
        return 1;
    }

    private static int end(CommandSourceStack source) {
        if (!PollManager.isActive()) {
            source.sendFailure(MessageUtils.prefixed("&cThere is no active poll."));
            return 0;
        }
        int yes = PollManager.yes();
        int no = PollManager.no();
        int total = PollManager.total();
        String question = PollManager.getQuestion();
        PollManager.end();

        var server = source.getServer();
        server.getPlayerList().broadcastSystemMessage(MessageUtils.formatBypass("&e&lPoll results: &f" + question), false);
        server.getPlayerList().broadcastSystemMessage(MessageUtils.formatBypass(
                "&aYes: &f" + yes + " &8| &cNo: &f" + no + " &8| &7Total: &f" + total), false);
        return 1;
    }
}
