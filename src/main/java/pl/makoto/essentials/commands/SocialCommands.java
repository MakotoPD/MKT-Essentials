package pl.makoto.essentials.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.LuckPermsHook;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.PlayerListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assorted social/QoL commands ported from FlectonePulse: /stream, /anon, /online, /symbol,
 * /chatsetting.
 */
public class SocialCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stream")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.stream", 0))
                .then(Commands.argument("url", StringArgumentType.greedyString())
                        .executes(context -> stream(context.getSource(), StringArgumentType.getString(context, "url")))));

        dispatcher.register(Commands.literal("anon")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.anon", 0))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> anon(context.getSource(), StringArgumentType.getString(context, "message")))));

        dispatcher.register(Commands.literal("online")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.online", 0))
                .executes(context -> online(context.getSource())));

        dispatcher.register(Commands.literal("symbol")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.symbol", 0))
                .executes(context -> symbols(context.getSource())));

        dispatcher.register(Commands.literal("chatsetting")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.chatsetting", 0))
                .executes(context -> openMenu(context.getSource()))
                // toggle receiving mention pings
                .then(Commands.literal("mentions").executes(context -> toggleMentions(context.getSource())))
                // toggle receiving private messages entirely (/msgtoggle)
                .then(Commands.literal("messages").executes(context -> toggleMessages(context.getSource())))
                // ignore/unignore a specific player (chat + private messages)
                .then(Commands.literal("ignore")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> ignoreToggle(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                // list players you're ignoring
                .then(Commands.literal("ignorelist").executes(context -> ignoreList(context.getSource()))));
    }

    private static int stream(CommandSourceStack source, String url) {
        if (!Settings.isStreamEnabled()) {
            source.sendFailure(MessageUtils.prefixed("&cStream announcements are disabled."));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerData data = DataManager.getPlayerData(player.getUUID());
        data.setStreaming(true);
        data.setRecording(false);
        DataManager.savePlayerData(player.getUUID());
        PlayerListener.refreshNickname(player);

        String announce = Settings.getStreamFormat().replace("{url}", url);
        source.getServer().getPlayerList().broadcastSystemMessage(MessageUtils.formatBypass(player, announce), false);
        return 1;
    }

    private static int anon(CommandSourceStack source, String message) {
        if (!Settings.isAnonEnabled()) {
            source.sendFailure(MessageUtils.prefixed("&cAnonymous chat is disabled."));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        // Respect mute
        long mute = DataManager.getPlayerData(player.getUUID()).getMuteExpiration();
        if (mute == -1 || (mute > 0 && mute > System.currentTimeMillis())) {
            player.sendSystemMessage(MessageUtils.prefixed("&cYou are muted and cannot use this."));
            return 0;
        }

        var line = MessageUtils.format(Settings.getAnonFormat().replace("{message}", ""))
                .append(MessageUtils.formatWithPermissions(player, message));
        source.getServer().getPlayerList().broadcastSystemMessage(line, false);
        return 1;
    }

    private static int online(CommandSourceStack source) {
        var players = source.getServer().getPlayerList().getPlayers();
        int count = players.size();
        int max = source.getServer().getPlayerList().getMaxPlayers();

        // Group by LuckPerms primary group
        Map<String, List<String>> byGroup = new LinkedHashMap<>();
        for (ServerPlayer player : players) {
            String group = LuckPermsHook.getPrimaryGroup(player);
            if (group == null || group.isBlank()) group = "default";
            byGroup.computeIfAbsent(group, g -> new java.util.ArrayList<>()).add(player.getScoreboardName());
        }

        source.sendSuccess(() -> MessageUtils.prefixed("&7Online: &a" + count + "&7/&a" + max), false);
        for (var entry : byGroup.entrySet()) {
            source.sendSuccess(() -> MessageUtils.format("&6" + entry.getKey() + "&8: &f" + String.join("&7, &f", entry.getValue())), false);
        }
        return 1;
    }

    private static int symbols(CommandSourceStack source) {
        Map<String, String> emojis = Settings.getEmojis();
        if (emojis.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed("&7No symbols configured."), false);
            return 1;
        }
        source.sendSuccess(() -> MessageUtils.prefixed("&7Available symbols (type the code in chat):"), false);
        for (var entry : emojis.entrySet()) {
            source.sendSuccess(() -> MessageUtils.format("&8- &f" + entry.getKey() + " &7→ &f" + entry.getValue()), false);
        }
        return 1;
    }

    private static int openMenu(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(MessageUtils.prefixed("&cThis command can only be used by players."));
            return 0;
        }
        pl.makoto.essentials.util.ChatSettingsMenu.open(player);
        return 1;
    }

    private static String onOff(boolean value) {
        return value ? "&aon" : "&coff";
    }

    private static int toggleMentions(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerData data = DataManager.getPlayerData(player.getUUID());
        boolean now = data.toggleChatSetting("mentions", true);
        DataManager.savePlayerData(player.getUUID());
        source.sendSuccess(() -> MessageUtils.prefixed("&7Mention pings are now " + onOff(now) + "&7."), false);
        return 1;
    }

    private static int toggleMessages(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerData data = DataManager.getPlayerData(player.getUUID());
        boolean nowDisabled = !data.isMsgDisabled();
        data.setMsgDisabled(nowDisabled);
        DataManager.savePlayerData(player.getUUID());
        source.sendSuccess(() -> MessageUtils.prefixed("&7Private messages are now " + onOff(!nowDisabled) + "&7."), false);
        return 1;
    }

    private static int ignoreToggle(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        if (player.getUUID().equals(target.getUUID())) {
            source.sendFailure(MessageUtils.prefixed("&cYou cannot ignore yourself."));
            return 0;
        }
        PlayerData data = DataManager.getPlayerData(player.getUUID());
        boolean nowIgnored = data.toggleIgnore(target.getUUID());
        DataManager.savePlayerData(player.getUUID());
        source.sendSuccess(() -> MessageUtils.prefixed(nowIgnored
                ? "&7You are now ignoring &6" + target.getScoreboardName() + "&7 (chat and private messages)."
                : "&7You are no longer ignoring &6" + target.getScoreboardName() + "&7."), false);
        return 1;
    }

    private static int ignoreList(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        var ids = DataManager.getPlayerData(player.getUUID()).getIgnoredPlayerIds();
        if (ids.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.prefixed("&7You are not ignoring anyone."), false);
            return 1;
        }
        source.sendSuccess(() -> MessageUtils.prefixed("&7You are ignoring (&e" + ids.size() + "&7):"), false);
        for (String id : ids) {
            String name;
            try {
                java.util.UUID uuid = java.util.UUID.fromString(id);
                ServerPlayer online = source.getServer().getPlayerList().getPlayer(uuid);
                name = online != null ? online.getScoreboardName()
                        : source.getServer().getProfileCache().get(uuid).map(GameProfile::getName).orElse(id);
            } catch (IllegalArgumentException e) {
                name = id;
            }
            final String display = name;
            source.sendSuccess(() -> MessageUtils.format("&8- &f" + display), false);
        }
        return 1;
    }
}
