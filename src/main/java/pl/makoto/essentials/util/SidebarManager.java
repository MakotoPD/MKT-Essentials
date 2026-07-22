package pl.makoto.essentials.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;

import java.util.List;

/**
 * Native scoreboard sidebar (side panel), inspired by FlectonePulse's sidebar. Each line's text is
 * carried by the score's own display component (with the number hidden), avoiding the old
 * unique-entry/team-prefix hack. Server-wide (one objective for everyone): {@code {online}}/{@code {max}}
 * are supported, but not per-player placeholders. Disabled by default.
 */
@EventBusSubscriber(modid = MKTEssentials.MODID)
public final class SidebarManager {

    private static final String OBJECTIVE = "mkt_sidebar";
    private static long tickCounter = 0;
    private static boolean active = false;
    private static int lastLineCount = 0;

    private SidebarManager() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        if (!Settings.isSidebarEnabled()) {
            if (active) remove(server);
            return;
        }

        int interval = Math.max(1, Settings.getSidebarInterval());
        if (++tickCounter < interval) return;
        tickCounter = 0;
        if (server.getPlayerList().getPlayers().isEmpty()) return;

        int online = server.getPlayerList().getPlayerCount();
        int max = server.getPlayerList().getMaxPlayers();

        try {
            ServerScoreboard scoreboard = server.getScoreboard();
            Component title = line(Settings.getSidebarTitle(), online, max);
            Objective objective = scoreboard.getObjective(OBJECTIVE);
            if (objective == null) {
                objective = scoreboard.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY, title,
                        ObjectiveCriteria.RenderType.INTEGER, false, null);
            } else {
                objective.setDisplayName(title);
            }
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
            active = true;

            List<String> lines = Settings.getSidebarLines();
            int count = lines.size();
            for (int i = 0; i < count; i++) {
                ScoreAccess score = scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(key(i)), objective);
                score.set(count - i); // higher score = higher in the list
                score.display(line(lines.get(i), online, max));
                score.numberFormatOverride(BlankFormat.INSTANCE); // hide the number
            }
            // Clear leftover lines if the list shrank
            for (int i = count; i < lastLineCount; i++) {
                scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly(key(i)), objective);
            }
            lastLineCount = count;
        } catch (Exception e) {
            MKTEssentials.LOGGER.warn("Sidebar objective failed: {}", e.getMessage());
        }
    }

    private static String key(int index) {
        return "mkt_sb_" + index;
    }

    private static Component line(String text, int online, int max) {
        return MessageUtils.formatBypass(text
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max)));
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
        lastLineCount = 0;
    }
}
