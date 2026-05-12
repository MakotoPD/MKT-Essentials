package pl.makoto.essentials.config;

import java.util.*;

public final class Settings {
    // General
    private static String language = "en_us";
    private static String messagePrefix = "&8[&6MKT&8] &r";
    private static int maxHomes = 3;

    // Teleportation
    private static int teleportDelay = 3;
    private static int teleportCooldown = 10;
    private static boolean teleportEffects = true;
    private static int tpaTimeout = 60;

    // RTP
    private static int rtpMinDistance = 500;
    private static int rtpMaxDistance = 5000;
    private static boolean rtpRelative = true;
    private static double rtpCenterX = 0.0;
    private static double rtpCenterZ = 0.0;
    private static List<String> rtpBiomeBlacklist = List.of("minecraft:ocean", "minecraft:deep_ocean", "minecraft:river");

    // AFK
    private static int afkTimeout = 300;

    // Data
    private static int autoSaveInterval = 300;

    // Vanish
    private static boolean vanishFakeMessages = true;

    // Backup
    private static boolean backupOnDeath = true;
    private static boolean backupOnJoin = false;
    private static boolean backupOnQuit = true;
    private static int backupInterval = 0; // 0 = disabled, seconds between scheduled backups
    private static int maxBackupsPerPlayer = 10;

    // Messages
    private static String chatFormat = "%mktessentials:dot%%mktessentials:prefix%%mktessentials:name%%mktessentials:suffix%&8: &f{message}";
    private static Map<String, String> groupChatFormats = new HashMap<>();
    private static boolean joinQuitEnabled = true;
    private static String joinMessage = "&8[&a+&8] &7%mktessentials:full_name/safe% joined the game.";
    private static String quitMessage = "&8[&c-&8] &7%mktessentials:full_name/safe% left the game.";
    private static int broadcastInterval = 300;
    private static List<String> broadcastMessages = List.of("&7Welcome to our server!", "&7Join our Discord: &b/discord", "&7Use &6/rtp &7to start your adventure!");
    private static String broadcastPrefix = "&8[&bINFO&8] &r";
    private static String broadcastOrder = "random";

    // Commands
    private static Map<String, Boolean> commandToggles = new HashMap<>();

    private Settings() {}

    // Getters
    public static String getLanguage() { return language; }
    public static String getMessagePrefix() { return messagePrefix; }
    public static int getMaxHomes() { return maxHomes; }
    public static int getTeleportDelay() { return teleportDelay; }
    public static int getTeleportCooldown() { return teleportCooldown; }
    public static boolean getTeleportEffects() { return teleportEffects; }
    public static int getTpaTimeout() { return tpaTimeout; }
    public static int getRtpMinDistance() { return rtpMinDistance; }
    public static int getRtpMaxDistance() { return rtpMaxDistance; }
    public static boolean isRtpRelative() { return rtpRelative; }
    public static double getRtpCenterX() { return rtpCenterX; }
    public static double getRtpCenterZ() { return rtpCenterZ; }
    public static List<String> getRtpBiomeBlacklist() { return rtpBiomeBlacklist; }
    public static int getAfkTimeout() { return afkTimeout; }
    public static int getAutoSaveInterval() { return autoSaveInterval; }
    public static boolean isVanishFakeMessages() { return vanishFakeMessages; }
    public static boolean isBackupOnDeath() { return backupOnDeath; }
    public static boolean isBackupOnJoin() { return backupOnJoin; }
    public static boolean isBackupOnQuit() { return backupOnQuit; }
    public static int getBackupInterval() { return backupInterval; }
    public static int getMaxBackupsPerPlayer() { return maxBackupsPerPlayer; }
    public static String getChatFormat() { return chatFormat; }
    public static Map<String, String> getGroupChatFormats() { return groupChatFormats; }
    
    /**
     * Gets the chat format for a specific player based on their LuckPerms primary group.
     * Falls back to the default chat format if no group-specific format is defined.
     */
    public static String getChatFormatForGroup(String group) {
        if (group != null && groupChatFormats.containsKey(group)) {
            return groupChatFormats.get(group);
        }
        return chatFormat;
    }
    public static boolean isJoinQuitEnabled() { return joinQuitEnabled; }
    public static String getJoinMessage() { return joinMessage; }
    public static String getQuitMessage() { return quitMessage; }
    public static int getBroadcastInterval() { return broadcastInterval; }
    public static List<String> getBroadcastMessages() { return broadcastMessages; }
    public static String getBroadcastPrefix() { return broadcastPrefix; }
    public static String getBroadcastOrder() { return broadcastOrder; }

