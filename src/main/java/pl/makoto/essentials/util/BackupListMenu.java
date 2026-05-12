package pl.makoto.essentials.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.data.InventoryBackup;

import java.util.*;

/**
 * A GUI menu that displays a list of inventory backups for a target player.
 * Each backup is represented by an item in a 6-row chest:
 * - Death backups: Skeleton skull
 * - Manual backups: Writable book
 * - Scheduled backups: Clock
 *
 * Clicking a backup item restores it to the target player.
 */
public class BackupListMenu extends ChestMenu {
    private static final int ROWS = 6;
    private static final int SLOTS = ROWS * 9;

    private final ServerPlayer viewer;
    private final UUID targetUuid;
    private final String targetName;
    private final List<BackupManager.BackupInfo> backups;

    private BackupListMenu(int containerId, Inventory playerInventory,
                           SimpleContainer container, ServerPlayer viewer,
                           UUID targetUuid, String targetName,
                           List<BackupManager.BackupInfo> backups) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, ROWS);
        this.viewer = viewer;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.backups = backups;
    }

    /**
     * Opens the backup list GUI for the viewer, showing backups of the target player.
     */
    public static void open(ServerPlayer viewer, UUID targetUuid, String targetName) {
        List<BackupManager.BackupInfo> backups = BackupManager.listBackups(targetUuid);

        SimpleContainer container = new SimpleContainer(SLOTS);

        // Fill with backup items (max 54)
        int count = Math.min(backups.size(), SLOTS);
        for (int i = 0; i < count; i++) {
            BackupManager.BackupInfo info = backups.get(i);
            container.setItem(i, createBackupItem(info));
        }

        // Fill remaining slots with glass panes
        ItemStack pane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        pane.set(DataComponents.CUSTOM_NAME, Component.empty());
        for (int i = count; i < SLOTS; i++) {
            container.setItem(i, pane.copy());
        }

        final List<BackupManager.BackupInfo> finalBackups = backups;
        viewer.openMenu(new SimpleMenuProvider(
                (cId, inv, player) -> new BackupListMenu(cId, inv, container, viewer, targetUuid, targetName, finalBackups),
                Component.literal(targetName + "'s Backups")
        ));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= SLOTS) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        // Check if this slot has a backup
        if (slotId >= backups.size()) return; // Glass pane slot, ignore

        BackupManager.BackupInfo info = backups.get(slotId);

        // Load the full backup
        InventoryBackup backup = BackupManager.loadBackup(targetUuid, info.filename());
        if (backup == null) {
            viewer.sendSystemMessage(MessageUtils.prefixed("&cFailed to load backup."));
            return;
        }

        // Find the target player (must be online)
        ServerPlayer target = viewer.getServer().getPlayerList().getPlayer(targetUuid);
        if (target == null) {
            viewer.sendSystemMessage(MessageUtils.prefixed("&cTarget player is not online."));
            return;
        }

        // Restore the backup
        boolean success = BackupManager.restoreBackup(target, backup);
        if (success) {
            viewer.sendSystemMessage(MessageUtils.prefixed(
                    "&aRestored backup from &6" + BackupManager.formatTimestamp(info.timestamp()) + " &ato &6" + targetName + "&a."));
            target.sendSystemMessage(MessageUtils.prefixed(
                    "&aYour inventory has been restored from a backup by &6" + viewer.getScoreboardName() + "&a."));
        } else {
            viewer.sendSystemMessage(MessageUtils.prefixed("&cFailed to restore backup."));
        }

        // Close the menu
        viewer.closeContainer();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Prevent shift-clicking
        return ItemStack.EMPTY;
    }

    /**
     * Creates a display item for a backup entry.
     */
    private static ItemStack createBackupItem(BackupManager.BackupInfo info) {
        ItemStack stack;
        String displayName;

        switch (info.reason()) {
            case "death" -> {
                stack = new ItemStack(Items.SKELETON_SKULL);
                displayName = "\u00a7c\u2620 Death Backup";
            }
            case "manual" -> {
                stack = new ItemStack(Items.WRITABLE_BOOK);
                displayName = "\u00a76\u270e Manual Backup";
            }
            case "join" -> {
                stack = new ItemStack(Items.LIME_DYE);
                displayName = "\u00a7a\u25b6 Join Backup";
            }
            case "quit" -> {
                stack = new ItemStack(Items.RED_DYE);
                displayName = "\u00a7c\u25a0 Quit Backup";
            }
            default -> {
                stack = new ItemStack(Items.CLOCK);
                displayName = "\u00a7b\u23f0 Scheduled Backup";
            }
        }

        stack.set(DataComponents.CUSTOM_NAME, Component.literal(displayName));

        // Build lore
        List<Component> loreLines = new ArrayList<>();
        loreLines.add(Component.literal("\u00a77Date: \u00a7f" + BackupManager.formatTimestamp(info.timestamp())));
        loreLines.add(Component.literal("\u00a77Reason: \u00a7f" + info.reason()));
        loreLines.add(Component.literal("\u00a77Items: \u00a7f" + info.totalItems()));

        if (info.note() != null && !info.note().isEmpty()) {
            loreLines.add(Component.literal("\u00a77Note: \u00a7f" + info.note()));
        }

        loreLines.add(Component.empty());
        loreLines.add(Component.literal("\u00a7eClick to restore this backup"));

        stack.set(DataComponents.LORE, new ItemLore(loreLines));

        return stack;
    }
}
