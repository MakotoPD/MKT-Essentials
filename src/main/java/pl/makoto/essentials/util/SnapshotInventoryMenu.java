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
import net.neoforged.fml.ModList;
import pl.makoto.essentials.MKTEssentials;

import java.util.ArrayList;
import java.util.List;

/**
 * A server-side container menu that displays a snapshot of another player's
 * inventory or ender chest. Changes made in the GUI are applied back to the
 * target player when the viewer closes the menu.
 * 
 * Supports Curios API integration: if Curios is installed, curios items
 * are shown in the 6th row of the inventory view.
 */
public class SnapshotInventoryMenu extends ChestMenu {

    private static final int ENDER_CHEST_SLOTS = 27;
    private static final int ENDER_CHEST_ROWS = 3;

    private final SimpleContainer snapshotContainer;
    private final ServerPlayer target;
    private final boolean isEnderChest;
    private final int curiosStartSlot;
    private final List<CuriosSlotInfo> curiosSlots;

    private SnapshotInventoryMenu(int containerId, Inventory playerInventory,
                                  SimpleContainer container, ServerPlayer target,
                                  boolean isEnderChest, int rows,
                                  int curiosStartSlot, List<CuriosSlotInfo> curiosSlots) {
        super(getMenuType(rows), containerId, playerInventory, container, rows);
        this.snapshotContainer = container;
        this.target = target;
        this.isEnderChest = isEnderChest;
        this.curiosStartSlot = curiosStartSlot;
        this.curiosSlots = curiosSlots;
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
     */
    public static void openInventory(ServerPlayer viewer, ServerPlayer target) {
        // Collect curios items if available
        List<CuriosSlotInfo> curiosItems = getCuriosItems(target);
        
        // Calculate total slots needed: 41 vanilla (36 main + 4 armor + 1 offhand) + curios
        int vanillaSlots = 41; // 36 + 4 + 1
        int totalCuriosSlots = curiosItems.size();
        int totalSlots = vanillaSlots + totalCuriosSlots;
        
        // Round up to nearest row of 9, max 54 (6 rows)
        int rows = Math.min(6, (int) Math.ceil(totalSlots / 9.0));
        int containerSize = rows * 9;
        
        SimpleContainer container = new SimpleContainer(containerSize);

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

        // Copy curios items starting at slot 41
        int curiosStartSlot = 41;
        for (int i = 0; i < curiosItems.size() && (curiosStartSlot + i) < containerSize; i++) {
            container.setItem(curiosStartSlot + i, curiosItems.get(i).item().copy());
        }

        final int finalRows = rows;
        final int finalCuriosStart = curiosStartSlot;
        viewer.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) -> new SnapshotInventoryMenu(
                        containerId, playerInventory, container, target, false, finalRows,
                        finalCuriosStart, curiosItems),
                Component.literal(target.getScoreboardName() + "'s Inventory" + 
                        (totalCuriosSlots > 0 ? " (+" + totalCuriosSlots + " curios)" : ""))
        ));
    }

    /**
     * Opens a snapshot of the target player's ender chest for the viewer.
     */
    public static void openEnderChest(ServerPlayer viewer, ServerPlayer target) {
        SimpleContainer container = new SimpleContainer(ENDER_CHEST_SLOTS);

        for (int i = 0; i < ENDER_CHEST_SLOTS; i++) {
            container.setItem(i, target.getEnderChestInventory().getItem(i).copy());
        }

        viewer.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) -> new SnapshotInventoryMenu(
                        containerId, playerInventory, container, target, true, ENDER_CHEST_ROWS,
                        -1, List.of()),
                Component.literal(target.getScoreboardName() + "'s Ender Chest")
        ));
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

    private void copyBackCurios() {
        if (curiosSlots.isEmpty() || curiosStartSlot < 0) return;
        
        try {
            for (int i = 0; i < curiosSlots.size() && (curiosStartSlot + i) < snapshotContainer.getContainerSize(); i++) {
                CuriosSlotInfo info = curiosSlots.get(i);
                ItemStack newItem = snapshotContainer.getItem(curiosStartSlot + i).copy();
                setCuriosItem(target, info.slotType(), info.slotIndex(), newItem);
            }
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to copy back curios items", e);
        }
    }

    private void copyBackEnderChest() {
        for (int i = 0; i < ENDER_CHEST_SLOTS; i++) {
            target.getEnderChestInventory().setItem(i, snapshotContainer.getItem(i).copy());
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
