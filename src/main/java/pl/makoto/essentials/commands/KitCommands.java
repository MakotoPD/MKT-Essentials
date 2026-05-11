package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.KitData;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class KitCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kit")
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests((context, builder) -> {
                    DataManager.getAllKits().forEach(k -> builder.suggest(k.getName()));
                    return builder.buildFuture();
                })
                .executes(context -> claimKit(context.getSource(), StringArgumentType.getString(context, "name"))))
        );

        dispatcher.register(Commands.literal("kits")
            .executes(context -> listKits(context.getSource()))
        );

        dispatcher.register(Commands.literal("createkit")
            .requires(s -> Permissions.hasPermission(s, "mktessentials.admin.kits", 2))
            .then(Commands.argument("name", StringArgumentType.word())
                .then(Commands.argument("cooldown", LongArgumentType.longArg(0))
                    .executes(context -> createKit(context.getSource(), StringArgumentType.getString(context, "name"), LongArgumentType.getLong(context, "cooldown")))))
        );

        dispatcher.register(Commands.literal("deletekit")
            .requires(s -> Permissions.hasPermission(s, "mktessentials.admin.kits", 2))
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests((context, builder) -> {
                    DataManager.getAllKits().forEach(k -> builder.suggest(k.getName()));
                    return builder.buildFuture();
                })
                .executes(context -> deleteKit(context.getSource(), StringArgumentType.getString(context, "name"))))
        );
    }

    private static int claimKit(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        KitData kit = DataManager.getKit(name);
        if (kit == null) {
            player.sendSystemMessage(MessageUtils.format("&cKit '" + name + "' does not exist."));
            return 0;
        }

        // Permission check
        if (!Permissions.hasPermission(player, "mktessentials.kit." + name.toLowerCase(), 0)) {
            player.sendSystemMessage(MessageUtils.format("&cYou don't have permission to use this kit."));
            return 0;
        }

        PlayerData data = DataManager.getPlayerData(player.getUUID());
        long lastUsed = data.getKitCooldowns().getOrDefault(name.toLowerCase(), 0L);
        long now = System.currentTimeMillis();

        if (lastUsed > now) {
            long remaining = (lastUsed - now) / 1000;
            player.sendSystemMessage(MessageUtils.format("&cYou must wait &6" + formatTime(remaining) + " &cbefore using this kit again."));
            return 0;
        }

        // Give items
        List<ItemStack> items = kit.restoreItems(player.registryAccess());
        for (ItemStack item : items) {
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
        }

        // Set cooldown
        if (kit.getCooldownSeconds() > 0) {
            data.setKitCooldown(name.toLowerCase(), now + (kit.getCooldownSeconds() * 1000));
            DataManager.savePlayerData(player.getUUID());
        }

        player.sendSystemMessage(MessageUtils.format("&aYou have claimed kit &6" + kit.getName() + "&a!"));
        return 1;
    }

    private static int listKits(CommandSourceStack source) {
        Collection<KitData> kits = DataManager.getAllKits();
        if (kits.isEmpty()) {
            source.sendSuccess(() -> MessageUtils.format("&cNo kits available."), false);
            return 0;
        }

        String kitList = kits.stream()
            .map(KitData::getName)
            .collect(Collectors.joining("&r, &6"));

        source.sendSuccess(() -> MessageUtils.format("&aAvailable kits: &6" + kitList), false);
        return 1;
    }

    private static int createKit(CommandSourceStack source, String name, long cooldown) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        KitData kit = new KitData(name, cooldown);
        kit.setItems(player.getInventory().items, player.registryAccess()); // Only main inventory items
        
        DataManager.addKit(kit);
        player.sendSystemMessage(MessageUtils.format("&aKit &6" + name + " &ahas been created with &6" + cooldown + "s &acooldown."));
        return 1;
    }

    private static int deleteKit(CommandSourceStack source, String name) {
        DataManager.deleteKit(name);
        source.sendSuccess(() -> MessageUtils.format("&aKit &6" + name + " &ahas been deleted."), false);
        return 1;
    }

    private static String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }
}
