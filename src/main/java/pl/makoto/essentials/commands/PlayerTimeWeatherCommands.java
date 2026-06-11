package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.PlayerEnvironmentManager;

/**
 * /ptime day|noon|night|midnight|<ticks>|reset — client-side time, only for you.
 * /pweather clear|rain|reset — client-side weather, only for you.
 */
public class PlayerTimeWeatherCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ptime")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.ptime", 0))
                .then(Commands.literal("day").executes(ctx -> setTime(ctx.getSource(), 1000, "day")))
                .then(Commands.literal("noon").executes(ctx -> setTime(ctx.getSource(), 6000, "noon")))
                .then(Commands.literal("night").executes(ctx -> setTime(ctx.getSource(), 13000, "night")))
                .then(Commands.literal("midnight").executes(ctx -> setTime(ctx.getSource(), 18000, "midnight")))
                .then(Commands.literal("reset").executes(ctx -> resetTime(ctx.getSource())))
                .then(Commands.argument("ticks", LongArgumentType.longArg(0, 23999))
                        .executes(ctx -> setTime(ctx.getSource(), LongArgumentType.getLong(ctx, "ticks"),
                                String.valueOf(LongArgumentType.getLong(ctx, "ticks"))))));

        dispatcher.register(Commands.literal("pweather")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.pweather", 0))
                .then(Commands.literal("clear").executes(ctx -> setWeather(ctx.getSource(), false)))
                .then(Commands.literal("sun").executes(ctx -> setWeather(ctx.getSource(), false)))
                .then(Commands.literal("rain").executes(ctx -> setWeather(ctx.getSource(), true)))
                .then(Commands.literal("reset").executes(ctx -> resetWeather(ctx.getSource()))));
    }

    private static int setTime(CommandSourceStack source, long ticks, String label) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerEnvironmentManager.setPlayerTime(player, ticks);
        source.sendSuccess(() -> MessageUtils.prefixed("&7Your personal time is now &6" + label
                + "&7. Use &6/ptime reset &7to go back to server time."), false);
        return 1;
    }

    private static int resetTime(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerEnvironmentManager.resetPlayerTime(player);
        source.sendSuccess(() -> MessageUtils.prefixed("&7Your time is back in sync with the server."), false);
        return 1;
    }

    private static int setWeather(CommandSourceStack source, boolean rain) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerEnvironmentManager.setPlayerWeather(player, rain);
        source.sendSuccess(() -> MessageUtils.prefixed("&7Your personal weather is now &6" + (rain ? "rain" : "clear")
                + "&7. Use &6/pweather reset &7to go back to server weather."), false);
        return 1;
    }

    private static int resetWeather(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerEnvironmentManager.resetPlayerWeather(player);
        source.sendSuccess(() -> MessageUtils.prefixed("&7Your weather is back in sync with the server."), false);
        return 1;
    }
}
