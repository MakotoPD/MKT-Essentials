package pl.makoto.essentials.util;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.Config;
import pl.makoto.essentials.MKTEssentials;

import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = MKTEssentials.MODID)
public class BroadcastManager {
    private static final Random RANDOM = new Random();
    private static long lastBroadcastTicks = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int intervalSeconds = Config.BROADCAST_INTERVAL.get();
        if (intervalSeconds <= 0) return;

        long intervalTicks = intervalSeconds * 20L;
        lastBroadcastTicks++;

        if (lastBroadcastTicks >= intervalTicks) {
            lastBroadcastTicks = 0;
            broadcastRandom(event.getServer());
        }
    }

    private static void broadcastRandom(net.minecraft.server.MinecraftServer server) {
        List<? extends String> messages = Config.BROADCAST_MESSAGES.get();
        if (messages.isEmpty()) return;

        String msg = messages.get(RANDOM.nextInt(messages.size()));
        server.getPlayerList().broadcastSystemMessage(
            MessageUtils.formatBypass(Config.BROADCAST_PREFIX.get() + msg), false
        );
    }
}
