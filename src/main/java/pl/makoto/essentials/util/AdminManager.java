package pl.makoto.essentials.util;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages admin vanish state with full tab list hiding.
 * Vanished players are:
 * - Invisible (potion-style invisibility)
 * - Removed from the tab list for non-admin players
 * - Hidden from entity selectors for non-admin players (best-effort)
 */
public class AdminManager {
    private static final Set<UUID> vanished = new HashSet<>();

    /**
     * Toggles vanish for the given player.
     *
     * @return true if the player is now vanished, false if unvanished
     */
    public static boolean toggleVanish(UUID uuid) {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
        if (player == null) return false;

        if (vanished.contains(uuid)) {
            // Unvanish
            vanished.remove(uuid);
            player.setInvisible(false);
            showInTabList(player);
            return false;
        } else {
            // Vanish
            vanished.add(uuid);
            player.setInvisible(true);
            hideFromTabList(player);
            return true;
        }
    }

    public static boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    /**
     * Hides a vanished player from the tab list for all non-admin players.
     * Admins (OP level 2+ or mktessentials.admin.vanish permission) can still see them.
     */
    public static void hideFromTabList(ServerPlayer vanishedPlayer) {
        // Send a remove packet to all players who shouldn't see the vanished player
        ClientboundPlayerInfoRemovePacket removePacket = new ClientboundPlayerInfoRemovePacket(List.of(vanishedPlayer.getUUID()));

        for (ServerPlayer viewer : vanishedPlayer.getServer().getPlayerList().getPlayers()) {
            if (viewer.getUUID().equals(vanishedPlayer.getUUID())) continue; // Don't hide from self
            if (canSeeVanished(viewer)) continue; // Admins can still see

            viewer.connection.send(removePacket);
        }
    }

    /**
     * Shows an unvanished player in the tab list for all players who had them hidden.
     */
    public static void showInTabList(ServerPlayer unvanishedPlayer) {
        // Send an add packet to all players to restore the player in their tab list
        ClientboundPlayerInfoUpdatePacket addPacket = new ClientboundPlayerInfoUpdatePacket(
                ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, unvanishedPlayer
        );
        // Also send display name and listed status
        ClientboundPlayerInfoUpdatePacket listedPacket = new ClientboundPlayerInfoUpdatePacket(
                java.util.EnumSet.of(
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
                ),
                List.of(unvanishedPlayer)
        );

        for (ServerPlayer viewer : unvanishedPlayer.getServer().getPlayerList().getPlayers()) {
            if (viewer.getUUID().equals(unvanishedPlayer.getUUID())) continue;
            if (canSeeVanished(viewer)) continue; // Admins already see them

            viewer.connection.send(addPacket);
            viewer.connection.send(listedPacket);
        }
    }

    /**
     * Called when a new player joins the server.
     * Hides all currently vanished players from the joining player (if they're not an admin).
     */
    public static void hideVanishedFromJoiningPlayer(ServerPlayer joiningPlayer) {
        if (canSeeVanished(joiningPlayer)) return; // Admins see everyone

        for (UUID vanishedUuid : vanished) {
            ServerPlayer vanishedPlayer = joiningPlayer.getServer().getPlayerList().getPlayer(vanishedUuid);
            if (vanishedPlayer == null) continue;

            ClientboundPlayerInfoRemovePacket removePacket = new ClientboundPlayerInfoRemovePacket(List.of(vanishedUuid));
            joiningPlayer.connection.send(removePacket);
        }
    }

    /**
     * Checks if a player has permission to see vanished players.
     */
    public static boolean canSeeVanished(ServerPlayer player) {
        return Permissions.hasPermission(player, "mktessentials.admin.vanish", 2);
    }

    /**
     * Cleans up vanish state when a player disconnects.
     */
    public static void cleanupOnDisconnect(UUID uuid) {
        vanished.remove(uuid);
    }
}
