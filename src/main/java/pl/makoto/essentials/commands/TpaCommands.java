package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.TpaManager;
import pl.makoto.essentials.util.TeleportUtils;
import pl.makoto.essentials.util.TeleportManager;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

public class TpaCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.tpa", 0))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> tpa(context.getSource(), EntityArgument.getPlayer(context, "player"), false))));

        dispatcher.register(Commands.literal("tpahere")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.tpahere", 0))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> tpa(context.getSource(), EntityArgument.getPlayer(context, "player"), true))));

        dispatcher.register(Commands.literal("tpaccept")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.tpa", 0))
                .executes(context -> tpaccept(context.getSource())));

        dispatcher.register(Commands.literal("tpdeny")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.tpa", 0))
                .executes(context -> tpdeny(context.getSource())));
    }

    private static int tpa(CommandSourceStack source, ServerPlayer target, boolean here) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) return 0;
        
        if (sender == target && net.neoforged.fml.loading.FMLLoader.isProduction()) {
            source.sendFailure(MessageUtils.prefixed("&cYou cannot teleport to yourself!"));
            return 0;
        }

        TpaManager.addRequest(sender, target, here);
        
        target.sendSystemMessage(MessageUtils.prefixed("&6" + sender.getScoreboardName() + " &7has requested to teleport " + (here ? "you to them" : "to you") + ".")
                .copy().append("\n")
                .append(MessageUtils.format(" &a&l[ACCEPT] ").withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"))))
                .append(MessageUtils.format(" &c&l[DENY]").withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny")))));
        
        source.sendSuccess(() -> MessageUtils.prefixed("&aTeleport request sent to &6" + target.getScoreboardName() + "."), true);
        return 1;
    }

    private static int tpaccept(CommandSourceStack source) {
        ServerPlayer target = source.getPlayer();
        if (target == null) return 0;

        TpaManager.TpaRequest req = TpaManager.getRequest(target.getUUID());
        if (req == null) {
            source.sendFailure(MessageUtils.prefixed("&cNo pending teleport requests found."));
            return 0;
        }

        ServerPlayer sender = target.getServer().getPlayerList().getPlayer(req.senderUuid());
        if (sender == null) {
            source.sendFailure(MessageUtils.prefixed("&cThe sender is no longer online."));
            TpaManager.removeRequest(target.getUUID());
            return 0;
        }

        if (req.here()) {
            TeleportManager.requestTeleport(target, new PlayerData.SavedLocation(
                    sender.level().dimension().location().toString(),
                    sender.position(), sender.getYRot(), sender.getXRot()
            ), false);
        } else {
            TeleportManager.requestTeleport(sender, new PlayerData.SavedLocation(
                    target.level().dimension().location().toString(),
                    target.position(), target.getYRot(), target.getXRot()
            ), false);
        }

        TpaManager.removeRequest(target.getUUID());
        return 1;
    }

    private static int tpdeny(CommandSourceStack source) {
        ServerPlayer target = source.getPlayer();
        if (target == null) return 0;

        TpaManager.TpaRequest req = TpaManager.getRequest(target.getUUID());
        if (req == null) {
            source.sendFailure(MessageUtils.prefixed("&cNo pending teleport requests found."));
            return 0;
        }

        ServerPlayer sender = target.getServer().getPlayerList().getPlayer(req.senderUuid());
        if (sender != null) {
            sender.sendSystemMessage(MessageUtils.prefixed("&6" + target.getScoreboardName() + " &cdenied your teleport request."));
        }

        TpaManager.removeRequest(target.getUUID());
        source.sendSuccess(() -> MessageUtils.prefixed("&cTeleport request denied."), true);
        return 1;
    }
}
