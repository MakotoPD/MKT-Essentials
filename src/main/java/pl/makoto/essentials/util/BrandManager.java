package pl.makoto.essentials.util;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;

import java.util.List;

/**
 * Overrides the server brand shown in the F3 debug screen, inspired by FlectonePulse's brand module.
 * We simply send our own {@link BrandPayload} in the play phase (on join, and on a timer when
 * several texts are configured to rotate), which replaces whatever the client received earlier.
 */
@EventBusSubscriber(modid = MKTEssentials.MODID)
public final class BrandManager {

    private static long tickCounter = 0;
    private static int index = 0;

    private BrandManager() {}

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Settings.isBrandEnabled()) return;
        if (event.getEntity() instanceof ServerPlayer player) send(player, current());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!Settings.isBrandEnabled()) return;
        List<String> texts = Settings.getBrandTexts();
        if (texts.size() <= 1) return; // static brand is sent on join; nothing to rotate

        int interval = Math.max(1, Settings.getBrandUpdateInterval());
        if (++tickCounter < interval) return;
        tickCounter = 0;
        index++;

        String text = current();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            send(player, text);
        }
    }

    private static String current() {
        List<String> texts = Settings.getBrandTexts();
        if (texts.isEmpty()) return "";
        return texts.get(Math.floorMod(index, texts.size()));
    }

    private static void send(ServerPlayer player, String text) {
        String brand = MessageUtils.legacyColors(null, text);
        player.connection.send(new ClientboundCustomPayloadPacket(new BrandPayload(brand)));
    }
}
