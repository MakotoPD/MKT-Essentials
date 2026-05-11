package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

public class MKTCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mkt")
            .then(Commands.literal("permissions")
                .requires(s -> Permissions.hasPermission(s, "mktessentials.admin.permissions", 2))
                .executes(context -> listPermissions(context.getSource())))
        );
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
}
