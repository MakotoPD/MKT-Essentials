package pl.makoto.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import pl.makoto.essentials.util.ItemStorageManager.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * Category editor: the chest opens holding copies of everything currently stored, and whatever
 * is left in it on close becomes the new contents. Adding, removing, replacing and reordering
 * are therefore all just "edit the chest".
 */
public class ItemStoreMenu extends ChestMenu {
    private static final int ROWS = 6;

    private final ServerPlayer editor;
    private final SimpleContainer container;
    private final Category category;
    private boolean saved = false;

    private ItemStoreMenu(int containerId, Inventory playerInventory, SimpleContainer container,
                          ServerPlayer editor, Category category) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, ROWS);
        this.editor = editor;
        this.container = container;
        this.category = category;
    }

    public static void open(ServerPlayer editor, Category category) {
        SimpleContainer container = new SimpleContainer(ROWS * 9);
        List<ItemStack> existing = ItemStorageManager.toStacks(category, editor.registryAccess());
        for (int i = 0; i < existing.size() && i < container.getContainerSize(); i++) {
            container.setItem(i, existing.get(i));
        }

        editor.openMenu(new SimpleMenuProvider(
                (containerId, inventory, player) -> new ItemStoreMenu(containerId, inventory, container, editor, category),
                MessageUtils.format("&8Items: &6" + category.name)
        ));
        editor.sendSystemMessage(MessageUtils.prefixed(
                "&7Edit the chest and close it to save category &6" + category.name + "&7."));
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

        ItemStorageManager.setItems(category, items, editor.registryAccess());

        // The chest held copies, so nothing needs returning in creative. A survival admin may have
        // dragged real items in, though, and those must not disappear.
        if (!editor.isCreative()) {
            for (ItemStack stack : items) {
                if (!editor.getInventory().add(stack.copy())) editor.drop(stack.copy(), false);
            }
        }

        editor.sendSystemMessage(MessageUtils.prefixed("&aCategory &6" + category.name + " &asaved with &6"
                + items.size() + " &aitem" + (items.size() == 1 ? "" : "s") + "."));
    }
}
