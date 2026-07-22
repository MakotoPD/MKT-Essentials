package pl.makoto.essentials.util;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.data.DataManager;
import pl.makoto.essentials.mixin.ChunkMapAccessor;
import pl.makoto.essentials.mixin.TrackedEntityInvoker;

/**
 * Central resolver for how a nicked player is presented in the player-info packet, so a nickname
 * shows in BOTH the tab list and above the head — the way FlectonePulse does it (there via
 * packetevents, here via a mixin on {@code ClientboundPlayerInfoUpdatePacket.Entry}).
 * <ul>
 *   <li><b>Tab list</b> uses the entry's display name (a Component) → colored full name.</li>
 *   <li><b>Above the head</b> uses the entry's GameProfile name (plain String, max 16) → the client
 *       renders it as the entity name tag.</li>
 * </ul>
 */
public final class NicknameService {

    private NicknameService() {}

    /** GameProfile to advertise: a nick-named copy (keeping skin) when nicked, else the original. */
    public static GameProfile profile(ServerPlayer player) {
        String nick = visibleNick(player);
        if (nick == null) return player.getGameProfile();

        GameProfile original = player.getGameProfile();
        GameProfile nicked = new GameProfile(original.getId(), nick);
        // Copy properties (skin/cape textures) so the appearance is unchanged
        nicked.getProperties().putAll(original.getProperties());
        return nicked;
    }

    /** Tab-list display name: the colored full display name when nicked, else the vanilla value. */
    public static Component displayName(ServerPlayer player) {
        if (visibleNick(player) == null) return player.getTabListDisplayName();
        return MessageUtils.format(player, "%mktessentials:full_name%");
    }

    /**
     * Forces the player's entity to be removed and re-spawned for every observer. An already-rendered
     * entity caches its GameProfile, so this is required after a nick change for the nickname to
     * appear above the head (the re-spawn reads the freshly-sent, rewritten player-info entry).
     */
    public static void respawnEntity(ServerPlayer player) {
        try {
            ServerLevel level = player.serverLevel();
            ChunkMap chunkMap = level.getChunkSource().chunkMap;
            Object tracked = ((ChunkMapAccessor) (Object) chunkMap).mktessentials$entityMap().get(player.getId());
            if (!(tracked instanceof TrackedEntityInvoker invoker)) return;

            for (ServerPlayer viewer : level.players()) {
                if (viewer == player) continue;
                invoker.mktessentials$removePlayer(viewer); // drop the entity + clear "seen by"
                invoker.mktessentials$updatePlayer(viewer);  // re-spawn with the updated profile
            }
        } catch (Throwable t) {
            MKTEssentials.LOGGER.warn("Nickname entity re-spawn failed: {}", t.getMessage());
        }
    }

    /**
     * The name the client uses to render this player's entity (the nickname when nicked, else the
     * real name). Scoreboard team membership must use this so the rank prefix/suffix still applies
     * above a nicked player's head.
     */
    public static String teamMemberName(ServerPlayer player) {
        String nick = visibleNick(player);
        return nick != null ? nick : player.getScoreboardName();
    }

    /** Plain, ≤16-char nickname suitable for a GameProfile name, or null when not nicked. */
    private static String visibleNick(ServerPlayer player) {
        String nickname = DataManager.getPlayerData(player.getUUID()).getNickname();
        if (nickname == null || nickname.isBlank()) return null;

        // Stored as MiniMessage → strip to visible text, then drop any stray legacy codes
        String plain = MiniMessageParser.parse(nickname).getString().replaceAll("(?i)[§&][0-9a-fk-or]", "");
        if (plain.isBlank()) return null;
        return plain.length() > 16 ? plain.substring(0, 16) : plain;
    }
}
