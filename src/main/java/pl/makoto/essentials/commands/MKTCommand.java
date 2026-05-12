package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import pl.makoto.essentials.config.ConfigManager;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

public class MKTCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mkt")
            .executes(context -> help(context.getSource()))
            .then(Commands.literal("help")
                .executes(context -> help(context.getSource())))
            .then(Commands.literal("permissions")
                .requires(s -> Permissions.hasPermission(s, "mktessentials.admin.permissions", 2))
                .executes(context -> listPermissions(context.getSource())))
            .then(Commands.literal("reload")
                .requires(s -> Permissions.hasPermission(s, "mktessentials.admin.reload", 3))
                .executes(context -> reload(context.getSource())))
        );

        dispatcher.register(Commands.literal("essentials")
            .executes(context -> help(context.getSource())));
    }

    private static int help(CommandSourceStack source) {
        boolean isAdmin = Permissions.hasPermission(source, "mktessentials.admin.heal", 2);
        boolean isMod = Permissions.hasPermission(source, "mktessentials.moderation.kick", 2);

        source.sendSuccess(() -> MessageUtils.format(""), false);
        source.sendSuccess(() -> MessageUtils.format("&8&m━━━━━━━━━━━━&r &6&lMKT Essentials &8&m━━━━━━━━━━━━"), false);
        source.sendSuccess(() -> MessageUtils.format(""), false);

        // Teleportation — always visible
        source.sendSuccess(() -> MessageUtils.format(" &e⬡ &6Teleportation"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/home, /sethome, /delhome, /homes"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/warp, /setwarp, /delwarp, /warps"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/spawn, /back, /rtp, /top"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/tpa, /tpahere, /tpaccept, /tpdeny"), false);
        source.sendSuccess(() -> MessageUtils.format(""), false);

        // Communication — always visible
        source.sendSuccess(() -> MessageUtils.format(" &e⬡ &6Communication"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/msg <player> <message>, /reply <message>"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/nick <nickname>, /recording, /streaming"), false);
        source.sendSuccess(() -> MessageUtils.format(""), false);

        // Utility — always visible
        source.sendSuccess(() -> MessageUtils.format(" &e⬡ &6Utility"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/kit <name>, /hat, /ping, /near, /seen"), false);
        if (isAdmin) {
            source.sendSuccess(() -> MessageUtils.format("   &7/repair, /enchant <enchantment> <level>"), false);
        }
        source.sendSuccess(() -> MessageUtils.format(""), false);

        // Admin — only for admins
        if (isAdmin) {
            source.sendSuccess(() -> MessageUtils.format(" &c⬡ &4Admin"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/heal, /feed, /fly, /god, /vanish"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/speed fly|walk <0-10>, /clearinv"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/tpall, /invsee <player>, /enderchest <player>"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/invbackup save|list|restore|delete"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/day, /night, /sun, /rain"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/gm, /gmc, /gms, /gma, /gmsp, /tp, /tphere, /tppos"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/i <item> [amount], /more, /skull, /sudo"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/clearitems [radius]"), false);
            source.sendSuccess(() -> MessageUtils.format(""), false);
        }

        // Moderation — only for mods+
        if (isMod) {
            source.sendSuccess(() -> MessageUtils.format(" &c⬡ &4Moderation"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/kick <player> [reason]"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/ban <player> [reason], /tempban <player> <duration> [reason]"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/unban <player>"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/mute <player> [duration], /unmute <player>"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/shadowban <player> [reason], /unshadowban <player>"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/shadowbanlist"), false);
            source.sendSuccess(() -> MessageUtils.format(""), false);
        }

        // Management — only for admins
        if (isAdmin) {
            source.sendSuccess(() -> MessageUtils.format(" &c⬡ &4Management"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/mkt reload &8- &7Reload configuration"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/mkt permissions &8- &7List all permission nodes"), false);
            source.sendSuccess(() -> MessageUtils.format(""), false);
        }

        source.sendSuccess(() -> MessageUtils.format("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        return 1;
    }

    private static int listPermissions(CommandSourceStack source) {
        source.sendSuccess(() -> MessageUtils.format("&8&m       &r &6MKT Essentials Permissions &8&m       "), false);
        source.sendSuccess(() -> MessageUtils.format("&7Admin nodes:"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.kits"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.mute"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.vanish"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.speed"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.tpall"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.broadcast"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.weather"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.time"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.heal"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.feed"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.fly"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.god"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.clearinv"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.nick"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.gamemode"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.tp"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.give"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.more"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.skull"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.sudo"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.clearitems"), false);
        
        source.sendSuccess(() -> MessageUtils.format("&7Kit nodes:"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.kit.<name>"), false);
        
        source.sendSuccess(() -> MessageUtils.format("&7Player nodes:"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.home"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.sethome"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.delhome"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.listhomes"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.spawn"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.back"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.rtp"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.tpa"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.tpahere"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.warp"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.listwarps"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.nick"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.recording"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.streaming"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.hat"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.ping"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.near"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.seen"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.kickme"), false);
        source.sendSuccess(() -> MessageUtils.format("&8&m                                           "), false);
        
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        try {
            boolean success = ConfigManager.reload();
            if (success) {
                source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("general.reload-success")), true);
            } else {
                source.sendFailure(MessageUtils.prefixed(I18n.get("general.reload-failed")));
                return 0;
            }
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to reload config", e);
            source.sendFailure(MessageUtils.prefixed(I18n.get("general.reload-failed")));
            return 0;
        }
        return 1;
    }
}
