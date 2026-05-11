package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.TeleportUtils;
import pl.makoto.essentials.util.TeleportManager;
import pl.makoto.essentials.util.MessageUtils;

public class WarpCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("warp")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.warp", 0))
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(DataManager.getWarps().keySet(), builder))
                        .executes(context -> warp(context.getSource(), StringArgumentType.getString(context, "name")))));

        dispatcher.register(Commands.literal("setwarp")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.setwarp", 2))
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> setwarp(context.getSource(), StringArgumentType.getString(context, "name")))));

        dispatcher.register(Commands.literal("delwarp")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.delwarp", 2))
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(DataManager.getWarps().keySet(), builder))
                        .executes(context -> delwarp(context.getSource(), StringArgumentType.getString(context, "name")))));

        dispatcher.register(Commands.literal("listwarps")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.listwarps", 0))
                .executes(context -> listwarps(context.getSource())));
    }

    private static int setwarp(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        DataManager.getWarps().put(name, new PlayerData.SavedLocation(
                player.level().dimension().location().toString(),
                player.position(),
                player.getYRot(),
                player.getXRot()
        ));
        DataManager.saveWarps();

        source.sendSuccess(() -> MessageUtils.prefixed("&aWarp '" + name + "' set!"), true);
        return 1;
    }

    private static int warp(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerData.SavedLocation loc = DataManager.getWarps().get(name);

        if (loc == null) {
            source.sendFailure(MessageUtils.prefixed("&cWarp '" + name + "' not found!"));
            return 0;
        }

        TeleportManager.requestTeleport(player, loc, false);
        return 1;
    }

    private static int delwarp(CommandSourceStack source, String name) {
        if (DataManager.getWarps().remove(name) != null) {
            DataManager.saveWarps();
            source.sendSuccess(() -> MessageUtils.prefixed("&aWarp '" + name + "' deleted."), true);
            return 1;
        } else {
            source.sendFailure(MessageUtils.prefixed("&cWarp '" + name + "' not found!"));
            return 0;
        }
    }

    private static int listwarps(CommandSourceStack source) {
        if (DataManager.getWarps().isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed("&eThere are no warps set."), false);
        } else {
            String warps = String.join(", ", DataManager.getWarps().keySet());
            source.sendSuccess(() -> MessageUtils.prefixed("&7Warps: &f" + warps), false);
        }
        return 1;
    }
}
