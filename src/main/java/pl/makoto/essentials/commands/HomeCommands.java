package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.TeleportUtils;
import pl.makoto.essentials.util.TeleportManager;
import pl.makoto.essentials.util.MessageUtils;

public class HomeCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sethome")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.sethome", 0))
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> sethome(context.getSource(), StringArgumentType.getString(context, "name"))))
                .executes(context -> sethome(context.getSource(), "home")));

        dispatcher.register(Commands.literal("home")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.home", 0))
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                return SharedSuggestionProvider.suggest(DataManager.getPlayerData(player.getUUID()).getHomes().keySet(), builder);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> home(context.getSource(), StringArgumentType.getString(context, "name"))))
                .executes(context -> home(context.getSource(), "home")));

        dispatcher.register(Commands.literal("delhome")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.delhome", 0))
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                return SharedSuggestionProvider.suggest(DataManager.getPlayerData(player.getUUID()).getHomes().keySet(), builder);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> delhome(context.getSource(), StringArgumentType.getString(context, "name"))))
                .executes(context -> delhome(context.getSource(), "home")));

        dispatcher.register(Commands.literal("listhomes")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.listhomes", 0))
                .executes(context -> listhomes(context.getSource())));
    }

    private static int sethome(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerData data = DataManager.getPlayerData(player.getUUID());
        
        // Check home limit
        int maxHomes = Permissions.getIntPermission(player, "mktessentials.max_homes", pl.makoto.essentials.Config.MAX_HOMES.get());
        if (data.getHomes().size() >= maxHomes && !data.getHomes().containsKey(name)) {
            source.sendFailure(MessageUtils.prefixed("&cYou have reached your maximum number of homes (" + maxHomes + ")!"));
            return 0;
        }

        data.getHomes().put(name, new PlayerData.SavedLocation(
                player.level().dimension().location().toString(),
                player.position(),
                player.getYRot(),
                player.getXRot()
        ));
        DataManager.savePlayerData(player.getUUID());

        source.sendSuccess(() -> MessageUtils.prefixed("&aHome '" + name + "' set!"), true);
        return 1;
    }

    private static int home(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerData data = DataManager.getPlayerData(player.getUUID());
        PlayerData.SavedLocation loc = data.getHomes().get(name);

        if (loc == null) {
            source.sendFailure(MessageUtils.prefixed("&cHome '" + name + "' not found!"));
            return 0;
        }

        TeleportManager.requestTeleport(player, loc, false);
        return 1;
    }

    private static int delhome(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerData data = DataManager.getPlayerData(player.getUUID());
        if (data.getHomes().remove(name) != null) {
            DataManager.savePlayerData(player.getUUID());
            source.sendSuccess(() -> MessageUtils.prefixed("&aHome '" + name + "' deleted."), true);
            return 1;
        } else {
            source.sendFailure(MessageUtils.prefixed("&cHome '" + name + "' not found!"));
            return 0;
        }
    }

    private static int listhomes(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerData data = DataManager.getPlayerData(player.getUUID());
        if (data.getHomes().isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed("&eYou have no homes set."), false);
        } else {
            String homes = String.join(", ", data.getHomes().keySet());
            source.sendSuccess(() -> MessageUtils.prefixed("&7Your homes: &f" + homes), false);
        }
        return 1;
    }
}
