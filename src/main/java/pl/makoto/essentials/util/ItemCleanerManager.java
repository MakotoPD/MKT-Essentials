package pl.makoto.essentials.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.config.Settings;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ground item cleanup: stacking, holograms, despawn timers, and global sweep.
 * Stores direct ArmorStand references to avoid entity lookup issues.
 */
@EventBusSubscriber(modid = MKTEssentials.MODID)
public class ItemCleanerManager {
    private static final String HOLOGRAM_TAG = "mkt_hologram";

    // Direct reference storage — no entity ID/UUID lookup needed
    private static final Map<UUID, Long> itemSpawnTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, ArmorStand> hologramRefs = new ConcurrentHashMap<>();

    private static long tickCounter = 0;
    private static long sweepTickCounter = 0;
    private static boolean sweepWarned = false;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (Settings.getItemDespawnTime() <= 0) return;

        tickCounter++;
        
        // Position update every 5 ticks (4x per second) — smooth hologram following
        boolean updatePositions = (tickCounter % 5 == 0);
        // Full processing (text, despawn, stacking, new items) every 20 ticks (1x per second)
        boolean fullProcess = (tickCounter % 20 == 0);

        if (!updatePositions && !fullProcess) return;

        MinecraftServer server = event.getServer();

        if (fullProcess) {
            for (ServerLevel level : server.getAllLevels()) {
                processItemsInLevel(level);
            }
            handleSweep(server);
        } else {
            // Only update hologram positions (lightweight)
            for (Map.Entry<UUID, ArmorStand> entry : hologramRefs.entrySet()) {
                ArmorStand h = entry.getValue();
                if (h == null || !h.isAlive()) continue;
                if (!(h.level() instanceof ServerLevel sLevel)) continue;
                // Find the item in the same level
                for (Entity entity : sLevel.getAllEntities()) {
                    if (entity instanceof ItemEntity item && item.getUUID().equals(entry.getKey()) && item.isAlive()) {
                        h.moveTo(item.getX(), item.getY() - 1.0, item.getZ());
                        break;
                    }
                }
            }
        }
    }

    private static void processItemsInLevel(ServerLevel level) {
        List<ItemEntity> allItems = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity item && item.isAlive()) {
                allItems.add(item);
            }
        }

        Set<UUID> activeItemUuids = new HashSet<>();

        for (ItemEntity item : allItems) {
            if (!item.isAlive()) continue;
            UUID itemUuid = item.getUUID();
            activeItemUuids.add(itemUuid);

            // New item — register
            if (!itemSpawnTimes.containsKey(itemUuid)) {
                itemSpawnTimes.put(itemUuid, System.currentTimeMillis());

                if (Settings.isItemStacking()) {
                    if (tryStackItem(item, level, allItems)) {
                        activeItemUuids.remove(itemUuid);
                        continue;
                    }
                }
            }

            // Hologram: create or move existing
            if (Settings.isItemHologramEnabled()) {
                ArmorStand hologram = hologramRefs.get(itemUuid);
                if (hologram != null && hologram.isAlive()) {
                    // MOVE existing hologram
                    hologram.moveTo(item.getX(), item.getY() - 1.0, item.getZ());
                    hologram.setCustomName(buildHologramName(item));
                } else {
                    // Create new hologram
                    hologram = createHologram(item, level);
                    if (hologram != null) {
                        hologramRefs.put(itemUuid, hologram);
                    }
                }
            }

            // Despawn check
            if (!isWhitelisted(item.getItem())) {
                Long spawnTime = itemSpawnTimes.get(itemUuid);
                if (spawnTime != null) {
                    long elapsed = (System.currentTimeMillis() - spawnTime) / 1000;
                    if (elapsed >= Settings.getItemDespawnTime()) {
                        discardHologram(itemUuid);
                        item.discard();
                        itemSpawnTimes.remove(itemUuid);
                        activeItemUuids.remove(itemUuid);
                    }
                }
            }
        }

        // Clean up holograms for items that no longer exist IN THIS LEVEL ONLY
        Iterator<Map.Entry<UUID, ArmorStand>> it = hologramRefs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ArmorStand> entry = it.next();
            ArmorStand h = entry.getValue();
            // Only clean up holograms that belong to this level
            if (h == null || !h.isAlive()) {
                it.remove();
                itemSpawnTimes.remove(entry.getKey());
                continue;
            }
            if (h.level() != level) continue; // Skip holograms from other levels
            if (!activeItemUuids.contains(entry.getKey())) {
                h.discard();
                it.remove();
                itemSpawnTimes.remove(entry.getKey());
            }
        }
    }

    // ========== STACKING ==========

    private static boolean tryStackItem(ItemEntity newItem, ServerLevel level, List<ItemEntity> allItems) {
        ItemStack newStack = newItem.getItem();
        if (newStack.isEmpty()) return false;

        double radiusSq = Settings.getItemStackingRadius() * Settings.getItemStackingRadius();

        for (ItemEntity existing : allItems) {
            if (existing == newItem || !existing.isAlive()) continue;
            if (existing.distanceToSqr(newItem) > radiusSq) continue;

            ItemStack existingStack = existing.getItem();
            if (existingStack.isEmpty()) continue;

            if (ItemStack.isSameItemSameComponents(newStack, existingStack)) {
                int maxSize = existingStack.getMaxStackSize();
                if (existingStack.getCount() + newStack.getCount() <= maxSize) {
                    existingStack.setCount(existingStack.getCount() + newStack.getCount());
                    existing.setItem(existingStack);
                    discardHologram(newItem.getUUID());
                    newItem.discard();
                    itemSpawnTimes.remove(newItem.getUUID());
                    return true;
                }
            }
        }
        return false;
    }

    // ========== HOLOGRAM ==========

    private static ArmorStand createHologram(ItemEntity item, ServerLevel level) {
        ArmorStand hologram = new ArmorStand(level, item.getX(), item.getY() - 1.5, item.getZ());
        hologram.setInvisible(true);
        hologram.setNoGravity(true);
        hologram.setCustomNameVisible(true);
        hologram.setSilent(true);
        hologram.setCustomName(buildHologramName(item));
        hologram.addTag(HOLOGRAM_TAG);
        level.addFreshEntity(hologram);
        return hologram;
    }

    private static void discardHologram(UUID itemUuid) {
        ArmorStand h = hologramRefs.remove(itemUuid);
        if (h != null && h.isAlive()) h.discard();
    }

    private static Component buildHologramName(ItemEntity item) {
        ItemStack stack = item.getItem();
        String name = stack.getHoverName().getString();
        int count = stack.getCount();

        if (isWhitelisted(stack)) {
            if (count > 1) return Component.literal("\u00a7f\u2B26 " + name + " \u00a77x" + count);
            return Component.literal("\u00a7f\u2B26 " + name);
        }

        Long spawnTime = itemSpawnTimes.get(item.getUUID());
        int remaining = Settings.getItemDespawnTime();
        if (spawnTime != null) {
            long elapsed = (System.currentTimeMillis() - spawnTime) / 1000;
            remaining = Math.max(0, Settings.getItemDespawnTime() - (int) elapsed);
        }

        if (count > 1) {
            return Component.literal("\u00a7f\u2B26 " + name + " \u00a77x" + count + " \u00a77(\u00a7e" + remaining + "s\u00a77)");
        }
        return Component.literal("\u00a7f\u2B26 " + name + " \u00a77(\u00a7e" + remaining + "s\u00a77)");
    }

    // ========== SWEEP ==========

    private static void handleSweep(MinecraftServer server) {
        int sweepInterval = Settings.getItemSweepInterval();
        if (sweepInterval <= 0) return;

        sweepTickCounter++;

        if (!sweepWarned && sweepTickCounter >= (sweepInterval - Settings.getItemSweepWarning())) {
            sweepWarned = true;
            long secondsLeft = sweepInterval - sweepTickCounter;
            server.getPlayerList().broadcastSystemMessage(
                MessageUtils.formatBypass(I18n.get("items.sweep-warning", "seconds", String.valueOf(Math.max(1, secondsLeft)))), false);
        }

        if (sweepTickCounter >= sweepInterval) {
            int count = sweepAllItems(server);
            sweepTickCounter = 0;
            sweepWarned = false;
            server.getPlayerList().broadcastSystemMessage(
                MessageUtils.formatBypass(I18n.get("items.sweep-cleared", "count", String.valueOf(count))), false);
        }
    }

    private static int sweepAllItems(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<ItemEntity> items = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity item && item.isAlive()) items.add(item);
            }
            for (ItemEntity item : items) {
                if (isWhitelisted(item.getItem())) continue;
                discardHologram(item.getUUID());
                item.discard();
                itemSpawnTimes.remove(item.getUUID());
                count++;
            }
        }
        return count;
    }

    // ========== WHITELIST ==========

    private static boolean isWhitelisted(ItemStack stack) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (String pattern : Settings.getItemWhitelist()) {
            if (pattern.endsWith("*")) {
                if (itemId.startsWith(pattern.substring(0, pattern.length() - 1))) return true;
            } else {
                if (itemId.equals(pattern)) return true;
            }
        }
        return false;
    }

    // ========== CLEANUP ON START ==========

    /**
     * Removes ALL hologram ArmorStands tagged with "mkt_hologram" on server start.
     */
    public static void cleanupOrphanedHolograms(MinecraftServer server) {
        int total = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<ArmorStand> toRemove = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ArmorStand stand && stand.getTags().contains(HOLOGRAM_TAG)) {
                    toRemove.add(stand);
                }
            }
            for (ArmorStand stand : toRemove) stand.discard();
            total += toRemove.size();
        }
        hologramRefs.clear();
        itemSpawnTimes.clear();
        if (total > 0) {
            MKTEssentials.LOGGER.info("Cleaned up {} orphaned item holograms.", total);
        }
    }

    // ========== PUBLIC API ==========

    public static int clearAllItems(MinecraftServer server) { return sweepAllItems(server); }

    public static int clearItemsInRadius(ServerLevel level, double x, double y, double z, int radius) {
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
            new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius));
        int count = 0;
        for (ItemEntity item : items) {
            if (isWhitelisted(item.getItem())) continue;
            discardHologram(item.getUUID());
            item.discard();
            itemSpawnTimes.remove(item.getUUID());
            count++;
        }
        return count;
    }
}
