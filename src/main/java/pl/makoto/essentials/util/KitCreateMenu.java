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
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.KitData;

import java.util.ArrayList;
import java.util.List;

/**
 * Kit creation GUI: the admin places the kit contents into a chest; when the menu
 * is closed the kit is saved and the items are returned to the admin's inventory.
 */
public class KitCreateMenu extends ChestMenu {
    private static final int ROWS = 3;

    private final ServerPlayer creator;
    private final SimpleContainer container;
    private final String kitName;
    private final long cooldownSeconds;
    private boolean saved = false;

    private KitCreateMenu(int containerId, Inventory playerInventory, SimpleContainer container,
                          ServerPlayer creator, String kitName, long cooldownSeconds) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, container, ROWS);
        this.creator = creator;
        this.container = container;
        this.kitName = kitName;
        this.cooldownSeconds = cooldownSeconds;
    }

    public static void open(ServerPlayer creator, String kitName, long cooldownSeconds) {
        SimpleContainer container = new SimpleContainer(ROWS * 9);
        creator.openMenu(new SimpleMenuProvider(
                (cId, inv, p) -> new KitCreateMenu(cId, inv, container, creator, kitName, cooldownSeconds),
                MessageUtils.format("&8Kit contents: &6" + kitName)
        ));
        creator.sendSystemMessage(MessageUtils.prefixed(
                "&7Place the kit items in the chest and close it to save kit &6" + kitName + "&7."));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (saved) return;
        saved = true;

        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) items.add(stack);
        }

        if (items.isEmpty()) {
            creator.sendSystemMessage(MessageUtils.prefixed("&cKit creation cancelled — no items were placed."));
            return;
        }

        KitData kit = new KitData(kitName, cooldownSeconds);
        kit.setItems(items, creator.registryAccess());
        DataManager.addKit(kit);

        // Give the items back — the kit stores copies
        for (ItemStack stack : items) {
            if (!creator.getInventory().add(stack)) {
                creator.drop(stack, false);
            }
        }

        creator.sendSystemMessage(MessageUtils.prefixed("&aKit &6" + kitName + " &acreated with &6"
                + items.size() + " &aitem" + (items.size() == 1 ? "" : "s") + " and &6" + cooldownSeconds + "s &acooldown."));
    }
}
