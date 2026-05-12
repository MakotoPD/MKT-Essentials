package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.config.I18n;

public class RepairCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("repair")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.utility.repair", 2))
                .executes(ctx -> repair(ctx.getSource())));
    }

    private static int repair(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ItemStack item = player.getMainHandItem();
        if (item.isEmpty()) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("utility.repair-empty-hand")));
            return 0;
        }
        if (!item.isDamageableItem()) {
            source.sendFailure(MessageUtils.prefixed(I18n.get("utility.repair-not-damageable")));
            return 0;
        }

        item.setDamageValue(0);
        source.sendSuccess(() -> MessageUtils.prefixed(I18n.get("utility.repair-success")), true);
        return 1;
    }
}
