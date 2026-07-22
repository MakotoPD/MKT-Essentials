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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for per-player chat settings (mentions, private messages, ignore management), inspired by
 * FlectonePulse's chatsetting menu. Opened by /chatsetting.
 */
public class ChatSettingsMenu extends ChestMenu {

    private static final int ROWS = 3;
    private static final int SLOTS = ROWS * 9;
    private static final int MENTIONS_SLOT = 11;
    private static final int MESSAGES_SLOT = 13;
    private static final int IGNORE_SLOT = 15;

    private final ServerPlayer viewer;

    private ChatSettingsMenu(int containerId, Inventory playerInventory, SimpleContainer container, ServerPlayer viewer) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, container, ROWS);
        this.viewer = viewer;
    }

    public static void open(ServerPlayer viewer) {
        SimpleContainer container = new SimpleContainer(SLOTS);
        PlayerData data = DataManager.getPlayerData(viewer.getUUID());

        ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        pane.set(DataComponents.CUSTOM_NAME, Component.empty());
        for (int i = 0; i < SLOTS; i++) container.setItem(i, pane.copy());

        container.setItem(MENTIONS_SLOT, toggle(Items.NOTE_BLOCK, "&e&lMention Pings",
                "&7Play a sound when someone @mentions you.", data.getChatSetting("mentions", true)));
        container.setItem(MESSAGES_SLOT, toggle(Items.WRITABLE_BOOK, "&e&lPrivate Messages",
                "&7Allow other players to /msg you.", !data.isMsgDisabled()));

        ItemStack ignore = new ItemStack(Items.BARRIER);
        ignore.set(DataComponents.CUSTOM_NAME, MessageUtils.format("&c&lIgnored Players"));
        List<Component> lore = new ArrayList<>();
        lore.add(MessageUtils.format("&7Currently ignoring: &f" + data.getIgnoredPlayerIds().size()));
        lore.add(Component.empty());
        lore.add(MessageUtils.format("&e▶ Click to manage"));
        ignore.set(DataComponents.LORE, new ItemLore(lore));
        container.setItem(IGNORE_SLOT, ignore);

        viewer.openMenu(new SimpleMenuProvider(
                (cId, inv, p) -> new ChatSettingsMenu(cId, inv, container, viewer),
                Component.literal("Chat Settings")));
    }

    private static ItemStack toggle(Item icon, String name, String description, boolean enabled) {
        ItemStack item = new ItemStack(icon);
        item.set(DataComponents.CUSTOM_NAME, MessageUtils.format(name));
        List<Component> lore = new ArrayList<>();
        lore.add(MessageUtils.format(description));
        lore.add(Component.empty());
        lore.add(MessageUtils.format("&7State: " + (enabled ? "&a&lENABLED" : "&c&lDISABLED")));
        lore.add(MessageUtils.format("&e▶ Click to toggle"));
        item.set(DataComponents.LORE, new ItemLore(lore));
        return item;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= SLOTS) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        PlayerData data = DataManager.getPlayerData(viewer.getUUID());
        switch (slotId) {
            case MENTIONS_SLOT -> {
                data.toggleChatSetting("mentions", true);
                DataManager.savePlayerData(viewer.getUUID());
                open(viewer);
            }
            case MESSAGES_SLOT -> {
                data.setMsgDisabled(!data.isMsgDisabled());
                DataManager.savePlayerData(viewer.getUUID());
                open(viewer);
            }
            case IGNORE_SLOT -> IgnoreMenu.open(viewer);
            default -> { /* glass pane */ }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
