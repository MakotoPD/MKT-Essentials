package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

public class ExpCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("exp")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.exp", 2))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("levels", IntegerArgumentType.integer())
                                        .executes(context -> give(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "levels"))))))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("levels", IntegerArgumentType.integer(0))
                                        .executes(context -> set(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "levels")))))));
    }

    private static int give(CommandSourceStack source, ServerPlayer target, int levels) {
        target.giveExperienceLevels(levels);
        source.sendSuccess(() -> MessageUtils.prefixed("&7" + (levels >= 0 ? "Gave &6" + levels : "Took &6" + (-levels))
                + " &7level" + (Math.abs(levels) == 1 ? "" : "s") + (levels >= 0 ? " to" : " from")
                + " &6" + target.getScoreboardName() + "&7. Now: &f" + target.experienceLevel), true);
        return 1;
    }

    private static int set(CommandSourceStack source, ServerPlayer target, int levels) {
        target.setExperienceLevels(levels);
        target.setExperiencePoints(0);
        source.sendSuccess(() -> MessageUtils.prefixed("&7Set experience of &6" + target.getScoreboardName()
                + " &7to &6" + levels + " &7level" + (levels == 1 ? "" : "s") + "."), true);
        return 1;
    }
}
