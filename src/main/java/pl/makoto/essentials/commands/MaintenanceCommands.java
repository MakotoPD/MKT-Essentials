package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.util.MaintenanceManager;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

/**
 * /maintenance on|off — toggles maintenance mode (inspired by FlectonePulse maintenance).
 * Enabling it kicks every online player that lacks {@code mktessentials.maintenance.bypass}.
 */
public class MaintenanceCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maintenance")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.admin.maintenance", 3))
                .then(Commands.literal("on").executes(context -> set(context.getSource(), true)))
                .then(Commands.literal("off").executes(context -> set(context.getSource(), false)))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> MessageUtils.prefixed(
                            "&7Maintenance is currently " + (MaintenanceManager.isActive() ? "&cON" : "&aOFF") + "&7."), false);
                    return 1;
                }));
    }

    private static int set(CommandSourceStack source, boolean on) {
        MaintenanceManager.setActive(on);

        if (on) {
            var kick = MessageUtils.format(Settings.getMaintenanceKick());
            int kicked = 0;
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                if (Permissions.hasPermission(player, "mktessentials.maintenance.bypass", 2)) continue;
                player.connection.disconnect(kick);
                kicked++;
            }
            final int total = kicked;
            source.sendSuccess(() -> MessageUtils.prefixed("&cMaintenance enabled&7. Kicked &6" + total + "&7 player(s)."), true);
        } else {
            source.sendSuccess(() -> MessageUtils.prefixed("&aMaintenance disabled&7."), true);
        }
        return 1;
    }
}
