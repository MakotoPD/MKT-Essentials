package pl.makoto.essentials.util;

import com.mojang.authlib.properties.PropertyMap;
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
import net.minecraft.world.item.component.ResolvableProfile;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.data.PlayerData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * GUI to ignore/unignore online players (a sub-menu of {@link ChatSettingsMenu}). Clicking a head
 * toggles ignoring that player; a back arrow returns to the chat settings.
 */
public class IgnoreMenu extends ChestMenu {

    private static final int ROWS = 6;
    private static final int SLOTS = ROWS * 9;
    private static final int BACK_SLOT = 49;

    private final ServerPlayer viewer;
    private final List<ServerPlayer> targets;

    private IgnoreMenu(int containerId, Inventory playerInventory, SimpleContainer container,
                       ServerPlayer viewer, List<ServerPlayer> targets) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, ROWS);
        this.viewer = viewer;
        this.targets = targets;
    }

    public static void open(ServerPlayer viewer) {
        PlayerData data = DataManager.getPlayerData(viewer.getUUID());

        List<ServerPlayer> online = new ArrayList<>();
        for (ServerPlayer player : viewer.getServer().getPlayerList().getPlayers()) {
            if (!player.getUUID().equals(viewer.getUUID())) online.add(player);
        }

        int limit = SLOTS - 9; // leave the last row for the back button
        List<ServerPlayer> targets = new ArrayList<>(online.subList(0, Math.min(online.size(), limit)));

        SimpleContainer container = new SimpleContainer(SLOTS);
        for (int i = 0; i < targets.size(); i++) {
            container.setItem(i, head(targets.get(i), data.isIgnoring(targets.get(i).getUUID())));
        }

        ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        pane.set(DataComponents.CUSTOM_NAME, Component.empty());
        for (int i = targets.size(); i < SLOTS; i++) container.setItem(i, pane.copy());

        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponents.CUSTOM_NAME, MessageUtils.format("&e◀ Back"));
        container.setItem(BACK_SLOT, back);

        viewer.openMenu(new SimpleMenuProvider(
                (cId, inv, p) -> new IgnoreMenu(cId, inv, container, viewer, targets),
                Component.literal("Ignore Players")));
    }

    private static ItemStack head(ServerPlayer target, boolean ignored) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        PropertyMap properties = target.getGameProfile().getProperties();
        head.set(DataComponents.PROFILE, new ResolvableProfile(
                Optional.of(target.getScoreboardName()), Optional.of(target.getUUID()), properties));
        head.set(DataComponents.CUSTOM_NAME, MessageUtils.format("&f" + target.getScoreboardName()));

        List<Component> lore = new ArrayList<>();
        lore.add(MessageUtils.format(ignored ? "&cIgnored" : "&aNot ignored"));
        lore.add(Component.empty());
        lore.add(MessageUtils.format(ignored ? "&e▶ Click to unignore" : "&e▶ Click to ignore"));
        head.set(DataComponents.LORE, new ItemLore(lore));
        return head;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= SLOTS) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        if (slotId == BACK_SLOT) {
            ChatSettingsMenu.open(viewer);
            return;
        }
        if (slotId < targets.size()) {
            DataManager.getPlayerData(viewer.getUUID()).toggleIgnore(targets.get(slotId).getUUID());
            DataManager.savePlayerData(viewer.getUUID());
            open(viewer); // refresh
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
