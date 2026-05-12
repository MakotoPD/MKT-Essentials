package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.util.ItemCleanerManager;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

public class ClearItemsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("clearitems")
            .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.clearitems", 2))
            .then(Commands.argument("radius", IntegerArgumentType.integer(1, 1000))
                .executes(context -> clearRadius(context.getSource(), IntegerArgumentType.getInteger(context, "radius"))))
            .executes(context -> clearAll(context.getSource()))
        );
    }

    private static int clearAll(CommandSourceStack source) {
        int count = ItemCleanerManager.clearAllItems(source.getServer());
        if (count == 0) {
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("items.clearitems-none")), false);
        } else {
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("items.clearitems-success", "count", String.valueOf(count))), true);
        }
        return count;
    }

    private static int clearRadius(CommandSourceStack source, int radius) {
        Vec3 pos = source.getPosition();
        ServerLevel level = source.getLevel();
        int count = ItemCleanerManager.clearItemsInRadius(level, pos.x, pos.y, pos.z, radius);
        if (count == 0) {
            source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("items.clearitems-none")), false);
        } else {
            source.sendSuccess(() -> MessageUtils.prefixed(
                I18n.get("items.clearitems-radius", "count", String.valueOf(count), "radius", String.valueOf(radius))
            ), true);
        }
        return count;
    }
}
