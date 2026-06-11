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
import pl.makoto.essentials.commands.KitCommands;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.KitData;
import pl.makoto.essentials.data.PlayerData;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only GUI listing all kits. Clicking a kit claims it (same logic as /kit <name>).
 */
public class KitsMenu extends ChestMenu {
    private static final int ROWS = 6;
    private static final int SLOTS = ROWS * 9;

    private final ServerPlayer viewer;
    private final List<KitData> kits;

    private KitsMenu(int containerId, Inventory playerInventory, SimpleContainer container,
                     ServerPlayer viewer, List<KitData> kits) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, ROWS);
        this.viewer = viewer;
        this.kits = kits;
    }

    public static void open(ServerPlayer viewer) {
        List<KitData> kits = new ArrayList<>(DataManager.getAllKits());
        SimpleContainer container = new SimpleContainer(SLOTS);
        PlayerData data = DataManager.getPlayerData(viewer.getUUID());
        long now = System.currentTimeMillis();

        int count = Math.min(kits.size(), SLOTS);
        for (int i = 0; i < count; i++) {
            container.setItem(i, createKitItem(viewer, kits.get(i), data, now));
        }

        ItemStack pane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        pane.set(DataComponents.CUSTOM_NAME, Component.empty());
        for (int i = count; i < SLOTS; i++) {
            container.setItem(i, pane.copy());
        }

        viewer.openMenu(new SimpleMenuProvider(
                (cId, inv, p) -> new KitsMenu(cId, inv, container, viewer, kits),
                Component.literal("Kits")
        ));
    }

    private static ItemStack createKitItem(ServerPlayer viewer, KitData kit, PlayerData data, long now) {
        // Icon: first item of the kit, fallback to a chest
        List<ItemStack> items = kit.restoreItems(viewer.registryAccess());
        ItemStack icon = items.isEmpty() ? new ItemStack(Items.CHEST) : items.get(0).copyWithCount(1);

        icon.set(DataComponents.CUSTOM_NAME, MessageUtils.format("&6&l" + kit.getName()));

        List<Component> lore = new ArrayList<>();
        lore.add(MessageUtils.format("&7Items: &f" + items.size()));
        if (kit.getCooldownSeconds() > 0) {
            lore.add(MessageUtils.format("&7Cooldown: &f" + DurationParser.format(kit.getCooldownSeconds() * 1000L)));
            long availableAt = data.getKitCooldowns().getOrDefault(kit.getName().toLowerCase(), 0L);
            if (availableAt > now) {
                lore.add(MessageUtils.format("&cAvailable in: &f" + DurationParser.format(availableAt - now)));
            } else {
                lore.add(MessageUtils.format("&aReady to claim!"));
            }
        } else {
            lore.add(MessageUtils.format("&aReady to claim!"));
        }
        lore.add(Component.empty());
        lore.add(MessageUtils.format("&e▶ Click to claim"));
        icon.set(DataComponents.LORE, new ItemLore(lore));
        return icon;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= SLOTS) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        if (slotId >= kits.size()) return; // glass pane

        String kitName = kits.get(slotId).getName();
        viewer.closeContainer();
        KitCommands.claim(viewer, kitName);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
