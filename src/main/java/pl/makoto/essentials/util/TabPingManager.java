package pl.makoto.essentials.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;

/**
 * Shows each player's ping as a number next to their name in the tab list, via a vanilla
 * scoreboard objective in the LIST display slot. This is the "tabname" component FlectonePulse
 * renders through packetevents — done here with the native scoreboard API. Disabled by default.
 */
@EventBusSubscriber(modid = MKTEssentials.MODID)
public final class TabPingManager {

    private static final String OBJECTIVE = "mkt_ping";
    private static long tickCounter = 0;
    private static boolean active = false;

    private TabPingManager() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        if (!Settings.isTablistPingEnabled() || IntegrationsManager.isTabDeferred()) {
            if (active) remove(server);
            return;
        }

        int interval = Math.max(1, Settings.getTablistPingInterval());
        if (++tickCounter < interval) return;
        tickCounter = 0;

        try {
            ServerScoreboard scoreboard = server.getScoreboard();
            Objective objective = scoreboard.getObjective(OBJECTIVE);
            if (objective == null) {
                objective = scoreboard.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY,
                        Component.literal("ping"), ObjectiveCriteria.RenderType.INTEGER, false, null);
            }
            scoreboard.setDisplayObjective(DisplaySlot.LIST, objective);
            active = true;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(player.getScoreboardName()), objective)
                        .set(player.connection.latency());
            }
        } catch (Exception e) {
            MKTEssentials.LOGGER.warn("Tab ping objective failed: {}", e.getMessage());
        }
    }

    private static void remove(MinecraftServer server) {
        try {
            ServerScoreboard scoreboard = server.getScoreboard();
            Objective objective = scoreboard.getObjective(OBJECTIVE);
            if (objective != null) scoreboard.removeObjective(objective);
        } catch (Exception ignored) {
            // scoreboard may be gone during shutdown
        }
        active = false;
    }
}
