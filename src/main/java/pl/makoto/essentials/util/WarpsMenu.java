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
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only GUI listing all warps. Clicking a warp teleports the viewer there.
 */
public class WarpsMenu extends ChestMenu {
    private static final int ROWS = 6;
    private static final int SLOTS = ROWS * 9;

    private final ServerPlayer viewer;
    private final List<String> warpNames;

    private WarpsMenu(int containerId, Inventory playerInventory, SimpleContainer container,
                      ServerPlayer viewer, List<String> warpNames) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, ROWS);
        this.viewer = viewer;
        this.warpNames = warpNames;
    }

    public static void open(ServerPlayer viewer) {
        List<String> names = new ArrayList<>(DataManager.getWarps().keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);

        SimpleContainer container = new SimpleContainer(SLOTS);
        int count = Math.min(names.size(), SLOTS);
        for (int i = 0; i < count; i++) {
            String name = names.get(i);
            PlayerData.SavedLocation loc = DataManager.getWarps().get(name);

            ItemStack item = new ItemStack(Items.ENDER_PEARL);
            item.set(DataComponents.CUSTOM_NAME, MessageUtils.format("&b&l" + name));
            List<Component> lore = new ArrayList<>();
            if (loc != null) {
                lore.add(MessageUtils.format("&7World: &f" + loc.dimension));
                lore.add(MessageUtils.format("&7Position: &f" + (int) loc.x + ", " + (int) loc.y + ", " + (int) loc.z));
            }
            lore.add(Component.empty());
            lore.add(MessageUtils.format("&e▶ Click to teleport"));
            item.set(DataComponents.LORE, new ItemLore(lore));
            container.setItem(i, item);
        }

        ItemStack pane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        pane.set(DataComponents.CUSTOM_NAME, Component.empty());
        for (int i = count; i < SLOTS; i++) {
            container.setItem(i, pane.copy());
        }

        viewer.openMenu(new SimpleMenuProvider(
                (cId, inv, p) -> new WarpsMenu(cId, inv, container, viewer, names),
                Component.literal("Warps")
        ));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= SLOTS) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        if (slotId >= warpNames.size()) return; // glass pane

        String name = warpNames.get(slotId);
        PlayerData.SavedLocation loc = DataManager.getWarps().get(name);
        viewer.closeContainer();
        if (loc == null) {
            viewer.sendSystemMessage(MessageUtils.prefixed("&cWarp '" + name + "' no longer exists."));
            return;
        }
        TeleportManager.requestTeleport(viewer, loc, false, TeleportManager.Type.WARP);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
