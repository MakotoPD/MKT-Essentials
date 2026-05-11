package pl.makoto.essentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AdminManager {
    private static final java.util.Set<java.util.UUID> vanished = new java.util.HashSet<>();

    public static boolean toggleVanish(java.util.UUID uuid) {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
        if (player == null) return false;

        if (vanished.contains(uuid)) {
            vanished.remove(uuid);
            player.setInvisible(false);
            PlayerListener.refreshNickname(player);
            return false;
        } else {
            vanished.add(uuid);
            player.setInvisible(true);
            PlayerListener.refreshNickname(player);
            return true;
        }
    }

    public static boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }
}
