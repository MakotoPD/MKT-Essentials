package pl.makoto.essentials.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * A server-side container menu that displays a snapshot of another player's
 * inventory or ender chest. Changes made in the GUI are applied back to the
 * target player when the viewer closes the menu.
 */
public class SnapshotInventoryMenu extends ChestMenu {

    /**
     * Number of slots for inventory mode:
     * 36 main inventory + 4 armor + 1 offhand + 4 padding = 45 (5 rows of 9)
     */
    private static final int INVENTORY_SLOTS = 45;
    private static final int INVENTORY_ROWS = 5;

    /**
     * Number of slots for ender chest mode: 27 (3 rows of 9)
     */
    private static final int ENDER_CHEST_SLOTS = 27;
    private static final int ENDER_CHEST_ROWS = 3;

    private final SimpleContainer snapshotContainer;
    private final ServerPlayer target;
    private final boolean isEnderChest;

    private SnapshotInventoryMenu(int containerId, Inventory playerInventory,
                                  SimpleContainer container, ServerPlayer target,
                                  boolean isEnderChest, int rows) {
        super(isEnderChest ? MenuType.GENERIC_9x3 : MenuType.GENERIC_9x5,
                containerId, playerInventory, container, rows);
        this.snapshotContainer = container;
        this.target = target;
        this.isEnderChest = isEnderChest;
    }

    /**
     * Opens a snapshot of the target player's inventory for the viewer.
     * The viewer sees a 5-row chest GUI containing the target's items.
     * On close, items are copied back to the target.
     *
     * @param viewer the admin viewing the inventory
     * @param target the player whose inventory is being viewed
     */
    public static void openInventory(ServerPlayer viewer, ServerPlayer target) {
        SimpleContainer container = new SimpleContainer(INVENTORY_SLOTS);

        // Copy main inventory (slots 0-35)
        for (int i = 0; i < 36; i++) {
            container.setItem(i, target.getInventory().getItem(i).copy());
        }

        // Copy armor (slots 36-39)
        for (int i = 0; i < 4; i++) {
            container.setItem(36 + i, target.getInventory().getArmor(i).copy());
        }

        // Copy offhand (slot 40)
        container.setItem(40, target.getInventory().offhand.get(0).copy());

        // Slots 41-44 remain empty (padding to fill the 5th row)

        viewer.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) -> new SnapshotInventoryMenu(
                        containerId, playerInventory, container, target, false, INVENTORY_ROWS),
                Component.literal(target.getScoreboardName() + "'s Inventory")
        ));
    }

    /**
     * Opens a snapshot of the target player's ender chest for the viewer.
     * The viewer sees a 3-row chest GUI containing the target's ender chest items.
     * On close, items are copied back to the target's ender chest.
     *
     * @param viewer the admin viewing the ender chest
     * @param target the player whose ender chest is being viewed
     */
    public static void openEnderChest(ServerPlayer viewer, ServerPlayer target) {
        SimpleContainer container = new SimpleContainer(ENDER_CHEST_SLOTS);

        // Copy ender chest items (27 slots)
        for (int i = 0; i < ENDER_CHEST_SLOTS; i++) {
            container.setItem(i, target.getEnderChestInventory().getItem(i).copy());
        }

        viewer.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) -> new SnapshotInventoryMenu(
                        containerId, playerInventory, container, target, true, ENDER_CHEST_ROWS),
                Component.literal(target.getScoreboardName() + "'s Ender Chest")
        ));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        // Copy items back to the target player when the menu is closed
        if (target != null && !target.hasDisconnected()) {
            if (isEnderChest) {
                copyBackEnderChest();
            } else {
                copyBackInventory();
            }
        }
    }

    private void copyBackInventory() {
        // Copy main inventory (slots 0-35)
        for (int i = 0; i < 36; i++) {
            target.getInventory().setItem(i, snapshotContainer.getItem(i).copy());
        }

        // Copy armor (slots 36-39)
        for (int i = 0; i < 4; i++) {
            target.getInventory().armor.set(i, snapshotContainer.getItem(36 + i).copy());
        }

        // Copy offhand (slot 40)
        target.getInventory().offhand.set(0, snapshotContainer.getItem(40).copy());
    }

    private void copyBackEnderChest() {
        for (int i = 0; i < ENDER_CHEST_SLOTS; i++) {
            target.getEnderChestInventory().setItem(i, snapshotContainer.getItem(i).copy());
        }
    }
}