    public static boolean isCommandEnabled(String commandName) {
        return commandToggles.getOrDefault(commandName, true);
    }

    // Loaders (called by ConfigManager)
    @SuppressWarnings("unchecked")
    static void loadSettings(Map<String, Object> map) {
        language = ConfigManager.getNestedValue(map, "language", "en_us");
        messagePrefix = ConfigManager.getNestedValue(map, "general.message-prefix", "&8[&6MKT&8] &r");
        maxHomes = ConfigManager.getNestedValue(map, "general.max-homes", 3);
        teleportDelay = ConfigManager.getNestedValue(map, "teleportation.delay", 3);
        teleportCooldown = ConfigManager.getNestedValue(map, "teleportation.cooldown", 10);
        teleportEffects = ConfigManager.getNestedValue(map, "teleportation.effects", true);
        tpaTimeout = ConfigManager.getNestedValue(map, "teleportation.tpa-timeout", 60);
        rtpMinDistance = ConfigManager.getNestedValue(map, "rtp.min-distance", 500);
        rtpMaxDistance = ConfigManager.getNestedValue(map, "rtp.max-distance", 5000);
        rtpRelative = ConfigManager.getNestedValue(map, "rtp.relative-to-player", true);
        rtpCenterX = ConfigManager.getNestedValue(map, "rtp.center-x", 0.0);
        rtpCenterZ = ConfigManager.getNestedValue(map, "rtp.center-z", 0.0);
        Object blacklist = ConfigManager.getNestedValue(map, "rtp.biome-blacklist", (Object) List.of("minecraft:ocean", "minecraft:deep_ocean", "minecraft:river"));
        if (blacklist instanceof List<?> list) {
            rtpBiomeBlacklist = list.stream().map(Object::toString).toList();
        }
        afkTimeout = ConfigManager.getNestedValue(map, "afk.timeout", 300);
        autoSaveInterval = ConfigManager.getNestedValue(map, "data.auto-save-interval", 300);
        vanishFakeMessages = ConfigManager.getNestedValue(map, "vanish.fake-messages", true);
        backupOnDeath = ConfigManager.getNestedValue(map, "backup.on-death", true);
        backupOnJoin = ConfigManager.getNestedValue(map, "backup.on-join", false);
        backupOnQuit = ConfigManager.getNestedValue(map, "backup.on-quit", true);
        backupInterval = ConfigManager.getNestedValue(map, "backup.interval", 0);
        maxBackupsPerPlayer = ConfigManager.getNestedValue(map, "backup.max-per-player", 10);
    }

    @SuppressWarnings("unchecked")
    static void loadCommands(Map<String, Object> map) {
        Map<String, Boolean> toggles = new HashMap<>();
        for (Map.Entry<String, Object> category : map.entrySet()) {
            if (category.getValue() instanceof Map<?, ?> commands) {
                for (Map.Entry<?, ?> cmd : commands.entrySet()) {
                    String name = cmd.getKey().toString();
                    boolean enabled = cmd.getValue() instanceof Boolean b ? b : true;
                    toggles.put(name, enabled);
                }
            }
        }
        commandToggles = toggles;
    }

    @SuppressWarnings("unchecked")
    static void loadMessages(Map<String, Object> map) {
        chatFormat = ConfigManager.getNestedValue(map, "chat.format", chatFormat);
        
        // Load per-group chat formats
        Object groupFormatsObj = ConfigManager.getNestedValue(map, "chat.group-formats", (Object) null);
        Map<String, String> formats = new HashMap<>();
        if (groupFormatsObj instanceof Map<?, ?> groupMap) {
            for (Map.Entry<?, ?> entry : groupMap.entrySet()) {
                formats.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        groupChatFormats = formats;
        
        joinQuitEnabled = ConfigManager.getNestedValue(map, "join-quit.enabled", true);
        joinMessage = ConfigManager.getNestedValue(map, "join-quit.join-message", joinMessage);
        quitMessage = ConfigManager.getNestedValue(map, "join-quit.quit-message", quitMessage);
        broadcastInterval = ConfigManager.getNestedValue(map, "broadcast.interval", 300);
        broadcastPrefix = ConfigManager.getNestedValue(map, "broadcast.prefix", broadcastPrefix);
        broadcastOrder = ConfigManager.getNestedValue(map, "broadcast.order", "random");
        Object msgs = ConfigManager.getNestedValue(map, "broadcast.messages", (Object) broadcastMessages);
        if (msgs instanceof List<?> list) {
            broadcastMessages = list.stream().map(Object::toString).toList();
        }
    }
}
