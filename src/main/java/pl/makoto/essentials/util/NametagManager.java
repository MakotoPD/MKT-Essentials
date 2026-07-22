package pl.makoto.essentials.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native above-head nametags via scoreboard teams, replacing the external TAB mod's nametag
 * feature. Each online player gets their own team whose prefix/suffix carry the LuckPerms rank
 * (falling back to nothing when LuckPerms is absent). Teams are cleaned up on quit.
 */
public final class NametagManager {

    private static final Map<UUID, String> TEAM_NAMES = new ConcurrentHashMap<>();
    private static int counter = 0;

    private NametagManager() {}

    /** Creates/updates this player's nametag team from their current prefix/suffix. */
    public static void refresh(ServerPlayer player) {
        if (!Settings.isNametagEnabled() || IntegrationsManager.isTabDeferred()) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        try {
            ServerScoreboard scoreboard = server.getScoreboard();
            String teamName = TEAM_NAMES.computeIfAbsent(player.getUUID(), u -> "mkt" + (counter++ & 0xFFFFF));

            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team == null) team = scoreboard.addPlayerTeam(teamName);

            team.setPlayerPrefix(MessageUtils.format(PlayerListener.getPrefixForTab(player)));
            team.setPlayerSuffix(MessageUtils.format(PlayerListener.getSuffixForTab(player)));
            // Use the name the client renders (nickname when nicked) so the prefix applies above the head
            scoreboard.addPlayerToTeam(NicknameService.teamMemberName(player), team);
        } catch (Exception e) {
            MKTEssentials.LOGGER.warn("Failed to update nametag for {}: {}", player.getScoreboardName(), e.getMessage());
        }
    }

    /** Removes this player's nametag team (call on quit). */
    public static void remove(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        String teamName = TEAM_NAMES.remove(player.getUUID());
        if (teamName == null) return;
        try {
            PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);
            if (team != null) server.getScoreboard().removePlayerTeam(team);
        } catch (Exception ignored) {
            // scoreboard may already be gone during shutdown
        }
    }
}
