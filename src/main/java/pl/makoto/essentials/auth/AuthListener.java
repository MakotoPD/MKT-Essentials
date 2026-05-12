package pl.makoto.essentials.auth;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import pl.makoto.essentials.MKTEssentials;
import pl.makoto.essentials.config.Settings;

import net.minecraft.server.TickTask;

@EventBusSubscriber(modid = MKTEssentials.MODID)
public final class AuthListener {

    private AuthListener() {}

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Delay by 1 tick to ensure player is fully initialized
        player.getServer().tell(new TickTask(player.getServer().getTickCount() + 1, () -> {
            if (player.getServer().getPlayerList().getPlayer(player.getUUID()) == null) return;
            AuthManager.handleJoin(player);
        }));
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (Settings.getAuthMode() == AuthMode.DISABLED) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        AuthManager.handleQuit(player.getUUID());
    }
}
