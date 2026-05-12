package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.data.InventoryBackup;
import pl.makoto.essentials.util.BackupListMenu;
import pl.makoto.essentials.util.BackupManager;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

import java.util.UUID;

/**
 * Commands for the inventory backup system.
 *
 * /invbackup save <player> [note] — manual backup
 * /invbackup list <player> — opens GUI with backup list
 * /invbackup restore <player> <filename> — restores backup
 * /invbackup delete <player> <filename> — deletes backup
 *
 * Permission: mktessentials.admin.backup (OP 2)
 */
public class BackupCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("invbackup")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.backup", 2))
                .then(Commands.literal("save")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> save(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), null))
                                .then(Commands.argument("note", StringArgumentType.greedyString())
                                        .executes(ctx -> save(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "note"))))))
                .then(Commands.literal("list")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> list(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("restore")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("filename", StringArgumentType.string())
                                        .executes(ctx -> restore(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "filename"))))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("filename", StringArgumentType.string())
                                        .executes(ctx -> delete(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "filename")))))));
    }

    private static int save(CommandSourceStack source, ServerPlayer target, String note) {
        String filename = BackupManager.createBackup(target, "manual", note);
        if (filename != null) {
            source.sendSuccess(() -> MessageUtils.prefixed(
                    "&aCreated backup for &6" + target.getScoreboardName() + "&a: &7" + filename), true);
        } else {
            source.sendFailure(MessageUtils.prefixed("&cFailed to create backup for " + target.getScoreboardName()));
        }
        return filename != null ? 1 : 0;
    }

    private static int list(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer viewer = source.getPlayer();
        if (viewer == null) {
            source.sendFailure(MessageUtils.prefixed("&cThis command can only be used by players."));
            return 0;
        }

        BackupListMenu.open(viewer, target.getUUID(), target.getScoreboardName());
        return 1;
    }

    private static int restore(CommandSourceStack source, ServerPlayer target, String filename) {
        UUID uuid = target.getUUID();
        InventoryBackup backup = BackupManager.loadBackup(uuid, filename);
        if (backup == null) {
            source.sendFailure(MessageUtils.prefixed("&cBackup not found: " + filename));
            return 0;
        }

        boolean success = BackupManager.restoreBackup(target, backup);
        if (success) {
            source.sendSuccess(() -> MessageUtils.prefixed(
                    "&aRestored backup &7" + filename + " &ato &6" + target.getScoreboardName() + "&a."), true);
            target.sendSystemMessage(MessageUtils.prefixed(
                    "&aYour inventory has been restored from a backup."));
        } else {
            source.sendFailure(MessageUtils.prefixed("&cFailed to restore backup."));
        }
        return success ? 1 : 0;
    }

    private static int delete(CommandSourceStack source, ServerPlayer target, String filename) {
        boolean deleted = BackupManager.deleteBackup(target.getUUID(), filename);
        if (deleted) {
            source.sendSuccess(() -> MessageUtils.prefixed(
                    "&aDeleted backup &7" + filename + " &afor &6" + target.getScoreboardName() + "&a."), true);
        } else {
            source.sendFailure(MessageUtils.prefixed("&cBackup not found or could not be deleted: " + filename));
        }
        return deleted ? 1 : 0;
    }
}
