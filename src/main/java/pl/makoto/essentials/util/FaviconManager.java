package pl.makoto.essentials.util;

import net.minecraft.network.protocol.status.ServerStatus;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.ConfigManager;
import pl.makoto.essentials.config.Settings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Loads custom server-list icons (favicons) from {@code config/mktessentials/icon/}.
 * The config stores only the file name; drop a 64x64 PNG into that folder. Loads both the normal
 * icon ({@code motd.icon}) and the maintenance icon ({@code maintenance.icon}). Reloaded on startup
 * and on {@code /mkt reload}.
 */
public final class FaviconManager {

    private static Optional<ServerStatus.Favicon> favicon = Optional.empty();
    private static Optional<ServerStatus.Favicon> maintenanceFavicon = Optional.empty();

    private FaviconManager() {}

    /** Normal server-list icon (empty = keep the vanilla server-icon.png). */
    public static Optional<ServerStatus.Favicon> get() {
        return favicon;
    }

    /** Icon shown while maintenance is active (empty = keep the normal icon). */
    public static Optional<ServerStatus.Favicon> getMaintenance() {
        return maintenanceFavicon;
    }

    public static void reload() {
        favicon = load(Settings.getMotdIcon());
        maintenanceFavicon = load(Settings.getMaintenanceIcon());
    }

    private static Optional<ServerStatus.Favicon> load(String fileName) {
        if (fileName == null || fileName.isBlank()) return Optional.empty();
        try {
            Path file = ConfigManager.getIconDir().resolve(fileName);
            if (!Files.exists(file)) {
                MKTEssentials.LOGGER.warn("Server icon '{}' not found in {}", fileName, ConfigManager.getIconDir());
                return Optional.empty();
            }
            byte[] bytes = Files.readAllBytes(file);
            MKTEssentials.LOGGER.info("Loaded server icon '{}' (must be a 64x64 PNG).", fileName);
            return Optional.of(new ServerStatus.Favicon(bytes));
        } catch (Exception e) {
            MKTEssentials.LOGGER.warn("Could not load server icon '{}': {}", fileName, e.getMessage());
            return Optional.empty();
        }
    }
}
