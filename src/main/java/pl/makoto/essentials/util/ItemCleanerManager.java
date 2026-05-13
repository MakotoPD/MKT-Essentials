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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.I18n;
import pl.makoto.essentials.config.Settings;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ground item cleanup: stacking, holograms, despawn timers, and global sweep.
 * Stores direct ArmorStand and ItemEntity references to avoid entity lookup issues.
 */
@EventBusSubscriber(modid = MKTEssentials.MODID)
public class ItemCleanerManager {
    private static final String HOLOGRAM_TAG = "mkt_hologram";
    private static final double HOLOGRAM_Y_OFFSET = 0.5;

    private static final Map<UUID, Long> itemSpawnTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, ArmorStand> hologramRefs = new ConcurrentHashMap<>();
    private static final Map<UUID, ItemEntity> itemRefs = new ConcurrentHashMap<>();

    // Tracks ArmorStands being added to the world by this mod right now — distinguishes
    // freshly created holograms from orphans loading from disk in unloaded chunks.
    private static final Set<ArmorStand> pendingHolograms = ConcurrentHashMap.newKeySet();

    private static long tickCounter = 0;
    private static long sweepTickCounter = 0;
    private static boolean sweepWarned = false;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;

        boolean updatePositions = (tickCounter % 5 == 0);
        boolean fullProcess = (tickCounter % 20 == 0);

        if (!updatePositions && !fullProcess) return;

        MinecraftServer server = event.getServer();

