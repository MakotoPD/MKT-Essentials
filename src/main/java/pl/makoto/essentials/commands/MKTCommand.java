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
        source.sendSuccess(() -> MessageUtils.format("   &7/home, /sethome, /delhome, /listhomes"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/warp, /setwarp, /delwarp, /warps, /listwarps"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/spawn, /back, /rtp, /top"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/tpa, /tpahere, /tpaccept, /tpdeny"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/tpacancel, /tptoggle"), false);
        source.sendSuccess(() -> MessageUtils.format(""), false);

        // Communication — always visible
        source.sendSuccess(() -> MessageUtils.format(" &e⬡ &6Communication"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/msg <player> <message>, /reply <message>"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/msgtoggle, /ignore <player>, /afk"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/helpop <message>, /report <player> <reason>"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/nick <nickname>, /recording, /streaming"), false);
        source.sendSuccess(() -> MessageUtils.format(""), false);

        // Utility — always visible
        source.sendSuccess(() -> MessageUtils.format(" &e⬡ &6Utility"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/kit <name>, /kits, /hat, /ping, /near, /seen"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/trash, /workbench (/craft), /anvil, /grindstone"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/stonecutter, /smithing, /kickme"), false);
        source.sendSuccess(() -> MessageUtils.format("   &7/playtime, /tps, /ptime, /pweather"), false);
        // Text commands are config-defined — list the first alias of each
        String textCmds = pl.makoto.essentials.config.Settings.getTextCommands().stream()
                .map(tc -> "/" + tc.aliases().get(0))
                .collect(java.util.stream.Collectors.joining(", "));
        if (!textCmds.isEmpty()) {
            final String line = "   &7" + textCmds;
            source.sendSuccess(() -> MessageUtils.format(line), false);
        }
        if (isAdmin) {
            source.sendSuccess(() -> MessageUtils.format("   &7/repair, /enchant <enchantment> <level>"), false);
        }
        source.sendSuccess(() -> MessageUtils.format(""), false);

        // Account — only when the auth system is enabled
        var authMode = pl.makoto.essentials.config.Settings.getAuthMode();
        if (authMode != pl.makoto.essentials.auth.AuthMode.DISABLED) {
            source.sendSuccess(() -> MessageUtils.format(" &e⬡ &6Account"), false);
            if (authMode.requiresPassword()) {
                source.sendSuccess(() -> MessageUtils.format("   &7/register <password> <confirm>, /login <password>"), false);
                source.sendSuccess(() -> MessageUtils.format("   &7/changepassword <old> <new> <confirm>"), false);
            }
            if (authMode.requiresLink() || authMode == pl.makoto.essentials.auth.AuthMode.OPTIONAL) {
                source.sendSuccess(() -> MessageUtils.format("   &7/link, /unlink"), false);
            }
            source.sendSuccess(() -> MessageUtils.format("   &7/discord"), false);
            if (isAdmin) {
                source.sendSuccess(() -> MessageUtils.format("   &7/auth reset|unlink|info <player>"), false);
            }
            source.sendSuccess(() -> MessageUtils.format(""), false);
        }

        // Admin — only for admins
        if (isAdmin) {
            source.sendSuccess(() -> MessageUtils.format(" &c⬡ &4Admin"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/heal, /feed, /fly, /god, /vanish"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/speed fly|walk <0-10>, /clearinv"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/tpall, /invsee <player>, /enderchest <player>"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/invbackup save|list|restore|delete"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/reports [page] &8(staff report log)"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/cmdqueue add|list|info|cancel &8(command queue)"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/itemstore create|edit|give|giverandom &8(saved items)"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/scheduler list|info|run|reload &8(automated commands)"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/day, /noon, /night, /midnight, /sun, /rain, /storm"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/gm, /gmc, /gms, /gma, /gmsp, /tp, /tphere, /tppos"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/i <item> [amount], /more, /skull, /sudo"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/clearitems [radius], /setspawn, /lag"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/whois <player>, /exp give|set <player> <levels>"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/createkit <name> <cooldown> [frominv], /deletekit <name>"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/socialspy, /broadcast <message>"), false);
            source.sendSuccess(() -> MessageUtils.format(""), false);
        }

        // Moderation — only for mods+
        if (isMod) {
            source.sendSuccess(() -> MessageUtils.format(" &c⬡ &4Moderation"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/kick <player> [reason]"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/ban <player> [reason], /tempban <player> <duration> [reason]"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/unban <player>, /banip <player|ip> [reason], /unbanip <ip>"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/mute <player> [duration], /unmute <player>"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/tempmute <player> <duration> [reason]"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/warn <player> [reason], /unwarn <player>"), false);
            source.sendSuccess(() -> MessageUtils.format("   &7/warns <player>, /history <player>"), false);
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
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.setspawn"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.whois"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.playtime"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.exp"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.lag"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.helpop &8(receive /helpop and /report)"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.queue.view &8(view the command queue)"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.queue.add &8(queue commands — equals console access)"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.queue.manage &8(cancel, retry, remove)"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.itemstore.view &8(view saved item categories)"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.itemstore.edit &8(create, edit, weights)"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.itemstore.give &8(hand items out)"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.scheduler.view &8(view schedulers)"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.scheduler.manage &8(run, enable, reload)"), false);
        source.sendSuccess(() -> MessageUtils.format(" &8   (grant &f...queue.* / ...itemstore.* / ...scheduler.* &8for all of a group)"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.msgbypass"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.admin.tptoggle.bypass"), false);

        source.sendSuccess(() -> MessageUtils.format("&7Moderation nodes:"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.moderation.kick"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.moderation.ban"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.moderation.tempban"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.moderation.unban"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.moderation.banip"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.moderation.warn"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.moderation.history"), false);

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
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.tptoggle"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.msgtoggle"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.ignore"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.afk"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.playtime"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.trash"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.workbench"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.anvil"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.grindstone"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.stonecutter"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.smithing"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.ptime"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.pweather"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.helpop"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.report"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.tps"), false);
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.text.<name>"), false);
        source.sendSuccess(() -> MessageUtils.format("&8&m                                           "), false);
        
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        try {
            boolean success = ConfigManager.reload();
            if (success) {
                pl.makoto.essentials.util.FaviconManager.reload();
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
