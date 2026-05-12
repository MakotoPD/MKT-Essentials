package pl.makoto.essentials.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.data.InventoryBackup;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@EventBusSubscriber(modid = MKTEssentials.MODID)
public class BackupManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path backupsDir;
    private static long lastScheduledBackupTick = 0;
    private static MinecraftServer serverInstance;
    private static final DateTimeFormatter FILE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());

    public static void init(MinecraftServer server) {
        serverInstance = server;
        backupsDir = server.getWorldPath(LevelResource.ROOT).resolve("mktessentials").resolve("backups");
        try {
            Files.createDirectories(backupsDir);
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to create backups directory", e);
        }
    }

    /**
     * Create a backup of a player's inventory.
     * @return the filename of the created backup, or null on failure
     */
    public static String createBackup(ServerPlayer player, String reason, String note) {
        long timestamp = System.currentTimeMillis();
        InventoryBackup backup = buildBackup(player, timestamp, reason, note);

        String filename = timestamp + "_" + reason + ".json";
        Path playerDir = backupsDir.resolve(player.getUUID().toString());
        try {
            Files.createDirectories(playerDir);
            Path file = playerDir.resolve(filename);
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(backup, writer);
            }
            cleanupOldBackups(player.getUUID());
            return filename;
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to create backup for " + player.getScoreboardName(), e);
            return null;
        }
    }

    /**
     * Create a death backup with coordinates.
     * @return the filename of the created backup, or null on failure
     */
    public static String createDeathBackup(ServerPlayer player, String deathMessage) {
        long timestamp = System.currentTimeMillis();
        InventoryBackup backup = buildBackup(player, timestamp, "death", deathMessage);

        // Add death coordinates
        backup.setDeathX(player.getX());
        backup.setDeathY(player.getY());
        backup.setDeathZ(player.getZ());
        backup.setDeathDimension(player.level().dimension().location().toString());

        String filename = timestamp + "_death.json";
        Path playerDir = backupsDir.resolve(player.getUUID().toString());
        try {
            Files.createDirectories(playerDir);
            Path file = playerDir.resolve(filename);
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(backup, writer);
            }
            cleanupOldBackups(player.getUUID());
            return filename;
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to create death backup for " + player.getScoreboardName(), e);
            return null;
        }
    }

    /**
     * List all backups for a player, sorted newest first.
     */
    public static List<BackupInfo> listBackups(UUID playerUuid) {
        Path playerDir = backupsDir.resolve(playerUuid.toString());
        if (!Files.isDirectory(playerDir)) return List.of();

        List<BackupInfo> results = new ArrayList<>();
        try (Stream<Path> files = Files.list(playerDir)) {
            files.filter(p -> p.toString().endsWith(".json"))
                 .forEach(p -> {
                     String filename = p.getFileName().toString();
                     try {
                         // Parse basic info from file without loading full backup
                         InventoryBackup backup = loadBackupFromPath(p);
                         if (backup != null) {
                             results.add(new BackupInfo(
                                     filename,
                                     backup.getTimestamp(),
                                     backup.getReason(),
                                     backup.getNote(),
                                     backup.getTotalItems()
                             ));
                         }
                     } catch (Exception e) {
                         MKTEssentials.LOGGER.debug("Failed to parse backup info: {}", filename, e);
                     }
                 });
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to list backups for " + playerUuid, e);
        }

        // Sort newest first
        results.sort((a, b) -> Long.compare(b.timestamp(), a.timestamp()));
        return results;
    }

    /**
     * Load a specific backup.
     */
    public static InventoryBackup loadBackup(UUID playerUuid, String filename) {
        Path file = backupsDir.resolve(playerUuid.toString()).resolve(filename);
        return loadBackupFromPath(file);
    }

    /**
     * Restore a backup to a player.
     * @return true if successful
     */
    public static boolean restoreBackup(ServerPlayer target, InventoryBackup backup) {
        if (backup == null || serverInstance == null) return false;

        try {
            // Clear current inventory
            target.getInventory().clearContent();

            // Restore main inventory (36 slots)
            List<String> inventory = backup.getInventory();
            if (inventory != null) {
                for (int i = 0; i < Math.min(inventory.size(), 36); i++) {
                    ItemStack stack = deserializeItem(inventory.get(i));
                    target.getInventory().setItem(i, stack);
                }
            }

            // Restore armor (4 slots)
            List<String> armor = backup.getArmor();
            if (armor != null) {
                for (int i = 0; i < Math.min(armor.size(), 4); i++) {
                    ItemStack stack = deserializeItem(armor.get(i));
                    target.getInventory().armor.set(i, stack);
                }
            }

            // Restore offhand
            String offhand = backup.getOffhand();
            if (offhand != null) {
                target.getInventory().offhand.set(0, deserializeItem(offhand));
            }

            // Restore experience level
            target.experienceLevel = backup.getExperienceLevel();
            target.experienceProgress = 0;
            target.totalExperience = 0;

            // Restore Curios items if available
            restoreCurios(target, backup.getCurios());

            return true;
        } catch (Exception e) {
            MKTEssentials.LOGGER.error("Failed to restore backup for " + target.getScoreboardName(), e);
            return false;
        }
    }

    /**
     * Delete a backup file.
     * @return true if deleted
     */
    public static boolean deleteBackup(UUID playerUuid, String filename) {
        Path file = backupsDir.resolve(playerUuid.toString()).resolve(filename);
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            MKTEssentials.LOGGER.error("Failed to delete backup: " + filename, e);
            return false;
        }
    }

    // --- Event Handlers ---

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int interval = Settings.getBackupInterval();
        if (interval <= 0 || serverInstance == null) return;

        lastScheduledBackupTick++;
        if (lastScheduledBackupTick >= interval * 20L) {
            lastScheduledBackupTick = 0;
            for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
                createBackup(player, "scheduled", null);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!Settings.isBackupOnDeath()) return;
        createDeathBackup(player, event.getSource().getLocalizedDeathMessage(player).getString());
    }

    @SubscribeEvent
    public static void onPlayerJoin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!Settings.isBackupOnJoin()) return;
        createBackup(player, "join", "Backup on login");
    }

    @SubscribeEvent
    public static void onPlayerQuit(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!Settings.isBackupOnQuit()) return;
        createBackup(player, "quit", "Backup on logout");
    }

    // --- Internal Helpers ---

    private static InventoryBackup buildBackup(ServerPlayer player, long timestamp, String reason, String note) {
        List<String> inventory = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            inventory.add(serializeItem(player.getInventory().getItem(i)));
        }

        List<String> armor = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            armor.add(serializeItem(player.getInventory().getArmor(i)));
        }

        String offhand = serializeItem(player.getInventory().offhand.get(0));

        // Count non-empty items
        int totalItems = 0;
        for (String s : inventory) { if (!s.isEmpty()) totalItems++; }
        for (String s : armor) { if (!s.isEmpty()) totalItems++; }
        if (!offhand.isEmpty()) totalItems++;

        InventoryBackup backup = new InventoryBackup(timestamp, reason, note,
                player.experienceLevel, totalItems, inventory, armor, offhand);

        // Backup Curios items if available
        backup.setCurios(serializeCurios(player));

        return backup;
    }

    private static InventoryBackup loadBackupFromPath(Path file) {
        if (!Files.exists(file)) return null;
        try (Reader reader = Files.newBufferedReader(file)) {
            return GSON.fromJson(reader, InventoryBackup.class);
        } catch (Exception e) {
            MKTEssentials.LOGGER.debug("Failed to load backup: {}", file, e);
            return null;
        }
    }

    private static void cleanupOldBackups(UUID playerUuid) {
        int max = Settings.getMaxBackupsPerPlayer();
        if (max <= 0) return;

        List<BackupInfo> backups = listBackups(playerUuid);
        if (backups.size() <= max) return;

        // Delete oldest backups beyond the limit
        List<BackupInfo> toDelete = backups.subList(max, backups.size());
        for (BackupInfo info : toDelete) {
            deleteBackup(playerUuid, info.filename());
        }
    }

    private static String serializeItem(ItemStack stack) {
        if (stack.isEmpty()) return "";
        CompoundTag tag = (CompoundTag) stack.save(serverInstance.registryAccess());
        return tag.toString();
    }

    private static ItemStack deserializeItem(String nbtString) {
        if (nbtString == null || nbtString.isEmpty()) return ItemStack.EMPTY;
        try {
            CompoundTag tag = TagParser.parseTag(nbtString);
            return ItemStack.parse(serverInstance.registryAccess(), tag).orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            MKTEssentials.LOGGER.debug("Failed to deserialize item: {}", nbtString, e);
            return ItemStack.EMPTY;
        }
    }

    /**
     * Format a timestamp for display.
     */
    public static String formatTimestamp(long timestamp) {
        return FILE_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }

    // --- Curios Integration (reflection-based) ---

    private static List<InventoryBackup.CuriosBackupEntry> serializeCurios(ServerPlayer player) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) return null;

        List<InventoryBackup.CuriosBackupEntry> entries = new ArrayList<>();
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            var getCuriosInv = curiosApiClass.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
            Object optionalHandler = getCuriosInv.invoke(null, player);

            var isPresentMethod = optionalHandler.getClass().getMethod("isPresent");
            if (!(boolean) isPresentMethod.invoke(optionalHandler)) return null;

            var getMethod = optionalHandler.getClass().getMethod("get");
            Object handler = getMethod.invoke(optionalHandler);

            var getCuriosMethod = handler.getClass().getMethod("getCurios");
            @SuppressWarnings("unchecked")
            var curiosMap = (java.util.Map<String, Object>) getCuriosMethod.invoke(handler);

            for (var entry : curiosMap.entrySet()) {
                String slotType = entry.getKey();
                Object stacksHandler = entry.getValue();

                var getStacksMethod = stacksHandler.getClass().getMethod("getStacks");
                Object dynamicHandler = getStacksMethod.invoke(stacksHandler);

                var getSlotsMethod = dynamicHandler.getClass().getMethod("getSlots");
                int slots = (int) getSlotsMethod.invoke(dynamicHandler);

                for (int i = 0; i < slots; i++) {
                    var getStackMethod = dynamicHandler.getClass().getMethod("getStackInSlot", int.class);
                    ItemStack stack = (ItemStack) getStackMethod.invoke(dynamicHandler, i);
                    if (!stack.isEmpty()) {
                        entries.add(new InventoryBackup.CuriosBackupEntry(slotType, i, serializeItem(stack)));
                    }
                }
            }
        } catch (Exception e) {
            MKTEssentials.LOGGER.debug("Curios backup: Could not serialize curios items", e);
            return null;
        }
        return entries.isEmpty() ? null : entries;
    }

    private static void restoreCurios(ServerPlayer player, List<InventoryBackup.CuriosBackupEntry> curios) {
        if (curios == null || curios.isEmpty()) return;
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) return;

        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            var getCuriosInv = curiosApiClass.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
            Object optionalHandler = getCuriosInv.invoke(null, player);

            var isPresentMethod = optionalHandler.getClass().getMethod("isPresent");
            if (!(boolean) isPresentMethod.invoke(optionalHandler)) return;

            var getMethod = optionalHandler.getClass().getMethod("get");
            Object handler = getMethod.invoke(optionalHandler);

            var getCuriosMethod = handler.getClass().getMethod("getCurios");
            @SuppressWarnings("unchecked")
            var curiosMap = (java.util.Map<String, Object>) getCuriosMethod.invoke(handler);

            for (InventoryBackup.CuriosBackupEntry entry : curios) {
                Object stacksHandler = curiosMap.get(entry.getSlotType());
                if (stacksHandler == null) continue;

                var getStacksMethod = stacksHandler.getClass().getMethod("getStacks");
                Object dynamicHandler = getStacksMethod.invoke(stacksHandler);

                ItemStack stack = deserializeItem(entry.getItemNbt());
                var setStackMethod = dynamicHandler.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
                setStackMethod.invoke(dynamicHandler, entry.getSlotIndex(), stack);
            }
        } catch (Exception e) {
            MKTEssentials.LOGGER.debug("Curios restore: Could not restore curios items", e);
        }
    }

    /**
     * Info record for listing backups without loading full data.
     */
    public record BackupInfo(String filename, long timestamp, String reason, String note, int totalItems) {}
}
