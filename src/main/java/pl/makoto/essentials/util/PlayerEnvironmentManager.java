package pl.makoto.essentials.util;

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import pl.makoto.essentials.MKTEssentials;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player client-side time (/ptime) and weather (/pweather) overrides.
 * Vanilla re-syncs time every 20 ticks and weather on respawn/dimension change,
 * so overrides are re-sent every 20 ticks from ServerTickEvent.Post — that fires
 * after the vanilla sync, so our packet always wins.
 */
@EventBusSubscriber(modid = MKTEssentials.MODID)
public class PlayerEnvironmentManager {

    /** uuid → fixed time of day in ticks (0-23999) */
    private static final Map<UUID, Long> playerTime = new ConcurrentHashMap<>();
    /** uuid → true = raining, false = clear */
    private static final Map<UUID, Boolean> playerWeather = new ConcurrentHashMap<>();

    private static long tickCounter = 0;

    public static void setPlayerTime(ServerPlayer player, long timeOfDay) {
        playerTime.put(player.getUUID(), timeOfDay % 24000L);
        sendTime(player);
    }

    public static void resetPlayerTime(ServerPlayer player) {
        playerTime.remove(player.getUUID());
        // Restore real level time
        ServerLevel level = (ServerLevel) player.level();
        boolean cycle = level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
        player.connection.send(new ClientboundSetTimePacket(level.getGameTime(), level.getDayTime(), cycle));
    }

    public static boolean hasPlayerTime(UUID uuid) {
        return playerTime.containsKey(uuid);
    }

    public static void setPlayerWeather(ServerPlayer player, boolean raining) {
        playerWeather.put(player.getUUID(), raining);
        sendWeather(player);
    }

    public static void resetPlayerWeather(ServerPlayer player) {
        playerWeather.remove(player.getUUID());
        // Restore real level weather
        ServerLevel level = (ServerLevel) player.level();
        boolean raining = level.isRaining();
        player.connection.send(new ClientboundGameEventPacket(
                raining ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING, 0));
        if (raining) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level.getRainLevel(1.0F)));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level.getThunderLevel(1.0F)));
        }
    }

    public static boolean hasPlayerWeather(UUID uuid) {
        return playerWeather.containsKey(uuid);
    }

    public static void cleanupPlayer(UUID uuid) {
        playerTime.remove(uuid);
        playerWeather.remove(uuid);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (playerTime.isEmpty() && playerWeather.isEmpty()) return;

        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        for (UUID uuid : playerTime.keySet()) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) sendTime(player);
        }
        for (UUID uuid : playerWeather.keySet()) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) sendWeather(player);
        }
    }

    private static void sendTime(ServerPlayer player) {
        Long fixed = playerTime.get(player.getUUID());
        if (fixed == null) return;
        ServerLevel level = (ServerLevel) player.level();
        // daylightCycle=false freezes the client clock at the given day time
        player.connection.send(new ClientboundSetTimePacket(level.getGameTime(), fixed, false));
    }

    private static void sendWeather(ServerPlayer player) {
        Boolean raining = playerWeather.get(player.getUUID());
        if (raining == null) return;
        if (raining) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1.0F));
        } else {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0));
        }
    }
}
