package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.Config;
import pl.makoto.essentials.util.AdminManager;
import pl.makoto.essentials.util.PlayerListener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AdminCommands {
    private static final Set<UUID> godMode = new HashSet<>();

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
                .then(Commands.argument("speed", IntegerArgumentType.integer(0, 10))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> speed(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "speed"))))
                        .executes(context -> speed(context.getSource(), context.getSource().getPlayer(), IntegerArgumentType.getInteger(context, "speed")))));
    }

    private static int heal(CommandSourceStack source, ServerPlayer target) {
        if (target == null) return 0;
        target.setHealth(target.getMaxHealth());
        target.getFoodData().setFoodLevel(20);
        target.clearFire();
        source.sendSuccess(() -> MessageUtils.prefixed("&aHealed &6" + target.getScoreboardName() + "&a."), true);
        return 1;
    }

    private static int feed(CommandSourceStack source, ServerPlayer target) {
        if (target == null) return 0;
        target.getFoodData().setFoodLevel(20);
        source.sendSuccess(() -> MessageUtils.prefixed("&aFed &6" + target.getScoreboardName() + "&a."), true);
        return 1;
    }

    private static int fly(CommandSourceStack source, ServerPlayer target) {
        if (target == null) return 0;
        boolean canFly = !target.getAbilities().mayfly;
        target.getAbilities().mayfly = canFly;
        if (!canFly) target.getAbilities().flying = false;
        target.onUpdateAbilities();
        source.sendSuccess(() -> MessageUtils.prefixed("&7Flight " + (canFly ? "&aenabled" : "&cdisabled") + " &7for &6" + target.getScoreboardName() + "&7."), true);
        return 1;
    }

    private static int god(CommandSourceStack source, ServerPlayer target) {
        if (target == null) return 0;
        UUID uuid = target.getUUID();
        if (godMode.contains(uuid)) {
            godMode.remove(uuid);
            target.setInvulnerable(false);
            source.sendSuccess(() -> MessageUtils.prefixed("&7God mode &cdisabled &7for &6" + target.getScoreboardName() + "&7."), true);
        } else {
            godMode.add(uuid);
            target.setInvulnerable(true);
            source.sendSuccess(() -> MessageUtils.prefixed("&7God mode &aenabled &7for &6" + target.getScoreboardName() + "&7."), true);
        }
        return 1;
    }

    private static int vanish(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        boolean vanishedState = AdminManager.toggleVanish(player.getUUID());
        
        if (vanishedState) {
            source.sendSuccess(() -> MessageUtils.prefixed("&7Vanish &aenabled&7. You are now hidden."), true);
            if (Config.VANISH_FAKE_MESSAGES.get()) {
                player.getServer().getPlayerList().broadcastSystemMessage(MessageUtils.format(player, Config.QUIT_MESSAGE.get()), false);
            }
        } else {
            source.sendSuccess(() -> MessageUtils.prefixed("&7Vanish &cdisabled&7. You are now visible."), true);
            if (Config.VANISH_FAKE_MESSAGES.get()) {
                player.getServer().getPlayerList().broadcastSystemMessage(MessageUtils.format(player, Config.JOIN_MESSAGE.get()), false);
            }
        }
        return 1;
    }

    private static int clearinv(CommandSourceStack source, ServerPlayer target) {
        if (target == null) return 0;
        target.getInventory().clearContent();
        source.sendSuccess(() -> MessageUtils.prefixed("&7Cleared inventory of &6" + target.getScoreboardName() + "&7."), true);
        return 1;
    }

    private static int speed(CommandSourceStack source, ServerPlayer target, int speedLevel) {
        if (target == null) return 0;

        // Set only flying speed: scale 0-10, default is 1 (0.05f)
        float finalSpeed = (float) speedLevel * 0.05f;
        target.getAbilities().setFlyingSpeed(finalSpeed);

        target.onUpdateAbilities();
        source.sendSuccess(() -> MessageUtils.prefixed("&7Flying speed set to &6" + speedLevel + " &7for &6" + target.getScoreboardName() + "&7."), true);
        return 1;
    }
    
    public static boolean isGod(UUID uuid) { return godMode.contains(uuid); }
    public static boolean isVanished(UUID uuid) { return AdminManager.isVanished(uuid); }
}
