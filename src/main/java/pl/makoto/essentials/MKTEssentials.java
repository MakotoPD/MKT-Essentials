package pl.makoto.essentials;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;

import pl.makoto.essentials.config.ConfigManager;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.auth.AuthManager;
import pl.makoto.essentials.auth.AuthMode;
import pl.makoto.essentials.util.BanManager;
import pl.makoto.essentials.util.BackupManager;
import pl.makoto.essentials.util.ItemCleanerManager;
import pl.makoto.essentials.util.LuckPermsHook;
import pl.makoto.essentials.util.Permissions;
import pl.makoto.essentials.util.ShadowBanManager;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.commands.*;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import pl.makoto.essentials.util.MKTPlaceholders;
import pl.makoto.essentials.util.TABHook;

@Mod(MKTEssentials.MODID)
public class MKTEssentials {
    public static final String MODID = "mktessentials";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static MinecraftServer server;

    public MKTEssentials(IEventBus modEventBus, ModContainer modContainer) {
        // Register only once to the server event bus
        NeoForge.EVENT_BUS.register(this);
    }

    public static MinecraftServer getServer() {
        return server;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        
        // Ensure config is loaded before command registration
        // (RegisterCommandsEvent fires before ServerStartingEvent)
        if (!ConfigManager.isInitialized()) {
            ConfigManager.init();
        }
        
        LOGGER.info("Registering MKT Essentials commands...");

        if (Settings.isCommandEnabled("home")) HomeCommands.register(dispatcher);
        if (Settings.isCommandEnabled("warp")) WarpCommands.register(dispatcher);
        if (Settings.isCommandEnabled("spawn") || Settings.isCommandEnabled("back") || Settings.isCommandEnabled("top") || Settings.isCommandEnabled("rtp")) MiscTeleportCommands.register(dispatcher);
        if (Settings.isCommandEnabled("tpa")) TpaCommands.register(dispatcher);
        if (Settings.isCommandEnabled("msg")) MessagingCommands.register(dispatcher);
        if (Settings.isCommandEnabled("heal") || Settings.isCommandEnabled("fly") || Settings.isCommandEnabled("god")) AdminCommands.register(dispatcher);
        if (Settings.isCommandEnabled("kit")) KitCommands.register(dispatcher);
        MKTCommand.register(dispatcher); // Always register /mkt
        if (Settings.isCommandEnabled("mute")) MuteCommands.register(dispatcher);
        MiscCommands.register(dispatcher); // help etc
        TimeWeatherCommands.register(dispatcher);
        if (Settings.isCommandEnabled("ban")) BanCommands.register(dispatcher);
        if (Settings.isCommandEnabled("kick")) KickCommand.register(dispatcher);
        if (Settings.isCommandEnabled("repair")) RepairCommand.register(dispatcher);
        if (Settings.isCommandEnabled("enchant")) EnchantCommand.register(dispatcher);
        if (Settings.isCommandEnabled("backup")) BackupCommands.register(dispatcher);
        if (Settings.getAuthMode() != AuthMode.DISABLED) AuthCommands.register(dispatcher);
        if (Settings.isCommandEnabled("shadowban")) ShadowBanCommands.register(dispatcher);
        if (Settings.isCommandEnabled("shortcuts")) ShortcutCommands.register(dispatcher);
        if (Settings.isCommandEnabled("clearitems")) ClearItemsCommand.register(dispatcher);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("MKT Essentials starting...");
        server = event.getServer();
        ConfigManager.init();
        DataManager.init(server);
        BanManager.init(server);
        ShadowBanManager.init(server);
        BackupManager.init(server);
        AuthManager.init(server);
        LuckPermsHook.init();
        Permissions.init();
        try {
            MKTPlaceholders.register();
        } catch (NoClassDefFoundError e) {
            LOGGER.info("Text Placeholder API not found — placeholders disabled.");
        }

        // Clean up orphaned hologram ArmorStands from previous sessions
        ItemCleanerManager.cleanupOrphanedHolograms(server);

        // Log loaded integrations
        logIntegrations();
    }

    @SubscribeEvent
    public void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        // TAB must be initialized after all mods are fully loaded
        TABHook.init();
        if (TABHook.isPresent()) {
            LOGGER.info("  ✓ TAB — Tab list placeholders active");
        }
        LOGGER.info("MKT Essentials fully loaded.");
    }

    private void logIntegrations() {
        LOGGER.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        LOGGER.info("  MKT Essentials v1.0.0 — Integrations");
        LOGGER.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        LOGGER.info("  ✓ Config — YAML (config/mktessentials/)");
        LOGGER.info("  ✓ Language — {} loaded", Settings.getLanguage());

        if (net.neoforged.fml.ModList.get().isLoaded("luckperms")) {
            LOGGER.info("  ✓ LuckPerms — Permissions & chat prefixes");
        } else {
            LOGGER.info("  ✗ LuckPerms — Not found (using vanilla OP)");
        }

        try {
            Class.forName("eu.pb4.placeholders.api.Placeholders");
            LOGGER.info("  ✓ Placeholder API — Placeholders active");
        } catch (ClassNotFoundException e) {
            LOGGER.info("  ✗ Placeholder API — Not found");
        }

        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            LOGGER.info("  ✓ Curios API — /invsee shows curios slots");
        }

        if (Settings.getAuthMode() != AuthMode.DISABLED) {
            LOGGER.info("  ✓ Auth System — Mode: {}", Settings.getAuthMode().name().toLowerCase());
            if (Settings.isDiscordEnabled()) {
                LOGGER.info("  ✓ Discord Bot — Connecting...");
            }
        } else {
            LOGGER.info("  ✗ Auth System — Disabled");
        }

        LOGGER.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        AuthManager.shutdown();
    }
}
