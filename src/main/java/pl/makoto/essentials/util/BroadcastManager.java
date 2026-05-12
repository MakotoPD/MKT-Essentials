package pl.makoto.essentials.util;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.config.Settings;
import pl.makoto.essentials.MKTEssentials;

import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = MKTEssentials.MODID)
public class BroadcastManager {
    private static final Random RANDOM = new Random();
    private static long lastBroadcastTicks = 0;
    private static int currentIndex = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int intervalSeconds = Settings.getBroadcastInterval();
        if (intervalSeconds <= 0) return;

        long intervalTicks = intervalSeconds * 20L;
        lastBroadcastTicks++;

        if (lastBroadcastTicks >= intervalTicks) {
            lastBroadcastTicks = 0;
            broadcast(event.getServer());
        }
    }

    private static void broadcast(net.minecraft.server.MinecraftServer server) {
        List<? extends String> messages = Settings.getBroadcastMessages();
        if (messages.isEmpty()) return;

        String msg;
        if ("sequential".equalsIgnoreCase(Settings.getBroadcastOrder())) {
            if (currentIndex >= messages.size()) currentIndex = 0;
            msg = messages.get(currentIndex);
            currentIndex++;
        } else {
            msg = messages.get(RANDOM.nextInt(messages.size()));
        }

        server.getPlayerList().broadcastSystemMessage(
            MessageUtils.formatBypass(Settings.getBroadcastPrefix() + msg), false
        );
    }
}
