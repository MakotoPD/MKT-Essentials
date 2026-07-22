package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.CommandUtils;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Roleplay chat commands (/me, /do, /try), inspired by FlectonePulse.
 * Formats are configurable under {@code roleplay:}; the action text is permission-filtered like
 * normal chat, muted players are blocked, and viewers who ignore the sender don't see it.
 */
public class RoleplayCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Vanilla registers /me with a MessageArgument named "action". Brigadier merges same-named
        // nodes, so our greedyString "action" clashes with vanilla's Message type at execution.
        // Drop the vanilla node first (like /tp) and register our own. /do and /try aren't vanilla.
        CommandUtils.removeRootCommand(dispatcher, "me");
        dispatcher.register(Commands.literal("me")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.me", 0))
                .then(Commands.argument("action", StringArgumentType.greedyString())
                        .executes(context -> roleplay(context.getSource(),
                                Settings.getMeFormat(), StringArgumentType.getString(context, "action"), null))));

        dispatcher.register(Commands.literal("do")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.do", 0))
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(context -> roleplay(context.getSource(),
                                Settings.getDoFormat(), StringArgumentType.getString(context, "text"), null))));

        dispatcher.register(Commands.literal("try")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.try", 0))
                .then(Commands.argument("action", StringArgumentType.greedyString())
                        .executes(context -> {
                            boolean success = ThreadLocalRandom.current().nextBoolean();
                            String result = success ? Settings.getTrySuccess() : Settings.getTryFail();
                            return roleplay(context.getSource(), Settings.getTryFormat(),
                                    StringArgumentType.getString(context, "action"), result);
                        })));
    }

    private static int roleplay(CommandSourceStack source, String format, String action, String resultText) {
        if (!Settings.isRoleplayEnabled()) {
            source.sendFailure(MessageUtils.prefixed("&cRoleplay commands are disabled."));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        // Muted players can't roleplay-chat either
        PlayerData data = DataManager.getPlayerData(player.getUUID());
        long mute = data.getMuteExpiration();
        if (mute == -1 || (mute > 0 && mute > System.currentTimeMillis())) {
            player.sendSystemMessage(MessageUtils.prefixed("&cYou are muted and cannot use this."));
            return 0;
        }

        String withResult = format.replace("{result}", resultText == null ? "" : resultText);
        String[] parts = withResult.split("\\{message\\}", 2);

        MutableComponent full = MessageUtils.format(player, parts[0]);
        full.append(MessageUtils.formatWithPermissions(player, action));
        if (parts.length > 1) full.append(MessageUtils.format(player, parts[1]));

        for (ServerPlayer viewer : player.getServer().getPlayerList().getPlayers()) {
            if (!viewer.getUUID().equals(player.getUUID())
                    && DataManager.getPlayerData(viewer.getUUID()).isIgnoring(player.getUUID())) {
                continue;
            }
            viewer.sendSystemMessage(full);
        }
        MKTEssentials.LOGGER.info("[RP] " + full.getString());
        return 1;
    }
}
