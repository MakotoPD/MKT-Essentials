package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.PlayerListener;
import pl.makoto.essentials.util.Permissions;

public class MiscCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kickme")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.kickme", 0))
                .executes(context -> kickme(context.getSource())));

        dispatcher.register(Commands.literal("hat")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.hat", 0))
                .executes(context -> hat(context.getSource())));

        dispatcher.register(Commands.literal("ping")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.ping", 0))
                .executes(context -> ping(context.getSource())));

        dispatcher.register(nickCommand("nick"));
        dispatcher.register(nickCommand("nickname"));

        dispatcher.register(Commands.literal("recording")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.recording", 0))
                .executes(context -> recording(context.getSource())));

        dispatcher.register(Commands.literal("streaming")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.streaming", 0))
                .executes(context -> streaming(context.getSource())));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> nickCommand(String name) {
        return Commands.literal(name)
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.nick", 0))
                .then(Commands.literal("reset")
                        .executes(context -> nick(context.getSource(), context.getSource().getPlayer(), null)))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.nick", 2))
                        .then(Commands.literal("reset")
                                .executes(context -> nick(context.getSource(), EntityArgument.getPlayer(context, "player"), null)))
                        .then(Commands.argument("nickname", StringArgumentType.greedyString())
                                .executes(context -> nick(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "nickname")))))
                .then(Commands.argument("nickname", StringArgumentType.greedyString())
                        .executes(context -> nick(context.getSource(), context.getSource().getPlayer(), StringArgumentType.getString(context, "nickname"))));
    }

    private static int kickme(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        player.connection.disconnect(Component.literal("You kicked yourself!"));
        return 1;
    }

    private static int hat(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ItemStack hand = player.getMainHandItem();
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);

        if (hand.isEmpty() && head.isEmpty()) return 0;

        player.setItemSlot(EquipmentSlot.HEAD, hand.copy());
        player.setItemInHand(player.getUsedItemHand(), head.copy());
        
        source.sendSuccess(() -> MessageUtils.prefixed("&aEnjoy your new hat!"), true);
        return 1;
    }

    private static int recording(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerData data = DataManager.getPlayerData(player.getUUID());
        boolean newState = !data.isRecording();
        
        data.setRecording(newState);
        if (newState) data.setStreaming(false);
        
        DataManager.savePlayerData(player.getUUID());
        PlayerListener.refreshNickname(player);

        source.sendSuccess(() -> MessageUtils.format("&8[&6MKT&8] &7Recording mode " + (newState ? "&aenabled" : "&cdisabled") + "&7."), true);
        return 1;
    }

    private static int nick(CommandSourceStack source, ServerPlayer target, String nickname) {
        if (target == null) return 0;

        PlayerData data = DataManager.getPlayerData(target.getUUID());
        String normalized = normalizeNickname(nickname);
        data.setNickname(normalized);
        DataManager.savePlayerData(target.getUUID());
        PlayerListener.refreshNickname(target);

        if (normalized == null) {
            source.sendSuccess(() -> MessageUtils.prefixed("&7Nickname reset for &6" + target.getScoreboardName() + "&7."), true);
        } else if (source.getPlayer() != null && source.getPlayer().getUUID().equals(target.getUUID())) {
            source.sendSuccess(() -> MessageUtils.prefixed("&7Your nickname is now &r" + normalized + "&7."), true);
        } else {
            source.sendSuccess(() -> MessageUtils.prefixed("&7Nickname for &6" + target.getScoreboardName() + " &7is now &r" + normalized + "&7."), true);
        }
        return 1;
    }

    private static int streaming(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerData data = DataManager.getPlayerData(player.getUUID());
        boolean newState = !data.isStreaming();
        
        data.setStreaming(newState);
        if (newState) data.setRecording(false);

        DataManager.savePlayerData(player.getUUID());
        PlayerListener.refreshNickname(player);

        source.sendSuccess(() -> MessageUtils.format("&8[&6MKT&8] &7Streaming mode " + (newState ? "&aenabled" : "&cdisabled") + "&7."), true);
        return 1;
    }

    private static int ping(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        int ping = player.connection.latency();
        String color = ping < 100 ? "&a" : (ping < 200 ? "&e" : "&c");
        source.sendSuccess(() -> MessageUtils.prefixed("&7Your ping: " + color + ping + "ms"), false);
        return 1;
    }

    private static String normalizeNickname(String nickname) {
        if (nickname == null) return null;

        String trimmed = nickname.trim();
        if (trimmed.isEmpty()
                || trimmed.equalsIgnoreCase("reset")
                || trimmed.equalsIgnoreCase("clear")
                || trimmed.equalsIgnoreCase("off")) {
            return null;
        }

        return trimmed.length() > 48 ? trimmed.substring(0, 48) : trimmed;
    }
}
