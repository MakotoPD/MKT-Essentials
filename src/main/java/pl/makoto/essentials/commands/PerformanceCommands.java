package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

public class PerformanceCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tps")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.tps", 0))
                .executes(context -> tps(context.getSource())));

        dispatcher.register(Commands.literal("lag")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.lag", 2))
                .executes(context -> lag(context.getSource())));
    }

    private static int tps(CommandSourceStack source) {
        double mspt = source.getServer().getAverageTickTimeNanos() / 1_000_000.0;
        double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 0.01));
        String color = tps >= 18 ? "&a" : (tps >= 14 ? "&e" : "&c");
        final String line = "&7TPS: " + color + String.format("%.1f", tps)
                + " &7(MSPT: &f" + String.format("%.2f", mspt) + "&7)";
        source.sendSuccess(() -> MessageUtils.prefixed(line), false);
        return 1;
    }

    private static int lag(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
        double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 0.01));
        String tpsColor = tps >= 18 ? "&a" : (tps >= 14 ? "&e" : "&c");

        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMb = runtime.maxMemory() / (1024 * 1024);

        send(source, "&7--- &6Server Performance &7---");
        send(source, "&7TPS: " + tpsColor + String.format("%.1f", tps) + " &7MSPT: &f" + String.format("%.2f", mspt));
        send(source, "&7Memory: &f" + usedMb + " &7/ &f" + maxMb + " MB");
        send(source, "&7Players: &f" + server.getPlayerList().getPlayerCount() + " &7/ &f" + server.getMaxPlayers());

        for (ServerLevel level : server.getAllLevels()) {
            int entities = 0;
            for (Entity ignored : level.getAllEntities()) entities++;
            send(source, "&7" + level.dimension().location() + "&8: &f"
                    + level.getChunkSource().getLoadedChunksCount() + " &7chunks, &f" + entities + " &7entities");
        }
        return 1;
    }

    private static void send(CommandSourceStack source, String line) {
        source.sendSuccess(() -> MessageUtils.format(line), false);
    }
}
