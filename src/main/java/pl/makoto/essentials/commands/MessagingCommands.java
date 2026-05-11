package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.MessageUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MessagingCommands {
    private static final Map<UUID, UUID> lastMessaged = new HashMap<>();
    private static final Set<UUID> socialSpy = new HashSet<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var msgCmd = Commands.literal("msg")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.msg", 0))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(context -> msg(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "message")))));

        dispatcher.register(msgCmd);
        dispatcher.register(Commands.literal("m").requires(msgCmd.getRequirement()).redirect(msgCmd.build()));
        dispatcher.register(Commands.literal("w").requires(msgCmd.getRequirement()).redirect(msgCmd.build()));
        dispatcher.register(Commands.literal("tell").requires(msgCmd.getRequirement()).redirect(msgCmd.build()));

        var replyCmd = Commands.literal("reply")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.msg", 0))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> reply(context.getSource(), StringArgumentType.getString(context, "message"))));

        dispatcher.register(replyCmd);
        dispatcher.register(Commands.literal("r").requires(replyCmd.getRequirement()).redirect(replyCmd.build()));

        dispatcher.register(Commands.literal("socialspy")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.socialspy", 2))
                .executes(context -> socialspy(context.getSource())));

        dispatcher.register(Commands.literal("broadcast")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.broadcast", 2))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> broadcast(context.getSource(), StringArgumentType.getString(context, "message")))));
    }

    private static int msg(CommandSourceStack source, ServerPlayer target, String message) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) return 0;

        sendMessage(sender, target, message);
        return 1;
    }

    private static int reply(CommandSourceStack source, String message) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) return 0;

        UUID targetUuid = lastMessaged.get(sender.getUUID());
        if (targetUuid == null) {
            source.sendFailure(MessageUtils.prefixed("&cYou have no one to reply to."));
            return 0;
        }

        ServerPlayer target = sender.getServer().getPlayerList().getPlayer(targetUuid);
        if (target == null) {
            source.sendFailure(MessageUtils.prefixed("&cThe player is no longer online."));
            return 0;
        }

        sendMessage(sender, target, message);
        return 1;
    }

    private static void sendMessage(ServerPlayer sender, ServerPlayer target, String message) {
        if (sender.getUUID().equals(target.getUUID())) {
            if (net.neoforged.fml.loading.FMLLoader.isProduction()) {
                sender.sendSystemMessage(MessageUtils.prefixed("&cYou cannot message yourself!"));
                return;
            }
            Component selfMsg = MessageUtils.format("&d[&7me &d-> &7me&d] &f" + message);
            sender.sendSystemMessage(selfMsg);
            return;
        }

        Component toTarget = MessageUtils.format("&d[&6" + sender.getScoreboardName() + " &d-> &7me&d] &f" + message);
        Component toSender = MessageUtils.format("&d[&7me &d-> &6" + target.getScoreboardName() + "&d] &f" + message);

        target.sendSystemMessage(toTarget);
        sender.sendSystemMessage(toSender);

        lastMessaged.put(sender.getUUID(), target.getUUID());
        lastMessaged.put(target.getUUID(), sender.getUUID());

        // Social Spy
        Component spyMsg = MessageUtils.format("&c&l[Spy] &7" + sender.getScoreboardName() + " &d-> &7" + target.getScoreboardName() + ": &f" + message);

        for (UUID spyUuid : socialSpy) {
            if (spyUuid.equals(sender.getUUID()) || spyUuid.equals(target.getUUID())) continue;
            ServerPlayer spy = sender.getServer().getPlayerList().getPlayer(spyUuid);
            if (spy != null) spy.sendSystemMessage(spyMsg);
        }
    }

    private static int socialspy(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        if (socialSpy.contains(player.getUUID())) {
            socialSpy.remove(player.getUUID());
            source.sendSuccess(() -> MessageUtils.prefixed("&7Social Spy &cdisabled&7."), true);
        } else {
            socialSpy.add(player.getUUID());
            source.sendSuccess(() -> MessageUtils.prefixed("&7Social Spy &aenabled&7."), true);
        }
        return 1;
    }

    private static int broadcast(CommandSourceStack source, String message) {
        Component broadcast = MessageUtils.format("&c&l[Broadcast] &f" + message);

        source.getServer().getPlayerList().broadcastSystemMessage(broadcast, false);
        return 1;
    }
}