        if (fullProcess) {
            for (ServerLevel level : server.getAllLevels()) {
                processItemsInLevel(level);
            }
            handleSweep(server);
        } else {
            // Lightweight position update — O(n) via direct item references
            for (Map.Entry<UUID, ArmorStand> entry : hologramRefs.entrySet()) {
                ArmorStand h = entry.getValue();
                if (h == null || !h.isAlive()) continue;
                ItemEntity item = itemRefs.get(entry.getKey());
                if (item == null || !item.isAlive()) continue;
                h.moveTo(item.getX(), item.getY() + HOLOGRAM_Y_OFFSET, item.getZ());
            }
        }
    }

    // Intercepts every ArmorStand joining the level — cancels orphan holograms from previous
    // sessions that load from disk when players enter chunks far from spawn.
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;
        if (!stand.getTags().contains(HOLOGRAM_TAG)) return;
        if (!pendingHolograms.contains(stand)) {
            event.setCanceled(true);
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
                itemRefs.put(itemUuid, item);

                // Prevent vanilla 5-min despawn when mod controls timing or item is whitelisted
                if (Settings.getItemDespawnTime() > 0 || isWhitelisted(item.getItem())) {
                    item.lifespan = Integer.MAX_VALUE;
                }

                if (Settings.isItemStacking()) {
                    if (tryStackItem(item, level, allItems)) {
                        activeItemUuids.remove(itemUuid);
                        continue;
                    }
                }
            }

            // Hologram: create or update — independent of despawn setting
            if (Settings.isItemHologramEnabled()) {
                ArmorStand hologram = hologramRefs.get(itemUuid);
                if (hologram != null && hologram.isAlive()) {
                    hologram.moveTo(item.getX(), item.getY() + HOLOGRAM_Y_OFFSET, item.getZ());
                    hologram.setCustomName(buildHologramName(item));
                } else {
                    hologram = createHologram(item, level);
                    if (hologram != null) {
                        hologramRefs.put(itemUuid, hologram);
                    }
                }
            }

            // Despawn check — only when mod despawn is enabled and item is not whitelisted
            if (Settings.getItemDespawnTime() > 0 && !isWhitelisted(item.getItem())) {
                Long spawnTime = itemSpawnTimes.get(itemUuid);
                if (spawnTime != null) {
                    long elapsed = (System.currentTimeMillis() - spawnTime) / 1000;
                    if (elapsed >= Settings.getItemDespawnTime()) {
                        discardHologram(itemUuid);
                        itemRefs.remove(itemUuid);
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
            if (h == null || !h.isAlive()) {
                it.remove();
                itemSpawnTimes.remove(entry.getKey());
                itemRefs.remove(entry.getKey());
                continue;
            }
            if (h.level() != level) continue;
            if (!activeItemUuids.contains(entry.getKey())) {
                h.discard();
                it.remove();
                itemSpawnTimes.remove(entry.getKey());
                itemRefs.remove(entry.getKey());
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

            if (!ItemStack.isSameItemSameComponents(newStack, existingStack)) continue;

            int effectiveMax = Math.max(existingStack.getMaxStackSize(), Settings.getItemMaxStackSize());
            int available = effectiveMax - existingStack.getCount();
            if (available <= 0) continue;

            int transfer = Math.min(newStack.getCount(), available);
            existingStack.grow(transfer);
            existing.setItem(existingStack);
            newStack.shrink(transfer);

            if (newStack.isEmpty()) {
                discardHologram(newItem.getUUID());
                itemRefs.remove(newItem.getUUID());
                newItem.discard();
                itemSpawnTimes.remove(newItem.getUUID());
                return true;
            }
            // Partial merge — newItem still has remaining count, continue looking for space
            newItem.setItem(newStack);
        }
        return false;
    }

    // ========== HOLOGRAM ==========

    private static ArmorStand createHologram(ItemEntity item, ServerLevel level) {
        ArmorStand hologram = new ArmorStand(level, item.getX(), item.getY() + HOLOGRAM_Y_OFFSET, item.getZ());
        hologram.setInvisible(true);
        hologram.setNoGravity(true);
        hologram.setCustomNameVisible(true);
        hologram.setSilent(true);
        hologram.setInvulnerable(true);
        setMarkerFlag(hologram);
        hologram.setCustomName(buildHologramName(item));
        hologram.addTag(HOLOGRAM_TAG);
        pendingHolograms.add(hologram);
        level.addFreshEntity(hologram); // fires EntityJoinLevelEvent synchronously
        pendingHolograms.remove(hologram);
        return hologram;
    }

    // setMarker(boolean) is private in ArmorStand with no public equivalent
    private static void setMarkerFlag(ArmorStand stand) {
        try {
            java.lang.reflect.Method m = ArmorStand.class.getDeclaredMethod("setMarker", boolean.class);
            m.setAccessible(true);
            m.invoke(stand, true);
        } catch (ReflectiveOperationException ignored) {
            // Falls back to invulnerable-only — hitbox exists but cannot be destroyed
        }
    }

    private static void discardHologram(UUID itemUuid) {
        ArmorStand h = hologramRefs.remove(itemUuid);
        if (h != null && h.isAlive()) h.discard();
    }

    private static Component buildHologramName(ItemEntity item) {
        ItemStack stack = item.getItem();
        String name = stack.getHoverName().getString();
        int count = stack.getCount();
        boolean showTimer = Settings.getItemDespawnTime() > 0 && !isWhitelisted(stack);

        if (!showTimer) {
            if (count > 1) return Component.literal("§f⬦ " + name + " §7x" + count);
            return Component.literal("§f⬦ " + name);
        }

        Long spawnTime = itemSpawnTimes.get(item.getUUID());
        int remaining = Settings.getItemDespawnTime();
        if (spawnTime != null) {
            long elapsed = (System.currentTimeMillis() - spawnTime) / 1000;
            remaining = Math.max(0, Settings.getItemDespawnTime() - (int) elapsed);
        }

        if (count > 1) {
            return Component.literal("§f⬦ " + name + " §7x" + count + " §7(§e" + remaining + "s§7)");
        }
        return Component.literal("§f⬦ " + name + " §7(§e" + remaining + "s§7)");
    }

    // ========== SWEEP ==========

    private static void handleSweep(MinecraftServer server) {
        int sweepInterval = Settings.getItemSweepInterval();
        if (sweepInterval <= 0) return;

        sweepTickCounter++;

        // Clamp warning threshold to at least 1 to handle sweepWarning >= sweepInterval
        int warningThreshold = Math.max(1, sweepInterval - Settings.getItemSweepWarning());
        if (!sweepWarned && sweepTickCounter >= warningThreshold) {
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
                itemRefs.remove(item.getUUID());
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
        itemRefs.clear();
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
            itemRefs.remove(item.getUUID());
            item.discard();
            itemSpawnTimes.remove(item.getUUID());
            count++;
        }
        return count;
    }
}
