package pl.makoto.essentials.util;

/**
 * Runtime maintenance-mode flag, inspired by FlectonePulse maintenance.
 * When active, only players with {@code mktessentials.maintenance.bypass} may join.
 * The initial state comes from {@code maintenance.enabled-on-start}; the flag is otherwise
 * toggled at runtime with {@code /maintenance on|off} and resets on restart.
 */
public final class MaintenanceManager {

    private static boolean active = false;

    private MaintenanceManager() {}

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}
