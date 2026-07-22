package pl.makoto.essentials.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;

import java.util.List;

/**
 * Native tab list header/footer, replacing the external TAB mod integration.
 * Refreshed on a timer and immediately on join. Supports MKT placeholders plus {@code {online}}
 * and {@code {max}}.
 */
@EventBusSubscriber(modid = MKTEssentials.MODID)
public final class TabListManager {

    private static long tickCounter = 0;
    private static int frameIndex = 0;

    private TabListManager() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!Settings.isTablistEnabled() || IntegrationsManager.isTabDeferred()) return;

        boolean animate = isAnimating();
        int interval = Math.max(1, animate ? Settings.getTablistAnimationInterval() : Settings.getTablistUpdateInterval());
        if (++tickCounter < interval) return;
        tickCounter = 0;
        if (animate) frameIndex++;

        MinecraftServer server = event.getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        int online = players.size();
        int max = server.getPlayerList().getMaxPlayers();
        for (ServerPlayer player : players) {
            send(player, online, max);
        }
    }

    /** Immediately refreshes the tab list header/footer for a single player. */
    public static void refresh(ServerPlayer player) {
        if (!Settings.isTablistEnabled() || IntegrationsManager.isTabDeferred()) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        send(player, server.getPlayerList().getPlayerCount(), server.getPlayerList().getMaxPlayers());
    }

    private static boolean isAnimating() {
        return Settings.isTablistAnimate()
                && (!Settings.getTablistHeaderFrames().isEmpty() || !Settings.getTablistFooterFrames().isEmpty());
    }

    private static void send(ServerPlayer player, int online, int max) {
        Component header = build(player, currentLines(Settings.getTablistHeaderFrames(), Settings.getTablistHeader()), online, max);
        Component footer = build(player, currentLines(Settings.getTablistFooterFrames(), Settings.getTablistFooter()), online, max);
        player.connection.send(new ClientboundTabListPacket(header, footer));
    }

    /** The lines to show now: the current animation frame if animating with frames, else the static lines. */
    private static List<String> currentLines(List<List<String>> frames, List<String> staticLines) {
        if (isAnimating() && !frames.isEmpty()) {
            return frames.get(Math.floorMod(frameIndex, frames.size()));
        }
        return staticLines;
    }

    private static Component build(ServerPlayer player, List<String> lines, int online, int max) {
        if (lines == null || lines.isEmpty()) return Component.empty();
        String joined = String.join("\n", lines)
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max));
        return MessageUtils.format(player, joined);
    }
}
