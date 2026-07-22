package pl.makoto.essentials.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent per-player boss bar, inspired by FlectonePulse bossbar. One {@link ServerBossEvent}
 * per player so the title can contain per-player placeholders. Refreshed on a timer.
 */
@EventBusSubscriber(modid = MKTEssentials.MODID)
public final class BossbarManager {

    private static final Map<UUID, ServerBossEvent> BARS = new ConcurrentHashMap<>();
    private static long tickCounter = 0;

    private BossbarManager() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!Settings.isBossbarEnabled()) {
            if (!BARS.isEmpty()) clearAll();
            return;
        }

        int interval = Math.max(1, Settings.getBossbarUpdateInterval());
        if (++tickCounter < interval) return;
        tickCounter = 0;

        BossEvent.BossBarColor color = parseColor(Settings.getBossbarColor());
        BossEvent.BossBarOverlay overlay = parseOverlay(Settings.getBossbarOverlay());

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            ServerBossEvent bar = BARS.computeIfAbsent(player.getUUID(), u -> {
                ServerBossEvent created = new ServerBossEvent(Component.empty(), color, overlay);
                created.setProgress(1.0f);
                created.addPlayer(player);
                return created;
            });
            bar.setColor(color);
            bar.setOverlay(overlay);
            bar.setName(MessageUtils.format(player, Settings.getBossbarText()));
        }
    }

    public static void remove(ServerPlayer player) {
        ServerBossEvent bar = BARS.remove(player.getUUID());
        if (bar != null) bar.removeAllPlayers();
    }

    private static void clearAll() {
        for (ServerBossEvent bar : BARS.values()) {
            bar.removeAllPlayers();
        }
        BARS.clear();
    }

    private static BossEvent.BossBarColor parseColor(String value) {
        try {
            return BossEvent.BossBarColor.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BossEvent.BossBarColor.PURPLE;
        }
    }

    private static BossEvent.BossBarOverlay parseOverlay(String value) {
        try {
            return BossEvent.BossBarOverlay.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BossEvent.BossBarOverlay.PROGRESS;
        }
    }
}
