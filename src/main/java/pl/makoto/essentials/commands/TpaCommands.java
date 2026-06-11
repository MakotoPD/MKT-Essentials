package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.TpaManager;
import pl.makoto.essentials.util.TeleportUtils;
import pl.makoto.essentials.util.TeleportManager;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.config.I18n;

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
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> tpacceptFrom(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                .executes(context -> tpaccept(context.getSource())));

        dispatcher.register(Commands.literal("tpdeny")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.tpa", 0))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> tpdenyFrom(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                .executes(context -> tpdeny(context.getSource())));

        dispatcher.register(Commands.literal("tpacancel")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.tpa", 0))
                .executes(context -> tpacancel(context.getSource())));

        dispatcher.register(Commands.literal("tptoggle")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.tptoggle", 0))
                .executes(context -> tptoggle(context.getSource())));
    }

    private static int tpa(CommandSourceStack source, ServerPlayer target, boolean here) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) return 0;
        
        if (sender == target && net.neoforged.fml.loading.FMLLoader.isProduction()) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("tpa.self")));
            return 0;
        }

        if (DataManager.getPlayerData(target.getUUID()).isTpaDisabled()
                && !Permissions.hasPermission(sender, "mktessentials.admin.tptoggle.bypass", 2)) {
            source.sendFailure(MessageUtils.prefixed("&c" + target.getScoreboardName() + " has teleport requests disabled."));
            return 0;
        }

        TpaManager.addRequest(sender, target, here);
        
        target.sendSystemMessage(MessageUtils.prefixed(I18n.get("tpa.received", "player", sender.getScoreboardName(), "direction", (here ? I18n.get("tpa.you-to-them") : I18n.get("tpa.to-you"))))
                .copy().append("\n")
                .append(MessageUtils.format(" &a&l[ACCEPT] ").withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"))))
                .append(MessageUtils.format(" &c&l[DENY]").withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny")))));
        
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("tpa.sent", "player", target.getScoreboardName())), true);
        return 1;
    }

    private static int tpaccept(CommandSourceStack source) {
        ServerPlayer target = source.getPlayer();
        if (target == null) return 0;

        TpaManager.TpaRequest req = TpaManager.getLatestRequest(target.getUUID());
        if (req == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("tpa.no-request")));
            return 0;
        }

        ServerPlayer sender = target.getServer().getPlayerList().getPlayer(req.senderUuid());
        if (sender == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", "sender")));
            TpaManager.removeRequest(target.getUUID(), req);
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

        TpaManager.removeRequest(target.getUUID(), req);
        return 1;
    }

    private static int tpacceptFrom(CommandSourceStack source, ServerPlayer sender) {
        ServerPlayer target = source.getPlayer();
        if (target == null) return 0;

        TpaManager.TpaRequest req = TpaManager.getRequestFrom(target.getUUID(), sender.getUUID());
        if (req == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("tpa.no-request-from")));
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

        TpaManager.removeRequest(target.getUUID(), req);
        return 1;
    }

    private static int tpdenyFrom(CommandSourceStack source, ServerPlayer sender) {
        ServerPlayer target = source.getPlayer();
        if (target == null) return 0;

        TpaManager.TpaRequest req = TpaManager.getRequestFrom(target.getUUID(), sender.getUUID());
        if (req == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("tpa.no-request-from")));
            return 0;
        }

        sender.sendSystemMessage(MessageUtils.prefixed(I18n.get("tpa.denied")));
        TpaManager.removeRequest(target.getUUID(), req);
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("tpa.denied")), true);
        return 1;
    }

    private static int tpacancel(CommandSourceStack source) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) return 0;

        int cancelled = TpaManager.cancelRequestsFrom(sender.getUUID());
        if (cancelled == 0) {
            source.sendFailure(MessageUtils.prefixed("&cYou have no outgoing teleport requests."));
            return 0;
        }
        source.sendSuccess(() -> MessageUtils.prefixed("&7Cancelled &6" + cancelled + " &7outgoing teleport request"
                + (cancelled == 1 ? "" : "s") + "."), false);
        return 1;
    }

    private static int tptoggle(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerData data = DataManager.getPlayerData(player.getUUID());
        boolean nowDisabled = !data.isTpaDisabled();
        data.setTpaDisabled(nowDisabled);
        DataManager.savePlayerData(player.getUUID());

        source.sendSuccess(() -> MessageUtils.prefixed(nowDisabled
                ? "&7Incoming teleport requests are now &cblocked&7."
                : "&7Incoming teleport requests are now &aallowed&7."), false);
        return 1;
    }

    private static int tpdeny(CommandSourceStack source) {
        ServerPlayer target = source.getPlayer();
        if (target == null) return 0;

        TpaManager.TpaRequest req = TpaManager.getLatestRequest(target.getUUID());
        if (req == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("tpa.no-request")));
            return 0;
        }

        ServerPlayer sender = target.getServer().getPlayerList().getPlayer(req.senderUuid());
        if (sender != null) {
            sender.sendSystemMessage(MessageUtils.prefixed(I18n.get("tpa.denied")));
        }

        TpaManager.removeRequest(target.getUUID(), req);
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("tpa.denied")), true);
        return 1;
    }
}
