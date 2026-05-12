package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import pl.makoto.essentials.Config;
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
        source.sendSuccess(() -> MessageUtils.format("&8&m       &r &6&lMKT Essentials &8&m       "), false);
        source.sendSuccess(() -> MessageUtils.format("&e/home, /sethome, /delhome, /listhomes &7- Home system"), false);
        source.sendSuccess(() -> MessageUtils.format("&e/warp, /setwarp, /delwarp, /listwarps &7- Warp system"), false);
        source.sendSuccess(() -> MessageUtils.format("&e/tpa, /tpahere, /tpaccept, /tpdeny &7- Teleport requests"), false);
        source.sendSuccess(() -> MessageUtils.format("&e/spawn, /back, /rtp, /top &7- General teleportation"), false);
        source.sendSuccess(() -> MessageUtils.format("&e/msg, /reply, /socialspy, /broadcast &7- Messaging"), false);
        source.sendSuccess(() -> MessageUtils.format("&e/heal, /feed, /fly, /god, /vanish, /speed &7- Admin tools"), false);
        source.sendSuccess(() -> MessageUtils.format("&e/mute, /unmute, /clearinv &7- Moderation"), false);
        source.sendSuccess(() -> MessageUtils.format("&e/nick, /hat, /ping, /kickme &7- Misc"), false);
        source.sendSuccess(() -> MessageUtils.format("&e/recording, /streaming &7- Status"), false);
        source.sendSuccess(() -> MessageUtils.format("&e/day, /night, /sun, /rain &7- Time & Weather"), false);
        source.sendSuccess(() -> MessageUtils.format("&8&m                             "), false);
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
        source.sendSuccess(() -> MessageUtils.format(" &e- &fmktessentials.command.kickme"), false);
        source.sendSuccess(() -> MessageUtils.format("&8&m                                           "), false);
        
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        try {
            // Force NeoForge config spec to re-read values from the file
            Config.SPEC.afterReload();
            source.sendSuccess(() -> MessageUtils.prefixed("&aConfiguration reloaded successfully!"), true);
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to reload config", e);
            source.sendFailure(MessageUtils.prefixed("&cFailed to reload configuration. Check console for details."));
            return 0;
        }
        return 1;
    }
}
