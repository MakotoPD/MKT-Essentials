package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.config.I18n;

public class KickCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kick")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.moderation.kick", 2))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> kick(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "reason"))))
                        .executes(ctx -> kick(
                                ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "player"),
                                "Kicked by an operator"))));
    }

    private static int kick(CommandSourceStack source, ServerPlayer target, String reason) {
        if (target == null) return 0;

        target.connection.disconnect(Component.literal("\u00a7c" + reason));
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("moderation.kicked", "player", target.getScoreboardName(), "reason", reason)), true);
        return 1;
    }
}
