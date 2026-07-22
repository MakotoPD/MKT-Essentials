package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Offline mail: /mail send|read|clear (+ /clearmail), inspired by FlectonePulse mail.
 * Messages are stored on the recipient's {@link PlayerData} and delivered when they log in.
 */
public class MailCommands {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mail")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.mail", 0))
                .then(Commands.literal("send")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(context -> send(context.getSource(),
                                                StringArgumentType.getString(context, "player"),
                                                StringArgumentType.getString(context, "message"))))))
                .then(Commands.literal("read").executes(context -> read(context.getSource())))
                .then(Commands.literal("clear").executes(context -> clear(context.getSource())))
                .executes(context -> read(context.getSource())));

        dispatcher.register(Commands.literal("clearmail")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.mail", 0))
                .executes(context -> clear(context.getSource())));
    }

    private static int send(CommandSourceStack source, String targetName, String message) {
        if (!Settings.isMailEnabled()) {
            source.sendFailure(MessageUtils.prefixed("&cThe mail system is disabled."));
            return 0;
        }

        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(targetName);
        UUID targetUuid = online != null ? online.getUUID()
                : DataManager.resolveOfflineUUID(targetName, source.getServer());
        if (targetUuid == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", targetName)));
            return 0;
        }

        String from = source.getPlayer() != null ? source.getPlayer().getScoreboardName() : "Server";
        PlayerData data = DataManager.getPlayerData(targetUuid);
        data.addMail(new PlayerData.MailMessage(from, message, System.currentTimeMillis()));

        // Cap stored mail (drop oldest)
        List<PlayerData.MailMessage> box = data.getMail();
        int max = Math.max(1, Settings.getMailMaxPerPlayer());
        while (box.size() > max) box.remove(0);
        DataManager.savePlayerData(targetUuid);

        source.sendSuccess(() -> MessageUtils.prefixed("&aMail sent to &6" + targetName + "&a."), false);
        if (online != null) {
            online.sendSystemMessage(MessageUtils.prefixed("&eYou have new mail from &6" + from + "&e. Use &6/mail read&e."));
        }
        return 1;
    }

    private static int read(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        List<PlayerData.MailMessage> box = DataManager.getPlayerData(player.getUUID()).getMail();
        if (box.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed("&7You have no mail."), false);
            return 1;
        }

        source.sendSuccess(() -> MessageUtils.prefixed("&7Your mail (&e" + box.size() + "&7):"), false);
        for (PlayerData.MailMessage mail : box) {
            String time = DATE_FORMAT.format(Instant.ofEpochMilli(mail.time));
            source.sendSuccess(() -> MessageUtils.format("&8[&7" + time + "&8] &6" + mail.from + "&7: &f" + mail.message), false);
        }
        source.sendSuccess(() -> MessageUtils.prefixed("&7Use &6/mail clear &7to delete your mail."), false);
        return 1;
    }

    private static int clear(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerData data = DataManager.getPlayerData(player.getUUID());
        int count = data.getMail().size();
        data.clearMail();
        DataManager.savePlayerData(player.getUUID());
        source.sendSuccess(() -> MessageUtils.prefixed("&aCleared &6" + count + "&a mail message(s)."), false);
        return 1;
    }
}
