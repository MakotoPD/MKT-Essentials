package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import pl.makoto.essentials.util.MessageUtils;

public class HelpCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mkt")
                .executes(context -> help(context.getSource()))
                .then(Commands.literal("help")
                        .executes(context -> help(context.getSource()))));
        
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
}
