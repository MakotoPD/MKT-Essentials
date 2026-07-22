package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.util.DurationParser;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Small chat games and stats, inspired by FlectonePulse (dice, coin, ball, toponline).
 */
public class FunCommands {

    private static final String[] EIGHT_BALL = {
            "It is certain.", "Without a doubt.", "Yes, definitely.", "Most likely.",
            "Ask again later.", "Cannot predict now.", "Don't count on it.",
            "My reply is no.", "Very doubtful.", "Signs point to yes."
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dice")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.dice", 0))
                .executes(context -> dice(context.getSource())));

        dispatcher.register(Commands.literal("coin")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.coin", 0))
                .executes(context -> coin(context.getSource())));

        dispatcher.register(Commands.literal("8ball")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.8ball", 0))
                .then(Commands.argument("question", StringArgumentType.greedyString())
                        .executes(context -> eightball(context.getSource(), StringArgumentType.getString(context, "question")))));

        dispatcher.register(Commands.literal("toponline")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.toponline", 0))
                .executes(context -> toponline(context.getSource())));
    }

    private static int dice(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        int roll = ThreadLocalRandom.current().nextInt(1, 7);
        broadcast(source, "&e" + player.getScoreboardName() + " &7rolled a &6" + roll + "&7.");
        return 1;
    }

    private static int coin(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        String side = ThreadLocalRandom.current().nextBoolean() ? "&6heads" : "&6tails";
        broadcast(source, "&e" + player.getScoreboardName() + " &7flipped a coin: " + side + "&7.");
        return 1;
    }

    private static int eightball(CommandSourceStack source, String question) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        String answer = EIGHT_BALL[ThreadLocalRandom.current().nextInt(EIGHT_BALL.length)];
        broadcast(source, "&e" + player.getScoreboardName() + " &7asked: &f" + question);
        broadcast(source, "&8[&b8ball&8] &f" + answer);
        return 1;
    }

    private static int toponline(CommandSourceStack source) {
        List<ServerPlayer> players = new ArrayList<>(source.getServer().getPlayerList().getPlayers());
        if (players.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed("&7No players online."), false);
            return 1;
        }
        players.sort((a, b) -> Long.compare(
                DataManager.getPlayerData(b.getUUID()).getTotalPlayTimeMillis(),
                DataManager.getPlayerData(a.getUUID()).getTotalPlayTimeMillis()));

        source.sendSuccess(() -> MessageUtils.prefixed("&7Top online playtime:"), false);
        int limit = Math.min(10, players.size());
        for (int i = 0; i < limit; i++) {
            ServerPlayer player = players.get(i);
            long time = DataManager.getPlayerData(player.getUUID()).getTotalPlayTimeMillis();
            int rank = i + 1;
            source.sendSuccess(() -> MessageUtils.format(
                    "&8" + rank + ". &f" + player.getScoreboardName() + " &7- &6" + DurationParser.format(time)), false);
        }
        return 1;
    }

    private static void broadcast(CommandSourceStack source, String message) {
        source.getServer().getPlayerList().broadcastSystemMessage(MessageUtils.formatBypass(message), false);
    }
}
