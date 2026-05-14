package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.util.SnapshotInventoryMenu;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.util.AdminManager;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;

import java.util.UUID;

public class AdminCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("heal")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.heal", 2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> heal(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                .executes(context -> heal(context.getSource(), context.getSource().getPlayer())));

        dispatcher.register(Commands.literal("feed")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.feed", 2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> feed(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                .executes(context -> feed(context.getSource(), context.getSource().getPlayer())));

        dispatcher.register(Commands.literal("fly")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.fly", 2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> fly(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                .executes(context -> fly(context.getSource(), context.getSource().getPlayer())));

        dispatcher.register(Commands.literal("god")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.god", 2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> god(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                .executes(context -> god(context.getSource(), context.getSource().getPlayer())));

        dispatcher.register(Commands.literal("vanish")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.vanish", 2))
                .executes(context -> vanish(context.getSource())));

        dispatcher.register(Commands.literal("clearinv")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.clearinv", 2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> clearinv(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                .executes(context -> clearinv(context.getSource(), context.getSource().getPlayer())));

        dispatcher.register(Commands.literal("speed")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.speed", 2))
                .then(Commands.literal("fly")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 10))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> speedFly(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "value"))))
                                .executes(ctx -> speedFly(ctx.getSource(), ctx.getSource().getPlayer(), IntegerArgumentType.getInteger(ctx, "value")))))
                .then(Commands.literal("walk")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 10))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> speedWalk(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "value"))))
                                .executes(ctx -> speedWalk(ctx.getSource(), ctx.getSource().getPlayer(), IntegerArgumentType.getInteger(ctx, "value"))))));

        dispatcher.register(Commands.literal("tpall")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.tpall", 2))
                .executes(context -> tpall(context.getSource())));

        dispatcher.register(Commands.literal("invsee")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.invsee", 2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> invsee(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                .then(Commands.argument("offlinePlayer", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(context -> invseeOffline(context.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(context, "offlinePlayer")))));

        dispatcher.register(Commands.literal("enderchest")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.enderchest", 2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> enderchest(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                .then(Commands.argument("offlinePlayer", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(context -> enderchestOffline(context.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(context, "offlinePlayer")))));
    }

    private static int heal(CommandSourceStack source, ServerPlayer target) {
        if (target == null) return 0;
        target.setHealth(target.getMaxHealth());
        target.getFoodData().setFoodLevel(20);
        target.clearFire();
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("admin.healed", "player", target.getScoreboardName())), true);
        return 1;
    }

    private static int feed(CommandSourceStack source, ServerPlayer target) {
        if (target == null) return 0;
        target.getFoodData().setFoodLevel(20);
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("admin.fed", "player", target.getScoreboardName())), true);
        return 1;
    }

    private static int fly(CommandSourceStack source, ServerPlayer target) {
        if (target == null) return 0;
        UUID uuid = target.getUUID();
        PlayerData data = DataManager.getPlayerData(uuid);
        boolean wasFly = data.isFlyEnabled();
        data.setFlyEnabled(!wasFly);
        DataManager.savePlayerData(uuid);
        boolean canFly = data.isFlyEnabled();
        target.getAbilities().mayfly = canFly;
        if (!canFly) target.getAbilities().flying = false;
        target.onUpdateAbilities();
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get(canFly ? "admin.fly-enabled" : "admin.fly-disabled", "player", target.getScoreboardName())), true);
        return 1;
    }

    private static int god(CommandSourceStack source, ServerPlayer target) {
        if (target == null) return 0;
        UUID uuid = target.getUUID();
        PlayerData data = DataManager.getPlayerData(uuid);
        boolean wasGod = data.isGodMode();
        data.setGodMode(!wasGod);
        DataManager.savePlayerData(uuid);
        if (wasGod) {
            target.setInvulnerable(false);
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("admin.god-disabled", "player", target.getScoreboardName())), true);
        } else {
            target.setInvulnerable(true);
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("admin.god-enabled", "player", target.getScoreboardName())), true);
        }
        return 1;
    }

    private static int vanish(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        boolean vanishedState = AdminManager.toggleVanish(player.getUUID());
        
        if (vanishedState) {
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("admin.vanish-enabled")), true);
            if (Settings.isVanishFakeMessages()) {
                player.getServer().getPlayerList().broadcastSystemMessage(MessageUtils.format(player, Settings.getQuitMessage()), false);
            }
        } else {
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("admin.vanish-disabled")), true);
            if (Settings.isVanishFakeMessages()) {
                player.getServer().getPlayerList().broadcastSystemMessage(MessageUtils.format(player, Settings.getJoinMessage()), false);
            }
        }
        return 1;
    }

    private static int clearinv(CommandSourceStack source, ServerPlayer target) {
        if (target == null) return 0;
        target.getInventory().clearContent();
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("admin.clearinv", "player", target.getScoreboardName())), true);
        return 1;
    }

    private static int speedFly(CommandSourceStack source, ServerPlayer target, int value) {
        if (target == null) return 0;
        float speed = value * 0.05f;
        target.getAbilities().setFlyingSpeed(speed);
        target.onUpdateAbilities();
        PlayerData data = DataManager.getPlayerData(target.getUUID());
        data.setFlySpeed(speed);
        DataManager.savePlayerData(target.getUUID());
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("admin.speed-fly", "value", String.valueOf(value), "player", target.getScoreboardName())), true);
        if (!target.getAbilities().mayfly) {
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("admin.speed-fly-no-fly", "player", target.getScoreboardName())), false);
        }
        return 1;
    }

    private static int speedWalk(CommandSourceStack source, ServerPlayer target, int value) {
        if (target == null) return 0;
        float speed = value * 0.1f;
        target.getAbilities().setWalkingSpeed(speed);
        target.onUpdateAbilities();
        PlayerData data = DataManager.getPlayerData(target.getUUID());
        data.setWalkSpeed(speed);
        DataManager.savePlayerData(target.getUUID());
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("admin.speed-walk", "value", String.valueOf(value), "player", target.getScoreboardName())), true);
        return 1;
    }
    
    private static int tpall(CommandSourceStack source) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) return 0;

        int count = 0;
        for (ServerPlayer target : sender.getServer().getPlayerList().getPlayers()) {
            if (target.getUUID().equals(sender.getUUID())) continue;
            target.teleportTo(
                    (ServerLevel) sender.level(),
                    sender.getX(), sender.getY(), sender.getZ(),
                    sender.getYRot(), sender.getXRot()
            );
            count++;
        }
        int finalCount = count;
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("admin.tpall", "count", String.valueOf(finalCount))), true);
        return 1;
    }

    private static int invsee(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer viewer = source.getPlayer();
        if (viewer == null || target == null) return 0;
        SnapshotInventoryMenu.openInventory(viewer, target);
        return 1;
    }

    private static int enderchest(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer viewer = source.getPlayer();
        if (viewer == null || target == null) return 0;
        SnapshotInventoryMenu.openEnderChest(viewer, target);
        return 1;
    }

    private static int invseeOffline(CommandSourceStack source, String playerName) {
        ServerPlayer viewer = source.getPlayer();
        if (viewer == null) return 0;
        
        UUID uuid = DataManager.resolveOfflineUUID(playerName, source.getServer());
        if (uuid == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", playerName)));
            return 0;
        }
        
        SnapshotInventoryMenu.openOfflineInventory(viewer, uuid, playerName, source.getServer());
        return 1;
    }

    private static int enderchestOffline(CommandSourceStack source, String playerName) {
        ServerPlayer viewer = source.getPlayer();
        if (viewer == null) return 0;
        
        UUID uuid = DataManager.resolveOfflineUUID(playerName, source.getServer());
        if (uuid == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.player-not-found", "player", playerName)));
            return 0;
        }
        
        SnapshotInventoryMenu.openOfflineEnderChest(viewer, uuid, playerName, source.getServer());
        return 1;
    }

    public static boolean isGod(UUID uuid) { return DataManager.getPlayerData(uuid).isGodMode(); }
    public static boolean isVanished(UUID uuid) { return AdminManager.isVanished(uuid); }
}
