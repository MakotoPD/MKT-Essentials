package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.MessageUtils;

public class MuteCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mute")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.mute", 2))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("until", StringArgumentType.string())
                                .executes(context -> mute(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "until"))))
                        .executes(context -> mute(context.getSource(), EntityArgument.getPlayer(context, "player"), "forever"))));

        dispatcher.register(Commands.literal("unmute")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.unmute", 2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> unmute(context.getSource(), EntityArgument.getPlayer(context, "player")))));
    }

    private static int mute(CommandSourceStack source, ServerPlayer target, String until) {
        if (target == null) return 0;

        long expiration = -1;
        if (!until.equalsIgnoreCase("forever")) {
            // Simple duration parser: 2h, 1d, etc.
            try {
                long duration = parseDuration(until);
                expiration = System.currentTimeMillis() + duration;
            } catch (Exception e) {
                source.sendFailure(MessageUtils.prefixed("&cInvalid duration format! Use e.g. 2h, 1d."));
                return 0;
            }
        }

        PlayerData data = DataManager.getPlayerData(target.getUUID());
        data.setMuteExpiration(expiration);
        DataManager.savePlayerData(target.getUUID());

        String timeStr = until.equalsIgnoreCase("forever") ? "forever" : "until " + until;
        source.sendSuccess(() -> MessageUtils.prefixed("&7Muted &6" + target.getScoreboardName() + " &7" + timeStr + "."), true);
        target.sendSystemMessage(MessageUtils.prefixed("&cYou have been muted " + timeStr + "."));
        return 1;
    }

    private static int unmute(CommandSourceStack source, ServerPlayer target) {
        if (target == null) return 0;

        PlayerData data = DataManager.getPlayerData(target.getUUID());
        data.setMuteExpiration(0);
        DataManager.savePlayerData(target.getUUID());

        source.sendSuccess(() -> MessageUtils.prefixed("&aUnmuted &6" + target.getScoreboardName() + "&a."), true);
        target.sendSystemMessage(MessageUtils.prefixed("&aYou have been unmuted."));
        return 1;
    }

    private static long parseDuration(String s) {
        s = s.toLowerCase();
        long multiplier = 1000; // ms
        if (s.endsWith("s")) { multiplier = 1000; s = s.substring(0, s.length() - 1); }
        else if (s.endsWith("m")) { multiplier = 60 * 1000; s = s.substring(0, s.length() - 1); }
        else if (s.endsWith("h")) { multiplier = 60 * 60 * 1000; s = s.substring(0, s.length() - 1); }
        else if (s.endsWith("d")) { multiplier = 24 * 60 * 60 * 1000; s = s.substring(0, s.length() - 1); }
        
        return Long.parseLong(s) * multiplier;
    }
}
