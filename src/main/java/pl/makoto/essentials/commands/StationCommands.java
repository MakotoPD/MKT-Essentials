package pl.makoto.essentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import pl.makoto.essentials.util.MessageUtils;
import pl.makoto.essentials.util.Permissions;

/**
 * Virtual workstation commands: /trash, /workbench (/craft), /anvil, /grindstone,
 * /stonecutter, /smithing. Each opens the vanilla menu with the block-proximity
 * check disabled.
 */
public class StationCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trash")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.trash", 0))
                .executes(context -> trash(context.getSource())));

        dispatcher.register(Commands.literal("workbench")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.workbench", 0))
                .executes(context -> workbench(context.getSource())));
        dispatcher.register(Commands.literal("craft")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.workbench", 0))
                .executes(context -> workbench(context.getSource())));

        dispatcher.register(Commands.literal("anvil")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.anvil", 0))
                .executes(context -> anvil(context.getSource())));

        dispatcher.register(Commands.literal("grindstone")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.grindstone", 0))
                .executes(context -> grindstone(context.getSource())));

        dispatcher.register(Commands.literal("stonecutter")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.stonecutter", 0))
                .executes(context -> stonecutter(context.getSource())));

        dispatcher.register(Commands.literal("smithing")
                .requires(source -> Permissions.hasPermission(source, "mktessentials.command.smithing", 0))
                .executes(context -> smithing(context.getSource())));
    }

    private static int trash(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        // Plain container that is simply discarded on close — items inside are destroyed
        SimpleContainer container = new SimpleContainer(36);
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x4, id, inv, container, 4),
                MessageUtils.format("&8Trash &7(items are destroyed on close)")
        ));
        return 1;
    }

    private static int workbench(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new CraftingMenu(id, inv, anywhere(p)) {
                    @Override
                    public boolean stillValid(Player pl) { return true; }
                },
                Component.translatable("container.crafting")
        ));
        return 1;
    }

    private static int anvil(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new AnvilMenu(id, inv, anywhere(p)) {
                    @Override
                    public boolean stillValid(Player pl) { return true; }
                },
                Component.translatable("container.repair")
        ));
        return 1;
    }

    private static int grindstone(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new GrindstoneMenu(id, inv, anywhere(p)) {
                    @Override
                    public boolean stillValid(Player pl) { return true; }
                },
                Component.translatable("container.grindstone_title")
        ));
        return 1;
    }

    private static int stonecutter(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new StonecutterMenu(id, inv, anywhere(p)) {
                    @Override
                    public boolean stillValid(Player pl) { return true; }
                },
                Component.translatable("container.stonecutter")
        ));
        return 1;
    }

    private static int smithing(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new SmithingMenu(id, inv, anywhere(p)) {
                    @Override
                    public boolean stillValid(Player pl) { return true; }
                },
                Component.translatable("container.upgrade")
        ));
        return 1;
    }

    private static ContainerLevelAccess anywhere(Player player) {
        return ContainerLevelAccess.create(player.level(), player.blockPosition());
    }
}
