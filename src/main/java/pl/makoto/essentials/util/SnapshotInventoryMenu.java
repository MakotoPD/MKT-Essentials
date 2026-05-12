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
import net.neoforged.fml.ModList;
import pl.makoto.essentials.MKTEssentials;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A server-side container menu that displays a snapshot of another player's
 * inventory or ender chest. Changes made in the GUI are applied back to the
 * target player when the viewer closes the menu.
 * 
 * Supports Curios API integration: if Curios is installed, curios items
 * are shown in the 6th row of the inventory view.
 * 
 * Unused slots are filled with black stained glass panes that cannot be
 * interacted with. If there are more than 13 curios items, only the first
 * 13 are shown and the title indicates truncation.
 */
public class SnapshotInventoryMenu extends ChestMenu {

    private static final int ENDER_CHEST_SLOTS = 27;
    private static final int ENDER_CHEST_ROWS = 3;
    private static final int VANILLA_INVENTORY_SLOTS = 41; // 36 main + 4 armor + 1 offhand
    private static final int MAX_CONTAINER_SIZE = 54; // 6 rows of 9
    private static final int MAX_CURIOS_SLOTS = MAX_CONTAINER_SIZE - VANILLA_INVENTORY_SLOTS; // 13

    private final SimpleContainer snapshotContainer;
    private final ServerPlayer target;
    private final boolean isEnderChest;
    private final int curiosStartSlot;
    private final List<CuriosSlotInfo> curiosSlots;
    private final Set<Integer> fillerSlots;

    private SnapshotInventoryMenu(int containerId, Inventory playerInventory,
                                  SimpleContainer container, ServerPlayer target,
                                  boolean isEnderChest, int rows,
                                  int curiosStartSlot, List<CuriosSlotInfo> curiosSlots,
                                  Set<Integer> fillerSlots) {
        super(getMenuType(rows), containerId, playerInventory, container, rows);
        this.snapshotContainer = container;
        this.target = target;
        this.isEnderChest = isEnderChest;
        this.curiosStartSlot = curiosStartSlot;
        this.curiosSlots = curiosSlots;
        this.fillerSlots = fillerSlots;
    }

    private static MenuType<?> getMenuType(int rows) {
        return switch (rows) {
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> MenuType.GENERIC_9x5;
        };
    }

