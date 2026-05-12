package pl.makoto.essentials;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- GENERAL SETTINGS ---
    public static final ModConfigSpec.ConfigValue<String> MESSAGE_PREFIX;
    public static final ModConfigSpec.IntValue MAX_HOMES;

    // --- TELEPORTATION ---
    public static final ModConfigSpec.IntValue TELEPORT_DELAY;
    public static final ModConfigSpec.IntValue TELEPORT_COOLDOWN;
    public static final ModConfigSpec.BooleanValue TELEPORT_EFFECTS;
    public static final ModConfigSpec.IntValue TPA_TIMEOUT;
    
    // RTP Settings
    public static final ModConfigSpec.IntValue RTP_MIN_DISTANCE;
    public static final ModConfigSpec.IntValue RTP_MAX_DISTANCE;
    public static final ModConfigSpec.BooleanValue RTP_RELATIVE;
    public static final ModConfigSpec.DoubleValue RTP_CENTER_X;
    public static final ModConfigSpec.DoubleValue RTP_CENTER_Z;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RTP_BIOME_BLACKLIST;

    // --- CHAT & IDENTITY ---
    public static final ModConfigSpec.ConfigValue<String> CHAT_FORMAT;
    public static final ModConfigSpec.BooleanValue JOIN_QUIT_MESSAGES;
    public static final ModConfigSpec.ConfigValue<String> JOIN_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> QUIT_MESSAGE;

    // --- BROADCASTS ---
    public static final ModConfigSpec.IntValue BROADCAST_INTERVAL;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BROADCAST_MESSAGES;
    public static final ModConfigSpec.ConfigValue<String> BROADCAST_PREFIX;
    public static final ModConfigSpec.ConfigValue<String> BROADCAST_ORDER;

    // --- DATA ---
    public static final ModConfigSpec.IntValue AUTO_SAVE_INTERVAL;

    // --- ADMIN FEATURES ---
    public static final ModConfigSpec.BooleanValue VANISH_FAKE_MESSAGES;

    static {
        BUILDER.push("General");
        MESSAGE_PREFIX = BUILDER
                .comment("Prefix for all messages from the mod (supports & color codes)")
                .define("messagePrefix", "&8[&6MKT&8] &r");
        
        MAX_HOMES = BUILDER
                .comment("Default maximum number of homes per player (LuckPerms can override with mktessentials.max_homes.<number>)")
                .defineInRange("maxHomes", 3, 1, 100);
        BUILDER.pop();

        BUILDER.push("Teleportation");
        TELEPORT_DELAY = BUILDER
                .comment("Delay in seconds before teleportation (0 to disable)")
                .defineInRange("teleportDelay", 3, 0, 60);

        TELEPORT_COOLDOWN = BUILDER
                .comment("Default cooldown between teleports in seconds (0 to disable)")
                .defineInRange("teleportCooldown", 10, 0, 3600);

        TELEPORT_EFFECTS = BUILDER
                .comment("Enable sound and particles on teleportation")
                .define("teleportEffects", true);

        TPA_TIMEOUT = BUILDER
                .comment("Time in seconds before a TPA request expires")
                .defineInRange("tpaTimeout", 60, 10, 300);

        BUILDER.push("RTP_Settings");
        RTP_MIN_DISTANCE = BUILDER
                .comment("Minimum distance for Random Teleport")
                .defineInRange("rtpMinDistance", 500, 0, 1000000);

        RTP_MAX_DISTANCE = BUILDER
                .comment("Maximum distance for Random Teleport")
                .defineInRange("rtpMaxDistance", 5000, 100, 1000000);

        RTP_RELATIVE = BUILDER
                .comment("If true, RTP distance is relative to player. If false, relative to center X/Z.")
                .define("rtpRelative", true);

        RTP_CENTER_X = BUILDER
                .comment("Center X coordinate for RTP (if rtpRelative is false)")
                .defineInRange("rtpCenterX", 0.0, -30000000.0, 30000000.0);

        RTP_CENTER_Z = BUILDER
                .comment("Center Z coordinate for RTP (if rtpRelative is false)")
                .defineInRange("rtpCenterZ", 0.0, -30000000.0, 30000000.0);

        RTP_BIOME_BLACKLIST = BUILDER
                .comment("Biomes to exclude from RTP")
                .defineList("rtpBiomeBlacklist", List.of("minecraft:ocean", "minecraft:deep_ocean", "minecraft:river"), o -> o instanceof String);
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.push("Chat_and_Identity");
        CHAT_FORMAT = BUILDER
                .comment("Chat format. Text Placeholder API tokens: %mktessentials:dot%, %mktessentials:prefix%, %mktessentials:name%, %mktessentials:suffix%. Keep {message} for the chat message.")
                .define("chatFormat", "%mktessentials:dot%%mktessentials:prefix%%mktessentials:name%%mktessentials:suffix%&8: &f{message}");

        JOIN_QUIT_MESSAGES = BUILDER
                .comment("Enable custom join/quit messages")
                .define("customJoinQuit", true);

        JOIN_MESSAGE = BUILDER
                .comment("Custom join message. Use %mktessentials:full_name/safe% for the player name.")
                .define("joinMessage", "&8[&a+&8] &7%mktessentials:full_name/safe% joined the game.");

        QUIT_MESSAGE = BUILDER
                .comment("Custom quit message. Use %mktessentials:full_name/safe% for the player name.")
                .define("quitMessage", "&8[&c-&8] &7%mktessentials:full_name/safe% left the game.");
        BUILDER.pop();

        BUILDER.push("Broadcasts");
        BROADCAST_INTERVAL = BUILDER
                .comment("Interval in seconds between automated broadcasts")
                .defineInRange("broadcastInterval", 300, 10, 36000);

        BROADCAST_MESSAGES = BUILDER
                .comment("List of messages to broadcast periodically")
                .defineList("broadcastMessages", List.of(
                        "&7Welcome to our server!",
                        "&7Join our Discord: &b/discord",
                        "&7Use &6/rtp &7to start your adventure!"
                ), o -> o instanceof String);

        BROADCAST_PREFIX = BUILDER
                .comment("Prefix for broadcast messages")
                .define("broadcastPrefix", "&8[&bINFO&8] &r");

        BROADCAST_ORDER = BUILDER
                .comment("Order of broadcast messages: 'random' or 'sequential'")
                .define("broadcastOrder", "random");
        BUILDER.pop();

        BUILDER.push("Data");
        AUTO_SAVE_INTERVAL = BUILDER
                .comment("Interval in seconds between automatic player data saves (0 to disable)")
                .defineInRange("autoSaveInterval", 300, 0, 3600);
        BUILDER.pop();

        BUILDER.push("Admin_Features");
        VANISH_FAKE_MESSAGES = BUILDER
                .comment("Send fake join/quit messages when admins vanish/unvanish")
                .define("vanishFakeMessages", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
