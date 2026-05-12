package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.util.Optional;

public class ShortcutCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Gamemode shortcuts
        registerGamemode(dispatcher, "gmc", GameType.CREATIVE);
        registerGamemode(dispatcher, "gms", GameType.SURVIVAL);
        registerGamemode(dispatcher, "gma", GameType.ADVENTURE);
        registerGamemode(dispatcher, "gmsp", GameType.SPECTATOR);

        // /gm <0-3> [player]
        dispatcher.register(Commands.literal("gm")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.gamemode", 2))
                .then(Commands.argument("mode", IntegerArgumentType.integer(0, 3))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> setGamemode(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player"),
                                        gamemodeFromInt(IntegerArgumentType.getInteger(ctx, "mode")))))
                        .executes(ctx -> setGamemode(ctx.getSource(),
                                ctx.getSource().getPlayerOrException(),
                                gamemodeFromInt(IntegerArgumentType.getInteger(ctx, "mode"))))));

        // /tp <player> — teleport to player
        dispatcher.register(Commands.literal("tp")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.tp", 2))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> tpTo(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));

        // /tphere <player> — teleport player to you
        dispatcher.register(Commands.literal("tphere")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.tp", 2))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> tpHere(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));

        // /tppos <x> <y> <z>
        dispatcher.register(Commands.literal("tppos")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.tp", 2))
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(ctx -> tpPos(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                IntegerArgumentType.getInteger(ctx, "y"),
                                                IntegerArgumentType.getInteger(ctx, "z")))))));

        // /i <item_id> [amount] (give item to self)
        dispatcher.register(Commands.literal("i")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.give", 2))
                .then(Commands.argument("item", StringArgumentType.word())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> giveItem(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "item"),
                                        IntegerArgumentType.getInteger(ctx, "amount"))))
                        .executes(ctx -> giveItem(ctx.getSource(),
                                StringArgumentType.getString(ctx, "item"), 1))));

        // /item alias for /i
        dispatcher.register(Commands.literal("item")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.give", 2))
                .then(Commands.argument("item", StringArgumentType.word())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> giveItem(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "item"),
                                        IntegerArgumentType.getInteger(ctx, "amount"))))
                        .executes(ctx -> giveItem(ctx.getSource(),
                                StringArgumentType.getString(ctx, "item"), 1))));

        // /more — max stack
        dispatcher.register(Commands.literal("more")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.more", 2))
                .executes(ctx -> more(ctx.getSource())));

        // /skull <player>
        dispatcher.register(Commands.literal("skull")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.skull", 2))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> skull(ctx.getSource(), StringArgumentType.getString(ctx, "player")))));

        // /near [radius]
        dispatcher.register(Commands.literal("near")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.near", 0))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 1000))
                        .executes(ctx -> near(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"))))
                .executes(ctx -> near(ctx.getSource(), 200)));

        // /seen <player>
        dispatcher.register(Commands.literal("seen")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.seen", 0))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> seen(ctx.getSource(), StringArgumentType.getString(ctx, "player")))));

        // /sudo <player> <command>
        dispatcher.register(Commands.literal("sudo")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.sudo", 3))
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> sudo(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target"),
                                        StringArgumentType.getString(ctx, "command"))))));
    }

    // ─── Gamemode ────────────────────────────────────────────────────────────────

    private static void registerGamemode(CommandDispatcher<CommandSourceStack> dispatcher, String name, GameType mode) {
        dispatcher.register(Commands.literal(name)
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.gamemode", 2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> setGamemode(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), mode)))
                .executes(ctx -> setGamemode(ctx.getSource(), ctx.getSource().getPlayerOrException(), mode)));
    }

    private static GameType gamemodeFromInt(int mode) {
        return switch (mode) {
            case 0 -> GameType.SURVIVAL;
            case 1 -> GameType.CREATIVE;
            case 2 -> GameType.ADVENTURE;
            case 3 -> GameType.SPECTATOR;
            default -> GameType.SURVIVAL;
        };
    }

    private static int setGamemode(CommandSourceStack source, ServerPlayer target, GameType mode) {
        if (target == null) return 0;
        target.setGameMode(mode);
        String modeName = mode.getName();
        if (source.getPlayer() != null && source.getPlayer().getUUID().equals(target.getUUID())) {
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.gamemode-self", "mode", modeName)), true);
        } else {
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.gamemode-other", "player", target.getScoreboardName(), "mode", modeName)), true);
        }
        return 1;
    }

    // ─── Teleport ────────────────────────────────────────────────────────────────

    private static int tpTo(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer player = source.getPlayer();
        if (player == null || target == null) return 0;

        // Save back location
        PlayerData data = DataManager.getPlayerData(player.getUUID());
        data.pushBackLocation(new PlayerData.SavedLocation(
                player.level().dimension().location().toString(),
                player.position(), player.getYRot(), player.getXRot()
        ));

        player.teleportTo(
                (ServerLevel) target.level(),
                target.getX(), target.getY(), target.getZ(),
                target.getYRot(), target.getXRot()
        );
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.tp-to", "player", target.getScoreboardName())), true);
        return 1;
    }

    private static int tpHere(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer player = source.getPlayer();
        if (player == null || target == null) return 0;

        // Save back location for target
        PlayerData data = DataManager.getPlayerData(target.getUUID());
        data.pushBackLocation(new PlayerData.SavedLocation(
                target.level().dimension().location().toString(),
                target.position(), target.getYRot(), target.getXRot()
        ));

        target.teleportTo(
                (ServerLevel) player.level(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()
        );
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.tp-here", "player", target.getScoreboardName())), true);
        return 1;
    }

    private static int tpPos(CommandSourceStack source, int x, int y, int z) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        // Save back location
        PlayerData data = DataManager.getPlayerData(player.getUUID());
        data.pushBackLocation(new PlayerData.SavedLocation(
                player.level().dimension().location().toString(),
                player.position(), player.getYRot(), player.getXRot()
        ));

        player.teleportTo(
                (ServerLevel) player.level(),
                x + 0.5, y, z + 0.5,
                player.getYRot(), player.getXRot()
        );
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.tp-pos", "x", String.valueOf(x), "y", String.valueOf(y), "z", String.valueOf(z))), true);
        return 1;
    }

    // ─── Give Item ───────────────────────────────────────────────────────────────

    private static int giveItem(CommandSourceStack source, String itemId, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        // Resolve item from registry
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) {
            // Try with minecraft: prefix
            loc = ResourceLocation.tryParse("minecraft:" + itemId);
        }

        if (loc == null) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("shortcuts.invalid-item", "item", itemId)));
            return 0;
        }

        Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(loc);
        if (itemOpt.isEmpty() || itemOpt.get() == Items.AIR) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("shortcuts.item-not-found", "item", itemId)));
            return 0;
        }

        Item item = itemOpt.get();
        ItemStack stack = new ItemStack(item, amount);

        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }

        String finalItemId = loc.toString();
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.gave-item", "amount", String.valueOf(amount), "item", finalItemId)), true);
        return 1;
    }

    // ─── More ────────────────────────────────────────────────────────────────────

    private static int more(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ItemStack hand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (hand.isEmpty()) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("shortcuts.must-hold-item")));
            return 0;
        }

        int maxStack = hand.getMaxStackSize();
        hand.setCount(maxStack);
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.more-success", "amount", String.valueOf(maxStack))), true);
        return 1;
    }

    // ─── Skull ───────────────────────────────────────────────────────────────────

    private static int skull(CommandSourceStack source, String playerName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
        skull.set(DataComponents.PROFILE, new ResolvableProfile(Optional.of(playerName), Optional.empty(), new com.mojang.authlib.properties.PropertyMap()));

        if (!player.getInventory().add(skull)) {
            player.drop(skull, false);
        }

        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.skull-given", "player", playerName)), true);
        return 1;
    }

    // ─── Near ────────────────────────────────────────────────────────────────────

    private static int near(CommandSourceStack source, int radius) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        Vec3 pos = player.position();
        double radiusSq = (double) radius * radius;

        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) {
            if (other.getUUID().equals(player.getUUID())) continue;
            if (!other.level().dimension().equals(player.level().dimension())) continue;

            double distSq = other.position().distanceToSqr(pos);
            if (distSq <= radiusSq) {
                int dist = (int) Math.sqrt(distSq);
                if (count > 0) sb.append("&7, ");
                sb.append("&6").append(other.getScoreboardName()).append(" &8(&7").append(dist).append("m&8)");
                count++;
            }
        }

        if (count == 0) {
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.near-none", "radius", String.valueOf(radius))), false);
        } else {
            int finalCount = count;
            String list = sb.toString();
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.near-found", "count", String.valueOf(finalCount), "list", list)), false);
        }
        return 1;
    }

    // ─── Seen ────────────────────────────────────────────────────────────────────

    private static int seen(CommandSourceStack source, String playerName) {
        // Check if player is currently online
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (online != null) {
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.seen-online", "player", playerName)), false);
            return 1;
        }

        // Check profile cache for last known info
        Optional<com.mojang.authlib.GameProfile> profile = source.getServer().getProfileCache().get(playerName);
        if (profile.isEmpty()) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("shortcuts.seen-never", "player", playerName)));
            return 0;
        }

        // Check player data file for last save time
        java.util.UUID uuid = profile.get().getId();
        java.nio.file.Path playerFile = source.getServer().getWorldPath(
                net.minecraft.world.level.storage.LevelResource.ROOT
        ).resolve("mktessentials").resolve("players").resolve(uuid.toString() + ".json");

        if (java.nio.file.Files.exists(playerFile)) {
            try {
                java.time.Instant lastModified = java.nio.file.Files.getLastModifiedTime(playerFile).toInstant();
                java.time.Duration ago = java.time.Duration.between(lastModified, java.time.Instant.now());
                String timeAgo = formatDuration(ago);
                source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.seen-ago", "player", playerName, "time", timeAgo)), false);
            } catch (java.io.IOException e) {
                source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.seen-unknown", "player", playerName)), false);
            }
        } else {
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.seen-unknown", "player", playerName)), false);
        }
        return 1;
    }

    private static String formatDuration(java.time.Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return Math.max(minutes, 1) + "m";
        }
    }

    // ─── Sudo ────────────────────────────────────────────────────────────────────

    private static int sudo(CommandSourceStack source, ServerPlayer target, String command) {
        if (target == null) return 0;

        // Strip leading slash if present
        String cmd = command.startsWith("/") ? command.substring(1) : command;

        // Execute command as the target player
        CommandSourceStack targetSource = target.createCommandSourceStack();
        source.getServer().getCommands().performPrefixedCommand(targetSource, cmd);

        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("shortcuts.sudo-executed", "player", target.getScoreboardName(), "command", cmd)), true);
        return 1;
    }
}
