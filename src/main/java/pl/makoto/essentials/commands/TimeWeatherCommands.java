package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

public class TimeWeatherCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Time Commands
        dispatcher.register(Commands.literal("day")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.time", 2))
                .executes(context -> setTime(context.getSource(), 1000, "day")));

        dispatcher.register(Commands.literal("night")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.time", 2))
                .executes(context -> setTime(context.getSource(), 13000, "night")));

        dispatcher.register(Commands.literal("noon")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.time", 2))
                .executes(context -> setTime(context.getSource(), 6000, "noon")));

        dispatcher.register(Commands.literal("midnight")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.time", 2))
                .executes(context -> setTime(context.getSource(), 18000, "midnight")));

        // Weather Commands
        dispatcher.register(Commands.literal("sun")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.weather", 2))
                .executes(context -> setWeather(context.getSource(), "clear")));

        dispatcher.register(Commands.literal("rain")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.weather", 2))
                .executes(context -> setWeather(context.getSource(), "rain")));

        dispatcher.register(Commands.literal("storm")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.weather", 2))
                .executes(context -> setWeather(context.getSource(), "thunder")));
    }

    private static int setTime(CommandSourceStack source, int time, String name) {
        ServerLevel level = source.getLevel();
        level.setDayTime(time);
        source.sendSuccess(() -> MessageUtils.prefixed("&7Time set to &6" + name + "&7."), true);
        return 1;
    }

    private static int setWeather(CommandSourceStack source, String type) {
        ServerLevel level = source.getLevel();
        switch (type) {
            case "clear" -> level.setWeatherParameters(6000, 0, false, false);
            case "rain" -> level.setWeatherParameters(0, 6000, true, false);
            case "thunder" -> level.setWeatherParameters(0, 6000, true, true);
        }
        source.sendSuccess(() -> MessageUtils.prefixed("&7Weather set to &6" + type + "&7."), true);
        return 1;
    }
}
