package pl.makoto.essentials.util;

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
 * A number under each player's name in the world (health or ping), via a vanilla scoreboard
 * objective in the BELOW_NAME slot — FlectonePulse's scoreboard "belowname". Disabled by default.
 */
@EventBusSubscriber(modid = MKTEssentials.MODID)
public final class BelowNameManager {

    private static final String OBJECTIVE = "mkt_belowname";
    private static long tickCounter = 0;
    private static boolean active = false;

    private BelowNameManager() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        if (!Settings.isBelowNameEnabled()) {
            if (active) remove(server);
            return;
        }

        int interval = Math.max(1, Settings.getBelowNameInterval());
        if (++tickCounter < interval) return;
        tickCounter = 0;

        try {
            ServerScoreboard scoreboard = server.getScoreboard();
            Objective objective = scoreboard.getObjective(OBJECTIVE);
            if (objective == null) {
                objective = scoreboard.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY,
                        MessageUtils.formatBypass(Settings.getBelowNameSuffix()),
                        ObjectiveCriteria.RenderType.INTEGER, false, null);
            }
            scoreboard.setDisplayObjective(DisplaySlot.BELOW_NAME, objective);
            active = true;

            boolean ping = "ping".equalsIgnoreCase(Settings.getBelowNameType());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                int value = ping ? player.connection.latency() : (int) Math.ceil(player.getHealth());
                // Key by the name the client renders (nickname when nicked) so it shows under the head
                scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(NicknameService.teamMemberName(player)), objective)
                        .set(value);
            }
        } catch (Exception e) {
            MKTEssentials.LOGGER.warn("Below-name objective failed: {}", e.getMessage());
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
