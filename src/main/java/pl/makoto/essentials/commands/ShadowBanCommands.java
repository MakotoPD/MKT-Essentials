package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.ShadowBanEntry;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.ShadowBanManager;

import java.util.Map;
import java.util.UUID;

public class ShadowBanCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /shadowban <player> [reason]
        dispatcher.register(Commands.literal("shadowban")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.moderation.shadowban", 3))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> shadowBan(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "reason"))))
                        .executes(context -> shadowBan(
                                context.getSource(),
                                StringArgumentType.getString(context, "player"),
                                "No reason specified"))));

        // /unshadowban <player>
        dispatcher.register(Commands.literal("unshadowban")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.moderation.shadowban", 3))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> unShadowBan(
                                context.getSource(),
                                StringArgumentType.getString(context, "player")))));

        // /shadowbanlist
        dispatcher.register(Commands.literal("shadowbanlist")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.moderation.shadowban", 3))
                .executes(context -> listShadowBans(context.getSource())));
    }

    private static int shadowBan(CommandSourceStack source, String playerName, String reason) {
        // Try to resolve as online player first
        ServerPlayer onlineTarget = source.getServer().getPlayerList().getPlayerByName(playerName);

        UUID uuid;
        if (onlineTarget != null) {
            uuid = onlineTarget.getUUID();
        } else {
            uuid = DataManager.resolveOfflineUUID(playerName, source.getServer());
        }

        if (uuid == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", playerName)));
            return 0;
        }

        String issuerName = source.getTextName();
        String method = Settings.getShadowbanMethod();
        ShadowBanManager.shadowBan(uuid, playerName, reason, issuerName);

        // If the player is online, apply the shadowban immediately
        if (onlineTarget != null) {
            applyShadowBanToOnlinePlayer(onlineTarget, method);
        }

        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("moderation.shadowbanned",
                "player", playerName, "method", method, "reason", reason)), true);
        return 1;
    }

    private static void applyShadowBanToOnlinePlayer(ServerPlayer player, String method) {
        switch (method) {
            case "timeout" -> player.connection.disconnect(Component.literal("io.netty.channel.ConnectTimeoutException: connection timed out"));
            case "full" -> player.connection.disconnect(Component.literal("Disconnected"));
            case "internal-error" -> {
                // Kick after 2-3 seconds (40-60 ticks)
                int delay = 40 + player.getServer().overworld().getRandom().nextInt(21);
                player.getServer().tell(new net.minecraft.server.TickTask(player.getServer().getTickCount() + delay, () -> {
                    ServerPlayer target = player.getServer().getPlayerList().getPlayer(player.getUUID());
                    if (target != null) {
                        target.connection.disconnect(Component.literal(
                                "Internal Exception: io.netty.handler.codec.DecoderException: java.lang.IndexOutOfBoundsException: readerIndex(47) + length(1) exceeds writerIndex(47)"));
                    }
                }));
            }
            case "phantom" -> {
                ShadowBanManager.addPhantom(player.getUUID());
                pl.makoto.essentials.util.PhantomIsolation.apply(player);
            }
        }
    }

    private static int unShadowBan(CommandSourceStack source, String playerName) {
        UUID uuid = DataManager.resolveOfflineUUID(playerName, source.getServer());

        // Also try online player
        if (uuid == null) {
            ServerPlayer onlineTarget = source.getServer().getPlayerList().getPlayerByName(playerName);
            if (onlineTarget != null) {
                uuid = onlineTarget.getUUID();
            }
        }

        if (uuid == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", playerName)));
            return 0;
        }

        if (!ShadowBanManager.isShadowBanned(uuid)) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("moderation.not-shadowbanned")));
            return 0;
        }

        // Clear the flag FIRST, then restore visibility: PhantomIsolation.reveal() re-spawns entities
        // via updatePlayer, which the isolation mixin cancels while the player is still a phantom.
        boolean wasPhantom = ShadowBanManager.isPhantom(uuid);
        ServerPlayer onlineTarget = source.getServer().getPlayerList().getPlayer(uuid);
        ShadowBanManager.unShadowBan(uuid);
        if (wasPhantom && onlineTarget != null) {
            pl.makoto.essentials.util.PhantomIsolation.reveal(onlineTarget);
        }
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("moderation.unshadowbanned", "player", playerName)), true);
        return 1;
    }

    private static int listShadowBans(CommandSourceStack source) {
        Map<String, ShadowBanEntry> all = ShadowBanManager.getAll();

        if (all.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("moderation.shadowban-list-empty")), false);
            return 1;
        }

        String method = Settings.getShadowbanMethod();
        source.sendSuccess(() -> MessageUtils.format(I18n.get("moderation.shadowban-list-header")), false);
        for (Map.Entry<String, ShadowBanEntry> entry : all.entrySet()) {
            ShadowBanEntry ban = entry.getValue();
            source.sendSuccess(() -> MessageUtils.format(I18n.get("moderation.shadowban-list-entry",
                    "player", ban.getPlayerName(), "method", method, "reason", ban.getReason())), false);
        }
        return 1;
    }
}