    /**
     * Opens a snapshot of the target player's inventory for the viewer.
     * If Curios is installed, curios items are included in additional rows.
     * Container is always 54 slots (6 rows); unused slots are filled with
     * black glass panes. If more than 13 curios exist, only the first 13
     * are shown.
     */
    public static void openInventory(ServerPlayer viewer, ServerPlayer target) {
        // Collect curios items if available
        List<CuriosSlotInfo> curiosItems = getCuriosItems(target);
        
        int totalCuriosSlots = curiosItems.size();
        int displayedCurios = Math.min(totalCuriosSlots, MAX_CURIOS_SLOTS);
        boolean truncated = totalCuriosSlots > MAX_CURIOS_SLOTS;
        
        // Always use 6 rows (54 slots) for inventory view
        int rows = 6;
        int containerSize = MAX_CONTAINER_SIZE;
        
        SimpleContainer container = new SimpleContainer(containerSize);
        Set<Integer> fillerSlots = new HashSet<>();

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

        // Copy curios items starting at slot 41 (max 13)
        int curiosStartSlot = VANILLA_INVENTORY_SLOTS;
        for (int i = 0; i < displayedCurios; i++) {
            container.setItem(curiosStartSlot + i, curiosItems.get(i).item().copy());
        }

        // Fill all remaining empty slots with black glass panes
        fillEmptyWithPanes(container, 0, containerSize, fillerSlots);

        // Build title
        String title;
        if (truncated) {
            title = target.getScoreboardName() + "'s Inventory (+" + totalCuriosSlots 
                    + " curios, showing " + displayedCurios + "/" + totalCuriosSlots + ")";
        } else if (displayedCurios > 0) {
            title = target.getScoreboardName() + "'s Inventory (+" + displayedCurios + " curios)";
        } else {
            title = target.getScoreboardName() + "'s Inventory";
        }

        // Only keep the displayed curios in the list for copy-back
        List<CuriosSlotInfo> displayedCuriosList = curiosItems.subList(0, displayedCurios);

        final int finalCuriosStart = curiosStartSlot;
        final Set<Integer> finalFillerSlots = fillerSlots;
        viewer.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) -> new SnapshotInventoryMenu(
                        containerId, playerInventory, container, target, false, rows,
                        finalCuriosStart, displayedCuriosList, finalFillerSlots),
                Component.literal(title)
        ));
    }

    /**
     * Opens a snapshot of the target player's ender chest for the viewer.
     */
    public static void openEnderChest(ServerPlayer viewer, ServerPlayer target) {
        SimpleContainer container = new SimpleContainer(ENDER_CHEST_SLOTS);
        Set<Integer> fillerSlots = new HashSet<>();

        for (int i = 0; i < ENDER_CHEST_SLOTS; i++) {
            container.setItem(i, target.getEnderChestInventory().getItem(i).copy());
        }

        // Fill remaining empty slots with panes (ender chest is always 27 slots)
        fillEmptyWithPanes(container, 0, ENDER_CHEST_SLOTS, fillerSlots);

        final Set<Integer> finalFillerSlots = fillerSlots;
        viewer.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) -> new SnapshotInventoryMenu(
                        containerId, playerInventory, container, target, true, ENDER_CHEST_ROWS,
                        -1, List.of(), finalFillerSlots),
                Component.literal(target.getScoreboardName() + "'s Ender Chest")
        ));
    }

    // --- Offline Player Support ---

    /**
     * Opens an offline player's inventory by reading their .dat file.
     * Changes are saved back to the .dat file when the GUI is closed.
     */
    public static void openOfflineInventory(ServerPlayer viewer, UUID targetUuid, String playerName, MinecraftServer server) {
        CompoundTag playerData = loadOfflinePlayerData(targetUuid, server);
        if (playerData == null) {
            viewer.sendSystemMessage(MessageUtils.prefixed("&cCould not load player data for " + playerName));
            return;
        }

        int rows = 6;
        int containerSize = MAX_CONTAINER_SIZE;
        SimpleContainer container = new SimpleContainer(containerSize);
        Set<Integer> fillerSlots = new HashSet<>();

        // Parse inventory from NBT
        ListTag inventoryTag = playerData.getList("Inventory", 10); // 10 = CompoundTag
        for (int i = 0; i < inventoryTag.size(); i++) {
            CompoundTag itemTag = inventoryTag.getCompound(i);
            int slot = itemTag.getByte("Slot") & 0xFF;
            ItemStack stack = ItemStack.parse(server.registryAccess(), itemTag).orElse(ItemStack.EMPTY);

            if (slot >= 0 && slot < 36) {
                container.setItem(slot, stack);
            } else if (slot >= 100 && slot <= 103) {
                container.setItem(36 + (slot - 100), stack); // armor slots 36-39
            } else if (slot == -106 || slot == 150) {
                container.setItem(40, stack); // offhand
            }
        }

        fillEmptyWithPanes(container, 0, containerSize, fillerSlots);

        final Set<Integer> finalFillerSlots = fillerSlots;
        final CompoundTag finalPlayerData = playerData;
        viewer.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) -> new OfflineSnapshotMenu(
                        containerId, playerInventory, container, targetUuid, playerName, server,
                        finalPlayerData, rows, finalFillerSlots, false),
                Component.literal(playerName + "'s Inventory (Offline)")
        ));
    }

    /**
     * Opens an offline player's ender chest by reading their .dat file.
     */
    public static void openOfflineEnderChest(ServerPlayer viewer, UUID targetUuid, String playerName, MinecraftServer server) {
        CompoundTag playerData = loadOfflinePlayerData(targetUuid, server);
        if (playerData == null) {
            viewer.sendSystemMessage(MessageUtils.prefixed("&cCould not load player data for " + playerName));
            return;
        }

        SimpleContainer container = new SimpleContainer(ENDER_CHEST_SLOTS);
        Set<Integer> fillerSlots = new HashSet<>();

        // Parse ender chest from NBT
        ListTag enderItems = playerData.getList("EnderItems", 10);
        for (int i = 0; i < enderItems.size(); i++) {
            CompoundTag itemTag = enderItems.getCompound(i);
            int slot = itemTag.getByte("Slot") & 0xFF;
            ItemStack stack = ItemStack.parse(server.registryAccess(), itemTag).orElse(ItemStack.EMPTY);
            if (slot >= 0 && slot < ENDER_CHEST_SLOTS) {
                container.setItem(slot, stack);
            }
        }

        fillEmptyWithPanes(container, 0, ENDER_CHEST_SLOTS, fillerSlots);

        final Set<Integer> finalFillerSlots = fillerSlots;
        final CompoundTag finalPlayerData = playerData;
        viewer.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) -> new OfflineSnapshotMenu(
                        containerId, playerInventory, container, targetUuid, playerName, server,
                        finalPlayerData, ENDER_CHEST_ROWS, finalFillerSlots, true),
                Component.literal(playerName + "'s Ender Chest (Offline)")
        ));
    }

    private static CompoundTag loadOfflinePlayerData(UUID uuid, MinecraftServer server) {
        Path worldDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        Path playerFile = worldDir.resolve(uuid.toString() + ".dat");
        if (!Files.exists(playerFile)) return null;
        try {
            return NbtIo.readCompressed(playerFile, NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to load offline player data for " + uuid, e);
            return null;
        }
    }

    /**
     * Inner class for offline inventory editing. Saves changes back to the .dat file on close.
     */
    private static class OfflineSnapshotMenu extends ChestMenu {
        private final SimpleContainer snapshotContainer;
        private final UUID targetUuid;
        private final String playerName;
        private final MinecraftServer server;
        private final CompoundTag playerData;
        private final Set<Integer> fillerSlots;
        private final boolean isEnderChest;

        OfflineSnapshotMenu(int containerId, Inventory playerInventory, SimpleContainer container,
                            UUID targetUuid, String playerName, MinecraftServer server,
                            CompoundTag playerData, int rows, Set<Integer> fillerSlots, boolean isEnderChest) {
            super(getMenuType(rows), containerId, playerInventory, container, rows);
            this.snapshotContainer = container;
            this.targetUuid = targetUuid;
            this.playerName = playerName;
            this.server = server;
            this.playerData = playerData;
            this.fillerSlots = fillerSlots;
            this.isEnderChest = isEnderChest;
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            if (slotId >= 0 && slotId < snapshotContainer.getContainerSize() && fillerSlots.contains(slotId)) {
                return;
            }
            super.clicked(slotId, button, clickType, player);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            if (fillerSlots.contains(index)) return ItemStack.EMPTY;
            return super.quickMoveStack(player, index);
        }

        @Override
        public void removed(Player player) {
            super.removed(player);
            saveOfflinePlayerData();
        }

        private void saveOfflinePlayerData() {
            try {
                if (isEnderChest) {
                    ListTag enderItems = new ListTag();
                    for (int i = 0; i < ENDER_CHEST_SLOTS; i++) {
                        if (fillerSlots.contains(i)) continue;
                        ItemStack stack = snapshotContainer.getItem(i);
                        if (!stack.isEmpty()) {
                            CompoundTag itemTag = (CompoundTag) stack.save(server.registryAccess());
                            itemTag.putByte("Slot", (byte) i);
                            enderItems.add(itemTag);
                        }
                    }
                    playerData.put("EnderItems", enderItems);
                } else {
                    ListTag inventoryTag = new ListTag();
                    // Main inventory (slots 0-35)
                    for (int i = 0; i < 36; i++) {
                        if (fillerSlots.contains(i)) continue;
                        ItemStack stack = snapshotContainer.getItem(i);
                        if (!stack.isEmpty()) {
                            CompoundTag itemTag = (CompoundTag) stack.save(server.registryAccess());
                            itemTag.putByte("Slot", (byte) i);
                            inventoryTag.add(itemTag);
                        }
                    }
                    // Armor (slots 36-39 → NBT slots 100-103)
                    for (int i = 0; i < 4; i++) {
                        if (fillerSlots.contains(36 + i)) continue;
                        ItemStack stack = snapshotContainer.getItem(36 + i);
                        if (!stack.isEmpty()) {
                            CompoundTag itemTag = (CompoundTag) stack.save(server.registryAccess());
                            itemTag.putByte("Slot", (byte) (100 + i));
                            inventoryTag.add(itemTag);
                        }
                    }
                    // Offhand (slot 40 → NBT slot -106)
                    if (!fillerSlots.contains(40)) {
                        ItemStack stack = snapshotContainer.getItem(40);
                        if (!stack.isEmpty()) {
                            CompoundTag itemTag = (CompoundTag) stack.save(server.registryAccess());
                            itemTag.putByte("Slot", (byte) -106);
                            inventoryTag.add(itemTag);
                        }
                    }
                    playerData.put("Inventory", inventoryTag);
                }

                // Save back to .dat file
                Path worldDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
                Path playerFile = worldDir.resolve(targetUuid.toString() + ".dat");
                NbtIo.writeCompressed(playerData, playerFile);
                MKTEssentials.LOGGER.info("Saved offline inventory changes for " + playerName + " (" + targetUuid + ")");
            } catch (Exception e) {
                MKTEssentials.LOGGER.error("Failed to save offline player data for " + playerName, e);
            }
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Prevent interaction with filler pane slots
        if (slotId >= 0 && slotId < snapshotContainer.getContainerSize() && fillerSlots.contains(slotId)) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (fillerSlots.contains(index)) {
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, index);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (target != null && !target.hasDisconnected()) {
            if (isEnderChest) {
                copyBackEnderChest();
            } else {
                copyBackInventory();
                copyBackCurios();
            }
        }
    }

    private void copyBackInventory() {
        // Copy main inventory (slots 0-35), skip filler slots
        for (int i = 0; i < 36; i++) {
            if (fillerSlots.contains(i)) continue;
            target.getInventory().setItem(i, snapshotContainer.getItem(i).copy());
        }

        // Copy armor (slots 36-39), skip filler slots
        for (int i = 0; i < 4; i++) {
            if (fillerSlots.contains(36 + i)) continue;
            target.getInventory().armor.set(i, snapshotContainer.getItem(36 + i).copy());
        }

        // Copy offhand (slot 40), skip filler slot
        if (!fillerSlots.contains(40)) {
            target.getInventory().offhand.set(0, snapshotContainer.getItem(40).copy());
        }
    }

    private void copyBackCurios() {
        if (curiosSlots.isEmpty() || curiosStartSlot < 0) return;
        
        try {
            for (int i = 0; i < curiosSlots.size() && (curiosStartSlot + i) < snapshotContainer.getContainerSize(); i++) {
                int slotIndex = curiosStartSlot + i;
                if (fillerSlots.contains(slotIndex)) continue;
                CuriosSlotInfo info = curiosSlots.get(i);
                ItemStack newItem = snapshotContainer.getItem(slotIndex).copy();
                setCuriosItem(target, info.slotType(), info.slotIndex(), newItem);
            }
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to copy back curios items", e);
        }
    }

    private void copyBackEnderChest() {
        for (int i = 0; i < ENDER_CHEST_SLOTS; i++) {
            if (fillerSlots.contains(i)) continue;
            target.getEnderChestInventory().setItem(i, snapshotContainer.getItem(i).copy());
        }
    }

    /**
     * Fills empty slots in the container with black stained glass panes.
     * Pane slots are tracked in the fillerSlots set to prevent interaction.
     */
    private static void fillEmptyWithPanes(SimpleContainer container, int startFill, int endFill, Set<Integer> fillerSlots) {
        ItemStack pane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        pane.set(DataComponents.CUSTOM_NAME, Component.empty());
        for (int i = startFill; i < endFill; i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, pane.copy());
                fillerSlots.add(i);
            }
        }
    }

    // --- Curios Integration (reflection-based for optional dependency) ---

    private static List<CuriosSlotInfo> getCuriosItems(ServerPlayer target) {
        if (!ModList.get().isLoaded("curios")) return List.of();
        
        try {
            // CuriosApi.getCuriosInventory(target)
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            var getCuriosInv = curiosApiClass.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
            Object optionalHandler = getCuriosInv.invoke(null, target);
            
            // Optional<ICuriosItemHandler>
            var isPresentMethod = optionalHandler.getClass().getMethod("isPresent");
            if (!(boolean) isPresentMethod.invoke(optionalHandler)) return List.of();
            
            var getMethod = optionalHandler.getClass().getMethod("get");
            Object handler = getMethod.invoke(optionalHandler);
            
            // handler.getCurios() -> Map<String, ICurioStacksHandler>
            var getCuriosMethod = handler.getClass().getMethod("getCurios");
            @SuppressWarnings("unchecked")
            var curiosMap = (java.util.Map<String, Object>) getCuriosMethod.invoke(handler);
            
            List<CuriosSlotInfo> items = new ArrayList<>();
            
            for (var entry : curiosMap.entrySet()) {
                String slotType = entry.getKey();
                Object stacksHandler = entry.getValue();
                
                // stacksHandler.getStacks() -> IDynamicStackHandler
                var getStacksMethod = stacksHandler.getClass().getMethod("getStacks");
                Object dynamicHandler = getStacksMethod.invoke(stacksHandler);
                
                // dynamicHandler.getSlots() -> int
                var getSlotsMethod = dynamicHandler.getClass().getMethod("getSlots");
                int slots = (int) getSlotsMethod.invoke(dynamicHandler);
                
                for (int i = 0; i < slots; i++) {
                    // dynamicHandler.getStackInSlot(i) -> ItemStack
                    var getStackMethod = dynamicHandler.getClass().getMethod("getStackInSlot", int.class);
                    ItemStack stack = (ItemStack) getStackMethod.invoke(dynamicHandler, i);
                    items.add(new CuriosSlotInfo(slotType, i, stack));
                }
            }
            
            return items;
        } catch (Exception e) {
            MKTEssentials.LOGGER.debug("Curios integration: Could not read curios items", e);
            return List.of();
        }
    }

    private static void setCuriosItem(ServerPlayer target, String slotType, int slotIndex, ItemStack item) {
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            var getCuriosInv = curiosApiClass.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
            Object optionalHandler = getCuriosInv.invoke(null, target);
            
            var isPresentMethod = optionalHandler.getClass().getMethod("isPresent");
            if (!(boolean) isPresentMethod.invoke(optionalHandler)) return;
            
            var getMethod = optionalHandler.getClass().getMethod("get");
            Object handler = getMethod.invoke(optionalHandler);
            
            var getCuriosMethod = handler.getClass().getMethod("getCurios");
            @SuppressWarnings("unchecked")
            var curiosMap = (java.util.Map<String, Object>) getCuriosMethod.invoke(handler);
            
            Object stacksHandler = curiosMap.get(slotType);
            if (stacksHandler == null) return;
            
            var getStacksMethod = stacksHandler.getClass().getMethod("getStacks");
            Object dynamicHandler = getStacksMethod.invoke(stacksHandler);
            
            var setStackMethod = dynamicHandler.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
            setStackMethod.invoke(dynamicHandler, slotIndex, item);
        } catch (Exception e) {
            MKTEssentials.LOGGER.debug("Curios integration: Could not set curios item", e);
        }
    }

    private record CuriosSlotInfo(String slotType, int slotIndex, ItemStack item) {}
}
