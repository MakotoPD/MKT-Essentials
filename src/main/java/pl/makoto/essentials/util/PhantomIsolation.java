package pl.makoto.essentials.util;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.mixin.ChunkMapAccessor;
import pl.makoto.essentials.mixin.TrackedEntityInvoker;

import java.util.EnumSet;
import java.util.List;

/**
 * "Full isolation" phantom shadowban: a phantom player joins into an empty world — they see no other
 * players (neither entities in-world nor tab-list entries) and no other player sees them. Non-player
 * entities (mobs, items) stay visible, so the world doesn't look broken.
 *
 * <p>Continuous enforcement lives in {@code TrackedEntityMixin}, which cancels entity pairing across
 * the phantom boundary so the server never (re)spawns a player entity for the wrong side — this
 * covers movement, dimension changes and newly joining players automatically. This class applies the
 * one-off packet fix-up when the phantom flag toggles (or on join), dropping entities/tab entries
 * that are already on-screen, and reverses it on un-shadowban.
 */
public final class PhantomIsolation {

    private PhantomIsolation() {}

    /**
     * Whether the tracked player entity must be hidden from this viewer because either side is a
     * phantom. Called on the hot entity-tracking path, so it stays allocation-free.
     */
    public static boolean hidden(ServerPlayer viewer, ServerPlayer tracked) {
        if (viewer == null || tracked == null || viewer == tracked) return false;
        return ShadowBanManager.isPhantom(viewer.getUUID()) || ShadowBanManager.isPhantom(tracked.getUUID());
    }

    /** Force-hides every other player from the phantom and the phantom from everyone (entities + tab). */
    public static void apply(ServerPlayer phantom) {
        MinecraftServer server = phantom.getServer();
        if (server == null) return;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == phantom) continue;
            // Entities (no-op across dimensions): drop both directions; the mixin blocks re-pairing.
            trackedRemove(other, phantom);
            trackedRemove(phantom, other);
            // Tab: phantom loses everyone, everyone loses the phantom.
            phantom.connection.send(removeTab(other));
            other.connection.send(removeTab(phantom));
        }
    }

    /**
     * Applies isolation across the phantom boundary for a player that just joined: if the joiner or
     * any online player is a phantom, hide them from each other (tab + already-tracked entities). The
     * {@code TrackedEntityMixin} keeps them hidden thereafter.
     */
    public static void onJoin(ServerPlayer joiner) {
        MinecraftServer server = joiner.getServer();
        if (server == null) return;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == joiner || !hidden(joiner, other)) continue;
            joiner.connection.send(removeTab(other));
            other.connection.send(removeTab(joiner));
            trackedRemove(other, joiner);
            trackedRemove(joiner, other);
        }
    }

    /**
     * Re-removes a phantom from every other player's tab list. Needed after a player-info re-broadcast
     * (e.g. a nick refresh) blindly re-adds the phantom's entry for everyone.
     */
    public static void hideFromOthersTab(ServerPlayer phantom) {
        MinecraftServer server = phantom.getServer();
        if (server == null) return;
        ClientboundPlayerInfoRemovePacket remove = removeTab(phantom);
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == phantom) continue;
            other.connection.send(remove);
        }
    }

    /** Reverses {@link #apply}: re-pairs entities and restores tab entries both directions. */
    public static void reveal(ServerPlayer phantom) {
        MinecraftServer server = phantom.getServer();
        if (server == null) return;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == phantom) continue;
            trackedAdd(other, phantom);
            trackedAdd(phantom, other);
            phantom.connection.send(addTab(other));
            other.connection.send(addTab(phantom));
        }
    }

    // --- entity tracker helpers ----------------------------------------------------------------

    private static TrackedEntityInvoker trackedOf(ServerPlayer p) {
        try {
            ChunkMap chunkMap = p.serverLevel().getChunkSource().chunkMap;
            Object tracked = ((ChunkMapAccessor) (Object) chunkMap).mktessentials$entityMap().get(p.getId());
            return tracked instanceof TrackedEntityInvoker inv ? inv : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Removes {@code tracked}'s entity from {@code viewer}'s view (clears "seen by" + sends removal). */
    private static void trackedRemove(ServerPlayer tracked, ServerPlayer viewer) {
        TrackedEntityInvoker inv = trackedOf(tracked);
        if (inv != null) {
            try {
                inv.mktessentials$removePlayer(viewer);
            } catch (Throwable t) {
                MKTEssentials.LOGGER.warn("Phantom isolation removePlayer failed: {}", t.getMessage());
            }
        }
    }

    /** Re-evaluates tracking of {@code tracked} for {@code viewer} (re-spawns if in range). */
    private static void trackedAdd(ServerPlayer tracked, ServerPlayer viewer) {
        TrackedEntityInvoker inv = trackedOf(tracked);
        if (inv != null) {
            try {
                inv.mktessentials$updatePlayer(viewer);
            } catch (Throwable t) {
                MKTEssentials.LOGGER.warn("Phantom isolation updatePlayer failed: {}", t.getMessage());
            }
        }
    }

    // --- tab-list packet helpers ---------------------------------------------------------------

    private static ClientboundPlayerInfoRemovePacket removeTab(ServerPlayer player) {
        return new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID()));
    }

    private static ClientboundPlayerInfoUpdatePacket addTab(ServerPlayer player) {
        return new ClientboundPlayerInfoUpdatePacket(EnumSet.of(
                Action.ADD_PLAYER, Action.UPDATE_LISTED, Action.UPDATE_LATENCY,
                Action.UPDATE_GAME_MODE, Action.UPDATE_DISPLAY_NAME), List.of(player));
    }
}
